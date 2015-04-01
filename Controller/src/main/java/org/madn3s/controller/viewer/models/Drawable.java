package org.madn3s.controller.viewer.models;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.opengl.GLES20;

import org.madn3s.controller.viewer.opengl.MidgetAngelo;

public abstract class Drawable {
	public float coords[];
	public float color[] = {0.0f, 1.0f, 0.0f, 1.0f};
	public int vertex;
	
	public int vertexCount;
	public int vertexStride;
	
	public FloatBuffer vertexBuffer;
	public int mProgram;
	public int mPositionHandle;
	public int mColorHandle;
	
	public int mMVPMatrixHandle;
	
	Point3D center;
	Point3D normal;
	
	public Drawable(float coords[], float color[]) {
		this.coords = coords;
		this.color = color;
		center = new Point3D();
		normal = new Point3D();
		setCenter();
		setNormal();
		init();
	}
	
	public Drawable(float coords[]) {
		this.coords = coords;
		center = new Point3D();
		normal = new Point3D();
		setCenter();
		setNormal();
		init();
	}
	
	public float[] getColor() {
		return color;
	}
	
	public void setColor(float[] color) {
		this.color = color;
	}
	
	public void init(){
		vertexCount = coords.length / Constants.COORDS_PER_VERTEX;
        vertexStride = Constants.COORDS_PER_VERTEX * 4;
		ByteBuffer bb = ByteBuffer.allocateDirect(this.coords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(this.coords);
        vertexBuffer.position(0);
        int vertexShader = MidgetAngelo.loadShader(GLES20.GL_VERTEX_SHADER, Constants.vertexShaderCode);
        int fragmentShader = MidgetAngelo.loadShader(GLES20.GL_FRAGMENT_SHADER, Constants.fragmentShaderCode);
        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);
	}
	
	public abstract void draw(float[] mvpMatrix);
	public abstract void setCenter();
	public abstract void setNormal();
}
