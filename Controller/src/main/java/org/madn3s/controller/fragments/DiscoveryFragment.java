package org.madn3s.controller.fragments;

import static org.madn3s.controller.Consts.*;
import static org.madn3s.controller.MADN3SController.isCameraDevice;
import static org.madn3s.controller.MADN3SController.isToyDevice;
import static org.madn3s.controller.MADN3SController.nxt;
import static org.madn3s.controller.MADN3SController.leftCamera;
import static org.madn3s.controller.MADN3SController.rightCamera;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.madn3s.controller.MADN3SController;
import org.madn3s.controller.MidgetOfSeville;
import org.madn3s.controller.MADN3SController.Mode;
import org.madn3s.controller.R;
import org.madn3s.controller.components.CameraSelectionDialogFragment;
import org.madn3s.controller.models.NewDevicesAdapter;
import org.madn3s.controller.models.PairedDevicesAdapter;
import org.madn3s.controller.vtk.Madn3sNative;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by inaki on 12/7/13.
 */
public class DiscoveryFragment extends BaseFragment {
	public static final String tag = DiscoveryFragment.class.getSimpleName();
	public static final String EXTRA_DEVICE_ADDRESS = "device_address";

	private BluetoothAdapter btAdapter;
	private BroadcastReceiver mReceiver;
	private ListView nxtNewDevicesListView, nxtPairedDevicesListView;
	private ListView cameraNewDevicesListView, cameraPairedDevicesListView;
	private LinearLayout nxtDevicesLayout, cameraDevicesLayout;
	private TextView cameraConnectionTextView, nxtConnectionTextView;
	private ProgressBar discoveryProgress;
	private Button connectButton;
	private Button scanButton;
	private NewDevicesAdapter nxtNewDevicesAdapter, cameraNewDevicesAdapter ;
	private PairedDevicesAdapter nxtPairedDevicesAdapter, cameraPairedDevicesAdapter;
	private boolean isNxtSelected;
	private int cams;
	private DiscoveryFragment mFragment;
	private CameraSelectionDialogFragment cameraSelectionDialogFragment;
	private Button testsButton;

