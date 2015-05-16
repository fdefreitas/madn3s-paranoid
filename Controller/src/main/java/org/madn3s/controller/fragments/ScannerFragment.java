package org.madn3s.controller.fragments;

import static org.madn3s.controller.Consts.*;

import java.io.File;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.madn3s.controller.Consts;
import org.madn3s.controller.MADN3SController;
import org.madn3s.controller.MidgetOfSeville;
import org.madn3s.controller.MADN3SController.Device;
import org.madn3s.controller.MADN3SController.State;
import org.madn3s.controller.R;
import org.madn3s.controller.io.BraveHeartMidgetService;
import org.madn3s.controller.io.UniversalComms;
import org.madn3s.controller.models.ScanStepViewHolder;
import org.madn3s.controller.models.StatusViewHolder;
import org.madn3s.controller.viewer.opengl.ModelDisplayActivity;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ScannerFragment extends BaseFragment {

	public static final String tag = ScannerFragment.class.getSimpleName();
	public static UniversalComms bridge;

	private ScanStepViewHolder calibrationViewHolder;
	private ScanStepViewHolder nxtActionViewHolder;
	private ScanStepViewHolder camera1ActionViewHolder;
	private ScanStepViewHolder camera2ActionViewHolder;
	private ScanStepViewHolder step1ViewHolder;
	private ScanStepViewHolder step2ViewHolder;
	private ScanStepViewHolder step3ViewHolder;
	private ProgressBar globalProgressBar;
	private ProgressBar generateModelProgressBar;
	private EditText projectNameEditText;
	private Button scanButton;
	private Button calibrateButton;
	private Button generateModelButton;
	private Button viewModelButton;
	private TextView scanStepCurrentTextView;
	private TextView scanStepTotalTextView;
	private Chronometer elapsedChronometer;

	public ScannerFragment() {
		BraveHeartMidgetService.scannerBridge = new UniversalComms() {
			@Override
			public void callback(Object msg) {
				Log.d(tag, "scannerFragment. Update UI on Scanner Fragment.");
				Bundle bundle = (Bundle) msg;
				final Device device = Device.setDevice(bundle.getInt(KEY_DEVICE));
				final State state = State.setState(bundle.getInt(KEY_STATE));
				final int iter = MADN3SController.sharedPrefsGetInt(KEY_ITERATION);
				final boolean scan_finished = bundle.containsKey(KEY_SCAN_FINISHED);
				Log.d(tag, "Device: " + device.toString() + " State: " + state.toString() + " " + iter);

				new Handler(Looper.getMainLooper()).post(new Runnable() {
	                @Override
	                public void run() {
	                	stopChron();
	                	showElapsedTime("last elapsed time");

	                	setDeviceActionState(device, state);
	                	setCurrentScanStep(iter + 1);
						if(scan_finished){
							generateModelButton.setEnabled(true);
							globalProgressBar.setVisibility(View.INVISIBLE);
						}

						resetChron();
						startChron();
	                }
				});
			}
		};

		BraveHeartMidgetService.finishedCalibrationBridge = new UniversalComms() {
			@Override
			public void callback(Object msg) {
				Log.d(tag, "ScannerFragment. finishedCalibrationBridge. Enabling Scanner button");

				new Handler(Looper.getMainLooper()).post(new Runnable() {
	                @Override
	                public void run() {
	                	calibrateButton.setEnabled(false);
	                	scanButton.setEnabled(true);
                        playSound("Calibration", "Calibration Finished");
	                }
				});
			}
		};
	}

	private void setDeviceActionState(Device device, State state){
		StatusViewHolder deviceActionViewHolder;
		switch (device) {
			case NXT:
				deviceActionViewHolder = nxtActionViewHolder;
				break;
			case RIGHT_CAMERA:
				deviceActionViewHolder = camera1ActionViewHolder;
				break;
			default:
				Log.d(tag, "Device switch unhandled default case");
			case LEFT_CAMERA:
				deviceActionViewHolder = camera2ActionViewHolder;
				break;
		}

		switch (state) {
			case CONNECTING:
				deviceActionViewHolder.working();
				break;
			case CONNECTED:
				deviceActionViewHolder.success();
				break;
			case FAILED:
				deviceActionViewHolder.failure();
				break;
			default:
				Log.d(tag, "State switch unhandled default case");
				break;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_scanner, container, false);
	}

	@Override
	public void onViewCreated (View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		globalProgressBar = (ProgressBar) getView().findViewById(R.id.scanner_global_progressBar);
		projectNameEditText = (EditText) getView().findViewById(R.id.scanner_project_name_editText);
		scanStepTotalTextView = (TextView) getView().findViewById(R.id.scan_step_total_textView);
		scanStepCurrentTextView = (TextView) getView().findViewById(R.id.scan_step_current_textView);
		scanButton = (Button) getView().findViewById(R.id.scan_button);
		scanButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
                scan();
			}
		});
		calibrateButton = (Button) getView().findViewById(R.id.calibrate_button);
		calibrateButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
                String projectName = projectNameEditText.getText().toString();
                if(projectName != null && !projectName.isEmpty()){
                    projectNameEditText.setEnabled(false);
                    MADN3SController.sharedPrefsPutString(KEY_PROJECT_NAME, projectName);
                    calibrate();
                } else {
                    //TODO extract String resource
                    Toast missingName = Toast.makeText(getActivity().getBaseContext(), "Falta el nombre del proyecto", Toast.LENGTH_LONG);
                    missingName.show();
                }
			}
		});
		calibrationViewHolder = new ScanStepViewHolder(
				getView().findViewById(R.id.calibration_failure_imageView),
				getView().findViewById(R.id.calibration_success_imageView),
				getView().findViewById(R.id.calibration_working_progressBar),
				getView().findViewById(R.id.calibration_textView)
			);
		calibrationViewHolder.hide();
		nxtActionViewHolder = new ScanStepViewHolder(
				getView().findViewById(R.id.nxt_action_failure_imageView),
				getView().findViewById(R.id.nxt_action_success_imageView),
				getView().findViewById(R.id.nxt_action_working_progressBar),
				getView().findViewById(R.id.nxt_action_textView)
			);
		nxtActionViewHolder.hide();
		camera1ActionViewHolder = new ScanStepViewHolder(
				getView().findViewById(R.id.camera1_action_failure_imageView),
				getView().findViewById(R.id.camera1_action_success_imageView),
				getView().findViewById(R.id.camera1_action_working_progressBar),
				getView().findViewById(R.id.camera1_action_textView)
			);
		camera1ActionViewHolder.hide();
		camera2ActionViewHolder = new ScanStepViewHolder(
				getView().findViewById(R.id.camera2_action_failure_imageView),
				getView().findViewById(R.id.camera2_action_success_imageView),
				getView().findViewById(R.id.camera2_action_working_progressBar),
				getView().findViewById(R.id.camera2_action_textView)
			);
		camera2ActionViewHolder.hide();
		step1ViewHolder = new ScanStepViewHolder(
				getView().findViewById(R.id.step1_generation_failure_imageView),
				getView().findViewById(R.id.step1_generation_success_imageView),
				getView().findViewById(R.id.step1_generation_working_progressBar),
				getView().findViewById(R.id.step1_generation_textView)
			);
		step1ViewHolder.hide();
		step2ViewHolder = new ScanStepViewHolder(
				getView().findViewById(R.id.step2_generation_failure_imageView),
				getView().findViewById(R.id.step2_generation_success_imageView),
				getView().findViewById(R.id.step2_generation_working_progressBar),
				getView().findViewById(R.id.step2_generation_textView)
			);
		step2ViewHolder.hide();
		step3ViewHolder = new ScanStepViewHolder(
				getView().findViewById(R.id.step3_generation_failure_imageView),
				getView().findViewById(R.id.step3_generation_success_imageView),
				getView().findViewById(R.id.step3_generation_working_progressBar),
				getView().findViewById(R.id.step3_generation_textView)
			);
		step3ViewHolder.hide();

		generateModelProgressBar = (ProgressBar) getView().findViewById(R.id.model_generation_progressBar);
		generateModelProgressBar.setVisibility(View.INVISIBLE);
		generateModelButton = (Button) view.findViewById(R.id.model_generation_button);
		generateModelButton.setEnabled(true);
		generateModelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
                generate();
			}
		});

		viewModelButton = (Button) view.findViewById(R.id.view_model_button);
		viewModelButton.setEnabled(true);
		viewModelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//TODO fix hardcoded path
				String fileName = Environment.getExternalStorageDirectory().getPath() + "/MADN3S/models/" + projectNameEditText.getText().toString() + MODEL_EXT;
				File file = new File(fileName);
				if(file.exists()){
					Intent intent = new Intent(getActivity().getBaseContext(), ModelDisplayActivity.class);
					intent.putExtra(MADN3SController.MODEL_MESSAGE, fileName);
					startActivity(intent);
				} else {
					Toast missingName = Toast.makeText(getActivity().getBaseContext(), "El archivo " + fileName + " no existe", Toast.LENGTH_LONG);
					missingName.show();
				}
			}
		});

		elapsedChronometer = (Chronometer) view.findViewById(R.id.elapsed_chronometer);
		resetChron();
	}

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    private void generate() {
        int points = MADN3SController.sharedPrefsGetInt(KEY_POINTS);
        JSONArray framesJson = MADN3SController.sharedPrefsGetJSONArray(Consts.KEY_FRAMES);
        JSONArray framePointsJsonArr = new JSONArray();
        JSONArray result;
        try {
            for(int f = 0; f < points; ++f){
                result = MidgetOfSeville.calculateFrameOpticalFlow(framesJson.getJSONObject(f));
                if(result != null){
                    int length = framePointsJsonArr.length();
                    for(int i = 0; i < result.length(); ++i){
                        framePointsJsonArr.put(i + length, result.get(i));
                    }
                } else {
                    Log.e(tag, "generateModel. onClick. Couldn't calculateOpticalFlow. result null");
                }
            }

        //Traido de ScannerFragment#Test

        String pointsJsonPath = MADN3SController.saveJsonToExternal(framePointsJsonArr.toString(1), "3d-points");
        result = new JSONArray();
        StringBuffer vtkFileBuffer = new StringBuffer();
        int vtkFileBufferLines = 0;

        points = MADN3SController.sharedPrefsGetInt(KEY_POINTS);
        framesJson = new JSONArray();
        for(int i = 0; i < points; i++){
        JSONObject frame = MADN3SController.sharedPrefsGetJSONObject(FRAME_PREFIX + i);
        framesJson.put(frame);
        }

        try {
            Log.d(tag, "Saving the complete framesJson to External. ");
            MADN3SController.saveJsonToExternal(framesJson.toString(1), "frames");
            } catch (JSONException e) {
            Log.d(tag, "Couldn't save the complete framesJson to External. ", e);
            }

            String[] pyrlksResults = new String[points];

            for(int frameIndex = 0; frameIndex < points; ++frameIndex){
                result = MidgetOfSeville.calculateFrameOpticalFlow(framesJson.getJSONObject(frameIndex));
                if(result != null){
                    pyrlksResults[frameIndex] = result.toString();
                } else {
                    Log.e(tag, "result null");
                }
            }

            String vtkFilePath = MADN3SController.createVtpFromPoints(vtkFileBuffer.toString(), vtkFileBufferLines, "vtkData");
            Log.d(tag, "vtkFilePath: " + vtkFilePath);
//            Madn3sNative.doIcp(pyrlksResults, MADN3SController.getAppDirectory().getAbsolutePath(), true, 40, 0.001, 15, true);
            //                            Madn3sNative.doDelaunay("khgks", 0.6);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

	@SuppressLint("SimpleDateFormat")
	public void scan(){
		//TODO FIX THIS SHIT! Ponerle modo debug para pruebas nativas
		try{
			MADN3SController.sharedPrefsPutInt(KEY_ITERATION, 0);
			int points = MADN3SController.sharedPrefsGetInt(KEY_POINTS);
			//TODO permtir borrar contenedor para no hacer for (ya esta hecho en otro lado)
			for(int i = 0; i < points; ++i){
				MADN3SController.removeKeyFromSharedPreferences("frame-"+i);
			}

			int iterations = MADN3SController.sharedPrefsGetInt(Consts.KEY_POINTS);
			setTotalScanSteps(iterations);

			JSONObject json = new JSONObject();
	        json.put(KEY_ACTION, ACTION_TAKE_PICTURE);
	        json.put(KEY_PROJECT_NAME, MADN3SController.sharedPrefsGetString(KEY_PROJECT_NAME));
	        Log.d(tag, "enviando comando");
	        bridge.callback(json.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void calibrate(){
        try {
        	MADN3SController.removeKeyFromSharedPreferences(KEY_CALIBRATION);
        	JSONObject json = new JSONObject();
			json.put(KEY_ACTION, ACTION_CALIBRATE);
			Log.d(tag, "calibrate. sending signal");
	        bridge.callback(json.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void setTotalScanSteps(int total){
		if(globalProgressBar != null){
			globalProgressBar.setMax(total);
		}
		if(scanStepTotalTextView != null){
			scanStepTotalTextView.setText(Integer.toString(total));
		}
	}

	private void setCurrentScanStep(int current){
		if(globalProgressBar != null){
			globalProgressBar.setProgress(current);
		}
		if(scanStepCurrentTextView != null){
			scanStepCurrentTextView.setText(Integer.toString(current));
		}
	}

	public void showElapsedTime(String msg) {
        long elapsedMillis = SystemClock.elapsedRealtime() - elapsedChronometer.getBase();
        Toast.makeText(getActivity(), (msg == null? "" : msg) + " : " + elapsedMillis,
                Toast.LENGTH_SHORT).show();
    }

    public void startChron(){
    	elapsedChronometer.start();
    }

    public void stopChron(){
    	elapsedChronometer.stop();
    }

    public void resetChron(){
    	elapsedChronometer.setBase(SystemClock.elapsedRealtime());
    	showElapsedTime("resetChron");
    }

    private void playSound(String title, String msg){
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Notification mNotification = new Notification.Builder(getActivity())
                .setContentTitle(title)
                .setContentText(msg)
                .setSmallIcon(R.drawable.ic_launcher)
//                .setContentIntent(pIntent)
                .setSound(soundUri)
                .build();

        NotificationManager mNotificationManager =
                (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, mNotification);
    }

}
