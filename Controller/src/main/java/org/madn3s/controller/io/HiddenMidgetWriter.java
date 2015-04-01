package org.madn3s.controller.io;

import java.io.OutputStream;
import java.lang.ref.WeakReference;

import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

public class HiddenMidgetWriter extends AsyncTask<Void, Void, Void> {

	private static final String tag = HiddenMidgetWriter.class.getSimpleName();
	private BluetoothSocket mSocket;
    private Exception e;
    private String msg;
    
    
    public HiddenMidgetWriter(WeakReference<BluetoothSocket> mBluetoothSocketWeakReference, String msg){
    	mSocket = mBluetoothSocketWeakReference.get();
    	this.msg = msg;
    	e = null;
    }

    @Override
    protected Void doInBackground(Void... params) {
    	try{
    		OutputStream os = mSocket.getOutputStream();
    		os.flush();
    		os.write(msg.getBytes());
    		this.e = null;
        } catch (Exception e){
        	this.e = e;
        	e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result){
        if(e != null){
        	Log.d(tag, "envie " + msg + " a " + mSocket.getRemoteDevice().getName());
        } else {
//        	Log.d(tag, "Ocurrio un error enviando " + msg + " a " + mSocket.getRemoteDevice().getName());
        }
    }
}
