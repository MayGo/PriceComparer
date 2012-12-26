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
package edu.sfsu.cs.orange.ocr;

import java.util.ArrayList;
import java.util.List;

import edu.sfsu.cs.orange.ocr.R;
import edu.sfsu.cs.orange.ocr.camera.BoxesManager;
import edu.sfsu.cs.orange.ocr.camera.CameraManager;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder
 * rectangle and partial transparency outside it, as well as the result text.
 * 
 * The code for this class was adapted from the ZXing project:
 * http://code.google.com/p/zxing
 */
public final class BoxesfinderView extends View {
	private static final String TAG = BoxesfinderView.class.getSimpleName();

	// private static final long ANIMATION_DELAY = 80L;

	/**
	 * Flag to draw boxes representing the results from
	 * TessBaseAPI::GetRegions().
	 */
	static final boolean DRAW_REGION_BOXES = false;

	/**
	 * Flag to draw boxes representing the results from
	 * TessBaseAPI::GetTextlines().
	 */
	static final boolean DRAW_TEXTLINE_BOXES = true;

	/**
	 * Flag to draw boxes representing the results from
	 * TessBaseAPI::GetStrips().
	 */
	static final boolean DRAW_STRIP_BOXES = false;

	/**
	 * Flag to draw boxes representing the results from TessBaseAPI::GetWords().
	 */
	static final boolean DRAW_WORD_BOXES = true;

	/**
	 * Flag to draw word text with a background varying from transparent to
	 * opaque.
	 */
	static final boolean DRAW_TRANSPARENT_WORD_BACKGROUNDS = false;

	/**
	 * Flag to draw boxes representing the results from
	 * TessBaseAPI::GetCharacters().
	 */
	static final boolean DRAW_CHARACTER_BOXES = false;

	/**
	 * Flag to draw the text of words within their respective boxes from
	 * TessBaseAPI::GetWords().
	 */
	static final boolean DRAW_WORD_TEXT = false;

	/**
	 * Flag to draw each character in its respective box from
	 * TessBaseAPI::GetCharacters().
	 */
	static final boolean DRAW_CHARACTER_TEXT = false;

	private BoxesManager boxesManager;
	private ArrayList<BoxesManager> boxesList;
	private final Paint paint;
	private final int maskColor;
	private final int frameColor;
	private final int cornerColor;
	// Rect bounds;
	private Rect previewFrame;
	private Rect rect;

	// This constructor is used when the class is built from an XML resource.
	public BoxesfinderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		boxesList = new ArrayList<BoxesManager>();
		// Initialize these once for performance rather than calling them every
		// time in onDraw().
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		Resources resources = getResources();
		maskColor = resources.getColor(R.color.viewfinder_mask);
		frameColor = resources.getColor(R.color.viewfinder_frame);
		cornerColor = resources.getColor(R.color.viewfinder_corners);

		// bounds = new Rect();
		previewFrame = new Rect();
		rect = new Rect();
	}

	public void setBoxesManager(BoxesManager boxesManager) {
		this.boxesManager = boxesManager;
	}

	public void setBoxesList(ArrayList<BoxesManager> boxesList) {
		this.boxesList = boxesList;
	}

	@SuppressWarnings("unused")
	@Override
	public void onDraw(Canvas canvas) {


		int width = canvas.getWidth();
		int height = canvas.getHeight();
		for (BoxesManager box : boxesList) {
			Rect frame = box.getFramingRect();
			if (frame == null) {
				return;
			}

			// Draw a two pixel solid border inside the framing rect
			paint.setAlpha(0);
			paint.setStyle(Style.FILL);
			paint.setColor(frameColor);
			canvas.drawRect(frame.left, frame.top, frame.right + 1,
					frame.top + 2, paint);
			canvas.drawRect(frame.left, frame.top + 2, frame.left + 2,
					frame.bottom - 1, paint);
			canvas.drawRect(frame.right - 1, frame.top, frame.right + 1,
					frame.bottom - 1, paint);
			canvas.drawRect(frame.left, frame.bottom - 1, frame.right + 1,
					frame.bottom + 1, paint);
		}
		Rect frame = boxesManager.getFramingRect();
		// Draw the exterior (i.e. outside the framing rect) darkened
		paint.setColor(maskColor);
		canvas.drawRect(0, 0, width, frame.top, paint);
		canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
		canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1,
				paint);
		canvas.drawRect(0, frame.bottom + 1, width, height, paint);
		// Draw the framing rect corner UI elements
		paint.setColor(cornerColor);
		canvas.drawRect(frame.left - 15, frame.top - 15, frame.left + 15,
				frame.top, paint);
		canvas.drawRect(frame.left - 15, frame.top, frame.left, frame.top + 15,
				paint);
		canvas.drawRect(frame.right - 15, frame.top - 15, frame.right + 15,
				frame.top, paint);
		canvas.drawRect(frame.right, frame.top - 15, frame.right + 15,
				frame.top + 15, paint);
		canvas.drawRect(frame.left - 15, frame.bottom, frame.left + 15,
				frame.bottom + 15, paint);
		canvas.drawRect(frame.left - 15, frame.bottom - 15, frame.left,
				frame.bottom, paint);
		canvas.drawRect(frame.right - 15, frame.bottom, frame.right + 15,
				frame.bottom + 15, paint);
		canvas.drawRect(frame.right, frame.bottom - 15, frame.right + 15,
				frame.bottom + 15, paint);

		// Request another update at the animation interval, but don't
		// repaint
		// the entire viewfinder mask.
		// postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top,
		// frame.right, frame.bottom);

	}

	public void drawViewfinder() {
		invalidate();
	}
}
