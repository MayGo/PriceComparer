/*
 * Copyright (C) 2008 ZXing authors
 * Copyright 2011 Robert Theis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.sfsu.cs.orange.ocr.camera;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import edu.sfsu.cs.orange.ocr.PlanarYUVLuminanceSource;
import edu.sfsu.cs.orange.ocr.PreferencesActivity;

import java.io.IOException;

/**
 * This object wraps the Camera service object and expects to be the only one
 * talking to it. The implementation encapsulates the steps needed to take
 * preview-sized images, which are used for both preview and decoding.
 * 
 * The code for this class was adapted from the ZXing project:
 * http://code.google.com/p/zxing
 */
public final class BoxesManager {

	private static final String TAG = BoxesManager.class.getSimpleName();

	private static final int MIN_FRAME_WIDTH = 50; // originally 240
	private static final int MIN_FRAME_HEIGHT = 20; // originally 240
	private static final int MAX_FRAME_WIDTH = 800; // originally 480
	private static final int MAX_FRAME_HEIGHT = 600; // originally 360

	private final Context context;
	private Rect framingRect;
	private Rect framingRectInPreview;
	private boolean initialized;
	private boolean previewing;
	private int requestedFramingRectWidth;
	private int requestedFramingRectHeight;
	private Point screenResolution;
	public String name = "BOX";

	private final CameraConfigurationManager configManager;

	public BoxesManager(Context context, String name, Rect r, CameraManager  configManager) {
		this.context = context;
		this.configManager = configManager.getConfigManager();
		this.name = name;

		initParameters();
		initialize();
		this.framingRect = r;
	}

	public synchronized void initialize() {

		if (!initialized) {
			initialized = true;
			if (requestedFramingRectWidth > 0 && requestedFramingRectHeight > 0) {
				adjustFramingRect(requestedFramingRectWidth,
						requestedFramingRectHeight);
				requestedFramingRectWidth = 0;
				requestedFramingRectHeight = 0;
			}
		}

	}

	/**
	 * Closes the camera driver if still in use.
	 */
	public synchronized void close() {
		// Make sure to clear these each time we close the camera, so that any
		// scanning rect
		// requested by intent is forgotten.
		framingRect = null;
		framingRectInPreview = null;
	}

	void initParameters() {
		WindowManager manager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = manager.getDefaultDisplay();
		int width = display.getWidth();
		int height = display.getHeight();
		// We're landscape-only, and have apparently seen issues with display
		// thinking it's portrait
		// when waking from sleep. If it's not landscape, assume it's mistaken
		// and reverse them:
		if (width < height) {
			Log.i(TAG,
					"Display reports portrait orientation; assuming this is incorrect");
			int temp = width;
			width = height;
			height = temp;
		}
		screenResolution = new Point(width, height);
		Log.i(TAG, "Screen resolution: " + screenResolution);
	}

	/**
	 * Calculates the framing rect which the UI should draw to show the user
	 * where to place the barcode. This target helps with alignment as well as
	 * forces the user to hold the device far enough away to ensure the image
	 * will be in focus.
	 * 
	 * @return The rectangle to draw on screen in window coordinates.
	 */
	public synchronized Rect getFramingRect() {
		if (framingRect == null) {
			if (screenResolution == null) {
				// Called early, before init even finished
				return null;
			}
			int width = screenResolution.x * 3 / 5;
			if (width < MIN_FRAME_WIDTH) {
				width = MIN_FRAME_WIDTH;
			} else if (width > MAX_FRAME_WIDTH) {
				width = MAX_FRAME_WIDTH;
			}
			int height = screenResolution.y * 1 / 5;
			if (height < MIN_FRAME_HEIGHT) {
				height = MIN_FRAME_HEIGHT;
			} else if (height > MAX_FRAME_HEIGHT) {
				height = MAX_FRAME_HEIGHT;
			}
			int leftOffset = (screenResolution.x - width) / 2;
			int topOffset = (screenResolution.y - height) / 2;
			framingRect = new Rect(leftOffset, topOffset, leftOffset + width,
					topOffset + height);
		}
		return framingRect;
	}

