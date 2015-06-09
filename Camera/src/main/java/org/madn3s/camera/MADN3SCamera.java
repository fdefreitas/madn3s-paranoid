package org.madn3s.camera;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

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

import static org.madn3s.camera.Consts.*;

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
    public static final int MEDIA_TYPE_JSON = 3;
    public static final int MEDIA_TYPE_VTU = 4;

    public static final String defaultJSONObjectString = "{}";
	public static final String defaultJSONArrayString = "[]";

    public static String position;
    private static File appDirectory;

    public static AtomicBoolean isPictureTaken;
    public static AtomicBoolean isRunning;

    private Handler mBluetoothHandler;
    private Handler.Callback mBluetoothHandlerCallback = null;
	private static Camera mCamera;

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

    public static String saveJsonToExternal(String output, String fileName) throws JSONException {
        try {
            String projectName = sharedPrefsGetString(KEY_PROJECT_NAME);
            File calibrationFile = getOutputMediaFile(MEDIA_TYPE_JSON, projectName, fileName);
            Log.i(TAG, "saveJsonToExternal. filepath: " + calibrationFile.getAbsolutePath());
            FileOutputStream fos = new FileOutputStream(calibrationFile);
            fos.write(output.getBytes());
            fos.flush();
            fos.close();
            return calibrationFile.getAbsolutePath();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "saveJsonToExternal. " + fileName + " FileNotFoundException", e);
        } catch (IOException e) {
            Log.e(TAG, "saveJsonToExternal. " + fileName + " IOException", e);
        }

        return null;
    }

    public static JSONObject getInputJson(String filename){
        return getInputJson(sharedPrefsGetString(KEY_PROJECT_NAME), filename);
    }

    public static JSONObject getInputJson(String projectName, String filename){
        String inputString;
        File inputFile = getInputMediaFile(projectName, filename);

        try{
            if(inputFile.exists()){
                BufferedReader inputReader = new BufferedReader(new FileReader(inputFile));
                inputString = inputReader.readLine();
                return new JSONObject(inputString);
            } else {
                Log.e(TAG, filename + " doesn't exists");
            }
        } catch (IOException e){
            Log.e(TAG, "Error Reading config JSONObject");
            e.printStackTrace();
        } catch (JSONException e){
            Log.e(TAG, "Error Parsing config JSONObject");
            e.printStackTrace();
        }

        return new JSONObject();
    }

    public static File getInputMediaFile(String projectName, String filename){
        File projectDirectory = new File(getAppDirectory(), projectName);
        return new File(projectDirectory.getPath(), filename);
    }

    public static File getInputMediaFile(String filename){
        return getInputMediaFile(sharedPrefsGetString(KEY_PROJECT_NAME), filename);
    }

    @SuppressLint("SimpleDateFormat")
	public static File getOutputMediaFile(int type){
    	return getOutputMediaFile(type, sharedPrefsGetString(KEY_PROJECT_NAME), position, sharedPrefsGetInt(KEY_ITERATION));
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

    @SuppressLint("SimpleDateFormat")
    public static File getOutputMediaFile(int type, String projectName, String name){
        Log.d(TAG, "getOutputMediaFile. projectName: " + projectName + " name: " + name);
        File mediaStorageDir = new File(getAppDirectory(), projectName);

        if (!mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d(TAG, "getOutputMediaFile. failed to create directory");
                return null;
            }
        }

        if(name == null){
            name = "";
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename;
        String iteration = String.valueOf(sharedPrefsGetInt(KEY_ITERATION));
        File mediaFile;

        if (type == MEDIA_TYPE_IMAGE){
            filename = "IMG_" + iteration + "_" + name + "_" + timeStamp + Consts.IMAGE_EXT;
        } else if(type == MEDIA_TYPE_JSON){
            filename = name + "_" + timeStamp + Consts.JSON_EXT;
        } else if(type == MEDIA_TYPE_VTU){
            filename = name + "_" + timeStamp + Consts.VTU_EXT;
        } else {
            return null;
        }

        mediaFile = new File(mediaStorageDir.getPath(), filename);

        return mediaFile;
    }

    public static String saveBitmapAsJpeg(Bitmap bitmap, String position){
    	return saveBitmapAsJpeg(bitmap, position, sharedPrefsGetInt(KEY_ITERATION));
    }

    public static String saveBitmapAsJpeg(Bitmap bitmap, String position, int iteration){
    	FileOutputStream out;
        try {
            final File imgFile = getOutputMediaFile(MEDIA_TYPE_IMAGE, sharedPrefsGetString(KEY_PROJECT_NAME), position, iteration);

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

    /**
     * Crea un Mat desde un String
     * @param str Matriz en forma de String
     * @return Instancia de Mat con valores en Matriz recibida como String
     */
    public static Mat getMatFromString(String str) throws NumberFormatException{
//		str = "[672.2618351846742, 0, 359.5; 0, 672.2618351846742, 239.5; 0, 0, 1]";
        int rows;
        int cols;
        double[] data;
        String[] colsStr;
        String rowStr;
        String colStr;
        str = str.replaceAll("^\\[|\\]$", "");
        String[] rowsStr = str.split(";");
        rows = rowsStr.length;
        //Por sacar cls
        rowStr = rowsStr[0];
        cols = rowStr.split(",").length;
        data = new double[rows*cols];

        for(int row = 0; row < rowsStr.length; ++row){
            rowStr = rowsStr[row];
            colsStr = rowStr.split(",");
            cols = colsStr.length;
//			Log.d(tag, "row[" + row + "]: " + rowStr);
            for(int col = 0; col < colsStr.length; ++col){
                colStr = colsStr[col];
                data[row*cols+col] = Double.valueOf(colStr);
//				Log.d(tag, "row[" + row + "]col[" + col + "]: " + colStr);
            }
        }
        int type = CvType.CV_64F;
        Mat mat = new Mat(rows, cols, type);
        mat.put(0, 0, data);
//		Log.d(tag, "getMatFromString. Result Mat: " + mat.dump());
        return mat;
    }
}
