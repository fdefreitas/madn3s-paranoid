package org.madn3s.camera.io;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import org.json.JSONObject;
import org.madn3s.camera.Consts;
import org.madn3s.camera.MADN3SCamera;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

/**
 * Created by ninja_midget on 2/1/14.
 */
public class HiddenMidgetReader extends HandlerThread implements Callback {
	
	private final static String tag = HiddenMidgetReader.class.getSimpleName();
	public static UniversalComms bridge;
	@SuppressWarnings("unused")
	private Handler handler;
	private WeakReference<BluetoothSocket> mBluetoothSocketWeakReference;
    private BluetoothSocket mSocket;

	public HiddenMidgetReader(String name, WeakReference<BluetoothSocket> mBluetoothSocketWeakReference) {
		super(name);
		this.mBluetoothSocketWeakReference = mBluetoothSocketWeakReference;
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
				
				while(MADN3SCamera.isRunning.get()){
					if(MADN3SCamera.isPictureTaken.get()){
						Log.d(tag, "Esperando mensaje.");
						message = getMessage();
						if(message != null && !message.isEmpty()){
//							Log.d(tag, "Mensaje Recibido: " + message);
							JSONObject msg = new JSONObject(message);
							if(msg.has(Consts.KEY_ACTION)){
								String action = msg.getString(Consts.KEY_ACTION);
								 if(action.equalsIgnoreCase(Consts.ACTION_EXIT_APP)){
									break;
								}
							}	
							bridge.callback(message);
//							Log.d(tag, "Iniciando wait().");
							MADN3SCamera.isPictureTaken.set(false);
						}
					}
				}
			 } catch (Exception e) {
				 e.printStackTrace();
			 }
	}
	
	private String getMessage(){
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
        	return bao != null ? bao.toString() : null;
        } catch (Exception e){
            Log.e(tag, "getMessage. Exception al leer Socket", e);
            return null;
        }
	}
}
