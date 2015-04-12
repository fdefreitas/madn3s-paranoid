package org.madn3s.controller.io;

import static org.madn3s.controller.Consts.*;
import static org.madn3s.controller.MADN3SController.leftCamera;
import static org.madn3s.controller.MADN3SController.leftCameraWeakReference;
import static org.madn3s.controller.MADN3SController.rightCamera;
import static org.madn3s.controller.MADN3SController.rightCameraWeakReference;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.madn3s.controller.MADN3SController;
import org.madn3s.controller.MADN3SController.Device;
import org.madn3s.controller.MADN3SController.State;
import org.madn3s.controller.MidgetOfSeville;
import org.madn3s.controller.R;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class BraveHeartMidgetService extends IntentService {

	private static final String tag = BraveHeartMidgetService.class.getSimpleName();
	private static Handler mHandler = null;
	private final IBinder mBinder = new LocalBinder();
	public static UniversalComms scannerBridge;
	public static UniversalComms calibrationBridge;

	public class LocalBinder extends Binder {
		 BraveHeartMidgetService getService() {
            return BraveHeartMidgetService.this;
        }
    }

	public BraveHeartMidgetService(String name) {
		super(name);
	}

	@Override
    public IBinder onBind(Intent intent) {
        mHandler = ((MADN3SController) getApplication()).getBluetoothHandler();
        Log.d(tag, "mHandler " + mHandler == null ? "NULL" : mHandler.toString());
        Log.d(tag, "mBinder " + mBinder == null ? "NULL" : mBinder.toString());
        return mBinder;
    }

	public BraveHeartMidgetService() {
		super(tag);
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent.hasExtra(EXTRA_CALLBACK_MSG) || intent.hasExtra(EXTRA_RESULT)
				|| intent.hasExtra(EXTRA_CALLBACK_SEND)
				|| intent.hasExtra(EXTRA_CALLBACK_NXT_MESSAGE)
				|| intent.hasExtra(EXTRA_CALLBACK_PICTURE)
				|| intent.hasExtra(EXTRA_CALLBACK_CALIBRATION_RESULT)){
    		return super.onStartCommand(intent,flags,startId);
    	} else {
	        String stopservice = intent.getStringExtra(EXTRA_STOP_SERVICE);
	        if (stopservice != null && stopservice.length() > 0) {
	            stopSelf();
	        }
	        return START_NOT_STICKY;
    	}

	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Iterator<String> extraIt = intent.getExtras().keySet().iterator();
		StringBuilder keyString = new StringBuilder();
		keyString.append("onHandleIntent. extras: ");
		while(extraIt.hasNext()){
			keyString.append(",").append(extraIt.next());
		}
		Log.d(tag, keyString.toString());

		String jsonString;
		if(intent.hasExtra(EXTRA_CALLBACK_MSG)){
			Log.d(tag, "EXTRA_CALLBACK_MSG");
            playSound("EXTRA_CALLBACK_MSG", "EXTRA_CALLBACK_MSG");
			jsonString = intent.getExtras().getString(EXTRA_CALLBACK_MSG);
			processCameraAnswer(jsonString);
		} else if(intent.hasExtra(EXTRA_CALLBACK_PICTURE)){
			Log.d(tag, "EXTRA_CALLBACK_PICTURE");
            playSound("EXTRA_CALLBACK_PICTURE", "EXTRA_CALLBACK_PICTURE");
			jsonString = intent.getExtras().getString(EXTRA_CALLBACK_PICTURE);
			processCameraPicture(jsonString);
		} else if(intent.hasExtra(EXTRA_CALLBACK_SEND)){
			Log.d(tag, "EXTRA_CALLBACK_SEND");
            playSound("EXTRA_CALLBACK_SEND", "EXTRA_CALLBACK_SEND");
			jsonString = intent.getExtras().getString(EXTRA_CALLBACK_SEND);
			sendMessageToCameras(jsonString, true, true);
		} else if(intent.hasExtra(EXTRA_CALLBACK_NXT_MESSAGE)){
			Log.d(tag, "EXTRA_CALLBACK_NXT_MESSAGE");
            playSound("EXTRA_CALLBACK_NXT_MESSAGE", "EXTRA_CALLBACK_NXT_MESSAGE");
			jsonString = intent.getExtras().getString(EXTRA_CALLBACK_NXT_MESSAGE);
			processNxtMessage(jsonString);
		} else if(intent.hasExtra(EXTRA_CALLBACK_CALIBRATION_RESULT)){
			Log.d(tag, "EXTRA_CALLBACK_CALIBRATION_RESULT");
            playSound("EXTRA_CALLBACK_CALIBRATION_RESULT", "EXTRA_CALLBACK_CALIBRATION_RESULT");
			jsonString = intent.getExtras().getString(EXTRA_CALLBACK_CALIBRATION_RESULT);
			Toast.makeText(getBaseContext(), "Calibration Result has Arrived", Toast.LENGTH_LONG).show();
			processCalibrationResult(jsonString);
		}
	}

	private void processCalibrationResult(String jsonString){
		Log.d(tag, "processCalibrationResult: " + jsonString);
		//TODO extraer que camara es y guardar en sharedPrefs
		try {
			JSONObject calibrationJson = MADN3SController.sharedPrefsGetJSONObject(KEY_CALIBRATION);
			JSONObject jsonResult = new JSONObject(jsonString);
			String side = jsonResult.getString(KEY_SIDE);

			JSONObject sideCalibration = new JSONObject();
			sideCalibration.put(KEY_CALIB_DISTORTION_COEFFICIENTS, jsonResult.getString(KEY_CALIB_DISTORTION_COEFFICIENTS));
			sideCalibration.put(KEY_CALIB_CAMERA_MATRIX, jsonResult.getString(KEY_CALIB_CAMERA_MATRIX));
			sideCalibration.put(KEY_CALIB_IMAGE_POINTS, jsonResult.getString(KEY_CALIB_IMAGE_POINTS));

			switch(side){
				case SIDE_LEFT:
					calibrationJson.put(SIDE_LEFT, sideCalibration);
					break;
				case SIDE_RIGHT:
					calibrationJson.put(SIDE_RIGHT, sideCalibration);
					break;
				default:

			}

			MADN3SController.sharedPrefsPutJSONObject(KEY_CALIBRATION, calibrationJson);
            boolean isFinished = calibrationJson.has(SIDE_LEFT) && calibrationJson.has(SIDE_RIGHT);

            Log.d(tag, "processCalibrationResult. isCalibrationFinished: " + isFinished);
			if(isFinished){
				MidgetOfSeville.doStereoCalibration();
                Log.d(tag, "processCalibrationResult. Enabling Scan Button");
				calibrationBridge.callback(null);
                Log.d(tag, "processCalibrationResult. Scan Button Enabled");
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

	//TODO cambiar nombre a algo que explique mejor que hace
	public void sendMessageToCameras(){
		try{
			String projectName = MADN3SController.sharedPrefsGetString(KEY_PROJECT_NAME);
			JSONObject json = new JSONObject();
	        json.put(KEY_ACTION, ACTION_TAKE_PICTURE);
	        json.put(KEY_PROJECT_NAME, projectName);
			sendMessageToCameras(json.toString(), true, true);
		} catch (JSONException e){
            Log.e(tag, "Error armando el JSON", e);
        } catch (Exception e){
            Log.e(tag, "Error generico enviando", e);
        }
	}

	public void sendMessageToCameras(String msgString, boolean left, boolean right){
		Log.d(tag, "sendMessageToCameras. message: " + msgString + " left: " + left + " right: " + right);
		try{
			JSONObject msg = new JSONObject(msgString);
			msg.put(KEY_ITERATION, MADN3SController.sharedPrefsGetInt(KEY_ITERATION));
			if(right){
				if(rightCameraWeakReference != null){
		        	msg.put(KEY_SIDE, SIDE_RIGHT);
		        	msg.put(KEY_CAMERA_NAME, rightCamera.getName());
					HiddenMidgetWriter sendCamera1 = new HiddenMidgetWriter(rightCameraWeakReference, msg.toString());
					sendCamera1.execute();
			        Log.d(tag, "Enviando a rightCamera: " + rightCamera.getName());
			        MADN3SController.readRightCamera.set(true);
				} else {
					Log.e(tag, "rightCameraWeakReference null. rightCamera null: " + (rightCamera == null));
				}
			}

			if(left) {
				if(leftCameraWeakReference != null){
					msg.put(KEY_SIDE, SIDE_LEFT);
					msg.put(KEY_CAMERA_NAME, leftCamera.getName());
					HiddenMidgetWriter sendCamera2 = new HiddenMidgetWriter(leftCameraWeakReference, msg.toString());
					sendCamera2.execute();
			        Log.d(tag, "Enviando a leftCamera: " + leftCamera.getName());
			        MADN3SController.readLeftCamera.set(true);
				} else {
					Log.e(tag, "leftCameraWeakReference null. leftCamera null: " + (leftCamera == null));
				}
			}

			Bundle bundle = new Bundle();
			bundle.putInt(KEY_STATE, State.CONNECTING.getState());

			bundle.putInt(KEY_DEVICE,  Device.RIGHT_CAMERA.getValue());
			scannerBridge.callback(bundle);

			bundle.putInt(KEY_DEVICE,  Device.LEFT_CAMERA.getValue());
			scannerBridge.callback(bundle);

		} catch (JSONException e){
            Log.e(tag, "Error armando JSONObject", e);
        } catch (Exception e){
            Log.e(tag, "Error enviando JSONObject", e);
        }
	}

	public void processCameraAnswer(String msgString){
		Log.d(tag, "processCameraAnswer. answer: " + msgString);
		try {
			JSONObject msg = new JSONObject(msgString);
			boolean left = false;
			boolean right = false;
			if(msg.has(KEY_ERROR) && !msg.getBoolean(KEY_ERROR)){
				int iter = MADN3SController.sharedPrefsGetInt(KEY_ITERATION);
				JSONObject frame = MADN3SController.sharedPrefsGetJSONObject(FRAME_PREFIX + iter);
				if(msg.has(KEY_SIDE)){
					String side = msg.getString(KEY_SIDE);
					msg.remove(KEY_SIDE);
					msg.remove(KEY_TIME);
					msg.remove(KEY_ERROR);
					msg.remove(KEY_CAMERA);

					if(side.equalsIgnoreCase(SIDE_RIGHT)){
						right = true;
						frame.put(SIDE_RIGHT, msg);
					} else if(side.equalsIgnoreCase(SIDE_LEFT)){
						left = true;
						frame.put(SIDE_LEFT, msg);
					}

					MADN3SController.sharedPrefsPutJSONObject(FRAME_PREFIX + iter, frame);

					JSONObject cameraMsg = new JSONObject();
					cameraMsg.put(KEY_ACTION, ACTION_SEND_PICTURE);
					sendMessageToCameras(cameraMsg.toString(), left, right);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void processCameraPicture(String jsonString){
		Log.d(tag, "processCameraPicture.");
		try {
			JSONObject msg = new JSONObject(jsonString);
			if(msg.has(KEY_ERROR) && !msg.getBoolean(KEY_ERROR)){
				int iter = MADN3SController.sharedPrefsGetInt(KEY_ITERATION);
				JSONObject frame = MADN3SController.sharedPrefsGetJSONObject(FRAME_PREFIX + iter);
				if(msg.has(KEY_SIDE)){
					int device = 1;
					String side = msg.getString(KEY_SIDE);

					Log.d(tag, "processCameraPicture. side: " + side);
					if(side.equalsIgnoreCase(SIDE_RIGHT)){
						JSONObject rightJson = frame.getJSONObject(SIDE_RIGHT);
						rightJson.put(KEY_FILE_PATH, msg.getString(KEY_FILE_PATH));
						frame.put(SIDE_RIGHT, rightJson);
						device = Device.RIGHT_CAMERA.getValue();
					} else if(side.equalsIgnoreCase(SIDE_LEFT)){
						JSONObject leftJson = frame.getJSONObject(SIDE_LEFT);
						leftJson.put(KEY_FILE_PATH, msg.getString(KEY_FILE_PATH));
						frame.put(SIDE_LEFT, leftJson);
						device = Device.LEFT_CAMERA.getValue();
					}

					Bundle bundle = new Bundle();
					bundle.putInt(KEY_STATE, State.CONNECTED.getState());
					bundle.putInt(KEY_DEVICE, device);

					//Updates UI
					scannerBridge.callback(bundle);

					MADN3SController.sharedPrefsPutJSONObject(FRAME_PREFIX + iter, frame);
					Log.d(tag, "processCameraPicture. " + FRAME_PREFIX + iter + ": " +  getPrintableFrame());

					if(frame.has(SIDE_RIGHT) && frame.getJSONObject(SIDE_RIGHT).has(KEY_FILE_PATH)
							&& frame.has(SIDE_LEFT) && frame.getJSONObject(SIDE_LEFT).has(KEY_FILE_PATH)){
						iter++;
						int points = MADN3SController.sharedPrefsGetInt(KEY_POINTS);
						MADN3SController.sharedPrefsPutInt(KEY_ITERATION, iter);
						if(iter < points){
							JSONObject json = new JSONObject();
							json.put(KEY_COMMAND, COMMAND_SCANNER);
					        json.put(KEY_ACTION, ACTION_MOVE);
					        Log.d(tag, "processCameraPicture. sending message to NXT");
					        sendMessageToNXT(json.toString());
						} else {
							JSONArray framesJson = new JSONArray();
							for(int i = 0; i < points; i++){
								JSONObject tempFrame = MADN3SController.sharedPrefsGetJSONObject(FRAME_PREFIX + i);
								framesJson.put(tempFrame);
							}

							try {
								Log.d(tag, "Saving the complete framesJson to External. ");
								MADN3SController.saveJsonToExternal(framesJson.toString(1), "frames");
								MADN3SController.sharedPrefsPutJSONArray(KEY_FRAMES, framesJson);
							} catch (JSONException e) {
								Log.d(tag, "Couldn't save the complete framesJson to External. ", e);
							}

							Log.d(tag, "processCameraPicture. notify scan finished");
							notifyScanFinished();
						}
						Log.d(tag, "processCameraPicture. iter = " + iter + " points = " + points);
					}
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void processNxtMessage(String jsonString){
		//TODO revisar si se puede arreglar
		jsonString = jsonString.substring(0, (jsonString.lastIndexOf("}") + 1));

		Bundle bundle = new Bundle();
		bundle.putInt(KEY_STATE, State.CONNECTED.getState());
		bundle.putInt(KEY_DEVICE, Device.NXT.getValue());
		scannerBridge.callback(bundle);
		try {
			JSONObject json = new JSONObject(jsonString);
			String message = json.getString(KEY_MESSAGE);
			if(message.equalsIgnoreCase(MESSAGE_PICTURE)){
				sendMessageToCameras();
			} else if(message.equalsIgnoreCase(MESSAGE_FINISH)){
				bundle.putInt(KEY_STATE, State.CONNECTED.getState());

				bundle.putInt(KEY_DEVICE,  Device.RIGHT_CAMERA.getValue());
				scannerBridge.callback(bundle);

				bundle.putInt(KEY_DEVICE,  Device.LEFT_CAMERA.getValue());
				scannerBridge.callback(bundle);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getPrintableFrame() {
		int iter = MADN3SController.sharedPrefsGetInt(KEY_ITERATION);
		JSONObject frame = MADN3SController.sharedPrefsGetJSONObject(FRAME_PREFIX + iter);
		try{

			if(frame.has(SIDE_RIGHT)){
				frame.getJSONObject(SIDE_RIGHT).remove(KEY_POINTS);
			}

			if(frame.has(SIDE_LEFT)){
				frame.getJSONObject(SIDE_LEFT).remove(KEY_POINTS);
			}

			return frame.toString(1);
		} catch (Exception e){
			Log.e(tag, "getPrintableFrame. Couldn't parse frame", e);
			return "{}";
		}


	}

	/**
	 *
	 */
	private void sendMessageToNXT(String msg) {
		Log.d(tag, "sendMessageToNXT. message: " + msg);
		Bundle bundle = new Bundle();
		bundle.putInt(KEY_STATE, State.CONNECTING.getState());
		bundle.putInt(KEY_DEVICE, Device.NXT.getValue());
		scannerBridge.callback(bundle);
		MADN3SController.talker.write(msg.getBytes());
	}

	/**
	 * Closes Scan Progress. Updates Controller UI and sends finish signals to Cameras and NXT
	 */
	private void notifyScanFinished() {
		Log.d(tag, "notifyScanFinished. terminando escaneo");
		Bundle bundle = new Bundle();
		bundle.putBoolean(KEY_SCAN_FINISHED, true);
		bundle.putInt(KEY_STATE, MADN3SController.State.CONNECTED.getState());

		bundle.putInt(KEY_DEVICE, Device.NXT.getValue());
		scannerBridge.callback(bundle);

		bundle.putInt(KEY_DEVICE,  Device.RIGHT_CAMERA.getValue());
		scannerBridge.callback(bundle);

		bundle.putInt(KEY_DEVICE,  Device.LEFT_CAMERA.getValue());
		scannerBridge.callback(bundle);

		try{
			JSONObject json = new JSONObject();
			json.put(KEY_ACTION, ACTION_END_PROJECT);
	        json.put(KEY_PROJECT_NAME, MADN3SController.sharedPrefsGetString(KEY_PROJECT_NAME));
	        json.put(KEY_CLEAN, MADN3SController.sharedPrefsGetBoolean(VALUE_CLEAN));
	        sendMessageToCameras(json.toString(), true, true);
	        JSONObject nxtJson = new JSONObject();
	        nxtJson.put(KEY_COMMAND, COMMAND_SCANNER);
	        nxtJson.put(KEY_ACTION, ACTION_FINISH);
	        sendMessageToNXT(nxtJson.toString());
		} catch (JSONException e) {
			e.printStackTrace();
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
