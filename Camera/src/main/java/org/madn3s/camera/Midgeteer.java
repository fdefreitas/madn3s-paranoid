package org.madn3s.camera;

import static org.madn3s.camera.MADN3SCamera.position;
import static org.madn3s.camera.MADN3SCamera.projectName;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class Midgeteer extends Thread {
	private JSONObject params;
	private Context actualContext;
	private JSONArray result;
	private Camera mCamera;
	
	public JSONObject getParams() {
		return params;
	}


	public void setParams(JSONObject params) {
		this.params = params;
	}

	public Midgeteer(JSONObject params, Context actualContext, JSONArray result, Camera mCamera) {
		super();
		this.params = params;
		this.actualContext = actualContext;
		this.result = result;
		this.mCamera = mCamera;
	}

	private final Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {

        @SuppressLint("SimpleDateFormat")
		@Override
        public void onPictureTaken(byte[] data, Camera camera) {
        	MidgetOfSeville figaro = new MidgetOfSeville();
        	int orientation;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 6;
            options.inDither = false; // Disable Dithering mode
            options.inPurgeable = true; // Tell to gc that whether it needs free
            options.inInputShareable = true; // Which kind of reference will be
            options.inTempStorage = new byte[32 * 1024];
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bMap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
            if(bMap.getHeight() < bMap.getWidth()){
                orientation = 90;
            } else {
                orientation = 0;
            }
            Bitmap bMapRotate;
            if (orientation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(orientation);
                bMapRotate = Bitmap.createBitmap(bMap, 0, 0, bMap.getWidth(), bMap.getHeight(), matrix, true);
            } else {
                bMapRotate = Bitmap.createScaledBitmap(bMap, bMap.getWidth(), bMap.getHeight(), true);
            }
            FileOutputStream out;
            try {
                File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) +"/MADN3SCamera", projectName);
                if (!mediaStorageDir.exists()){
                    if (!mediaStorageDir.mkdirs()){
                        Log.d("ERROR", "failed to create directory");
                        return;
                    }
                }
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String filePath = mediaStorageDir.getPath() + File.separator + position + "_" + timeStamp + ".jpg"; 
                out = new FileOutputStream(filePath);
                bMapRotate.compress(Bitmap.CompressFormat.JPEG, 90, out);
                Toast.makeText(actualContext, "Imagen almacenada en " + filePath, Toast.LENGTH_SHORT).show();
                JSONObject resultJsonObject = figaro.shapeUp(filePath, params);
                result = resultJsonObject.getJSONArray(Consts.KEY_POINTS);
                out = new FileOutputStream(String.format(mediaStorageDir.getPath() + File.separator + position + "grabCut" + "_" + timeStamp + ".jpg"));
                bMapRotate.compress(Bitmap.CompressFormat.JPEG, 90, out);
                if (bMapRotate != null) {
                    bMapRotate.recycle();
                    bMapRotate = null;
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (JSONException e) {
				e.printStackTrace();
			}
	        camera.startPreview();
        }
        
        
        
    };
	
	
    @Override
    public void run() {
    	if(mCamera != null){
    		mCamera.takePicture(null, null, mPictureCallback);
    	} else {
    		Toast.makeText(actualContext, "mCamera == null", Toast.LENGTH_SHORT).show();
    	}
    }
}