	public DiscoveryFragment() {
		mFragment = this;
		isNxtSelected =  false;
		cams = 0;
		btAdapter = BluetoothAdapter.getDefaultAdapter();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_discovery, container, false);
	}

	@Override
	public void onViewCreated (View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		setUpUi();
		setUpBtReceiver();
		doDiscovery();
	}

	private void setUpUi() {
		discoveryProgress = (ProgressBar) getView().findViewById(R.id.discovery_progressBar);
		discoveryProgress.setVisibility(View.GONE);

		nxtConnectionTextView = (TextView) getView().findViewById(R.id.nxt_connection_textView);
		nxtConnectionTextView.setVisibility(View.GONE);

		nxtDevicesLayout = (LinearLayout) getView().findViewById(R.id.nxt_devices_layout);
		nxtDevicesLayout.setVisibility(View.GONE);

		nxtPairedDevicesListView = (ListView) getView().findViewById(R.id.nxt_paired_devices_listView);

		nxtNewDevicesListView = (ListView) getView().findViewById(R.id.nxt_new_devices_listView);

		cameraConnectionTextView = (TextView) getView().findViewById(R.id.cameras_connection_textView);
		cameraConnectionTextView.setVisibility(View.GONE);

		cameraDevicesLayout = (LinearLayout) getView().findViewById(R.id.camera_devices_layout);
		cameraDevicesLayout.setVisibility(View.GONE);

		cameraPairedDevicesListView = (ListView) getView().findViewById(R.id.camera_paired_devices_listView);
		cameraNewDevicesListView = (ListView) getView().findViewById(R.id.cameras_new_devices_listView);

		scanButton = (Button) getView().findViewById(R.id.scan_button);
		scanButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				doDiscovery();
			}
		});

		connectButton = (Button) getView().findViewById(R.id.connect_button);
		connectButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(isNxtSelected && cams == 2){
					showCamerasSideSelectionDialog();
				} else {
					Toast.makeText(getActivity(), "Debe seleccionar un dispositivo NXT y 2 Cámaras"
							, Toast.LENGTH_LONG).show();
				}
			}
		});

		testsButton = (Button) getView().findViewById(R.id.tests_button);
		testsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new AsyncTask<Void, Void, JSONArray>() {

					@Override
					protected JSONArray doInBackground(Void... params) {

						int points = MADN3SController.sharedPrefsGetInt(KEY_POINTS);
						JSONArray framesJson = new JSONArray();
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

                        JSONArray pyrlkResultsJsonArr = new JSONArray();
                        JSONArray result = null;
                        int minLength = 2000;

						try {
							for(int frameIndex = 0; frameIndex < points; ++frameIndex){
								result = MidgetOfSeville.calculateFrameOpticalFlow(framesJson.getJSONObject(frameIndex));
								if(result != null){
                                    Log.e(tag, "framePyr[" + frameIndex + "] length:" + result.length());
//                                    if(result.length() < minLength && result.length() > 0){ minLength = result.length();}
                                    pyrlkResultsJsonArr.put(frameIndex, result);
								} else {
									Log.e(tag, "result null");
								}
							}

//                            JSONArray tempPyr;
//                            Log.e(tag, "minLength: " + minLength);
//                            for(int index = 0; index < pyrlkResultsJsonArr.length(); ++index){
//                                tempPyr = pyrlkResultsJsonArr.getJSONArray(index);
//                                if(tempPyr.length() > minLength) {
//                                    int length = tempPyr.length();
//                                    for (int delIndex = minLength; delIndex < length; ++delIndex) {
//                                        tempPyr.remove(minLength);
//                                    }
//                                }
//                            }

//                            for(int index = 0; index < pyrlkResultsJsonArr.length(); ++index){
//                                tempPyr = pyrlkResultsJsonArr.getJSONArray(index);
//                                if(tempPyr.length() == 0){
//                                    pyrlkResultsJsonArr.remove(index);
//                                }
//                            }

                            Log.e(tag, "pyrlkResultsJsonArr length: " + pyrlkResultsJsonArr.length());
                            MADN3SController.saveJsonToExternal(pyrlkResultsJsonArr.toString(1), "frames-after-pyr");

						} catch (JSONException e) {
							e.printStackTrace();
						}

						return pyrlkResultsJsonArr;
					}

					@Override
					protected void onPostExecute(JSONArray resultArgs) {
						super.onPostExecute(resultArgs);
                        try {
                            String[] pyrlksResults = new String[resultArgs.length()];
                            JSONArray tempPyr;

                            for(int index = 0; index < resultArgs.length(); ++index){
                                tempPyr = resultArgs.getJSONArray(index);
                                pyrlksResults[index] = tempPyr.toString();
                            }

                            String filepath = MADN3SController.getAppDirectory().getAbsolutePath()
                                    + "/" + MADN3SController.sharedPrefsGetString(KEY_PROJECT_NAME) + "/";
                            Log.d(tag, "filepath: " + filepath);
                            Madn3sNative.doIcp(pyrlksResults, filepath, true, 40, 0.001, 15, true);
    //                            Madn3sNative.doDelaunay("khgks", 0.6);

                            Toast.makeText(getActivity(), "Calibration Finished", Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
					}
				}.execute();

//				Imprime los puntos
//				try {
//					JSONObject calibrationJson = MADN3SController.sharedPrefsGetJSONObject(Consts.KEY_CALIBRATION);
//					JSONObject left = calibrationJson.getJSONObject(Consts.SIDE_LEFT);
//					JSONObject right = calibrationJson.getJSONObject(Consts.SIDE_RIGHT);
//
//
//					JSONArray leftArr = new JSONArray(left.getString(Consts.KEY_CALIB_IMAGE_POINTS));
//					left.put(Consts.KEY_CALIB_IMAGE_POINTS, "--Moved for testing--");
//					Log.d(tag, "calibration. left: " + left.toString(1));
//					for(int i = 0; i < leftArr.length(); ++i){
//						Log.d(tag, "[" + i + "]" + leftArr.get(i));
//					}
//
//					JSONArray rightArr = new JSONArray(right.getString(Consts.KEY_CALIB_IMAGE_POINTS));
//					right.put(Consts.KEY_CALIB_IMAGE_POINTS, "--Moved for testing--");
//					Log.d(tag, "calibration. right: " + right.toString(1));
//					for(int i = 0; i < rightArr.length(); ++i){
//						Log.d(tag, "[" + i + "]" + rightArr.get(i));
//					}
//
//				} catch (JSONException e) {
//					e.printStackTrace();
//				}
//				Fin imprime los puntos
//
//				try {
//					pointsJson.put(KEY_NAME, MADN3SController.sharedPrefsGetString(KEY_PROJECT_NAME));
//					pointsJson.put(KEY_PICTURES, framesJson);
//					Log.d(tag, "generateModelButton.OnClick. pointsJson: " + pointsJson.toString(1));
//					KiwiNative.doProcess(pointsJson.toString());
//				} catch (JSONException e) {
//					e.printStackTrace();
//					Log.e(tag, "generateModelButton.OnClick. Error composing points JSONObject");
//				}
			}
		});
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		if(getActivity() != null) {
			unregisterBtReceiver();
		}
	}

	public void showCamerasSideSelectionDialog() {
        cameraSelectionDialogFragment = new CameraSelectionDialogFragment();
        cameraSelectionDialogFragment.show(getFragmentManager(), null);
    }

	public void onDevicesSelectionCompleted(){
		if(isNxtSelected && cams == 2){
			cameraSelectionDialogFragment.dismiss();
			Log.d(tag, "Mode: SCANNER");
			listener.onObjectSelected(Mode.SCANNER, mFragment);
		}else if(isNxtSelected && cams == 2 && MADN3SController.rightCamera == MADN3SController.leftCamera) {
			Toast.makeText(getActivity()
					, "Debe seleccionar Cámaras diferentes para cada posición", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(getActivity()
					, "Debe seleccionar un dispositivo NXT y 2 Cámaras", Toast.LENGTH_LONG).show();
		}
	}

	public void onDevicesSelectionCancelled(){
		Log.d(tag, "Device Selection Cancelled by User");
	}

	private AdapterView.OnItemClickListener onDeviceAdapterClickListener = new AdapterView
		.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			cancelDiscovery();
			Log.d(tag, "view.isSelected() before: " + view.isSelected());
			if(view.isSelected()){
				view.setSelected(false);
			} else {
				view.setSelected(true);
			}
			Log.d(tag, "view.isSelected() after: " + view.isSelected());

			if(view.isSelected()){
				BluetoothDevice deviceTemp = (BluetoothDevice) parent.getAdapter().getItem(position);
				Log.d(tag, "ItemClick Device: " + deviceTemp.getName());

				if(isToyDevice(deviceTemp) && !isNxtSelected){
					nxt = deviceTemp;
					isNxtSelected = true;
				} else if(isToyDevice(deviceTemp) && isNxtSelected){
					Toast.makeText(getActivity(), "Ya fue seleccionado un dispositivo NXT"
							, Toast.LENGTH_LONG).show();
				} else if(cams == 0){
					rightCamera = deviceTemp;
					cams++;
				} else if(cams == 1 && deviceTemp != rightCamera){
					leftCamera = deviceTemp;
					cams++;
				} else if(cams == 2){
					Toast.makeText(getActivity(), "Ya fueron seleccionadas 2 camaras"
							, Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(getActivity(), "Debe seleccionar 2 cámaras diferentes"
							, Toast.LENGTH_LONG).show();
				}

				Log.d(tag, "Cameras Selected: " + cams + ", isNxtSelected: " + isNxtSelected);
			}
		}
	};

	private void enableBT(){
        if(!btAdapter.isEnabled()){
        	btAdapter.enable();
        }
    }

    private void doDiscovery() {
    	Log.d(tag, "Starting Discovery");
        enableBT();
        if (btAdapter.isDiscovering()) {
        	cancelDiscovery();
        }
        btAdapter.startDiscovery();

        discoveryProgress.setVisibility(View.VISIBLE);
		nxtConnectionTextView.setVisibility(View.GONE);
		nxtDevicesLayout.setVisibility(View.GONE);
		cameraConnectionTextView.setVisibility(View.GONE);
		cameraDevicesLayout.setVisibility(View.GONE);
		connectButton.setEnabled(false);

		try {

			nxtPairedDevicesAdapter = new PairedDevicesAdapter(getPairedToyDevices()
					, getActivity().getBaseContext());
			nxtPairedDevicesListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
			nxtPairedDevicesListView.setAdapter(nxtPairedDevicesAdapter);
			nxtPairedDevicesListView.setOnItemClickListener(onDeviceAdapterClickListener);

			nxtNewDevicesAdapter = new NewDevicesAdapter(getActivity().getBaseContext());
			nxtNewDevicesListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
			nxtNewDevicesListView.setAdapter(nxtNewDevicesAdapter);
			nxtNewDevicesListView.setOnItemClickListener(onDeviceAdapterClickListener);

			cameraPairedDevicesAdapter = new PairedDevicesAdapter(getPairedCameraDevices()
					, getActivity().getBaseContext());
			cameraPairedDevicesListView.setAdapter(cameraPairedDevicesAdapter);
			cameraPairedDevicesListView.setOnItemClickListener(onDeviceAdapterClickListener);

			cameraNewDevicesAdapter = new NewDevicesAdapter(getActivity().getBaseContext());
			cameraNewDevicesListView.setAdapter(cameraNewDevicesAdapter);
			cameraNewDevicesListView.setOnItemClickListener(onDeviceAdapterClickListener);

			registerBtReceiver();

		} catch (Exception e) {
			e.printStackTrace();
		}
    }

	private void cancelDiscovery(){
		Log.d(tag, "Stopping Discovery");
    	btAdapter.cancelDiscovery();
    	discoveryProgress.setVisibility(View.GONE);
		nxtConnectionTextView.setVisibility(View.VISIBLE);
		nxtDevicesLayout.setVisibility(View.VISIBLE);
		cameraConnectionTextView.setVisibility(View.VISIBLE);
		cameraDevicesLayout.setVisibility(View.VISIBLE);
		connectButton.setEnabled(true);
		unregisterBtReceiver();
    }

	private ArrayList<BluetoothDevice> getPairedToyDevices() {
		ArrayList<BluetoothDevice> temporaryPairedDevices = new ArrayList<BluetoothDevice>();
		for(BluetoothDevice device:  btAdapter.getBondedDevices()){
			if (isToyDevice(device)){
				temporaryPairedDevices.add(device);
				Log.d(tag, "Toy Paired Device: "+device.getName());
			}
		}
		return temporaryPairedDevices;
	}

	private ArrayList<BluetoothDevice> getPairedCameraDevices() {
		ArrayList<BluetoothDevice> temporaryPairedDevices = new ArrayList<BluetoothDevice>();
		for(BluetoothDevice device:  btAdapter.getBondedDevices()){
			if (isCameraDevice(device)){
				temporaryPairedDevices.add(device);
				Log.d(tag, "Camera Paired Device: "+device.getName());
			}
		}
		return temporaryPairedDevices;
	}

	private void setUpBtReceiver(){
		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();

				if (BluetoothDevice.ACTION_FOUND.equals(action)) {
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

					if(device.getBondState() != BluetoothDevice.BOND_BONDED){
						if (isToyDevice(device)) {
							Log.d(tag, "Device: {Name:" + device.getName() + ", Address: "
									+ device.getAddress() + ", Class: " + device.getClass() + "}");
							nxtNewDevicesAdapter.add(device);
							nxtNewDevicesAdapter.notifyDataSetChanged();

						} else if (isCameraDevice(device)) {
							Log.d(tag, "Device: {Name:" + device.getName() + ", Address: "
									+ device.getAddress() + ", Class: " + device.getBluetoothClass()
									.getDeviceClass()+"}");
							cameraNewDevicesAdapter.add(device);
							cameraNewDevicesAdapter.notifyDataSetChanged();
						}
					}

				} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
					Log.d(tag, "Busqueda Terminada");
					cancelDiscovery();
				}
			}
		};
	}

    private void registerBtReceiver() {
		IntentFilter intentFilter;

		intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		getActivity().registerReceiver(mReceiver, intentFilter);

		intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		getActivity().registerReceiver(mReceiver, intentFilter);
	}

    private void unregisterBtReceiver() {
    	try {
    		getActivity().unregisterReceiver(mReceiver);
		} catch (Exception e) {
			Log.d(tag, "unregisterBtReceiver. " + e.getMessage());
		}
    }
}
