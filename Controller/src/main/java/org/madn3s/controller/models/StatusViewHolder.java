package org.madn3s.controller.models;

import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class StatusViewHolder {
	private ImageView failure;
	private ImageView success;
	private ProgressBar working;
	
	public StatusViewHolder(View failure, View success, View working){
		this.success = (ImageView) success;
		this.failure = (ImageView) failure;
		this.working = (ProgressBar) working;
	}
	
	public void failure(){
		if(failure != null){
			failure.setVisibility(View.VISIBLE);
		}
		if(working != null){
			working.setVisibility(View.GONE);
		}
		if(success != null){
			success.setVisibility(View.GONE);
		}
	}
	
	public void working(){
		if(failure != null){
			failure.setVisibility(View.GONE);
		}
		if(working != null){
			working.setVisibility(View.VISIBLE);
		}
		if(success != null){
			success.setVisibility(View.GONE);
		}
	}
	
	public void success(){
		if(failure != null){
			failure.setVisibility(View.GONE);
		}
		if(working != null){
			working.setVisibility(View.GONE);
		}
		if(success != null){
			success.setVisibility(View.VISIBLE);
		}
	}
	
	public void hide(){
		if(failure != null){
			failure.setVisibility(View.GONE);
		}
		if(working != null){
			working.setVisibility(View.GONE);
		}
		if(success != null){
			success.setVisibility(View.GONE);
		}
	}
}
