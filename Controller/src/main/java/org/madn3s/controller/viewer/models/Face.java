package org.madn3s.controller.viewer.models;

import java.util.ArrayList;

public class Face{
	private ArrayList<Integer> indexes;
	
	@SuppressWarnings("unchecked")
	public Face(ArrayList<Integer> indexes) {
		this.indexes = (ArrayList<Integer>) indexes.clone();
	}
	
	public ArrayList<Integer> getIndexes() {
		return indexes;
	}
	
	public int getNVertex(){
		return indexes.size();
	}
	
	@Override
	public String toString() {
		StringBuilder toReturn = new StringBuilder();
		toReturn.append("{\"vertex\":\"");
		toReturn.append(indexes.size());
		toReturn.append("\",");
		for(int i = 0; i < indexes.size(); ++i){
			toReturn.append("\"");
			toReturn.append(i);
			toReturn.append("\":\"");
			toReturn.append(indexes.get(i));
			toReturn.append("\"");
			if(i != indexes.size()-1) toReturn.append(",");
		}
		toReturn.append("}");
		return toReturn.toString();
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new Face(indexes);
	}
	
	@SuppressWarnings("unchecked")
	public void setIndexes(ArrayList<Integer> indexes) {
		this.indexes = (ArrayList<Integer>) indexes.clone();
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Face)) return false;
		Face other = (Face)o;
		if(other.getNVertex() != indexes.size()) return false;
		ArrayList<Integer> oIndexes = other.getIndexes();
		for(int i : oIndexes){
			if(!indexes.contains(i)) return false;
		}
		return true;
	}

	
	
}
