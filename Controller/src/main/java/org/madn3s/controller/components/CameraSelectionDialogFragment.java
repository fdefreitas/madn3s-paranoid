/**
 * 
 */
package org.madn3s.controller.components;

import org.madn3s.controller.MADN3SController;
import org.madn3s.controller.R;
import org.madn3s.controller.models.DevicesAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

/**
 * @author fernando
 *
 */
public class CameraSelectionDialogFragment extends DialogFragment {
	private static final String tag = "CameraSelectionDialogFragment";
	public interface DialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }
    
    DialogListener mListener;
	private View view;
	private Spinner rightCameraSpinner;
	private Spinner leftCameraSpinner;
	private DevicesAdapter camerasAdapter;
    
    public CameraSelectionDialogFragment() {
	}
	
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.dialog_cameras_picker, null);
        camerasAdapter = new DevicesAdapter(view.getContext());
		camerasAdapter.add(MADN3SController.leftCamera);
		camerasAdapter.add(MADN3SController.rightCamera);
        rightCameraSpinner = (Spinner) view.findViewById(R.id.right_camera_spinner);
        rightCameraSpinner.setAdapter(camerasAdapter);
        rightCameraSpinner.setSelection(0);
        rightCameraSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				MADN3SController.rightCamera = (BluetoothDevice) parent.getAdapter().getItem(position);
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				
			}
		});
        leftCameraSpinner = (Spinner) view.findViewById(R.id.left_camera_spinner);
        leftCameraSpinner.setAdapter(camerasAdapter);
        rightCameraSpinner.setSelection(1);
        leftCameraSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				MADN3SController.leftCamera = (BluetoothDevice) parent.getAdapter().getItem(position);
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				
			}
		});
        builder.setView(view)
               .setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int id) {
                	   mListener.onDialogPositiveClick(CameraSelectionDialogFragment.this);
                   }
               })
               .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	   mListener.onDialogNegativeClick(CameraSelectionDialogFragment.this);
                   }
               });  
        return builder.create();
    } 
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (DialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }
}
