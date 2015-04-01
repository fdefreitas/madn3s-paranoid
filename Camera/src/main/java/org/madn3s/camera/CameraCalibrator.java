package org.madn3s.camera;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.util.Log;

public class CameraCalibrator {
	private static final String tag = "CameraCalibrator";
	private final Size mPatternSize = new Size(8, 10);
	private final int mCornersSize = (int)(mPatternSize.width * mPatternSize.height);
	private MatOfPoint2f mCorners = new MatOfPoint2f();
	private double mSquareSize = 0.0181;
	private Bitmap tempBitmap = null;
	private Mat mat = null;
	
	private Mat mCameraMatrix = new Mat();
    private Mat mDistortionCoefficients = new Mat();
    
    private Size mImageSize;
    private int mFlags;
    
    private List<Mat> mCornersBuffer = new ArrayList<Mat>();
    private double mRms;
    private boolean mIsCalibrated = false;
	
	public CameraCalibrator(String file){
		
		Log.d(tag, "file = " + file);
		
		tempBitmap = loadBitmap(file);
		int height = tempBitmap.getHeight();
		int width = tempBitmap.getWidth();
		
		mat = new Mat(height, width, CvType.CV_8UC3);
		Utils.bitmapToMat(tempBitmap, mat);
		Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
		
		Bitmap rightGrayBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.RGB_565);
		Utils.matToBitmap(mat, rightGrayBitmap);
		MADN3SCamera.saveBitmapAsJpeg(rightGrayBitmap, "board");//Controller.saveBitmapAsPng(rightGrayBitmap, "rightGray");
		
		mImageSize = new Size(width, height);
        mFlags = Calib3d.CALIB_FIX_PRINCIPAL_POINT +
                 Calib3d.CALIB_ZERO_TANGENT_DIST +
                 Calib3d.CALIB_FIX_ASPECT_RATIO +
                 Calib3d.CALIB_FIX_K4 +
                 Calib3d.CALIB_FIX_K5;
        Mat.eye(3, 3, CvType.CV_64FC1).copyTo(mCameraMatrix);
        mCameraMatrix.put(0, 0, 1.0);
        Mat.zeros(5, 1, CvType.CV_64FC1).copyTo(mDistortionCoefficients);
    }
	
	
	
	public void calibrate(){
		boolean found = Calib3d.findChessboardCorners(mat, mPatternSize, mCorners
				, Calib3d.CALIB_CB_ADAPTIVE_THRESH | Calib3d.CALIB_CB_FAST_CHECK 
					| Calib3d.CALIB_CB_NORMALIZE_IMAGE);
//		boolean found = Calib3d.findCirclesGridDefault(mat, mPatternSize, mCorners, Calib3d.CALIB_CB_ASYMMETRIC_GRID);
		if(found){
			ArrayList<Mat> rvecs = new ArrayList<Mat>();
	        ArrayList<Mat> tvecs = new ArrayList<Mat>();
	        Mat reprojectionErrors = new Mat();
	        ArrayList<Mat> objectPoints = new ArrayList<Mat>();
	        objectPoints.add(Mat.zeros(mCornersSize, 1, CvType.CV_32FC3));
	        calcBoardCornerPositions(objectPoints.get(0));
	        for (int i = 1; i < mCornersBuffer.size(); i++) {
	            objectPoints.add(objectPoints.get(0));
	        }
	        Calib3d.calibrateCamera(objectPoints, mCornersBuffer, mImageSize, mCameraMatrix, mDistortionCoefficients, rvecs, tvecs, mFlags);
	        mIsCalibrated = Core.checkRange(mCameraMatrix) && Core.checkRange(mDistortionCoefficients);
	        mRms = computeReprojectionErrors(objectPoints, rvecs, tvecs, reprojectionErrors);
	        Log.d(tag, String.format("Average re-projection error: %f", mRms));
	        Log.d(tag, "Camera matrix: " + mCameraMatrix.dump());
	        Log.d(tag, "Distortion coefficients: " + mDistortionCoefficients.dump());
		} else {
			Log.d(tag, "Missing pattern");
		}
		drawPoints(mat, found);
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
	
	private Bitmap loadBitmap(String filePath){
		Log.d(tag, "loadBitmap. filePath desde MidgetOfSeville: " + filePath);
		Options options = new Options();
		options.inPreferredConfig = Config.RGB_565;
		options.inDither = true;
		Bitmap imgBitmap = BitmapFactory.decodeFile(filePath, options);
		Log.d(tag, "imgBitmap config: " + imgBitmap.getConfig().toString() + " hasAlpha: " + imgBitmap.hasAlpha());
		return imgBitmap;
	}
	
	private void drawPoints(Mat rgbaFrame, boolean mPatternWasFound) {
        Calib3d.drawChessboardCorners(rgbaFrame, mPatternSize, mCorners, mPatternWasFound);
        Bitmap drawChessBitmap = Bitmap.createBitmap(rgbaFrame.cols(), rgbaFrame.rows(), Bitmap.Config.RGB_565);
		Utils.matToBitmap(rgbaFrame, drawChessBitmap);
        MADN3SCamera.saveBitmapAsJpeg(drawChessBitmap, "draw_chess");
    }
}