package org.madn3s.controller;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import java.util.ArrayList;

import static org.madn3s.controller.Consts.*;

public class MidgetOfSeville {
	static final String tag = MidgetOfSeville.class.getSimpleName();

	public static JSONArray calculateFrameOpticalFlow(JSONObject data, int frameIndex) throws JSONException{
		Log.d(tag, "calculateFrameOpticalFlow. Starting");
//		Log.d(tag, "calculateFrameOpticalFlow. data: " + data);
		JSONObject rightJson;
		JSONObject leftJson;
		JSONArray rightJsonArray;
		JSONArray leftJsonArray;
		String leftFilepath = null;
		String rightFilepath = null;
		Bitmap tempBitmap = null;
		Mat leftMat = null;
		Mat rightMat = null;
		ArrayList<Point> leftPoints = new ArrayList<Point>();
		ArrayList<Point> rightPoints = new ArrayList<Point>();
		MatOfPoint2f leftMop;
		MatOfPoint2f rightMop;
        JSONArray pointsJsonArr = new JSONArray();
//		vtkDataSet dataSet = new vtkDataSet();
//		dataSet
//		vtkIterativeClosestPointTransform icp = new vtkIterativeClosestPointTransform();
//		icp.SetSource(id0);

		try{
			rightJson = data.getJSONObject(SIDE_RIGHT);
			rightFilepath = rightJson.getString(KEY_FILE_PATH);
			rightJsonArray = rightJson.getJSONArray(KEY_POINTS);
			leftJson = data.getJSONObject(SIDE_LEFT);
			leftFilepath = leftJson.getString(KEY_FILE_PATH);
			leftJsonArray = leftJson.getJSONArray(KEY_POINTS);

			Log.d(tag, "calculateFrameOpticalFlow. Loading images.");
			if(rightFilepath != null){
				Log.d(tag, "calculateFrameOpticalFlow. Loading images. Right: " + rightFilepath);
				tempBitmap = loadBitmap(rightFilepath);
				int height = tempBitmap.getHeight();
				int width = tempBitmap.getWidth();

				rightMat = new Mat(height, width, CvType.CV_8UC3);
				Utils.bitmapToMat(tempBitmap, rightMat);
				Imgproc.cvtColor(rightMat, rightMat, Imgproc.COLOR_BGR2GRAY);

//				Log.d(tag, "calculateFrameOpticalFlow. rightMat: " + (rightMat == null));
			}

			if(leftFilepath != null){
				Log.d(tag, "calculateFrameOpticalFlow. Loading images. Left: " + leftFilepath);
				tempBitmap = loadBitmap(leftFilepath);
				int height = tempBitmap.getHeight();
				int width = tempBitmap.getWidth();

				leftMat = new Mat(height, width, CvType.CV_8UC3);
				Utils.bitmapToMat(tempBitmap, leftMat);
				Imgproc.cvtColor(leftMat, leftMat, Imgproc.COLOR_BGR2GRAY);

//				Log.d(tag, "calculateFrameOpticalFlow. leftMat: " + (leftMat == null));
			}

			JSONObject pointJson;
			for(int i = 0; i < rightJsonArray.length(); i++){
				pointJson = rightJsonArray.getJSONObject(i);
				rightPoints.add(new Point(pointJson.getDouble("x"), pointJson.getDouble("y")));
			}

			for(int i = 0; i < leftJsonArray.length(); i++){
				pointJson = leftJsonArray.getJSONObject(i);
				leftPoints.add(new Point(pointJson.getDouble("x"), pointJson.getDouble("y")));
			}

			leftMop = new MatOfPoint2f();
			leftMop.fromList(leftPoints);

			rightMop = new MatOfPoint2f();
            rightMop.fromList(rightPoints);

			MatOfByte opticalFlowFoundFeatures = new MatOfByte();
			MatOfFloat err = new MatOfFloat();
			TermCriteria termcrit = new TermCriteria(TermCriteria.MAX_ITER+TermCriteria.EPS, 10, 0.1);
			Size winSize = new Size(9, 9);
			int maxLevel = 5;
            double minEigThreshold = 0.0001;

			Video.calcOpticalFlowPyrLK(leftMat, rightMat, leftMop, rightMop, opticalFlowFoundFeatures,
                    err, winSize, maxLevel,termcrit, 0, minEigThreshold);

            MADN3SController.saveJsonToExternal(rightMop.dump(), "rightPoints");
            MADN3SController.saveJsonToExternal(leftMop.dump(), "leftPoints");

			byte[] statusBytes = opticalFlowFoundFeatures.toArray();
//			Log.d(tag, "lengths. leftPoints: " + leftPoints.size() + " rightPoints: " + rightPoints.size()
//					+ " status: " + statusBytes.length);

//			for(int i = 0; i < leftPoints.size(); ++i){
//				Log.d(tag, "calculateFrameOpticalFlow. status(" + String.format("%02d", i) + "): " + statusBytes[i]);
//			}

			JSONObject calibrationJson = MADN3SController.sharedPrefsGetJSONObject(KEY_CALIBRATION);
			JSONObject leftSide = calibrationJson.getJSONObject(SIDE_LEFT);
			JSONObject rightSide = calibrationJson.getJSONObject(SIDE_RIGHT);

			Mat rightCameraMatrix = MADN3SController.getMatFromString(
                    rightSide.getString(KEY_CALIB_CAMERA_MATRIX), CvType.CV_64F);
			Mat leftCameraMatrix = MADN3SController.getMatFromString(
                    leftSide.getString(KEY_CALIB_CAMERA_MATRIX), CvType.CV_64F);

			JSONArray result = new JSONArray();
			Scalar neutral = new Scalar(1,1,1);
			int last;
			int m = 4;
			int n = 3;
			Mat A = Mat.zeros(m, n, CvType.CV_64F);
			Mat wSingularValue = new Mat(m, n, CvType.CV_64F);
	        Mat uLeftOrthogonal = new Mat(m, m, CvType.CV_64F);
	        Mat vRightOrtogonal = new Mat(n, n, CvType.CV_64F);
			Mat mult = new Mat(1, n, CvType.CV_64FC2);
			Mat sub = new Mat(1, n, CvType.CV_64FC2);

            // Row temporal para resta y multipliacion en el for
            double tempRow[] = new double[4];

            Mat P1 = MADN3SController.getMatFromString(MADN3SController.sharedPrefsGetString(KEY_CALIB_P1), CvType.CV_64F);
            Mat P2 = MADN3SController.getMatFromString(MADN3SController.sharedPrefsGetString(KEY_CALIB_P2), CvType.CV_64F);

            // Rows de P1 necesarios para la multiplicacion en el for
            Mat p1Row0 = P1.row(0);
            Mat p1Row1 = P1.row(1);
            Mat p1Row2 = P1.row(2);

            // Rows de P2 necesarios para la multiplicacion en el for
            Mat p2Row0 = P2.row(0);
            Mat p2Row1 = P2.row(1);
            Mat p2Row2 = P2.row(2);

            //Punto para calculo de punto al final del for
            double tempPoint[] = new double[4];

			try{
				Log.d(tag, p1Row0.rows() + " x " + p1Row0.cols() + " = " + p1Row0.dump());
				Log.d(tag, p1Row0.get(0,0).length + " " + p1Row0.get(0, 0)[0]);
				Log.d(tag, p1Row0.get(1,0).length + " " + p1Row0.get(1, 0)[0]);
				Log.d(tag, p1Row0.get(2,0).length + " " + p1Row0.get(2, 0)[0]);
				Log.d(tag, p1Row0.get(3,0).length + " " + p1Row0.get(3, 0)[0]);
			} catch (Exception e){

			}

			try{
				Log.d(tag, p1Row0.rows() + " x " + p1Row0.cols() + " = " + p1Row0.dump());
				Log.d(tag, p1Row0.get(0,0).length + " " + p1Row0.get(0,0)[0]);
				Log.d(tag, p1Row0.get(0,1).length + " " + p1Row0.get(0,1)[0]);
				Log.d(tag, p1Row0.get(0,2).length + " " + p1Row0.get(0,2)[0]);
				Log.d(tag, p1Row0.get(0,3).length + " " + p1Row0.get(0,3)[0]);
			} catch (Exception e){
				Log.d(tag, "boom");
			}

			for(int index = 0; index < statusBytes.length; ++index){
				if(statusBytes[index] == 1 && index < leftPoints.size() && index < rightPoints.size()){
                    // TODO comentado para pruebas con codigo de Astrid
//					//Primer Row
//					Core.multiply(leftCameraMatrix.row(2), neutral, mult, leftPoints.get(index).x);
//					Core.subtract(mult, leftCameraMatrix.row(0), sub);
//					sub.copyTo(A.row(0));
//
//					//Segundo Row
//					Core.multiply(leftCameraMatrix.row(2), neutral, mult, leftPoints.get(index).y);
//					Core.subtract(mult, leftCameraMatrix.row(1), sub);
//					sub.copyTo(A.row(1));
//
//					//Tercer Row
//					Core.multiply(rightCameraMatrix.row(2), neutral, mult, rightPoints.get(index).x);
//					Core.subtract(mult, rightCameraMatrix.row(0), sub);
//					sub.copyTo(A.row(2));
//
//					//Cuarto Row
//					Core.multiply(rightCameraMatrix.row(2), neutral, mult, rightPoints.get(index).x);
//					Core.subtract(mult, rightCameraMatrix.row(1), sub);
//					sub.copyTo(A.row(3));

					Log.d(tag, "Pre Primer Row");
                    //Primer Row
                    tempRow[0] = leftPoints.get(index).x * p1Row2.get(0, 0)[0] - p1Row0.get(0, 0)[0];
                    tempRow[1] = leftPoints.get(index).x * p1Row2.get(0, 1)[0] - p1Row0.get(0, 1)[0];
                    tempRow[2] = leftPoints.get(index).x * p1Row2.get(0, 2)[0] - p1Row0.get(0, 2)[0];
//                    double test = leftPoints.get(index).x * p1Row2.get(0, 3)[0] - p1Row0.get(0, 3)[0];
                    tempRow[3] = leftPoints.get(index).x * p1Row2.get(0, 3)[0] - p1Row0.get(0, 3)[0];
                    A.put(0, 0, tempRow);
                    tempRow[0] = 0;
                    tempRow[1] = 0;
                    tempRow[2] = 0;
                    tempRow[3] = 0;
					Log.d(tag, "Post Primer Row");

					Log.d(tag, "Pre Segundo Row");
                    //Segundo Row
                    tempRow[0] = leftPoints.get(index).y * p1Row2.get(0, 0)[0] - p1Row1.get(0, 0)[0];
                    tempRow[1] = leftPoints.get(index).y * p1Row2.get(0, 1)[0] - p1Row1.get(0, 1)[0];
                    tempRow[2] = leftPoints.get(index).y * p1Row2.get(0, 2)[0] - p1Row1.get(0, 2)[0];
                    tempRow[3] = leftPoints.get(index).y * p1Row2.get(0, 3)[0] - p1Row1.get(0, 3)[0];
                    A.put(1, 0, tempRow);
                    tempRow[0] = 0;
                    tempRow[1] = 0;
                    tempRow[2] = 0;
                    tempRow[3] = 0;
					Log.d(tag, "Post Segundo Row");

					Log.d(tag, "Pre Tercer Row");
                    //Tercer Row
                    tempRow[0] = rightPoints.get(index).x * p2Row2.get(0, 0)[0] - p2Row0.get(0, 0)[0];
                    tempRow[1] = rightPoints.get(index).x * p2Row2.get(0, 1)[0] - p2Row0.get(0, 1)[0];
                    tempRow[2] = rightPoints.get(index).x * p2Row2.get(0, 2)[0] - p2Row0.get(0, 2)[0];
                    tempRow[3] = rightPoints.get(index).x * p2Row2.get(0, 3)[0] - p2Row0.get(0, 3)[0];
                    A.put(2, 0, tempRow);
                    tempRow[0] = 0;
                    tempRow[1] = 0;
                    tempRow[2] = 0;
                    tempRow[3] = 0;
					Log.d(tag, "Post Tercer Row");

					Log.d(tag, "Pre Cuarto Row");
                    //Cuarto Row
                    tempRow[0] = rightPoints.get(index).y * p2Row2.get(0, 0)[0] - p2Row1.get(0, 0)[0];
                    tempRow[1] = rightPoints.get(index).y * p2Row2.get(0, 1)[0] - p2Row1.get(0, 1)[0];
                    tempRow[2] = rightPoints.get(index).y * p2Row2.get(0, 2)[0] - p2Row1.get(0, 2)[0];
                    tempRow[3] = rightPoints.get(index).y * p2Row2.get(0, 3)[0] - p2Row1.get(0, 3)[0];
                    A.put(3, 0, tempRow);
                    tempRow[0] = 0;
                    tempRow[1] = 0;
                    tempRow[2] = 0;
                    tempRow[3] = 0;
					Log.d(tag, "Post Cuarto Row");

					Log.d(tag, "Pre SVDecomp");
			        Core.SVDecomp(A, wSingularValue, uLeftOrthogonal, vRightOrtogonal, Core.DECOMP_SVD);
					Log.d(tag, "Post SVDecomp");
					Log.d(tag, "wSingularValue(" + wSingularValue.rows() + ", " + wSingularValue.cols() + ") = " + wSingularValue.dump());
					Log.d(tag, "uLeftOrthogonal(" + uLeftOrthogonal.rows() + ", " + uLeftOrthogonal.cols() + ") = " + uLeftOrthogonal.dump());
					Log.d(tag, "vRightOrtogonal(" + vRightOrtogonal.rows() + ", " + vRightOrtogonal.cols() + ") = " + vRightOrtogonal.dump());
//wSingularValue es de 3x1
//uLeftOrthogonal es de 4x3
//vRightOrtogonal es de 3x3

//			        vRightOrtogonal = vRightOrtogonal.t();
//			        last = vRightOrtogonal.cols();
//			        Mat point = vRightOrtogonal.col(last - 1);
//			        JSONObject pointJsonResult = new JSONObject();
//			        if(point.rows() == 3){
//			        	double[] coordinates = new double[3];
//			        	point.get(0, 0, coordinates);
//			        	pointJsonResult.put(KEY_X, coordinates[0]);
//			        	pointJsonResult.put(KEY_Y, coordinates[1]);
//			        	pointJsonResult.put(KEY_Z, coordinates[2]);
//			        	result.put(pointJsonResult);
////			        	result.put(coordinates[0] + " " + coordinates[1] + " " + coordinates[2] + "\n" );
//			        } else {
//			        	Log.e(tag, "No se obtuvieron coordenadas X, Y y Z");
//			        }

//                    if(frameIndex == 0){
//                        tempPoint[0] = (vRightOrtogonal.get(0, 3)[0] / vRightOrtogonal.get(3, 3)[0]);
//                        tempPoint[1] = (vRightOrtogonal.get(1, 3)[0] / vRightOrtogonal.get(3, 3)[0]);
//                        tempPoint[2] = (vRightOrtogonal.get(2, 3)[0] / vRightOrtogonal.get(3, 3)[0]);
//                    } else {
                        JSONObject point = new JSONObject();
                        point.put(KEY_X, (vRightOrtogonal.get(0, 2)[0] / vRightOrtogonal.get(2, 2)[0]));
                        point.put(KEY_Y, (vRightOrtogonal.get(0, 2)[0] / vRightOrtogonal.get(2, 2)[0]));
                        point.put(KEY_Z, (vRightOrtogonal.get(0, 2)[0] / vRightOrtogonal.get(2, 2)[0]));
                        pointsJsonArr.put(point);
                        result.put(point);
//                    }
				}
			}

			return result;

		} catch (JSONException e){
			Log.e(tag, "calculateFrameOpticalFlow. Couldn't parse data JSONObject", e);
			return null;
		} finally {
			tempBitmap.recycle();
		}
	}

