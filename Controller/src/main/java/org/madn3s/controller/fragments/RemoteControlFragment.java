package org.madn3s.controller.fragments;

import org.json.JSONObject;
import org.madn3s.controller.MADN3SController;
import org.madn3s.controller.R;
import org.madn3s.controller.components.NXTTalker;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * Created by inaki on 1/11/14.
 */

//TODO de aqui se debe eliminar la parte referente a levantar la conexion y se debe recibir el talker por parametro
public class RemoteControlFragment extends BaseFragment {
    public static final int MESSAGE_STATE_CHANGE = 2;
    public static final int MESSAGE_TOAST = 1;
    public static final String TOAST = "toast";

    private static final String tag = "ControlsFragment";
    private int mPower = 80;
    private boolean mReverse = false;
    private boolean mReverseLR = false;
    private boolean mRegulateSpeed = false;
    private boolean mSynchronizeMotors = true;
    private int mState;
    private BluetoothDevice device;
//    private NXTTalker talker;

    private ImageView frontImageView, backImageView, rightImageView,leftImageView;

    public RemoteControlFragment(){
    	
        this.device = MADN3SController.nxt;

        final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_TOAST:
                        Toast.makeText(getActivity().getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                        break;
                    case MESSAGE_STATE_CHANGE:
                        mState = msg.arg1;
//                        displayState();
                        break;
                }
            }
        };
        if(MADN3SController.talker == null){
        	MADN3SController.talker = new NXTTalker(mHandler);
        	MADN3SController.talker.connect(device);
        }
        
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_remote_control, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        frontImageView = (ImageView) view.findViewById(R.id.top_arrow_imageView);
        backImageView = (ImageView) view.findViewById(R.id.bottom_arrow_imageView);
        leftImageView = (ImageView) view.findViewById(R.id.left_arrow_imageView);
        rightImageView = (ImageView) view.findViewById(R.id.right_arrow_imageView);
        frontImageView.setOnTouchListener(new DirectionButtonOnTouchListener(1,1));
        backImageView.setOnTouchListener(new DirectionButtonOnTouchListener(-1,-1));
        leftImageView.setOnTouchListener(new DirectionButtonOnTouchListener(-0.6,0.6));
        rightImageView.setOnTouchListener(new DirectionButtonOnTouchListener(0.6,-0.6));
    }

    private class DirectionButtonOnTouchListener implements View.OnTouchListener {
    	private boolean wait;
        private double lmod;
        private double rmod;

        public DirectionButtonOnTouchListener(double l, double r) {
            lmod = l;
            rmod = r;
            wait = false;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Log.i(tag, "onTouch event: " + Integer.toString(event.getAction()));
            int action = event.getAction();
            //if ((action == MotionEvent.ACTION_DOWN) || (action == MotionEvent.ACTION_MOVE)) {
            if (action == MotionEvent.ACTION_DOWN) {
//                byte power = (byte) mPower;
//                if (mReverse) {
//                    power *= -1;
//                }
//                byte l = (byte) (power*lmod);
//                byte r = (byte) (power*rmod);
//                if (!mReverseLR) {
//                	MADN3SController.talker.motors(l, r, mRegulateSpeed, mSynchronizeMotors);
//                } else {
//                	MADN3SController.talker.motors(r, l, mRegulateSpeed, mSynchronizeMotors);
//                }
            	if(!wait){
	            	try {
	            		JSONObject nxtJson = new JSONObject();
	         	        nxtJson.put("command", "rc");
	         	        nxtJson.put("action", "forward");
	         	        MADN3SController.talker.write(nxtJson.toString().getBytes());
	         	        wait = true;
	         	        Log.e(tag, "forward");
	         	    } catch (Exception e) {
						Log.e(tag, "error enviando configs al nxt");
					}
            	}
            } else if ((action == MotionEvent.ACTION_UP) || (action == MotionEvent.ACTION_CANCEL)) {
//            	MADN3SController.talker.motors((byte) 0, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
            	try {
            		JSONObject nxtJson = new JSONObject();
         	        nxtJson.put("command", "rc");
         	        nxtJson.put("action", "stop");
         	        MADN3SController.talker.write(nxtJson.toString().getBytes());
         	        wait = false;
         	        Log.e(tag, "stop");
         	    } catch (Exception e) {
						Log.e(tag, "error enviando configs al nxt");
				}
            }
            return true;
        }
    }


}
