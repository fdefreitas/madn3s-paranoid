package org.madn3s.controller.viewer.models.files;

import java.io.File;
import java.util.List;

import org.madn3s.controller.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FilePickerListAdapter extends ArrayAdapter<File> {
	private List<File> mObjects;
	
	public FilePickerListAdapter(Context context, List<File> objects) {
		super(context, R.layout.file_picker_list_item, android.R.id.text1, objects);
		mObjects = objects;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		View row = null;
		
		if(convertView == null) { 
			LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(R.layout.file_picker_list_item, parent, false);
		} else {
			row = convertView;
		}

		File object = mObjects.get(position);

		ImageView imageView = (ImageView)row.findViewById(R.id.file_picker_image);
		TextView textView = (TextView)row.findViewById(R.id.file_picker_text);
		// Set single line
		textView.setSingleLine(true);
		
		textView.setText(object.getName());
		if(object.isFile()) {
			// Show the file icon
			imageView.setImageResource(R.drawable.file);
		} else {
			// Show the folder icon
			imageView.setImageResource(R.drawable.folder);
		}
		
		return row;
	}
}
