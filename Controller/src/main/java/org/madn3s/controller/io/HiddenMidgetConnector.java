package org.madn3s.controller.io;

import static org.madn3s.controller.Consts.*;


import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONObject;
import org.madn3s.controller.MADN3SController;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

public class HiddenMidgetConnector extends AsyncTask<Void, Void, Void> {
	
	private static final String tag = HiddenMidgetConnector.class.getSimpleName();
	private WeakReference<BluetoothSocket> mSocketWeakReference;
    private BluetoothSocket mSocket;
    private Exception e;
    private AtomicBoolean read;
    
    public HiddenMidgetConnector(BluetoothDevice mBluetoothDevice, AtomicBoolean read){
    	try {
            mSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(MADN3SController.APP_UUID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    	this.read = read;
    }

	@Override
	protected Void doInBackground(Void... params) {
		 try {
            Log.d(tag, "isConnected: " + mSocket.isConnected() + " - " + mSocket.getRemoteDevice().getName());
            mSocket.connect();
        } catch (Exception e) {
            this.e = e;
            try {
                mSocket.close();
            } catch (IOException ex) {
                this.e = ex;
            }
        }
        return null;
	}
	
	@Override
    protected void onPostExecute(Void result){
		String deviceName = mSocket.getRemoteDevice().getName();
    	String deviceAddress = mSocket.getRemoteDevice().getAddress();
    	
        if (e!= null) {
        	e.printStackTrace();
        }
        
        Log.d(tag, deviceName + " " + mSocket.toString());
        if(mSocket.isConnected()){
            Log.d(tag, "Conexion levantada " + deviceName);
        }else{
            Log.d(tag, "Conexion fallida " + deviceName);
		}
    	
    	if (mSocket.getRemoteDevice()!= null){
            Log.d(tag, deviceName);
        }
        
        switch (mSocket.getRemoteDevice().getBondState()){
            case BluetoothDevice.BOND_BONDED: 
                Log.d(tag, "BOND_BONDED - " + deviceName);
                String side;
                if(MADN3SController.isRightCamera(deviceAddress)){
                	side = SIDE_RIGHT;
                	mSocketWeakReference = MADN3SController.rightCameraWeakReference = new WeakReference<BluetoothSocket>(mSocket);
                	
                } else if(MADN3SController.isLeftCamera(deviceAddress)){
                	side = SIDE_LEFT;
                	mSocketWeakReference = MADN3SController.leftCameraWeakReference = new WeakReference<BluetoothSocket>(mSocket);
                	
                } else {
                	side = VALUE_DEFAULT_SIDE;
                	Log.wtf(tag, "WHUT?!");
                }
                
                HiddenMidgetReader readerHandlerThread = new HiddenMidgetReader("readerTask-" + side 
                		+ "-" + deviceName, mSocketWeakReference, read, side);
                readerHandlerThread.start();
                sendConfigs(mSocketWeakReference, side, deviceName);
                Log.d(tag, "Ejecutando a HiddenMidgetReader");
                break;
                
            case BluetoothDevice.BOND_BONDING:
                Log.d(tag, "BOND_BONDING - " + deviceName);
                break;
            case BluetoothDevice.BOND_NONE:
                Log.d(tag, "BOND_NONE - " + deviceName);
                break;
            default:
                Log.d(tag, "Default - " + deviceName);
        }
    }

    @Override
    protected void onCancelled(){
        try {
            mSocket.close();
        } catch (IOException e) {
            this.e = e;
            e.printStackTrace();
        }
    }
    
    private void sendConfigs(WeakReference<BluetoothSocket> cameraWeakReference, String side, String name) {
		try{
			JSONObject json = new JSONObject();
	        json.put(KEY_ACTION, "config");
	        json.put(KEY_CLEAN, MADN3SController.sharedPrefsGetBoolean("clean"));
	        json.put("side", side);
	        json.put("camera_name", name);
	        JSONObject grabCut = new JSONObject();
	        JSONObject rectangle = new JSONObject();
	        JSONObject point1 = new JSONObject();
	        point1.put("x", MADN3SController.sharedPrefsGetInt("p1x"));
	        point1.put("y", MADN3SController.sharedPrefsGetInt("p1y"));
	        rectangle.put("point_1", point1);
	        JSONObject point2 = new JSONObject();
	        point2.put("x", MADN3SController.sharedPrefsGetInt("p2x"));
	        point2.put("y", MADN3SController.sharedPrefsGetInt("p2y"));
	        rectangle.put("point_2", point2);
	        grabCut.put("rectangle", rectangle);
	        grabCut.put("iterations", MADN3SController.sharedPrefsGetInt("iterations"));
	        json.put("grab_cut", grabCut);
	        
	        JSONObject goodFeatures = new JSONObject();
	        goodFeatures.put("max_corners", MADN3SController.sharedPrefsGetInt("maxCorners"));
	        goodFeatures.put("quality_level", MADN3SController.sharedPrefsGetFloat("qualityLevel"));
	        goodFeatures.put("min_distance", MADN3SController.sharedPrefsGetInt("minDistance"));
	        json.put("good_features", goodFeatures);
	        
	        JSONObject edgeDetection = new JSONObject();
	        edgeDetection.put("algorithm", MADN3SController.sharedPrefsGetString("algorithm"));
	        edgeDetection.put("algorithm_index", MADN3SController.sharedPrefsGetInt("algorithmIndex"));
	        JSONObject canny = new JSONObject();
	        canny.put("lower_threshold", MADN3SController.sharedPrefsGetFloat("lowerThreshold"));
	        canny.put("upper_threshold", MADN3SController.sharedPrefsGetFloat("upperThreshold"));
	        edgeDetection.put("canny_config", canny);
	        JSONObject sobel = new JSONObject();
	        sobel.put("d_depth", MADN3SController.sharedPrefsGetInt("dDepth"));
	        sobel.put("d_x", MADN3SController.sharedPrefsGetInt("dX"));
	        sobel.put("d_y", MADN3SController.sharedPrefsGetInt("dY"));
	        edgeDetection.put("sobel_config", sobel);
	        json.put("edge_detection", edgeDetection);
	        
	        HiddenMidgetWriter sendCamera = new HiddenMidgetWriter(cameraWeakReference, json.toString());
	        sendCamera.execute();
	    } catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
