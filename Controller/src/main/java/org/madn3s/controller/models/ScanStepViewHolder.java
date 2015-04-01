package org.madn3s.controller.models;

import android.view.View;
import android.widget.TextView;

public class ScanStepViewHolder extends StatusViewHolder {

	private TextView action;
	
	private ScanStepViewHolder(View failure, View success, View working) {
		super(failure, success, working);
	}
	
	public ScanStepViewHolder(View failure, View success, View working, View action) {
		this(failure, success, working);
		this.action = (TextView) action;
	}
	
	public void setActionText(String text){
		if(action != null){
			action.setText(text);
		}
	}

}
