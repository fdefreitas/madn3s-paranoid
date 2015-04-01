package org.madn3s.controller.io;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONObject;
import org.madn3s.controller.Consts;
import org.madn3s.controller.MADN3SController;
import org.madn3s.controller.MADN3SController.Device;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

public class HiddenMidgetReader extends HandlerThread implements Callback {
	
	private static final String tag = HiddenMidgetReader.class.getSimpleName();
	public static UniversalComms bridge;
	public static UniversalComms connectionFragmentBridge;
	public static UniversalComms pictureBridge;
	public static UniversalComms calibrationBridge;
	private Handler handler, callback;
	private WeakReference<BluetoothSocket> mBluetoothSocketWeakReference;
    private BluetoothSocket mSocket;
    private AtomicBoolean read;
    private String side;

	public HiddenMidgetReader(String name, WeakReference<BluetoothSocket> mBluetoothSocketWeakReference) {
		super(name);
		this.mBluetoothSocketWeakReference = mBluetoothSocketWeakReference;
		this.side = Consts.VALUE_DEFAULT_SIDE;
	}
	
	public HiddenMidgetReader(String name, WeakReference<BluetoothSocket> mBluetoothSocketWeakReference
			, AtomicBoolean read, String side) {
		super(name);
		this.mBluetoothSocketWeakReference = mBluetoothSocketWeakReference;
		this.read = read;
		this.side = side;
	}
	
	public HiddenMidgetReader(String name, int priority) {
		super(name, priority);
	}

	@Override
	public boolean handleMessage(Message msg) {
		return false;
	}

	@Override
	protected void onLooperPrepared() {
		handler =  new Handler(getLooper(), this);
	}