	private static Bitmap loadBitmap(String filePath){
		Log.d(tag, "loadBitmap. filePath desde MidgetOfSeville: " + filePath);
		Options options = new Options();
		options.inPreferredConfig = Config.RGB_565;
		options.inDither = true;
		Bitmap imgBitmap = BitmapFactory.decodeFile(filePath, options);
		Log.d(tag, "imgBitmap config: " + imgBitmap.getConfig().toString() + " hasAlpha: " + imgBitmap.hasAlpha());
		return imgBitmap;
	}

	public static JSONObject doStereoCalibration(){
		try{
			Log.d(tag, "doStereoCalibration. Starting");

			JSONObject calibrationJson = MADN3SController.sharedPrefsGetJSONObject(KEY_CALIBRATION);
			Log.d(tag, "doStereoCalibration. saving persisted calibrationJson.");
			MADN3SController.saveJsonToExternal(calibrationJson.toString(1), "original-calibration");

			JSONObject leftSide = calibrationJson.getJSONObject(SIDE_LEFT);
			JSONObject rightSide = calibrationJson.getJSONObject(SIDE_RIGHT);

			Log.d(tag, "doStereoCalibration. Parsing Dist Coeffs");
			Mat rightDistCoeff = MADN3SController.getMatFromString(rightSide.getString(KEY_CALIB_DISTORTION_COEFFICIENTS), CvType.CV_64F);
			Mat leftDistCoeff = MADN3SController.getMatFromString(leftSide.getString(KEY_CALIB_DISTORTION_COEFFICIENTS), CvType.CV_64F);

			Log.d(tag, "doStereoCalibration. Parsing Camera Matrix");
			Mat rightCameraMatrix = MADN3SController.getMatFromString(rightSide.getString(KEY_CALIB_CAMERA_MATRIX), CvType.CV_64F);
			Mat leftCameraMatrix = MADN3SController.getMatFromString(leftSide.getString(KEY_CALIB_CAMERA_MATRIX), CvType.CV_64F);

			Log.d(tag, "doStereoCalibration. Parsing Right Image Points");
			JSONArray rightJsonImagePoints = new JSONArray(rightSide.getString(KEY_CALIB_IMAGE_POINTS));
			ArrayList<Mat> rightImagePoints = new ArrayList<Mat>(rightJsonImagePoints.length());
			for(int i = 0; i < rightJsonImagePoints.length(); ++i){
				rightImagePoints.add(MADN3SController.getImagePointFromString(rightJsonImagePoints.getString(i)));
//				Log.d(tag, " rightImagePoints(" + i + ")[" + rightImagePoints.get(i).rows() + "][" + rightImagePoints.get(i).cols() + "]"
//						+ ":" + rightImagePoints.get(i).dump()
//						+ "  original: " + rightJsonImagePoints.getString(i));
			}

			Log.d(tag, "doStereoCalibration. Parsing Left Image Points");
			JSONArray leftJsonImagePoints = new JSONArray(leftSide.getString(KEY_CALIB_IMAGE_POINTS));
			ArrayList<Mat> leftImagePoints = new ArrayList<Mat>(leftJsonImagePoints.length());
			for(int i = 0; i < leftJsonImagePoints.length(); ++i){
				leftImagePoints.add(MADN3SController.getImagePointFromString(leftJsonImagePoints.getString(i)));
//				Log.d(tag, " leftImagePoints(" + i + ")[" + leftImagePoints.get(i).rows() + "][" + leftImagePoints.get(i).cols() + "]"
//						+ ":" + leftImagePoints.get(i).dump()
//						+ "  original: " + leftJsonImagePoints.getString(i));
			}

			Log.d(tag, "doStereoCalibration. Generating Object Points");
			int corners = leftImagePoints.size() >= rightImagePoints.size()? leftImagePoints.size(): rightImagePoints.size();
			ArrayList<Mat> objectPoints = new ArrayList<Mat>();
	        objectPoints.add(Mat.zeros(corners, 1, CvType.CV_32FC3));
	        calcBoardCornerPositions(objectPoints.get(0));
	        for (int i = 1; i < corners; i++) {
	            objectPoints.add(objectPoints.get(0));
//	            Log.d(tag, " objectPoints(" + i + ")[" + objectPoints.get(i).rows() + "][" + objectPoints.get(i).cols() + "]");
	        }

	        Size mPatternSize = new Size(4, 11);
	        Mat R = new Mat();
	        Mat T = new Mat();
	        Mat E = new Mat();
	        Mat F = new Mat();
	        double[] termCriteriaFlags = new double[3];
	        termCriteriaFlags[0] = TermCriteria.EPS+TermCriteria.MAX_ITER;
	        termCriteriaFlags[1] = 100;
	        termCriteriaFlags[2] = 1e-5;

	        Log.d(tag, "doStereoCalibration. Calibrating. objectPoints: " + objectPoints.size()
	        		+ " leftImagePoints: " + leftImagePoints.size()
	        		+ " rightImagePoints: " + rightImagePoints.size()
	        		+ " leftCameraMatrix[" + leftCameraMatrix.rows() + "][" + leftCameraMatrix.cols() + "]"
	        		+ " rightCameraMatrix[" + rightCameraMatrix.rows() + "][" + rightCameraMatrix.cols() + "]"
	        		+ " rightDistCoeff[" + rightDistCoeff.rows() + "][" + rightDistCoeff.cols() + "]"
	        		+ " leftDistCoeff[" + leftDistCoeff.rows() + "][" + leftDistCoeff.cols() + "]"
	        		);

	        Calib3d.stereoCalibrate(objectPoints, leftImagePoints, rightImagePoints
	        		, leftCameraMatrix, leftDistCoeff, rightCameraMatrix, rightDistCoeff
	        		, mPatternSize, R, T, E, F, new TermCriteria(termCriteriaFlags)
	        		, Calib3d.CALIB_FIX_ASPECT_RATIO + Calib3d.CALIB_ZERO_TANGENT_DIST + Calib3d.CALIB_SAME_FOCAL_LENGTH);

	        Log.d(tag, "doStereoCalibration. Calibration finished.");

//	        String rStr = R.dump();
//	        String tStr = T.dump();
//	        String eStr = E.dump();
//	        String fStr = F.dump();
//
//	        Log.d(tag, "R: " + rStr);
//	        Log.d(tag, "T: " + tStr);
//	        Log.d(tag, "E: " + eStr);
//	        Log.d(tag, "F: " + fStr);
//
//	        MADN3SController.sharedPrefsPutString(KEY_R, rStr);
//	        MADN3SController.sharedPrefsPutString(KEY_T, tStr);
//	        MADN3SController.sharedPrefsPutString(KEY_E, eStr);
//	        MADN3SController.sharedPrefsPutString(KEY_F, fStr);
	        Mat R1 = new Mat();
	        Mat R2 = new Mat();
	        Mat P1 = new Mat();
	        Mat P2 = new Mat();
            Mat P11 = new Mat(3, 3, CvType.CV_32FC1);
            Mat P21 = new Mat(3, 3, CvType.CV_32FC1);
	        Mat Q = new Mat();
            Mat rightMap1 = new Mat(mPatternSize, CvType.CV_32FC1);
            Mat rightMap2 = new Mat(mPatternSize, CvType.CV_32FC1);
            Mat leftMap1 = new Mat(mPatternSize, CvType.CV_32FC1);
            Mat leftMap2 = new Mat(mPatternSize, CvType.CV_32FC1);

            Log.d(tag, "Starting StereoRectify");
	        Calib3d.stereoRectify(leftCameraMatrix, leftDistCoeff
	        		, rightCameraMatrix, rightDistCoeff
	        		, mPatternSize, R, T, R1, R2, P1, P2, Q);
            MADN3SController.sharedPrefsPutString(KEY_CALIB_P1, P1.dump());
            MADN3SController.sharedPrefsPutString(KEY_CALIB_P2, P2.dump());
            Log.d(tag, "Finished StereoRectify");

            // Undistorted Right
            Log.d(tag, "Starting Right initUndistortRectifyMap");
	        Imgproc.initUndistortRectifyMap(rightCameraMatrix, rightDistCoeff, R1, P11,
                    mPatternSize, CvType.CV_32FC1, rightMap1, rightMap2);
            Log.d(tag, "Finished Right initUndistortRectifyMap");
            // Undistorted Left
            Log.d(tag, "Starting Left initUndistortRectifyMap");
	        Imgproc.initUndistortRectifyMap(leftCameraMatrix, leftDistCoeff, R2, P21,
                    mPatternSize, CvType.CV_32FC1, leftMap1, leftMap2);
            Log.d(tag, "Finished Right initUndistortRectifyMap");

//			rightSide.put(KEY_CALIB_DISTORTION_COEFFICIENTS, rightDistCoeff.dump());
//			leftSide.put(KEY_CALIB_DISTORTION_COEFFICIENTS, leftDistCoeff.dump());

            // Camera Matrix
			rightSide.put(KEY_CALIB_CAMERA_MATRIX, P11.dump());
			leftSide.put(KEY_CALIB_CAMERA_MATRIX, P21.dump());

            // Map 1
            rightSide.put(KEY_CALIB_MAP_1, rightMap1.dump());
            leftSide.put(KEY_CALIB_MAP_1, leftMap1.dump());

            // Map 2
            rightSide.put(KEY_CALIB_MAP_2, rightMap2.dump());
            leftSide.put(KEY_CALIB_MAP_2, leftMap2.dump());

            // Remove KEY_CALIB_IMAGE_POINTS
            rightSide.remove(KEY_CALIB_IMAGE_POINTS);
            leftSide.remove(KEY_CALIB_IMAGE_POINTS);

			calibrationJson.put(SIDE_RIGHT, rightSide);
            calibrationJson.put(SIDE_LEFT, leftSide);
            calibrationJson.put(KEY_CALIBRATION, true);

			Log.d(tag, "doStereoCalibration. saving modified calibrationJson.");
			MADN3SController.saveJsonToExternal(calibrationJson.toString(1), "modified-calibration");

			MADN3SController.sharedPrefsPutJSONObject(KEY_CALIBRATION, calibrationJson);

            JSONObject result = new JSONObject();
            result.put(KEY_ACTION, ACTION_RECEIVE_CALIBRATION_RESULT);
            result.put(KEY_CALIBRATION_RESULT, calibrationJson.toString());

            return result;

//		} catch (JSONException e){
//			e.printStackTrace();
		} catch (Exception e){
            e.printStackTrace();
        }

        return null;
	}

