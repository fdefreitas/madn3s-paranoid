package org.madn3s.controller.viewer.models;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import android.os.Environment;
import android.util.Log;

public class Off extends Object3D{
	
	public ArrayList<Point3D> vertex;
	
	public ArrayList<Face> faces;
	
	public Off(String route) {
		super(route);
	}
	
	public void init(){
		for(Face f : faces){
			float[] face = getFace(f);
			Drawable d;
		/*	for(int i = 1; i < face.length; ++i){
				if((i + 1) < face.length){
					float[] faux = new float[3];
					faux[0] = face[0];
					faux[1] = face[i];
					faux[2] = face[i + 1];
					d = new Triangle(face);
					figures.add(d);
				}else{
					break;
				}
			}*/
			if(f.getNVertex() == 3){
				d = new Triangle(face);
			} else {
				d = new Square(face);
			}
			figures.add(d);/**/
		}
		vertex.clear();
		faces.clear();
	}
	
	public void load(String route){
		File sdcard = Environment.getExternalStorageDirectory();
		File file = new File(sdcard, route);
		BufferedReader br = null;
		boolean first = true;
		try {
		    br = new BufferedReader(new FileReader(file));
		    String line;
		    line = br.readLine();
		    line = br.readLine();
		    String[] split = line.split(" ");
		    int nVertex = Integer.parseInt(split[0]);
		    int nFaces = Integer.parseInt(split[1]);
		    int nEdges = Integer.parseInt(split[2]);
		    vertex = new ArrayList<Point3D>(nVertex);
		    faces = new ArrayList<Face>(nFaces);
		    for(int i = 0; i < nVertex; ++i){
		    	line = br.readLine();
		    	String[] splitAux = line.split(" ");
		    	Point3D p = new Point3D(Float.parseFloat(splitAux[0]), Float.parseFloat(splitAux[1]), Float.parseFloat(splitAux[2]));
		    	if(first){
		    		min = (Point3D) p.clone();
		    		max = (Point3D) p.clone();
		    		first = false;
		    	} else {
		    		if(p.getX() < min.getX()) min.setX(p.getX());
					if(p.getY() < min.getY()) min.setY(p.getY());
					if(p.getZ() < min.getZ()) min.setZ(p.getZ());
					if(p.getX() > max.getX()) max.setX(p.getX());
					if(p.getY() > max.getY()) max.setY(p.getY());
					if(p.getZ() > max.getZ()) max.setZ(p.getZ());
		    	}
		    	vertex.add(p);
		    }
		    for(Point3D paux : vertex){
		    	paux.normalize(max);
		    }
		    min.normalize(max);
		    max.normalize(max);
		    center = new Point3D(((min.getX() + max.getX())/2), ((min.getY() + max.getY())/2), ((min.getZ() + max.getZ())/2));
		    for(Point3D paux : vertex){
		    	paux.setX(paux.getX() - center.getX());
		    	paux.setY(paux.getY() - center.getY());
		    	paux.setZ(paux.getZ() - center.getZ());
		    }
		    min.setX(min.getX() - center.getX());
		    min.setY(min.getY() - center.getY());
		    min.setZ(min.getZ() - center.getZ());
		    max.setX(max.getX() - center.getX());
		    max.setY(max.getY() - center.getY());
		    max.setZ(max.getZ() - center.getZ());
		    for(int i = 0; i < nFaces; ++i){
		    	line = br.readLine();
		    	String[] splitAux = line.split(" ");
		    	ArrayList<Integer> indexes = new ArrayList<Integer>(Integer.parseInt(splitAux[0]));
		    	for(int j = 1; j < splitAux.length; ++j){
		    		indexes.add(Integer.parseInt(splitAux[j]));
		    	}
		    	Face f = new Face(indexes);
		    	indexes.clear();
		    	faces.add(f);
		    }
		    for(int i = 0; i < nEdges && ((line = br.readLine()) != null); ++i){
		    	Log.e("Edges"+i, line);
		    }
		} catch (Exception e) {
			Log.e("Off", e.getMessage());
		} finally {
			try{br.close();} catch (Exception e){}
		}
	}
	
	public float[] getFace(Face f){
		float[] toReturn = new float[f.getNVertex() * Constants.COORDS_PER_VERTEX];
		ArrayList<Integer> indexes = f.getIndexes();
		int vIndex = 0;
		for(int i : indexes){
			float[] p = vertex.get(i).toArray();
			for(int j = 0; j < p.length; ++j){
				toReturn[vIndex++] = p[j];
			}
		}
		return toReturn;
	}
}
