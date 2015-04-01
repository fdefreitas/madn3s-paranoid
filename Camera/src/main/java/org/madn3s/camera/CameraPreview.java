package org.madn3s.camera;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    protected static final String CAMERA_ORIENTATION_PORTRAIT = "portrait";
	protected static final String CAMERA_ORIENTATION_LANDSCAPE = "landscape";
	protected static final String CAMERA_PARAM_ORIENTATION = "orientation";
	private static final String tag = CameraPreview.class.getSimpleName();
	private SurfaceHolder mHolder;
    private Camera mCamera;
    private Context context;

	public CameraPreview(Context context, Camera camera) {
        super(context);
        this.context = context;
        mCamera = camera;
        boolean hasFlash = context.getPackageManager()
        		.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if(hasFlash){
            Log.d(tag, "Flash Available");
//            Camera.Parameters p = mCamera.getParameters();
//            p.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
//            mCamera.setParameters(p);
        } else {
            Log.d(tag, "No Flash Available");
        }
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

	@Override
    public void surfaceCreated(SurfaceHolder holder) {
		Log.d(tag, "surfaceCreated. ");
//        try {
//            mCamera.setPreviewDisplay(holder);
//            mCamera.startPreview();
//        } catch (Exception e) {
//            Log.d(tag, "surfaceCreated. Error setting camera preview: ", e);
//        }
    }

	@Override
    public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(tag, "surfaceDestroyed. ");
//		if(mCamera != null){
//			mCamera.stopPreview();
//			mCamera.setPreviewCallback(null);
//	        mCamera.release();
//	        mCamera = null;
//		}
    }

	@Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		Log.d(tag, "surfaceChanged. ");
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        if(mCamera.getParameters().getSupportedPreviewSizes() != null){
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;
           // int pos = mCamera.getParameters().getSupportedPreviewSizes().size() - 1;
            Camera.Size previewSize = getOptimalPreviewSize(mCamera.getParameters().getSupportedPreviewSizes(), width, height);
//            for(Camera.Size sizeE : mCamera.getParameters().getSupportedPreviewSizes()){
//                Log.d("CameraPreview", "height: " + sizeE.height + " width: " + sizeE.width);
//            }

            Log.d(tag, "Selected height: " + previewSize.height + " width: " + previewSize.width);
            Log.d(tag, "Screen Size height: " + height + " width: " + width);

            mHolder.setFixedSize(previewSize.width, previewSize.height);
        }

//        Log.d(MADN3SCamera.TAG, "Rotation Before : " + mCamera.getParameters().get("rotation"));

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
       //     mCamera.setDisplayOrientation(90);
            Camera.Parameters p = mCamera.getParameters();
            p.set(CAMERA_PARAM_ORIENTATION, CAMERA_ORIENTATION_PORTRAIT);
            mCamera.setParameters(p);
        }else{
            Log.d(MADN3SCamera.TAG, "Landscape " 
            		+ (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE));
            mCamera.getParameters().set(CAMERA_PARAM_ORIENTATION, CAMERA_ORIENTATION_LANDSCAPE);
            mCamera.getParameters().setRotation(0);
        }

        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
            mCamera.getParameters().setFlashMode(Camera.Parameters.FLASH_MODE_ON);
            Log.d(MADN3SCamera.TAG, "FlashMode: "+mCamera.getParameters().getFlashMode());
        } catch (Exception e) {
            Log.d(MADN3SCamera.TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    /**
     * Returns optimal PreviewSize by iterating available sizes from 
     * camera hardware
     */
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int width, int heigth) {
        
    	final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double)(heigth / width);

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = heigth;
//        Log.d(MADN3SCamera.TAG, " minDiff " + minDiff + " targetHeight " + targetHeight + " targetRatio " + targetRatio + " ASPECT_TOLERANCE " + ASPECT_TOLERANCE);
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
//            Log.d(MADN3SCamera.TAG, "size.width " + size.width + " size.height " + size.height + " ratio " + ratio + " Math.abs(ratio - targetRatio) " + Math.abs(ratio - targetRatio));
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE){
                continue;
            }
//            Log.d(MADN3SCamera.TAG, "Math.abs(size.height - targetHeight) " + Math.abs(size.height - targetHeight));
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
//        Log.d(MADN3SCamera.TAG, "optimalSize " + (optimalSize==null?"no":"yes"));
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
//        Log.d(MADN3SCamera.TAG, "optimalSize " + (optimalSize==null?"no":"yes"));
        return optimalSize;
    }
    
    public Camera getmCamera() {
		return mCamera;
	}
}