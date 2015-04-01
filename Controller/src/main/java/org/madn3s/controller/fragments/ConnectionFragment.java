package org.madn3s.controller.fragments;

import static org.madn3s.controller.MADN3SController.rightCamera;
import static org.madn3s.controller.MADN3SController.leftCamera;

import java.util.ArrayList;

import org.json.JSONObject;
import org.madn3s.controller.MADN3SController;
import org.madn3s.controller.MADN3SController.Device;
import org.madn3s.controller.MADN3SController.Mode;
import org.madn3s.controller.MADN3SController.State;
import org.madn3s.controller.R;
import org.madn3s.controller.components.NXTTalker;
import org.madn3s.controller.io.HiddenMidgetConnector;
import org.madn3s.controller.io.HiddenMidgetReader;
import org.madn3s.controller.io.UniversalComms;
import org.madn3s.controller.models.StatusViewHolder;
import org.madn3s.controller.viewer.models.files.ModelPickerActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by inaki on 26/01/14.
 */
public class ConnectionFragment extends BaseFragment {
	
	private static final String TAG = ConnectionFragment.class.getSimpleName();
	
	public static final int REQUEST_PICK_FILE = 1;
    public static final int MESSAGE_STATE_CHANGE = 2;
    public static final int MESSAGE_TOAST = 1;
    public static final String TOAST = "toast";
    
    private int mState;
    private Handler mHandler;
    private ConnectionFragment mFragment;

    private TextView nxtNameTextView;
    private TextView nxtAddressTextView;
    private StatusViewHolder nxtStatusViewHolder;

    private TextView rightCameraNameTextView;
    private TextView rightCameraAddressTextView;
    private StatusViewHolder rightCameraStatusViewHolder;

    private TextView leftCameraNameTextView;
    private TextView leftCameraAddressTextView;
    private StatusViewHolder leftCameraStatusViewHolder;

    private Button scannerButton;
    private Button remoteControlButton;
    private Button modelGalleryButton;
    
    private boolean rightCameraStatus;
    private boolean leftCameraStatus;
    private boolean nxtStatus;
    
