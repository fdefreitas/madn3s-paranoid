package org.madn3s.controller.viewer.opengl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

public class Midgetci extends GLSurfaceView {
	
	private final MidgetAngelo mRenderer;
    public String offFile;
    public Midgetci(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        mRenderer = new MidgetAngelo();
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }
    
    public Midgetci(Context context, String off) {
        super(context);
        setEGLContextClientVersion(2);
        mRenderer = new MidgetAngelo();
        mRenderer.offFile = off;
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private float mPreviousX;
    private float mPreviousY;

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:
            	float dx = x - mPreviousX;
                float dy = y - mPreviousY;
                if (y > getHeight() / 2) {
                  dx = dx * -1 ;
                }
                if (x < getWidth() / 2) {
                  dy = dy * -1 ;
                }
                mRenderer.mAngle += (dx + dy) * TOUCH_SCALE_FACTOR;  // = 180.0f / 320
                requestRender();
        }
        mPreviousX = x;
        mPreviousY = y;
        return true;
    }

}
