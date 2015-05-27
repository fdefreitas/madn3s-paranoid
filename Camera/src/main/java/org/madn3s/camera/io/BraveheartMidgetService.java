package org.madn3s.camera.io;

import static org.madn3s.camera.Consts.*;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.Vector;

import org.json.JSONException;
import org.json.JSONObject;
import org.madn3s.camera.Consts;
import org.madn3s.camera.MADN3SCamera;
import org.madn3s.camera.MainActivity;
import org.madn3s.camera.MidgetOfSeville;
import org.madn3s.camera.R;
import org.opencv.core.Mat;

import java.io.File;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;
import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;


public class BraveheartMidgetService extends IntentService {

	private static final String tag = BraveheartMidgetService.class.getSimpleName();
	public static String projectName;
	public static String side;
	private JSONObject result;
	public static final String BT_DEVICE = "btdevice";

	public static final String SERVICE_NAME ="MADN3S";
	public static final UUID APP_UUID = UUID.fromString("65da7fe0-8b80-11e3-baa8-0800200c9a66");

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
	private static final String TOAST = null;

	//TODO incluir dentro de Handler Custom
	private static final int MESSAGE_TOAST = 0;
	private static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_WRITE = 2;

	private BluetoothServerSocket mBluetoothServerSocket;
	private WeakReference<BluetoothServerSocket> mBluetoothServerSocketWeakReference;
	private BluetoothSocket mSocket;
	public static WeakReference<BluetoothSocket> mSocketWeakReference;
    private static Handler mHandler = null;
    private BluetoothAdapter mBluetoothAdapter;
    public static int mState = STATE_NONE;

    public static String deviceName;
    public Vector<Byte> packdata = new Vector<Byte>(2048);
    public static BluetoothDevice device = null;
	public static UniversalComms cameraCallback;
	public static MainActivity mActivity;

    private JSONObject config;

	public BraveheartMidgetService() {
		super(tag);
	}

    @Override
    public IBinder onBind(Intent intent) {
        mHandler = ((MADN3SCamera) getApplication()).getBluetoothHandler();
        return mBinder;
    }

