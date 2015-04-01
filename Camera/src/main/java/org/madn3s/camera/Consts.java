package org.madn3s.camera;

import java.util.UUID;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.util.Log;

public class Consts {
	
	private static final String tag = Consts.class.getSimpleName();
	
	public static final String SERVICE_NAME ="MADN3S";
    public static final UUID APP_UUID = UUID.fromString("65da7fe0-8b80-11e3-baa8-0800200c9a66");

	public static final String KEY_PROJECT_NAME = "project_name";
	public static final String KEY_SIDE = "side";
	public static final String KEY_CONFIG = "config";
	public static final String KEY_COMMAND = "command";
	public static final String KEY_CAMERA_NAME = "camera_name";
	public static final String KEY_ACTION = "action";
	public static final String KEY_CLEAN = "clean";
	public static final String KEY_ERROR = "error";
	public static final String KEY_POINTS = "points";
	public static final String KEY_FILE_PATH = "filepath";
	public static final String KEY_MD5 = "md5";
	public static final String KEY_MD5_BASE_64 = "md5_base64";
	public static final String KEY_ITERATION = "iter";
	public static final String KEY_RESULT = "result";
	
	public static final String ACTION_TAKE_PICTURE = "take_picture";
	public static final String ACTION_SEND_PICTURE = "send_picture";
	public static final String ACTION_END_PROJECT = "end_project";
	public static final String ACTION_CALIBRATE = "calibrate";
	public static final String ACTION_SEND_CALIBRATION_RESULT = "send_calibration";
	public static final String ACTION_CALIBRATION_RESULT = "calibration_result";
	public static final String ACTION_EXIT_APP = "exit_app";
	
	public final static String EXTRA_CALLBACK_MSG = "message";
	public static final String EXTRA_RESULT = "result";
	
	public static final String VALUE_CLEAN = "clean";
	public static final String VALUE_DEFAULT_PROJECT_NAME = "default";
	public static final String VALUE_DEFAULT_POSITION = "default";
	public static final String IMAGE_EXT = ".jpg";
	public static final String EMPTY_JSON_OBJECT_STRING = "{}";  

	public static final CompressFormat BITMAP_COMPRESS_FORMAT = Bitmap.CompressFormat.JPEG;
	public static final int COMPRESSION_QUALITY = 100;
	public static final Bitmap.Config DEFAULT_IN_PREFERRED_CONFIG = Bitmap.Config.RGB_565;
	public static BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();
	public static BitmapFactory.Options bitmapFactoryOptionsOnSend = new BitmapFactory.Options();

	public static final String KEY_ACTIVITY_RESULT = "result";
	public static final String KEY_CALIBRATION_RESULT = "calib_result";
	public static final String KEY_CALIB_DISTORTION_COEFFICIENTS = "calib_distortion_coefficients";
    public static final String KEY_CALIB_CAMERA_MATRIX = "calib_camera_matrix";
    public static final String KEY_CALIB_IMAGE_POINTS = "calib_image_points";

	public static final String ORG_MADN3S_ACTION_CALIBRATE = "org.madn3s.action.CALIBRATE";
    
	public static void init(){
		Log.d(tag, "init()");
		bitmapFactoryOptions.inSampleSize = 4;
	    bitmapFactoryOptions.inDither = false;
	    bitmapFactoryOptions.inPurgeable = true;
	    bitmapFactoryOptions.inInputShareable = true;
	    bitmapFactoryOptions.inTempStorage = new byte[32 * 1024];
	    bitmapFactoryOptions.inPreferredConfig = DEFAULT_IN_PREFERRED_CONFIG;
	    
	    bitmapFactoryOptionsOnSend.inSampleSize = 1;
	    bitmapFactoryOptionsOnSend.inDither = false;
	    bitmapFactoryOptionsOnSend.inPurgeable = true;
	    bitmapFactoryOptionsOnSend.inInputShareable = true;
	    bitmapFactoryOptionsOnSend.inTempStorage = new byte[32 * 1024];
	    bitmapFactoryOptionsOnSend.inPreferredConfig = DEFAULT_IN_PREFERRED_CONFIG;
	}
}
