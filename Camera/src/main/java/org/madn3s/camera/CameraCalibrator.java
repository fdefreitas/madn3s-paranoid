package org.madn3s.camera;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Size;

import android.R.fraction;
import android.R.integer;
import android.os.Bundle;
import android.util.Log;

public class CameraCalibrator {

    private static final String TAG = "CameraCalibrator";
    private static final String KEY_CALIB_DISTORTION_COEFFICIENTS = "calib_distortion_coefficients";
    private static final String KEY_CALIB_CAMERA_MATRIX = "calib_camera_matrix";
    private static final String KEY_CALIB_IMAGE_POINTS = "calib_image_points";

    private final Size mPatternSize = new Size(4, 11);
    private final int mCornersSize = (int)(mPatternSize.width * mPatternSize.height);
    private boolean mPatternWasFound = false;
    private MatOfPoint2f mCorners = new MatOfPoint2f();
    private List<Mat> mCornersBuffer = new ArrayList<Mat>();
    private boolean mIsCalibrated = false;

    private Mat mCameraMatrix = new Mat();
    private Mat mDistortionCoefficients = new Mat();
    private int mFlags;
    private double mRms;
    private double mSquareSize = 0.0181;
    private Size mImageSize;

    public int mWidth;
    public int mHeight;

    public static MainActivity mActivity;
    private Mat distortionCoefficients;

    public CameraCalibrator(int width, int height) {
        mImageSize = new Size(width, height);
        mFlags = Calib3d.CALIB_FIX_PRINCIPAL_POINT +
                Calib3d.CALIB_ZERO_TANGENT_DIST +
                Calib3d.CALIB_FIX_ASPECT_RATIO +
                Calib3d.CALIB_FIX_K4 +
                Calib3d.CALIB_FIX_K5;
        Mat.eye(3, 3, CvType.CV_64FC1).copyTo(mCameraMatrix);
        mCameraMatrix.put(0, 0, 1.0);
        Mat.zeros(5, 1, CvType.CV_64FC1).copyTo(mDistortionCoefficients);
        mWidth = width;
        mHeight = height;
        Log.d(TAG, "Instantiated new " + this.getClass());
    }

    public void processFrame(Mat grayFrame, Mat rgbaFrame) {
        findPattern(grayFrame);
        renderFrame(rgbaFrame);
    }

    public Bundle calibrate() {
        ArrayList<Mat> rvecs = new ArrayList<Mat>();
        ArrayList<Mat> tvecs = new ArrayList<Mat>();
        Mat reprojectionErrors = new Mat();
        ArrayList<Mat> objectPoints = new ArrayList<Mat>();
        objectPoints.add(Mat.zeros(mCornersSize, 1, CvType.CV_32FC3));
        calcBoardCornerPositions(objectPoints.get(0));

        for (int i = 1; i < mCornersBuffer.size(); i++) {
            objectPoints.add(objectPoints.get(0));
        }

        Calib3d.calibrateCamera(objectPoints, mCornersBuffer, mImageSize,
                mCameraMatrix, mDistortionCoefficients, rvecs, tvecs, mFlags);

        mIsCalibrated = Core.checkRange(mCameraMatrix) && Core.checkRange(mDistortionCoefficients);

        mRms = computeReprojectionErrors(objectPoints, rvecs, tvecs, reprojectionErrors);
        Log.d(TAG, String.format("Average re-projection error: %f", mRms));
        Log.d(TAG, "Camera matrix: " + mCameraMatrix.dump());
        Log.d(TAG, "Distortion coefficients: " + mDistortionCoefficients.dump());
        Log.d(TAG, "ObjectPoints: " + objectPoints.size());
        for(Mat objectPoint : objectPoints){
            Log.d(TAG, "ObjectPoint: " + objectPoint.dump());
        }

        Bundle bundle = new Bundle();
        bundle.putString(KEY_CALIB_DISTORTION_COEFFICIENTS, mDistortionCoefficients.dump());
        bundle.putString(KEY_CALIB_CAMERA_MATRIX, mCameraMatrix.dump());
        JSONArray imagePointsArray = new JSONArray();
        Mat imagePoint;
        for(int i = 0; i < mCornersBuffer.size(); ++i){
            imagePoint = mCornersBuffer.get(i);
            imagePointsArray.put(imagePoint.dump());
            Log.d(TAG, " imagePoints(" + i + ")[" + imagePoint.rows() + "][" + imagePoint.cols() + "] = "
                    + imagePoint.dump());
        }
        bundle.putString(KEY_CALIB_IMAGE_POINTS, imagePointsArray.toString());

        return bundle;
    }