	/**
	 * Changes the size of the framing rect.
	 * 
	 * @param deltaWidth
	 *            Number of pixels to adjust the width
	 * @param deltaHeight
	 *            Number of pixels to adjust the height
	 */
	public synchronized void adjustFramingRect(int deltaWidth, int deltaHeight) {
		if (initialized) {
			Rect tempRect = framingRect;
			Log.i(TAG,
					"adjustFramingRect framingRect before: "
							+ framingRect.flattenToString());
			Log.i(TAG, "adjustFramingRect deltaWidth: " + deltaWidth
					+ ", deltaHeight:" + deltaHeight);
			// Set maximum and minimum sizes
			if ((tempRect.left - deltaWidth) <= 0
					&& (tempRect.right + deltaWidth) >= screenResolution.x) {
				deltaWidth = 0;
			}
			if ((tempRect.top - deltaHeight) <= 0
					&& (tempRect.bottom + deltaHeight) >= screenResolution.y) {
				deltaHeight = 0;
			}
			//
			// int newWidth = tempRect.width() + deltaWidth;
			// int newHeight = tempRect.height() + deltaHeight;
			// int leftOffset = (screenResolution.x - newWidth) / 2;
			// int topOffset = (screenResolution.y - newHeight) / 2;
			// framingRect = new Rect(leftOffset, topOffset,
			// leftOffset + newWidth, topOffset + newHeight);

			framingRect = new Rect(tempRect.left - deltaWidth, tempRect.top
					- deltaHeight, tempRect.right + deltaWidth, tempRect.bottom
					+ deltaHeight);
			framingRectInPreview = null;
			Log.i(TAG,
					"adjustFramingRect framingRect after: "
							+ framingRect.flattenToString());
		} else {
			// requestedFramingRectWidth = deltaWidth;
			// requestedFramingRectHeight = deltaHeight;
		}
	}

	public synchronized void moveFramingRect(int newX, int newY) {
		if (initialized) {
			Log.i(TAG,
					"moveFramingRect framingRect before: "
							+ framingRect.flattenToString());
			// // Set maximum and minimum sizes
			if ((framingRect.right + newX >= screenResolution.x)
					|| (framingRect.left + newX <= 0)) {
				newX = 0;
			}
			if ((framingRect.bottom + newY >= screenResolution.y)
					|| (framingRect.top + newY <= 0)) {
				newY = 0;
			}

			Rect tempRect = framingRect;

			framingRect = new Rect(tempRect.left + newX, tempRect.top + newY,
					tempRect.right + newX, tempRect.bottom + newY);
			framingRectInPreview = null;
			Log.i(TAG,
					"moveFramingRect framingRect after: "
							+ framingRect.flattenToString());
		} else {
			// requestedFramingRectWidth = deltaWidth;
			// requestedFramingRectHeight = deltaHeight;
		}
	}
	/**
	 * Like {@link #getFramingRect} but coordinates are in terms of the preview
	 * frame, not UI / screen.
	 */
	public synchronized Rect getFramingRectInPreview() {
		Log.i(TAG, "getFramingRectInPreview");
		if (framingRectInPreview == null) {
			Rect rect = new Rect(getFramingRect());
			Log.i(TAG, "getFramingRectInPreview rect:"+rect.flattenToString());
			Point cameraResolution = configManager.getCameraResolution();
			Point screenResolution = configManager.getScreenResolution();
			if (cameraResolution == null || screenResolution == null) {
				Log.i(TAG, "cameraResolution null:");
				// Called early, before init even finished
				return null;
			}
			rect.left = rect.left * cameraResolution.x / screenResolution.x;
			rect.right = rect.right * cameraResolution.x / screenResolution.x;
			rect.top = rect.top * cameraResolution.y / screenResolution.y;
			rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y;
			framingRectInPreview = rect;
		}
		return framingRectInPreview;
	}

	/**
	 * A factory method to build the appropriate LuminanceSource object based on
	 * the format of the preview buffers, as described by Camera.Parameters.
	 * 
	 * @param data
	 *            A preview frame.
	 * @param width
	 *            The width of the image.
	 * @param height
	 *            The height of the image.
	 * @return A PlanarYUVLuminanceSource instance.
	 */
	public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data,
			int width, int height) {
		Log.i(TAG,"buildLuminanceSource");
		Rect rect = getFramingRectInPreview();
		Log.i(TAG,"rect:"+rect.flattenToString());
		if (rect == null) {
			return null;
		}
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		boolean reverseImage = prefs.getBoolean(
				PreferencesActivity.KEY_REVERSE_IMAGE, false);
		// Go ahead and assume it's YUV rather than die.
		return new PlanarYUVLuminanceSource(data, width, height, rect.left,
				rect.top, rect.width(), rect.height(), reverseImage);
	}
}
