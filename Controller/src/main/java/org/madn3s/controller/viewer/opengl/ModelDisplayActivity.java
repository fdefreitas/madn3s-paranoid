package org.madn3s.controller.viewer.opengl;

import org.madn3s.controller.MADN3SController;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class ModelDisplayActivity extends Activity {
	private static final String TAG = "ModelDisplayActivity";
	private Midgetci mGLView;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String offFile = intent.getStringExtra(MADN3SController.MODEL_MESSAGE);
        Log.d(TAG, offFile);
        String[] split = offFile.split("/");
		StringBuffer aux = new StringBuffer();
		for(int i = 2; i < split.length; ++i){
			aux.append(split[i]);
			if(i != split.length-1) aux.append("/");
		}
		offFile = aux.toString();
		Log.d(TAG, offFile);
        mGLView = new Midgetci(this, offFile);
        setContentView(mGLView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLView.onResume();
    }
}