    public ConnectionFragment(){
    	rightCameraStatus = leftCameraStatus = nxtStatus = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFragment = this;
        
        HiddenMidgetReader.connectionFragmentBridge = new UniversalComms() {
			@Override
			public void callback(Object msg) {
				Bundle bundle = (Bundle)msg;
				final Device device = Device.setDevice(bundle.getInt("device"));
				final State state = State.setState(bundle.getInt("state"));
				mFragment.getView().post(
					new Runnable() { 
						public void run() { 
							setMarkers(state, device);
						} 
					}
				); 
			}
		};
    }

    

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_connection, container, false);
    }

    @SuppressLint("HandlerLeak")
	@Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        nxtNameTextView = (TextView) view.findViewById(R.id.nxt_name_connection_textView);
        nxtAddressTextView = (TextView) view.findViewById(R.id.nxt_address_connection_textView);
        
        if(MADN3SController.nxt != null){
	        nxtNameTextView.setText(MADN3SController.nxt.getName());
	        nxtAddressTextView.setText(MADN3SController.nxt.getAddress());
        }
        
        nxtStatusViewHolder = new StatusViewHolder(
        		view.findViewById(R.id.nxt_not_connected_imageView), 
        		view.findViewById(R.id.nxt_connected_imageView), 
        		view.findViewById(R.id.nxt_connecting_progressBar)
    		);
        if(nxtStatus){
        	nxtStatusViewHolder.success();
        }
        

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_TOAST:
                        Toast.makeText(getActivity().getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                        break;
                    case MESSAGE_STATE_CHANGE:
                        mState = msg.arg1;
                        switch(mState){
                            case NXTTalker.STATE_CONNECTED:
                                nxtStatusViewHolder.success();
                                nxtStatus = true;
                                try {
                                	JSONObject nxtJson = new JSONObject();
                        	        nxtJson.put("command", "scanner");
                        	        nxtJson.put("action", "config");
                        	        nxtJson.put("points", MADN3SController.sharedPrefsGetInt("points"));
                        	        nxtJson.put("radius", MADN3SController.sharedPrefsGetFloat("radius"));
                        	        nxtJson.put("speed", MADN3SController.sharedPrefsGetInt("speed"));
                        	        MADN3SController.talker.write(nxtJson.toString().getBytes());
                        	    } catch (Exception e) {
									Log.e(TAG, "error enviando configs al nxt");
								}
                                break;
                            case NXTTalker.STATE_NONE:
                                nxtStatusViewHolder.failure();
                                break;
                            case NXTTalker.STATE_CONNECTING:
                                nxtStatusViewHolder.working();
                                break;
                        }
                        break;
                }
            }
        };

        if(!nxtStatus){
        	MADN3SController.talker = new NXTTalker(mHandler);
            MADN3SController.talker.connect(MADN3SController.nxt);
            Log.d(TAG, "Iniciando Conexion con NXT: " + MADN3SController.nxt.getName());
        }
        
        
        
        if(!rightCameraStatus){
        	HiddenMidgetConnector rightCameraConnector = new HiddenMidgetConnector(rightCamera, MADN3SController.readRightCamera);
            rightCameraConnector.execute();
            Log.d(TAG, "Iniciando conexion con Right Camera: " + rightCamera.getName());
        }
        
        if(!leftCameraStatus){
        	HiddenMidgetConnector leftCameraConnector = new HiddenMidgetConnector(leftCamera, MADN3SController.readLeftCamera);
            leftCameraConnector.execute();
            Log.d(TAG, "Iniciando conexion con Left Camera: " + leftCamera.getName());
        }
        
        rightCameraNameTextView = (TextView) view.findViewById(R.id.right_camera_name_connection_textView);
        rightCameraAddressTextView = (TextView) view.findViewById(R.id.right_camera_address_connection_textView);
        
        if(rightCamera != null){
        	rightCameraNameTextView.setText(rightCamera.getName());
        	rightCameraAddressTextView.setText(rightCamera.getAddress());
        }
        
        rightCameraStatusViewHolder = new StatusViewHolder(
        		view.findViewById(R.id.right_camera_not_connected_imageView), 
        		view.findViewById(R.id.right_camera_connected_imageView), 
        		view.findViewById(R.id.right_camera_connecting_progressBar)
    		);
        
	    if(rightCameraStatus){
        	rightCameraStatusViewHolder.success();
        }
        
        leftCameraNameTextView = (TextView) view.findViewById(R.id.left_camera_name_connection_textView);
        leftCameraAddressTextView = (TextView) view.findViewById(R.id.left_camera_address_connection_textView);
        
        if(leftCamera != null){
	        leftCameraNameTextView.setText(leftCamera.getName());
	        leftCameraAddressTextView.setText(leftCamera.getAddress());
        }
        
        leftCameraStatusViewHolder = new StatusViewHolder(
        		view.findViewById(R.id.left_camera_not_connected_imageView), 
        		view.findViewById(R.id.left_camera_connected_imageView), 
        		view.findViewById(R.id.left_camera_connecting_progressBar)
    		);
        if(leftCameraStatus){
        	leftCameraStatusViewHolder.success();
        }

        scannerButton = (Button) view.findViewById(R.id.scanner_button);
        scannerButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(rightCameraStatus && leftCameraStatus && nxtStatus){
					listener.onObjectSelected(Mode.SCAN, mFragment);
				} else {
					Toast missingName = Toast.makeText(getActivity().getBaseContext(), "Faltan dispositivos por conectar", Toast.LENGTH_LONG);
					missingName.show();
				}
			}
		});
        remoteControlButton = (Button) view.findViewById(R.id.remote_control_button);
        remoteControlButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(nxtStatus){
					listener.onObjectSelected(Mode.CONTROLLER, mFragment);
				} else {
					Toast missingName = Toast.makeText(getActivity().getBaseContext(), "Falta el NXT por conectar", Toast.LENGTH_LONG);
					missingName.show();
				}
			}
		});
        modelGalleryButton = (Button) view.findViewById(R.id.model_gallery_button);
        modelGalleryButton.setEnabled(true);
        modelGalleryButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity().getBaseContext(), ModelPickerActivity.class);
				ArrayList<String> extensions = new ArrayList<String>();
				extensions.add(".off");
				intent.putExtra(ModelPickerActivity.EXTRA_ACCEPTED_FILE_EXTENSIONS, extensions);
				startActivityForResult(intent, REQUEST_PICK_FILE);
			}
		});
    }
    
    
    
    protected void setMarkers(State state, Device device) {
    	StatusViewHolder statusHolder;
    	switch (device){
	        case RIGHT_CAMERA:
	        	statusHolder = rightCameraStatusViewHolder;
	        	break;
	        case LEFT_CAMERA:
	        	statusHolder = leftCameraStatusViewHolder;
	        	break;
	        default:
	        case NXT:
	        	statusHolder = nxtStatusViewHolder;
	        	break;
	    }
    	
    	switch (state){
	        case CONNECTED:
        		statusHolder.success();
        		if(device == Device.RIGHT_CAMERA){
        			rightCameraStatus = true;
        		} else if(device == Device.LEFT_CAMERA){
        			leftCameraStatus = true;
        		} else if(device == Device.NXT){
        			nxtStatus = true;
        		}
	            break;
	        case CONNECTING:
	        	statusHolder.working();
	            break;
	        default:
	        case FAILED:
	        	statusHolder.failure();
	            break;
	    }
		
	}

}
