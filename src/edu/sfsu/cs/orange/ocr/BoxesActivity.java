package edu.sfsu.cs.orange.ocr;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.sfsu.cs.orange.ocr.camera.BoxesManager;

import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class BoxesActivity extends Activity {
	private static final String TAG = BoxesActivity.class.getSimpleName();
	public static final String PREFS_NAME = "MyPrefsFile";
	private static final String PREFS_BOXES = "boxes";
	private BoxesfinderView boxesfinderView;
	private BoxesManager boxesManager;
	private ArrayList<BoxesManager> boxesManagers;
	private Context _context;
	// YOU CAN EDIT THIS TO WHATEVER YOU WANT
	private static final int SELECT_PICTURE = 1;

	@Override
	protected void onStop() {
		super.onStop();

		// We need an Editor object to make preference changes.
		// All objects are from android.context.Context
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		String bStr = "";
		for (BoxesManager box : boxesManagers) {
			bStr += box.getFramingRect().flattenToString() + ";";
			// menu.add(0, boxesManagers.indexOf(box), 0, box.name);
		}
		editor.putString(PREFS_BOXES, bStr);

		// Commit the edits!
		editor.commit();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		_context = getApplicationContext();
		setContentView(R.layout.activity_boxes);
		boxesfinderView = (BoxesfinderView) findViewById(R.id.boxesfinder_view);

		// Restore preferences
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		String boxesString = settings.getString(PREFS_BOXES, "");
		String[] b = boxesString.split(";");
		if (b != null) {
			boxesManagers = new ArrayList<BoxesManager>();
			int i = 0;
			for (String boxStr : b) {
				Rect r = Rect.unflattenFromString(boxStr);
				if (r != null) {
					i++;
					boxesManager = new BoxesManager(getApplication(), "BOX_"
							+ i, r,null);
					boxesManagers.add(boxesManager);
				}
			}
		}
		boxesfinderView.setBoxesManager(boxesManager);
		boxesfinderView.setBoxesList(boxesManagers);
		// Set listener to change the size of the viewfinder rectangle.
		boxesfinderView.setOnTouchListener(new View.OnTouchListener() {
			int lastX = -1;
			int lastY = -1;

			// @Override
			public boolean onTouch(View v, MotionEvent event) {
				Log.i(TAG, "Created onTouch event");
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN :
						lastX = -1;
						lastY = -1;
						return true;
					case MotionEvent.ACTION_MOVE :
						int currentX = (int) event.getX();
						int currentY = (int) event.getY();
						Log.i(TAG, "ACTION_MOVE currentX:" + currentX);
						Log.i(TAG, "ACTION_MOVE currentY:" + currentY);
						try {
							Rect rect = boxesManager.getFramingRect();

							final int BUFFER = 20;
							final int BIG_BUFFER = 30;
							if (lastX >= 0) {
								// Adjust the size of the viewfinder rectangle.
								// Check if the touch event occurs in the corner
								// areas first, because the regions overlap.
								if (((currentX >= rect.left - BIG_BUFFER && currentX <= rect.left
										+ BIG_BUFFER) || (lastX >= rect.left
										- BIG_BUFFER && lastX <= rect.left
										+ BIG_BUFFER))
										&& ((currentY <= rect.top + BIG_BUFFER && currentY >= rect.top
												- BIG_BUFFER) || (lastY <= rect.top
												+ BIG_BUFFER && lastY >= rect.top
												- BIG_BUFFER))) {
									// Top left corner: adjust both top and left
									// sides
									boxesManager.adjustFramingRect(
											(lastX - currentX),
											(lastY - currentY));
								} else if (((currentX >= rect.right
										- BIG_BUFFER && currentX <= rect.right
										+ BIG_BUFFER) || (lastX >= rect.right
										- BIG_BUFFER && lastX <= rect.right
										+ BIG_BUFFER))
										&& ((currentY <= rect.top + BIG_BUFFER && currentY >= rect.top
												- BIG_BUFFER) || (lastY <= rect.top
												+ BIG_BUFFER && lastY >= rect.top
												- BIG_BUFFER))) {
									// Top right corner: adjust both top and
									// right
									// sides
									boxesManager.adjustFramingRect(
											(currentX - lastX),
											(lastY - currentY));
								} else if (((currentX >= rect.left - BIG_BUFFER && currentX <= rect.left
										+ BIG_BUFFER) || (lastX >= rect.left
										- BIG_BUFFER && lastX <= rect.left
										+ BIG_BUFFER))
										&& ((currentY <= rect.bottom
												+ BIG_BUFFER && currentY >= rect.bottom
												- BIG_BUFFER) || (lastY <= rect.bottom
												+ BIG_BUFFER && lastY >= rect.bottom
												- BIG_BUFFER))) {
									// Bottom left corner: adjust both bottom
									// and
									// left sides
									boxesManager.adjustFramingRect(
											(lastX - currentX),
											(currentY - lastY));
								} else if (((currentX >= rect.right
										- BIG_BUFFER && currentX <= rect.right
										+ BIG_BUFFER) || (lastX >= rect.right
										- BIG_BUFFER && lastX <= rect.right
										+ BIG_BUFFER))
										&& ((currentY <= rect.bottom
												+ BIG_BUFFER && currentY >= rect.bottom
												- BIG_BUFFER) || (lastY <= rect.bottom
												+ BIG_BUFFER && lastY >= rect.bottom
												- BIG_BUFFER))) {
									// Bottom right corner: adjust both bottom
									// and
									// right sides
									boxesManager.adjustFramingRect(
											(currentX - lastX),
											(currentY - lastY));
								} else if (((currentX >= rect.left - BUFFER && currentX <= rect.left
										+ BUFFER) || (lastX >= rect.left
										- BUFFER && lastX <= rect.left + BUFFER))
										&& ((currentY <= rect.bottom && currentY >= rect.top) || (lastY <= rect.bottom && lastY >= rect.top))) {
									// Adjusting left side: event falls within
									// BUFFER pixels of left side, and between
									// top
									// and bottom side limits
									boxesManager.adjustFramingRect(
											(lastX - currentX), 0);
								} else if (((currentX >= rect.right - BUFFER && currentX <= rect.right
										+ BUFFER) || (lastX >= rect.right
										- BUFFER && lastX <= rect.right
										+ BUFFER))
										&& ((currentY <= rect.bottom && currentY >= rect.top) || (lastY <= rect.bottom && lastY >= rect.top))) {
									// Adjusting right side: event falls within
									// BUFFER pixels of right side, and between
									// top
									// and bottom side limits
									boxesManager.adjustFramingRect(
											(currentX - lastX), 0);
								} else if (((currentY <= rect.top + BUFFER && currentY >= rect.top
										- BUFFER) || (lastY <= rect.top
										+ BUFFER && lastY >= rect.top - BUFFER))
										&& ((currentX <= rect.right && currentX >= rect.left) || (lastX <= rect.right && lastX >= rect.left))) {
									// Adjusting top side: event falls within
									// BUFFER
									// pixels of top side, and between left and
									// right side limits
									boxesManager.adjustFramingRect(0,
											(lastY - currentY));
								} else if (((currentY <= rect.bottom + BUFFER && currentY >= rect.bottom
										- BUFFER) || (lastY <= rect.bottom
										+ BUFFER && lastY >= rect.bottom
										- BUFFER))
										&& ((currentX <= rect.right && currentX >= rect.left) || (lastX <= rect.right && lastX >= rect.left))) {
									// Adjusting bottom side: event falls within
									// BUFFER pixels of bottom side, and between
									// left and right side limits
									boxesManager.adjustFramingRect(0,
											(currentY - lastY));

								} else {
									boxesManager.moveFramingRect(
											(currentX - lastX),
											(currentY - lastY));
								}
							}
						} catch (NullPointerException e) {
							Log.e(TAG, "Framing rect not available", e);
						}
						v.invalidate();
						lastX = currentX;
						lastY = currentY;
						return true;
					case MotionEvent.ACTION_UP :
						lastX = -1;
						lastY = -1;
						return true;
				}
				return false;
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode == RESULT_OK) {
			Log.i(TAG, "Result is OK");
			if (requestCode == SELECT_PICTURE) {
				try {
					Uri selectedImageUri = data.getData();
					Bitmap bitmap = MediaStore.Images.Media.getBitmap(
							_context.getContentResolver(), selectedImageUri);
					ImageView bg = (ImageView) findViewById(R.id.boxes_bg_view);

					Drawable drawable = new BitmapDrawable(getResources(),
							bitmap);
					// bg.setBackgroundDrawable(drawable);
					bg.setBackgroundDrawable(drawable);
					bg.setAdjustViewBounds(true);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}

	}

	/**
	 * Floating context menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_boxes_float, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		menu.add(0, R.id.new_box, 0, "Uus box");
		menu.add(0, R.id.choose_picture, 0, "Vali pilt");
		int i = 0;
		if (boxesManagers != null) {
			for (BoxesManager box : boxesManagers) {
				menu.add(0, boxesManagers.indexOf(box), 0, box.name);
				i++;
			}
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.new_box :
				newBoxItem();
				return true;
			case R.id.choose_picture :
				Log.i(TAG, "Choosing picture1");
				Intent intent = new Intent();
				Log.i(TAG, "Choosing picture2");
				intent.setType("image/*");
				Log.i(TAG, "Choosing picture3");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				Log.i(TAG, "Choosing picture4");
				startActivityForResult(
						Intent.createChooser(intent, "Select Picture"),
						SELECT_PICTURE);
				Log.i(TAG, "Choosing picture5");
				return true;

			default :
				selectBoxItem(item.getItemId());
				return true;
		}
	}

	private void selectBoxItem(int id) {
		boxesManager = boxesManagers.get(id);
		boxesfinderView.setBoxesManager(boxesManager);
		boxesfinderView.setBoxesList(boxesManagers);
		boxesfinderView.drawViewfinder();
	}

	private void newBoxItem() {
		boxesManager = new BoxesManager(getApplication(), "BOX_"
				+ boxesManagers.size(), null,null);
		boxesManagers.add(boxesManager);
		boxesfinderView.setBoxesManager(boxesManager);
		boxesfinderView.setBoxesList(boxesManagers);
		boxesfinderView.drawViewfinder();
	}
}
