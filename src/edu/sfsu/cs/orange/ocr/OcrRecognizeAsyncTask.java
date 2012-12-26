/*
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

import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.tesseract.android.TessBaseAPI;

import edu.sfsu.cs.orange.ocr.camera.BoxesManager;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Class to send OCR requests to the OCR engine in a separate thread, send a
 * success/failure message, and dismiss the indeterminate progress dialog box.
 * Used for non-continuous mode OCR only.
 */
final class OcrRecognizeAsyncTask extends AsyncTask<Void, Void, Boolean> {
	private static final String TAG = OcrRecognizeAsyncTask.class.getSimpleName();
	// private static final boolean PERFORM_FISHER_THRESHOLDING = false;
	// private static final boolean PERFORM_OTSU_THRESHOLDING = false;
	// private static final boolean PERFORM_SOBEL_THRESHOLDING = false;

	private CaptureActivity activity;
	private TessBaseAPI baseApi;
	private byte[] data;
	private int width;
	private int height;
	private ArrayList<OcrResult> ocrResults;
	private long timeRequired;

	OcrRecognizeAsyncTask(CaptureActivity activity, TessBaseAPI baseApi,
			byte[] data, int width, int height) {
		this.activity = activity;
		this.baseApi = baseApi;
		this.data = data;
		this.width = width;
		this.height = height;
	}

	@Override
	protected Boolean doInBackground(Void... arg0) {
		Log.i(TAG, "doInBackground");
		long start = System.currentTimeMillis();
		ArrayList<BoxesManager> boxesManagers = activity.getBoxesManagers();
		ocrResults = new ArrayList<OcrResult>();
		for (BoxesManager box : boxesManagers) {
			OcrResult ocrResult = new OcrResult();
			Log.i(TAG, "box:"+box.name);
			Bitmap bitmap = box.buildLuminanceSource(data, width, height)
					.renderCroppedGreyscaleBitmap();
			Log.i(TAG, "bitmap:"+bitmap.toString());
			// Bitmap bitmap =
			// activity.getCameraManager().buildLuminanceSource(data, width,
			// height).renderCroppedGreyscaleBitmap();

			String textResult;

			// if (PERFORM_FISHER_THRESHOLDING) {
			// Pix thresholdedImage =
			// Thresholder.fisherAdaptiveThreshold(ReadFile.readBitmap(bitmap),
			// 48, 48, 0.1F, 2.5F);
			// Log.e("OcrRecognizeAsyncTask",
			// "thresholding completed. converting to bmp. size:" +
			// bitmap.getWidth() + "x" + bitmap.getHeight());
			// bitmap = WriteFile.writeBitmap(thresholdedImage);
			// }
			// if (PERFORM_OTSU_THRESHOLDING) {
			// Pix thresholdedImage =
			// Binarize.otsuAdaptiveThreshold(ReadFile.readBitmap(bitmap), 48,
			// 48, 9, 9, 0.1F);
			// Log.e("OcrRecognizeAsyncTask",
			// "thresholding completed. converting to bmp. size:" +
			// bitmap.getWidth() + "x" + bitmap.getHeight());
			// bitmap = WriteFile.writeBitmap(thresholdedImage);
			// }
			// if (PERFORM_SOBEL_THRESHOLDING) {
			// Pix thresholdedImage =
			// Thresholder.sobelEdgeThreshold(ReadFile.readBitmap(bitmap), 64);
			// Log.e("OcrRecognizeAsyncTask",
			// "thresholding completed. converting to bmp. size:" +
			// bitmap.getWidth() + "x" + bitmap.getHeight());
			// bitmap = WriteFile.writeBitmap(thresholdedImage);
			// }

			try {
				Log.i(TAG,"setImage");
				baseApi.setImage(ReadFile.readBitmap(bitmap));
				Log.i(TAG,"getUTF8Text");
				textResult = baseApi.getUTF8Text();
				Log.i(TAG,"textResult:"+textResult);
				timeRequired = System.currentTimeMillis() - start;

				// Check for failure to recognize text
//				if (textResult == null || textResult.equals("")) {
//					return false;
//				}
				
				ocrResult.setWordConfidences(baseApi.wordConfidences());
				ocrResult.setMeanConfidence(baseApi.meanConfidence());
				ocrResult.setRegionBoundingBoxes(baseApi.getRegions()
						.getBoxRects());
				ocrResult.setTextlineBoundingBoxes(baseApi.getTextlines()
						.getBoxRects());
				ocrResult
						.setWordBoundingBoxes(baseApi.getWords().getBoxRects());
				ocrResult.setStripBoundingBoxes(baseApi.getStrips()
						.getBoxRects());
				ocrResult.setCharacterBoundingBoxes(baseApi.getCharacters()
						.getBoxRects());
			} catch (RuntimeException e) {
				Log.e("OcrRecognizeAsyncTask",
						"Caught RuntimeException in request to Tesseract. Setting state to CONTINUOUS_STOPPED.");
				e.printStackTrace();
				try {
					baseApi.clear();
					activity.stopHandler();
				} catch (NullPointerException e1) {
					// Continue
				}
				return false;
			}
			timeRequired = System.currentTimeMillis() - start;
			ocrResult.setBitmap(bitmap);
			ocrResult.setText(textResult);
			ocrResult.setRecognitionTimeRequired(timeRequired);
			ocrResults.add(ocrResult);
		}
		return true;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);

		Handler handler = activity.getHandler();
		if (handler != null) {
			// Send results for single-shot mode recognition.
			if (result) {
				Message message = Message.obtain(handler,
						R.id.ocr_decode_succeeded, ocrResults);
				message.sendToTarget();
			} else {
				Message message = Message.obtain(handler,
						R.id.ocr_decode_failed, ocrResults);
				message.sendToTarget();
			}
			activity.getProgressDialog().dismiss();
		}
		if (baseApi != null) {
			baseApi.clear();
		}
	}
}