	private static void calcBoardCornerPositions(Mat corners) {
		Size mPatternSize = new Size(4, 11);
	    int mCornersSize = (int)(mPatternSize.width * mPatternSize.height);
	    double mSquareSize = 0.0181;
        final int cn = 3;
        float positions[] = new float[mCornersSize * cn];

        for (int i = 0; i < mPatternSize.height; i++) {
            for (int j = 0; j < mPatternSize.width * cn; j += cn) {
                positions[(int) (i * mPatternSize.width * cn + j + 0)] = (2 * (j / cn) + i % 2)
                		* (float) mSquareSize;
                positions[(int) (i * mPatternSize.width * cn + j + 1)] = i * (float) mSquareSize;
                positions[(int) (i * mPatternSize.width * cn + j + 2)] = 0;
            }
        }
        corners.create(mCornersSize, 1, CvType.CV_32FC3);
        corners.put(0, 0, positions);
    }

	public static void restoreBackupJson(){
		String jsonBakString = "{}";
		try {
            Log.d(tag, "Restoring json backup");
			JSONObject jsonBak = new JSONObject(jsonBakString);
			JSONArray framesJsonArray = jsonBak.getJSONArray("frames");
			JSONObject frame;
			for(int i = 0; i < framesJsonArray.length(); i++){
				frame = framesJsonArray.getJSONObject(i);
				MADN3SController.sharedPrefsPutJSONObject(FRAME_PREFIX + i, frame);
				Log.d(tag, FRAME_PREFIX + i + " = " + frame.toString());
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
