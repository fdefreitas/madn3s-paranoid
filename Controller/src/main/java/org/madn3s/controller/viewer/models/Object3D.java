package org.madn3s.controller.viewer.models;

import java.util.ArrayList;

public abstract class Object3D {
	
	public ArrayList<Drawable> figures;
	
	public Point3D min;
	public Point3D max;
	public Point3D center;
	
	public Object3D(String route) {
		figures = new ArrayList<Drawable>();
		load(route);
		init();
	}
	
	public abstract void load(String route);
	public abstract void init();
	
	public void draw(float[] mvpMatrix){
		for(Drawable f : figures){
        	f.draw(mvpMatrix);
        }
	}
	
	
}
