package org.madn3s.controller.models;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.madn3s.controller.R;

import java.util.ArrayList;

/**
 * Created by inaki on 1/11/14.
 */
public class NewDevicesAdapter extends BaseAdapter {

    ArrayList<BluetoothDevice> devices;
    Context mContext;

    public NewDevicesAdapter(Context mContext) {
        this.mContext = mContext;
        devices = new ArrayList<BluetoothDevice>();
    }

    @SuppressWarnings("unchecked")
	public NewDevicesAdapter(ArrayList<BluetoothDevice> devices, Context mContext) {
        this(mContext);
        this.devices = (ArrayList<BluetoothDevice>) devices.clone();
    }

    public void add (BluetoothDevice device){
        devices.add(device);
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public Object getItem(int position) {
        return devices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return devices.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) convertView = ((LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.row_device, parent, false);

        assert convertView != null;
        TextView name = (TextView) convertView.findViewById(R.id.device_name_textView);
        name.setText(devices.get(position).getName());
        TextView address = (TextView) convertView.findViewById(R.id.device_address_textView);
        address.setText(devices.get(position).getAddress());

        return convertView;
    }
}
