package org.madn3s.controller.viewer.models;

import android.opengl.GLES20;
import android.util.Log;

import  org.madn3s.controller.viewer.opengl.MidgetAngelo;

public class Triangle extends Drawable{

    public Triangle(float coords[], float color[]) {
    	super(coords, color);
    }
    
    public Triangle(float coords[]) {
    	super(coords);
    	color[0] = Math.abs(normal.getX());
    	color[1] = Math.abs(normal.getY());
    	color[2] = Math.abs(normal.getZ());
    	color[3] = 1.0f;
    	Log.e("normal", normal.toString());
    }
    
    public void setCenter(){
    	float aux;
    	aux = (coords[0] + coords[3] + coords[6])/3;
    	center.setX(aux);
		aux = (coords[1] + coords[4] + coords[7])/3;
		center.setY(aux);
		aux = (coords[2] + coords[5] + coords[8])/3;
		center.setZ(aux);
    }
    
    public void setNormal(){
    	Point3D u;
		Point3D v;
		u = new Point3D();
		v = new Point3D();
		u.setX(coords[3] - coords[0]);
		u.setY(coords[4] - coords[1]);
		u.setZ(coords[5] - coords[2]);
		v.setX(coords[6] - coords[0]);
		v.setY(coords[7] - coords[1]);
		v.setZ(coords[8] - coords[2]);
		normal = new Point3D();
		normal.setX(u.getY()*v.getZ() - v.getY()*u.getZ());
		normal.setY(-u.getX()*v.getZ() + v.getX()*u.getZ());
		normal.setZ(u.getX()*v.getY() - v.getX()*u.getY());
		float magnitude = (normal.getX() * normal.getX()) + (normal.getY() * normal.getY()) + (normal.getZ() * normal.getZ());
		magnitude = (float) Math.sqrt(magnitude);
		normal.setX(normal.getX() / magnitude);
		normal.setY(normal.getY() / magnitude);
		normal.setZ(normal.getZ() / magnitude);
	}
    

    public void draw(float[] mvpMatrix) {
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, Constants.COORDS_PER_VERTEX,
                                     GLES20.GL_FLOAT, false,
                                     vertexStride, vertexBuffer);

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        MidgetAngelo.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        MidgetAngelo.checkGlError("glUniformMatrix4fv");

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}