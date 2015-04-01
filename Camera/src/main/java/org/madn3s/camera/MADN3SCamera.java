package org.madn3s.camera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by inaki on 3/4/14.
 */
public class MADN3SCamera extends Application {
    public static final String TAG = "MADN3SCamera";
    public static final int DISCOVERABLE_TIME = 300000;
    public static Context appContext;
	public static boolean isOpenCvLoaded = false;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    
    public static final String defaultJSONObjectString = "{}";
	public static final String defaultJSONArrayString = "[]";

    public static String projectName;
    public static String position;
    public static int iteration;
    private static File appDirectory;
    
    public static CameraPreview mPreview;
    
    public static AtomicBoolean isPictureTaken; 
    public static AtomicBoolean isRunning; 
    
    private Handler mBluetoothHandler;
    private Handler.Callback mBluetoothHandlerCallback = null;
	private static Camera mCamera;
    public static boolean hasInvokedCalibration = false;
    public static boolean hasReceivedCalibration = false;
    
    public static SharedPreferences sharedPreferences;
	public static Editor sharedPreferencesEditor;

    @SuppressLint("HandlerLeak")
	@Override
    public void onCreate() {
        super.onCreate();
        appContext = super.getBaseContext();
        appDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        		, appContext.getString(R.string.app_name));
        setSharedPreferences();
        
        mBluetoothHandler = new Handler() {
    	    public void handleMessage(android.os.Message msg) {
    	        if (mBluetoothHandlerCallback != null) {
    	            mBluetoothHandlerCallback.handleMessage(msg);
    	        }
    	    };
    	};
    	
    	Consts.init();
    }
    
    public static File getAppDirectory(){
    	return appDirectory;
    }

    public static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }
    
    public static Uri getOutputMediaFileUri(int type, String projectName, String position, int iteration){
        return Uri.fromFile(getOutputMediaFile(type, projectName, position, iteration));
    }

    @SuppressLint("SimpleDateFormat")
	public static File getOutputMediaFile(int type){
    	return getOutputMediaFile(type, projectName, position, iteration);
    }

    @SuppressLint("SimpleDateFormat")
	public static File getOutputMediaFile(int type, String projectName, String position, int iteration){
        File mediaStorageDir = new File(getAppDirectory(), projectName);

        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        if(position == null){
        	position = "";
        }
        
        String filename;
        File mediaFile;
        
        if (type == MEDIA_TYPE_IMAGE){
            filename = "IMG_" + position + "_" + iteration + Consts.IMAGE_EXT;
        } else {
            return null;
        }
        
        mediaFile = new File(mediaStorageDir.getPath(), filename);

        return mediaFile;
    }
    
    public static String saveBitmapAsJpeg(Bitmap bitmap, String position){
    	return saveBitmapAsJpeg(bitmap, position, iteration);
    }
    
    public static String saveBitmapAsJpeg(Bitmap bitmap, String position, int iteration){
    	FileOutputStream out;
        try {
            final File imgFile = getOutputMediaFile(MEDIA_TYPE_IMAGE, projectName, position, iteration);

            out = new FileOutputStream(imgFile.getAbsoluteFile());
            bitmap.compress(Consts.BITMAP_COMPRESS_FORMAT, Consts.COMPRESSION_QUALITY, out);
            
            new Handler(Looper.getMainLooper()).post(new Runnable() {             
                @Override
                public void run() { 
                	Toast.makeText(appContext, imgFile.getName(), Toast.LENGTH_SHORT).show();
                }
              });
            
            return imgFile.getPath();
            
        } catch (FileNotFoundException e) {
            Log.e(position, "saveBitmapAsJpeg: No se pudo guardar el Bitmap", e);
            return null;
        }
    }
    
    public static String getMD5EncryptedString(byte[] bytes){
        MessageDigest mdEnc = null;
        try {
            mdEnc = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Exception while encrypting to md5");
            e.printStackTrace();
        }
        
        mdEnc.update(bytes, 0, bytes.length);
        String md5 = new BigInteger(1, mdEnc.digest()).toString(16);
        while ( md5.length() < 32 ) {
            md5 = "0"+md5;
        }
        return md5;
    }
    
    public static Camera getCameraInstance(){
        if(mCamera == null){
	        try {
	            mCamera = Camera.open();
	            mCamera.setDisplayOrientation(90);
	            mCamera.getParameters().setFlashMode(Camera.Parameters.FLASH_MODE_ON);
	        }
	        catch (Exception e){
	            e.printStackTrace();
	            mCamera = null;
	        }
        }
        return mCamera;
    }

	public Handler getBluetoothHandler() {
		return mBluetoothHandler;
	}
	
	public void setBluetoothHandlerCallBack(Handler.Callback callback) {
	    this.mBluetoothHandlerCallback = callback;
	}
	
	/**
	 * Sets SharedPreferences and SharedPreferences Editor for later use with methods defined further
	 */
	private void setSharedPreferences() {
		sharedPreferences = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
		sharedPreferencesEditor = MADN3SCamera.sharedPreferences.edit();
	}
	
	public static void clearSharedPreferences() {
		sharedPreferencesEditor.clear().apply();
	}
	
	public static void removeKeyFromSharedPreferences(String key) {
		sharedPreferencesEditor.remove(key).apply();
	}
	
	public static void sharedPrefsPutJSONArray(String key, JSONArray value){
		sharedPreferencesEditor.putString(key, value.toString()).apply();
	}
	
	public static JSONArray sharedPrefsGetJSONArray(String key){
		String jsonString = sharedPreferences.getString(key, defaultJSONArrayString);
		try {
			return new JSONArray(jsonString);
		} catch (Exception e) {
			e.printStackTrace();
			return new JSONArray();
		}
	}
	
	public static void sharedPrefsPutJSONObject(String key, JSONObject value){
		sharedPreferencesEditor.putString(key, value.toString()).apply();
	}
	
	public static JSONObject sharedPrefsGetJSONObject(String key){
		String jsonString = sharedPreferences.getString(key, defaultJSONObjectString);
		try {
			return new JSONObject(jsonString);
		} catch (Exception e) {
			e.printStackTrace();
			return new JSONObject();
		}
	}
	
	public static void sharedPrefsPutString(String key, String value){
		sharedPreferencesEditor.putString(key, value).apply();
	}
	
	public static String sharedPrefsGetString(String key){
		return sharedPreferences.getString(key, "");
	}
	
	public static void sharedPrefsPutBoolean(String key, Boolean value){
		sharedPreferencesEditor.putBoolean(key, value).apply();
	}
	
	public static Boolean sharedPrefsGetBoolean(String key){
		return sharedPreferences.getBoolean(key, false);
	}
	
	public static void sharedPrefsPutInt(String key, int value){
		sharedPreferencesEditor.putInt(key, value).apply();
	}
	
	public static int sharedPrefsGetInt(String key){
		return sharedPreferences.getInt(key, 0);
	}
	
	public static void sharedPrefsPutLong(String key, Long value){
		sharedPreferencesEditor.putLong(key, value).apply();
	}
	
	public static Long sharedPrefsGetLong(String key){
		return sharedPreferences.getLong(key, (long) 0);
	}
	
	public static void sharedPrefsPutFloat(String key, Float value){
		sharedPreferencesEditor.putFloat(key, value).apply();
	}
	
	public static Float sharedPrefsGetFloat(String key){
		return sharedPreferences.getFloat(key, 0);
	}
}
