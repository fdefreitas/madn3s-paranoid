package org.madn3s.controller.viewer.models;

public class Point3D {
	private float x;
	private float y;
	private float z;
	
	public Point3D(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Point3D() {
		this.x = this.y = this.z = 0;
	}
	
	public float getX() {
		return x;
	}
	
	public float getY() {
		return y;
	}
	
	public float getZ() {
		return z;
	}
	
	public void setX(float x) {
		this.x = x;
	}
	
	public void setY(float y) {
		this.y = y;
	}
	
	public void setZ(float z) {
		this.z = z;
	}
	
	public float[] toArray(){
		float toReturn[] = new float[3];
		toReturn[0] = x;
		toReturn[1] = y;
		toReturn[2] = z;
		return toReturn;
	}
	
	@Override
	public String toString() {
		return "{\"x\" : \""+ x +"\",\"y\" : \""+ y +"\",\"z\" : \""+ z +"\"}";
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new Point3D(x, y, z);
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Point3D)) return false;
		Point3D other = (Point3D)o;
		if(other.getX() != x) return false;
		if(other.getY() != y) return false;
		if(other.getZ() != z) return false;
		return true;
	}
	
	public void normalize(Point3D p){
		x = x / p.getX();
		y = y / p.getY();
		z = z / p.getZ();
	}

}