    public void clearCorners() {
        mCornersBuffer.clear();
    }

    private void calcBoardCornerPositions(Mat corners) {
        final int cn = 3;
        float positions[] = new float[mCornersSize * cn];

        for (int i = 0; i < mPatternSize.height; i++) {
            for (int j = 0; j < mPatternSize.width * cn; j += cn) {
                positions[(int) (i * mPatternSize.width * cn + j + 0)] = (2 * (j / cn) + i % 2) * (float) mSquareSize;
                positions[(int) (i * mPatternSize.width * cn + j + 1)] = i * (float) mSquareSize;
                positions[(int) (i * mPatternSize.width * cn + j + 2)] = 0;
            }
        }
        corners.create(mCornersSize, 1, CvType.CV_32FC3);
        corners.put(0, 0, positions);
    }

    private double computeReprojectionErrors(List<Mat> objectPoints,
                                             List<Mat> rvecs, List<Mat> tvecs, Mat perViewErrors) {
        MatOfPoint2f cornersProjected = new MatOfPoint2f();
        double totalError = 0;
        double error;
        float viewErrors[] = new float[objectPoints.size()];

        MatOfDouble distortionCoefficients = new MatOfDouble(mDistortionCoefficients);
        int totalPoints = 0;
        for (int i = 0; i < objectPoints.size(); i++) {
            MatOfPoint3f points = new MatOfPoint3f(objectPoints.get(i));
            Calib3d.projectPoints(points, rvecs.get(i), tvecs.get(i),
                    mCameraMatrix, distortionCoefficients, cornersProjected);
            error = Core.norm(mCornersBuffer.get(i), cornersProjected, Core.NORM_L2);

            int n = objectPoints.get(i).rows();
            viewErrors[i] = (float) Math.sqrt(error * error / n);
            totalError  += error * error;
            totalPoints += n;
        }
        perViewErrors.create(objectPoints.size(), 1, CvType.CV_32FC1);
        perViewErrors.put(0, 0, viewErrors);

        return Math.sqrt(totalError / totalPoints);
    }

    private void findPattern(Mat grayFrame) {
        mPatternWasFound = Calib3d.findCirclesGridDefault(grayFrame, mPatternSize, mCorners
                , Calib3d.CALIB_CB_ASYMMETRIC_GRID);
//        mPatternWasFound = Calib3d.findChessboardCorners(grayFrame, mPatternSize, mCorners
//        		, Calib3d.CALIB_CB_ASYMMETRIC_GRID);
    }

    public void addCorners() {
        if (mPatternWasFound) {
            mCornersBuffer.add(mCorners.clone());
        }
    }

    private void drawPoints(Mat rgbaFrame) {
        Calib3d.drawChessboardCorners(rgbaFrame, mPatternSize, mCorners, mPatternWasFound);
    }

    private void renderFrame(Mat rgbaFrame) {
        drawPoints(rgbaFrame);
//        Movido a OnCameraFramRender
//    	double pX = rgbaFrame.cols() / 3 * 2;
//    	double pY = rgbaFrame.rows() * 0.1;
//
//        Core.putText(rgbaFrame, "Captured: " + mCornersBuffer.size(), new Point(pX , pY)
//        	, Core.FONT_HERSHEY_SIMPLEX, 1.0, new Scalar(255, 255, 0));
    }

    public Mat getCameraMatrix() {
        return mCameraMatrix;
    }

    public void setCameraMatrix(Mat cameraMatrix) {
        mCameraMatrix = cameraMatrix;
    }

    public Mat getDistortionCoefficients() {
        return mDistortionCoefficients;
    }

    public int getCornersBufferSize() {
        return mCornersBuffer.size();
    }

    public double getAvgReprojectionError() {
        return mRms;
    }

    public boolean isCalibrated() {
        return mIsCalibrated;
    }

    public void setCalibrated() {
        mIsCalibrated = true;
    }

    public void setDistortionCoefficients(Mat distortionCoefficients) {
        this.distortionCoefficients = distortionCoefficients;
    }
}