	@Override
	public void run() {
		try {
			String message;
			while (true) {
				if (mBluetoothSocketWeakReference != null){
					mSocket = mBluetoothSocketWeakReference.get();
					break;
				}
	        }
			
			int bondState = mSocket.getRemoteDevice().getBondState();
			boolean isConnected = mSocket.isConnected();
			int state, deviceIntValue;
			Bundle bundle = new Bundle();
			
			if(isConnected){
				switch (bondState){
		            case BluetoothDevice.BOND_BONDED:
		            	state = MADN3SController.State.CONNECTED.getState();
		                break;
		            case BluetoothDevice.BOND_BONDING:
		            	state = MADN3SController.State.CONNECTING.getState();
		                break;
		            default:
		            case BluetoothDevice.BOND_NONE:
		            	state = MADN3SController.State.FAILED.getState();
		                break;
		        }
			} else {
				state = MADN3SController.State.FAILED.getState();
			}
			
			if(MADN3SController.isRightCamera(mSocket.getRemoteDevice().getAddress())){
	        	deviceIntValue = Device.RIGHT_CAMERA.getValue();
	        	
	        	//TODO hacer setter en Application
	        	if(MADN3SController.leftCamera == null){
	        		Log.d(tag, "leftCamera null. Trying to recover.");
	        		MADN3SController.leftCamera = mSocket.getRemoteDevice();
	        	}
	        	if(MADN3SController.leftCameraWeakReference == null){
	        		Log.d(tag, "leftCameraWeakReference null. Trying to recover.");
	        		MADN3SController.leftCameraWeakReference = new WeakReference<BluetoothSocket>(mSocket);
	        	}
	        } else if(MADN3SController.isLeftCamera(mSocket.getRemoteDevice().getAddress())){
	        	deviceIntValue = Device.LEFT_CAMERA.getValue();
	        	
	        	//TODO hacer setter en Application
	        	if(MADN3SController.rightCamera == null){
	        		Log.d(tag, "rightCamera null. Trying to recover.");
	        		MADN3SController.rightCamera = mSocket.getRemoteDevice();
	        	}
	        	if(MADN3SController.rightCameraWeakReference == null){
	        		Log.d(tag, "rightCamera null. Trying to recover.");
	        		MADN3SController.rightCameraWeakReference = new WeakReference<BluetoothSocket>(mSocket);
	        	}
	        } else {
	        	deviceIntValue = Device.NXT.getValue();
	        }

			bundle.putInt(Consts.KEY_STATE, state);
			bundle.putInt(Consts.KEY_DEVICE, deviceIntValue);
			long start = 0;
			connectionFragmentBridge.callback(bundle);
			if(state == MADN3SController.State.CONNECTED.getState()){
				while(MADN3SController.isRunning.get()){
					if(read.get()){
						if(start == 0){
							start = System.currentTimeMillis();
						}
						
						JSONObject msg;
						ByteArrayOutputStream bao = getMessage();
						byte[] bytes = bao.toByteArray();
						
//						Log.d(tag, "Escuchando. " + mSocket.getRemoteDevice().getName() 
//								+ ". bao size: " + bytes.length);
						
						if(bytes.length > 0){
							bytes = Base64.decode(bytes, Base64.DEFAULT);
							Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length
									, Consts.bitmapFactoryOptions);
							message = new String(bytes);
							String md5Hex = new String(MADN3SController.getMD5EncryptedString(bytes));
							Log.d(tag, "device: " + mSocket.getRemoteDevice().getName()
									+ " side: " + side
									+ " iter: " + MADN3SController.sharedPrefsGetInt(Consts.KEY_ITERATION) 
									+ " MD5: " + md5Hex);
							
							if(bmp == null){
								Log.d(tag, "instanceof String");
								Log.d(tag, "Received String: " + message);
								
								if(message != null && !message.isEmpty()){
									msg = new JSONObject(message);
									String action = null;
									if(msg.has(Consts.KEY_ACTION)){
										action = msg.getString(Consts.KEY_ACTION);
										 if(action.equalsIgnoreCase(Consts.ACTION_EXIT_APP)){
											break;
										}
									} else {
										Log.d(tag, "Message Received with no action: " + msg.toString(1));
									}
									msg.put(Consts.KEY_CAMERA, mSocket.getRemoteDevice().getName());
									msg.put(Consts.KEY_SIDE, side);
									msg.put(Consts.KEY_TIME, System.currentTimeMillis() - start);
									if(action != null && action.equalsIgnoreCase(Consts.ACTION_CALIBRATION_RESULT)){
										calibrationBridge.callback(msg.toString());
									} else {
										bridge.callback(msg.toString());
									}
									start = 0;
									read.set(false);
								}
							} else {					
								Log.d(tag, "instancef Bitmap");
								msg = new JSONObject();
								msg.put(Consts.KEY_ERROR, false);
								msg.put(Consts.KEY_SIDE, side);
								String filepath = MADN3SController.saveBitmapAsPng(bmp, side);
								msg.put(Consts.KEY_FILE_PATH, filepath);
								
								pictureBridge.callback(msg.toString());
								start = 0;
								read.set(false);
							}
						}
					}
				}
			}
		 } catch (Exception e) {
			 e.printStackTrace();
		 }
	}
	
	private ByteArrayOutputStream getMessage(){
		try{
        	int byteTemp = 0;
        	int threshold = 0;
        	ByteArrayOutputStream bao = new ByteArrayOutputStream();
        	bao.reset();
        	InputStream inputStream = mSocket.getInputStream();
        	while(true){
        		while (inputStream.available() == 0 && threshold < 3000) { 
                    Thread.sleep(1);
                    threshold++;
                }
        		
        		if(threshold < 3000){
        			threshold = 0;
        			byteTemp = inputStream.read();
        			bao.write(byteTemp);
            		if(byteTemp == 255){
            			break;
            		}
            		Thread.sleep(1);
        		} else {
        			break;
        		}
        	}
        	return bao;
        } catch (Exception e){
            Log.e(tag, "getMessage. Exception al leer Socket: ", e);
            return null;
        }
	}
}