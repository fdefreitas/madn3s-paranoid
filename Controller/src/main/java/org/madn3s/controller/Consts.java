package org.madn3s.controller;

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
	public static final String KEY_CALIBRATION = "calibration";
	public static final String KEY_STEREO_CALIBRATION = "stereo_calibration";
    public static final String KEY_CALIBRATION_RESULT = "calib_result";
	public static final String KEY_COMMAND = "command";
	public static final String KEY_ACTION = "action";
	public static final String KEY_CLEAN = "clean";
	public static final String KEY_ERROR = "error";
	public static final String KEY_POINTS = "points";
	public static final String KEY_STATE = "state";
	public static final String KEY_DEVICE = "device";
	public static final String KEY_CAMERA = "camera";
	public static final String KEY_TIME = "time";
	public static final String KEY_FILE_PATH = "filepath";
	public static final String KEY_MESSAGE = "message";
	public static final String KEY_ITERATION = "iter";
    public static final String KEY_MD5 = "md5";
	public static final String KEY_ITERATIONS = "iterations";
	public static final String KEY_CAMERA_NAME = "camera_name";
	public static final String KEY_SCAN_FINISHED = "scan_finished";
	public static final String KEY_NAME = "name";
	public static final String KEY_PICTURES = "pictures";
	public static final String KEY_FILE = "file";
	public static final String KEY_FRAMES = "frames";
	public static final String KEY_X = "x";
	public static final String KEY_Y = "y";
	public static final String KEY_Z = "z";
	public static final String KEY_R = "R";
	public static final String KEY_T = "T";
	public static final String KEY_E = "E";
	public static final String KEY_F = "F";

	public static final String COMMAND_ABORT = "abort";
	public static final String COMMAND_SCANNER = "scanner";

	public static final String ACTION_ABORT = "abort";
	public static final String ACTION_MOVE = "move";
	public static final String ACTION_FINISH = "finish";
	public static final String ACTION_TAKE_PICTURE = "take_picture";
	public static final String ACTION_SEND_PICTURE = "send_picture";
	public static final String ACTION_END_PROJECT = "end_project";
	public static final String ACTION_CALIBRATE = "calibrate";
	public static final String ACTION_CALIBRATION_RESULT = "calibration_result";
    public static final String ACTION_RECEIVE_CALIBRATION_RESULT = "received_calibration_result";
	public static final String ACTION_EXIT_APP = "exit_app";

	public static final String MESSAGE_PICTURE = "picture";
	public static final String MESSAGE_FINISH = "finish";

	public static final String SIDE_RIGHT = "right";
	public static final String SIDE_LEFT = "left";

	public final static String EXTRA_CALLBACK_MSG = "message";
	public static final String EXTRA_CALLBACK_SEND = "send";
	public static final String EXTRA_CALLBACK_NXT_MESSAGE = "nxt_message";
	public static final String EXTRA_CALLBACK_CALIBRATION_RESULT = "calib_result";
	public static final String EXTRA_CALLBACK_PICTURE = "picture";
	public static final String EXTRA_RESULT = "result";
	public static final String EXTRA_STOP_SERVICE = "stopservice";

	public static final String VALUE_CLEAN = "clean";
	public static final String VALUE_DEFAULT_PROJECT_NAME = "default";
	public static final String VALUE_DEFAULT_SIDE = "default";
	public static final String FRAME_PREFIX = "frame-";
	public static final String IMAGE_EXT = ".jpg";
	public static final String JSON_EXT = ".json";
	public static final String VTU_EXT = ".vtu";
	public static final String MODEL_EXT = ".off";
	public static final String EMPTY_JSON_OBJECT_STRING = "{}";

	public static final String KEY_CALIB_DISTORTION_COEFFICIENTS = "calib_distortion_coefficients";
    public static final String KEY_CALIB_CAMERA_MATRIX = "calib_camera_matrix";
    public static final String KEY_CALIB_IMAGE_POINTS = "calib_image_points";
    public static final String KEY_CALIB_MAP_1 = "calib_map_1";
    public static final String KEY_CALIB_MAP_2 = "calib_map_2";

	public static final CompressFormat BITMAP_COMPRESS_FORMAT = Bitmap.CompressFormat.JPEG;
	public static final int COMPRESSION_QUALITY = 100;
	public static final Bitmap.Config DEFAULT_IN_PREFERRED_CONFIG = Bitmap.Config.RGB_565;

	public static BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();

	public static void init(){
		Log.d(tag, "init()");
		bitmapFactoryOptions.inSampleSize = 1;
	    bitmapFactoryOptions.inDither = false;
	    bitmapFactoryOptions.inPurgeable = true;
	    bitmapFactoryOptions.inInputShareable = true;
	    bitmapFactoryOptions.inTempStorage = new byte[32 * 1024];
	    bitmapFactoryOptions.inPreferredConfig = DEFAULT_IN_PREFERRED_CONFIG;
	}
}
