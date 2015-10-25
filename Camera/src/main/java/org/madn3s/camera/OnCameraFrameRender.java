package org.madn3s.camera;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.content.res.Resources;

abstract class FrameRender {
    protected CameraCalibrator mCalibrator;
    protected int mWidth;
    protected int mHeight;
    protected Mat mRgba;
    protected Mat mRgbaF;
    protected Mat mRgbaT;

    public abstract Mat render(CvCameraViewFrame inputFrame);
}

class PreviewFrameRender extends FrameRender {
	public PreviewFrameRender(int width, int height) {
		mWidth = width;
		mHeight = height;
		mRgba = new Mat(height, width, CvType.CV_8UC4);
    	mRgbaF = new Mat(height, width, CvType.CV_8UC4);
    	mRgbaT = new Mat(width, width, CvType.CV_8UC4);
    }

    @Override
    public Mat render(CvCameraViewFrame inputFrame) {
    	mRgba = inputFrame.rgba();
    	Core.transpose(mRgba, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0,0, 0);
        Core.flip(mRgbaF, mRgba, 1 );

        double pX = mRgba.cols() / 3 * 2;
        double pY = mRgba.rows() * 0.1;

        Core.putText(mRgba, "Preview", new Point(pX , pY)
                , Core.FONT_HERSHEY_SIMPLEX, 1.0, new Scalar(255, 255, 0));

        return mRgba;
    }
}

class CalibrationFrameRender extends FrameRender {
	private Mat grayFrame;
    public CalibrationFrameRender(CameraCalibrator calibrator) {
        mCalibrator = calibrator;
        mWidth = mCalibrator.mWidth;
		mHeight = mCalibrator.mHeight;
        mRgba = new Mat(mHeight, mWidth, CvType.CV_8UC4);
        grayFrame = new Mat(mHeight, mWidth, CvType.CV_8UC4);
    	mRgbaF = new Mat(mHeight, mWidth, CvType.CV_8UC4);
    	mRgbaT = new Mat(mWidth, mWidth, CvType.CV_8UC4);
    }

    @Override
    public Mat render(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        grayFrame = inputFrame.gray();
        mCalibrator.processFrame(grayFrame, mRgba);

        Core.transpose(mRgba, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0,0, 0);
        Core.flip(mRgbaF, mRgba, 1 );

        double pX = mRgba.cols() / 3 * 2;
    	double pY = mRgba.rows() * 0.1;

        Core.putText(mRgba, "Captured: " + mCalibrator.getCornersBufferSize(), new Point(pX , pY)
        	, Core.FONT_HERSHEY_SIMPLEX, 1.0, new Scalar(255, 255, 0));

        return mRgba;
    }
}

class UndistortionFrameRender extends FrameRender {
    public UndistortionFrameRender(CameraCalibrator calibrator) {
        mCalibrator = calibrator;
        mWidth = mCalibrator.mWidth;
		mHeight = mCalibrator.mHeight;
        mRgbaF = new Mat(mHeight, mWidth, CvType.CV_8UC4);
    	mRgbaT = new Mat(mWidth, mWidth, CvType.CV_8UC4);
    }

    @Override
    public Mat render(CvCameraViewFrame inputFrame) {
        Mat renderedFrame = new Mat(inputFrame.rgba().size(), inputFrame.rgba().type());
        Imgproc.undistort(inputFrame.rgba(), renderedFrame,
                mCalibrator.getCameraMatrix(), mCalibrator.getDistortionCoefficients());

        Core.transpose(renderedFrame, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0,0, 0);
        Core.flip(mRgbaF, renderedFrame, 1 );

        return renderedFrame;
    }
}

class OnCameraFrameRender {
    private FrameRender mFrameRender;

    public OnCameraFrameRender(FrameRender frameRender) {
        mFrameRender = frameRender;
    }
    public Mat render(CvCameraViewFrame inputFrame) {
        return mFrameRender.render(inputFrame);
    }
}