    public class LocalBinder extends Binder {
        BraveheartMidgetService getService() {
            return BraveheartMidgetService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	if(intent.hasExtra(Consts.EXTRA_CALLBACK_MSG) || intent.hasExtra(Consts.EXTRA_RESULT)){
    		Log.d(tag, "Onstart Command. Llamando a onHandleIntent.");
    		return super.onStartCommand(intent,flags,startId);
    	} else {
	        Log.d(tag, "Onstart Command");
	        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	        try {
	            mBluetoothServerSocket = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(Consts.SERVICE_NAME, Consts.APP_UUID);
	            mBluetoothServerSocketWeakReference = new WeakReference<BluetoothServerSocket>(mBluetoothServerSocket);

	            mSocket = null;
	            mSocketWeakReference = null;

	            HiddenMidgetConnector connectorTask = new HiddenMidgetConnector(mBluetoothServerSocketWeakReference, mSocketWeakReference);
	            Log.d(tag, "Ejecutando a HiddenMidgetConnector");
	            connectorTask.execute();

	        } catch (IOException e) {
	        	Log.e(tag, "No se pudo inicializar mBluetoothServerSocket.", e);
	        }

	        String stopservice = intent.getStringExtra("stopservice");
	        if (stopservice != null && stopservice.length() > 0) {
	            stopSelf();
	        }
	        return START_NOT_STICKY;
    	}
    }

	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			String jsonString;
			JSONObject msg;
			if(intent.hasExtra(EXTRA_CALLBACK_MSG)){
				jsonString = intent.getExtras().getString(EXTRA_CALLBACK_MSG
					, Consts.EMPTY_JSON_OBJECT_STRING);
				msg = new JSONObject(jsonString);
				if(msg.has(KEY_ITERATION)){
					MADN3SCamera.iteration = msg.getInt(KEY_ITERATION);
				}
				if(msg.has(KEY_ACTION)){
					String action = msg.getString(KEY_ACTION);
                    playSound("Action", action);
					if(msg.has(KEY_SIDE)){
						side = msg.getString(KEY_SIDE);
					}
					if(msg.has(KEY_PROJECT_NAME)){
                        MADN3SCamera.sharedPrefsPutString(KEY_PROJECT_NAME, msg.getString(KEY_PROJECT_NAME));
					}
					if(config == null){//kind of cheating...
						config = msg;
					}
					Log.d(tag, "action: " + action);
					if(action.equalsIgnoreCase(KEY_CONFIG)){
						Log.d(tag, "Received config: " + config.toString(1));
						config = msg;
						MADN3SCamera.sharedPrefsPutJSONObject(KEY_CONFIG, config);
						MADN3SCamera.isPictureTaken.set(true);
					} else if(action.equalsIgnoreCase(ACTION_TAKE_PICTURE)){
						Log.d(tag, "ACTION_TAKE_PICTURE");
						cameraCallback.callback(config);
					} else if(action.equalsIgnoreCase(ACTION_SEND_PICTURE)) {
						Log.d(tag, "ACTION_SEND_PICTURE");
						sendPicture();
					} else if(action.equalsIgnoreCase(ACTION_END_PROJECT)){
						Log.d(tag, "ACTION_END_PROJECT");
						if(msg.has(Consts.KEY_CLEAN) && msg.getBoolean(Consts.VALUE_CLEAN)){
							cleanTakenPictures();
						}
                        MADN3SCamera.sharedPrefsPutString(KEY_PROJECT_NAME, null);
					} else if(action.equalsIgnoreCase(Consts.ACTION_CALIBRATE)){
						Log.d(tag, "ACTION_CALIBRATE");
						calibrate();
					} else if(action.equalsIgnoreCase(Consts.ACTION_SEND_CALIBRATION_RESULT)){
						Log.d(tag, "ACTION_SEND_CALIBRATION_RESULT");
						if(msg.has(Consts.KEY_CALIBRATION_RESULT)){
							sendCalibrationResult(msg.getString(Consts.KEY_CALIBRATION_RESULT));
						} else {
							Log.d(tag, "Calibration result not present on msg");
						}
                    } else if(action.equalsIgnoreCase(Consts.ACTION_RECEIVE_CALIBRATION_RESULT)){
                        Log.d(tag, "ACTION_RECEIVE_CALIBRATION_RESULT");
                        if(msg.has(Consts.KEY_CALIBRATION_RESULT)){
                            applyCalibration(msg.getString(Consts.KEY_CALIBRATION_RESULT));
                        } else {
                            Log.d(tag, "Calibration result not present on msg");
                        }
					} else if(action.equalsIgnoreCase(Consts.ACTION_EXIT_APP)){
						Log.d(tag, "ACTION_EXIT_APP");
						Log.d(tag, "onHandleIntent: action: " + Consts.ACTION_EXIT_APP);
					} else {
						Log.d(tag, "onHandleIntent: unhandled action: " + action);
					}
				}
			} else if (intent.hasExtra(Consts.EXTRA_RESULT)) {
				Log.d(tag, "EXTRA_RESULT");
				jsonString = intent.getExtras().getString(Consts.EXTRA_RESULT);
				result = new JSONObject(jsonString);
                playSound("Result", "Result received");

				if(result.has(Consts.KEY_ERROR)){
					sendResult();
					MADN3SCamera.isPictureTaken.set(true);
				} else {
					Log.d(tag, "Malformed result JSONObject. error key not found");
				}
			}
		} catch (JSONException e) {
			Log.e(tag, "Could Not Parse JSON", e);
		}
	}

	private void sendPicture() {
		Log.d(tag, "sendPicture");
		Log.d(tag, "mSocketWeakReference null: " + (mSocketWeakReference == null));

		SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
		String filepath = sharedPreferences.getString(Consts.KEY_FILE_PATH, null);
		Bitmap bitmap = BitmapFactory.decodeFile(filepath, Consts.bitmapFactoryOptionsOnSend);

		if(filepath != null){
			if(mSocketWeakReference != null){
				HiddenMidgetWriter writerTask = new HiddenMidgetWriter(mSocketWeakReference, bitmap);
				writerTask.setmActivity(mActivity);
		        Log.d(tag, "Ejecutando a HiddenMidgetWriter desde sendPicture");
		        writerTask.execute();
		        MADN3SCamera.isPictureTaken.set(true);
			}
		} else {
			Log.d(tag, "filepath : null");
		}
	}

	private void calibrate() throws JSONException {
        mActivity.setCaptureMode(true);
        Log.i(tag, "Me dijeron que calibrase");
        playSound("Calibrate Please", "Me dijeron que calibrase");
	}

	private void sendCalibrationResult(String calibrationStr) throws JSONException {
		Log.d(tag, "calibrationStr: " + calibrationStr);
		try {
			result = new JSONObject(calibrationStr);
			result.put(Consts.KEY_ERROR, false);
//			result.put(KEY_CALIB_IMAGE_POINTS, "-- Empty for Testing --");

			JSONObject tempConfigJsonObject = MADN3SCamera.sharedPrefsGetJSONObject(KEY_CONFIG);
			if(tempConfigJsonObject.has(KEY_CAMERA_NAME)){
				result.put(Consts.KEY_CAMERA_NAME, tempConfigJsonObject.getString(KEY_CAMERA_NAME));
			} else {
				Log.d(tag, "CAMERA_NAME not found on persisted Config");
			}
		} catch (Exception e) {
			e.printStackTrace();
			result = new JSONObject();
			result.put(Consts.KEY_ERROR, true);
		} finally {
			sendResult();
		}
	}

    private void applyCalibration(String calibrationStr){
        boolean error = false;
        try {
            Log.d(tag, "applyCalibration. calibrationStr: " + calibrationStr);
            MADN3SCamera.saveJsonToExternal(calibrationStr, "calibration-result");
            JSONObject calibPayload = new JSONObject(calibrationStr);

            // Parsear Maps
            String map1Str = calibPayload.getJSONObject(side).getString(Consts.KEY_CALIB_MAP_1);
            String map2Str = calibPayload.getJSONObject(side).getString(Consts.KEY_CALIB_MAP_2);
            Mat map1 = MADN3SCamera.getMatFromString(map1Str);
            Mat map2 = MADN3SCamera.getMatFromString(map2Str);

            MidgetOfSeville figaro = MidgetOfSeville.getInstance();
            figaro.setMap1(map1);
            figaro.setMap2(map2);

            // Setear Camera Matrix y DistCoeffs en MidgetOfSeville
            String cameraMatrixStr = calibPayload.getJSONObject(side).getString(Consts.KEY_CALIB_CAMERA_MATRIX);
            String distCoeffsStr = calibPayload.getJSONObject(side).getString(Consts.KEY_CALIB_DISTORTION_COEFFICIENTS);
            Mat cameraMatrix = MADN3SCamera.getMatFromString(cameraMatrixStr);
            Mat distCoeffs = MADN3SCamera.getMatFromString(distCoeffsStr);
            Log.d(tag, "Obteniendo calib_camera_matrix:" + cameraMatrixStr);

            // Reemplazado por setCaptureMode
            //        mActivity.setCalibrationValues(cameraMatrix, distCoeffs);
            mActivity.setCaptureMode(false);
            // Puesto para que el socket de BT vuelva a escuchar
            MADN3SCamera.isPictureTaken.set(true);

        } catch (NumberFormatException e) {
            error = true;
            Log.e(tag, "Error parsing Mat from String");
            e.printStackTrace();
        } catch (JSONException e) {
            error = true;
            Log.e(tag, "Error parsing JSON Object from String");
            e.printStackTrace();
        } finally {
            if(error){
                //TODO notificar al controller que la está cagando loco!
            }
        }
    }

	private void sendResult() {
		Log.d(tag, "mSocketWeakReference == null: " + (mSocketWeakReference == null));
		Log.d(tag, "sendResult: " + result.toString());
		if(mSocketWeakReference != null){
			HiddenMidgetWriter writerTask = new HiddenMidgetWriter(mSocketWeakReference, result.toString());
	        Log.d(tag, "Ejecutando a HiddenMidgetWriter desde sendResult");
	        writerTask.execute();
            MADN3SCamera.isPictureTaken.set(true);
		}
	}

	/**
	 * Deletes all <code>projectName</code> files and it's folder
	 */
	private void cleanTakenPictures() {
        String projectName = MADN3SCamera.sharedPrefsGetString(KEY_PROJECT_NAME);
		Log.d(tag, "Cleaning project " + projectName);
		File projectMediaStorageDir = new File(MADN3SCamera.getAppDirectory(), projectName);
		if (projectMediaStorageDir.exists()){
			String[] files = projectMediaStorageDir.list();
			if(files != null){
                for (String file : files) {
                    //noinspection ResultOfMethodCallIgnored
                    new File(projectMediaStorageDir, file).delete();
                }
			}
            //noinspection ResultOfMethodCallIgnored
			projectMediaStorageDir.delete();
        }
	}

    private void playSound(String title, String msg){
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Notification mNotification = new Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(msg)
                .setSmallIcon(R.drawable.ic_launcher)
//                .setContentIntent(pIntent)
                .setSound(soundUri)
                .build();

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, mNotification);
    }
}
