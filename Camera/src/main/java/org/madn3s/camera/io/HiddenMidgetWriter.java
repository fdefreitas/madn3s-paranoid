package org.madn3s.camera.io;

import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import org.madn3s.camera.Consts;
import org.madn3s.camera.MADN3SCamera;
import org.madn3s.camera.MainActivity;


/**
 * Created by ninja_midget on 2/1/14.
 */
public class HiddenMidgetWriter extends AsyncTask<Void, Void, Void> {

    private static final String tag = HiddenMidgetWriter.class.getSimpleName();
	private BluetoothSocket mSocket;
	private MainActivity mActivity;
    private Exception e;
    private byte[] msg;
    
    public HiddenMidgetWriter(WeakReference<BluetoothSocket> mBluetoothSocketWeakReference, Object msg){
    	mSocket = mBluetoothSocketWeakReference.get();
    	if(msg instanceof String){
    		Log.d(tag, "instanceof String");
    		this.msg = ((String) msg).getBytes();
    	} else if(msg instanceof Bitmap){
    		Log.d(tag, "instanceof Bitmap");
    		ByteArrayOutputStream baos = new ByteArrayOutputStream();
    		((Bitmap) msg).compress(Consts.BITMAP_COMPRESS_FORMAT, Consts.COMPRESSION_QUALITY, baos);
    		this.msg = baos.toByteArray();
    	}
    	
    	this.msg = Base64.encode(this.msg, Base64.DEFAULT);
    	String md5HexBase64 = new String(MADN3SCamera.getMD5EncryptedString(this.msg));
		Log.d(tag, "MD5 Base64: " + md5HexBase64);
    }
    
    @Override
	protected void onPreExecute() {
    	if(mActivity != null){
    		new Handler(Looper.getMainLooper()).post(new Runnable() {             
                @Override
                public void run() { 
                	mActivity.resetChron();
            		mActivity.startChron();
                }
              });
    	}
		super.onPreExecute();
	}

    @Override
    protected Void doInBackground(Void... params) {
    	try{
    		OutputStream os = mSocket.getOutputStream();
    		os.flush();
    		os.write(msg);
    		this.e = null;
        } catch (Exception e){
        	this.e = e;
        	e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result){
    	if(mActivity != null){
    		new Handler(Looper.getMainLooper()).post(new Runnable() {             
                @Override
                public void run() { 
                	mActivity.stopChron();
            		mActivity.showElapsedTime("sending picture");
                }
              });
    	}
    	
        if(e == null){
        	Log.d(tag, "Mensaje: " + msg.toString() + " enviado a " + mSocket.getRemoteDevice().getName());
        } else {
        	Log.d(tag, "Ocurrio un error enviando mensaje: " + msg + " a " + mSocket.getRemoteDevice().getName());
        }
    }

	public void setmActivity(MainActivity mActivity) {
		this.mActivity = mActivity;
	}
    
}
