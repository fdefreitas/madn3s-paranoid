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

	public static JSONArray calculateFrameOpticalFlow(JSONObject data) throws JSONException{
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

			MatOfByte opticalFlowFoundFeatures = new MatOfByte();
			MatOfFloat err = new MatOfFloat();
//			TermCriteria termcrit = new TermCriteria(TermCriteria.MAX_ITER+TermCriteria.EPS, 20, 0.03);
//			Size winSize = new Size(10, 10);
//			int maxLevel = 8;

			Video.calcOpticalFlowPyrLK(leftMat, rightMat, leftMop, rightMop, opticalFlowFoundFeatures, err);

			byte[] statusBytes = opticalFlowFoundFeatures.toArray();
//			Log.d(tag, "lengths. leftPoints: " + leftPoints.size() + " rightPoints: " + rightPoints.size()
//					+ " status: " + statusBytes.length);

//			for(int i = 0; i < leftPoints.size(); ++i){
//				Log.d(tag, "calculateFrameOpticalFlow. status(" + String.format("%02d", i) + "): " + statusBytes[i]);
//			}

			JSONObject calibrationJson = MADN3SController.sharedPrefsGetJSONObject(KEY_CALIBRATION);
			JSONObject leftSide = calibrationJson.getJSONObject(SIDE_LEFT);
			JSONObject rightSide = calibrationJson.getJSONObject(SIDE_RIGHT);

			Mat rightCameraMatrix = MADN3SController.getMatFromString(rightSide.getString(KEY_CALIB_CAMERA_MATRIX));
			Mat leftCameraMatrix = MADN3SController.getMatFromString(leftSide.getString(KEY_CALIB_CAMERA_MATRIX));

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

			for(int index = 0; index < statusBytes.length; ++index){
				if(statusBytes[index] == 1 && index < leftPoints.size() && index < rightPoints.size()){
					//Primer Row
					Core.multiply(leftCameraMatrix.row(2), neutral, mult, leftPoints.get(index).x);
					Core.subtract(mult, leftCameraMatrix.row(0), sub);
					sub.copyTo(A.row(0));

					//Segundo Row
					Core.multiply(leftCameraMatrix.row(2), neutral, mult, leftPoints.get(index).y);
					Core.subtract(mult, leftCameraMatrix.row(1), sub);
					sub.copyTo(A.row(1));

					//Tercer Row
					Core.multiply(rightCameraMatrix.row(2), neutral, mult, rightPoints.get(index).x);
					Core.subtract(mult, rightCameraMatrix.row(0), sub);
					sub.copyTo(A.row(2));

					//Cuarto Row
					Core.multiply(rightCameraMatrix.row(2), neutral, mult, rightPoints.get(index).x);
					Core.subtract(mult, rightCameraMatrix.row(1), sub);
					sub.copyTo(A.row(3));

			        Core.SVDecomp(A, wSingularValue, uLeftOrthogonal, vRightOrtogonal, Core.DECOMP_SVD);

			        vRightOrtogonal = vRightOrtogonal.t();
			        last = vRightOrtogonal.cols();
			        Mat point = vRightOrtogonal.col(last - 1);
			        JSONObject pointJsonResult = new JSONObject();
			        if(point.rows() == 3){
			        	double[] coordinates = new double[3];
			        	point.get(0, 0, coordinates);
			        	pointJsonResult.put(KEY_X, coordinates[0]);
			        	pointJsonResult.put(KEY_Y, coordinates[1]);
			        	pointJsonResult.put(KEY_Z, coordinates[2]);
			        	result.put(pointJsonResult);
//			        	result.put(coordinates[0] + " " + coordinates[1] + " " + coordinates[2] + "\n" );
			        } else {
			        	Log.e(tag, "No se obtuvieron coordenadas X, Y y Z");
			        }
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

	public static void doStereoCalibration(){
		try{
			Log.d(tag, "doStereoCalibration. Starting");

			JSONObject calibrationJson = MADN3SController.sharedPrefsGetJSONObject(KEY_CALIBRATION);
			Log.d(tag, "doStereoCalibration. saving persisted calibrationJson.");
			MADN3SController.saveJsonToExternal(calibrationJson.toString(1), "original-calibration");

			JSONObject leftSide = calibrationJson.getJSONObject(SIDE_LEFT);
			JSONObject rightSide = calibrationJson.getJSONObject(SIDE_RIGHT);

			Log.d(tag, "doStereoCalibration. Parsing Dist Coeffs");
			Mat rightDistCoeff = MADN3SController.getMatFromString(rightSide.getString(KEY_CALIB_DISTORTION_COEFFICIENTS));
			Mat leftDistCoeff = MADN3SController.getMatFromString(leftSide.getString(KEY_CALIB_DISTORTION_COEFFICIENTS));

			Log.d(tag, "doStereoCalibration. Parsing Camera Matrix");
			Mat rightCameraMatrix = MADN3SController.getMatFromString(rightSide.getString(KEY_CALIB_CAMERA_MATRIX));
			Mat leftCameraMatrix = MADN3SController.getMatFromString(leftSide.getString(KEY_CALIB_CAMERA_MATRIX));

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
	        Mat Q = new Mat();

	        Calib3d.stereoRectify(leftCameraMatrix, leftDistCoeff
	        		, rightCameraMatrix, rightDistCoeff
	        		, mPatternSize, R, T, R1, R2, P1, P2, Q);

	        //Esto probablemente hace el ojo de pescado en la camara, puede que no sea necesario a esta altura
//	        Imgproc.initUndistortRectifyMap(cameraMatrix, distCoeffs, vRightOrtogonal, newCameraMatrix, size, m1type, map1, map2);


			rightSide.put(KEY_CALIB_DISTORTION_COEFFICIENTS, rightDistCoeff.dump());
			leftSide.put(KEY_CALIB_DISTORTION_COEFFICIENTS, leftDistCoeff.dump());

			rightSide.put(KEY_CALIB_CAMERA_MATRIX, rightCameraMatrix.dump());
			leftSide.put(KEY_CALIB_CAMERA_MATRIX, leftCameraMatrix.dump());

			calibrationJson.put(SIDE_LEFT, leftSide);
			calibrationJson.put(SIDE_RIGHT, rightSide);

			Log.d(tag, "doStereoCalibration. saving modified calibrationJson.");
			MADN3SController.saveJsonToExternal(calibrationJson.toString(1), "modified-calibration");

			MADN3SController.sharedPrefsPutJSONObject(KEY_CALIBRATION, calibrationJson);

//		} catch (JSONException e){
//			e.printStackTrace();
		} catch (Exception e){
            e.printStackTrace();
        }
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
		String jsonBakString = "{\"frames\":[ { \"right\": { \"md5\": \"e4bbbc54c67aaf1c1e8dea0b47ba87ee\", \"filepath\": \"\\/storage\\/emulated\\/0\\/Pictures\\/MADN3SController\\/scumbag-robin\\/IMG_0_right_20150412_173633.jpg\", \"points\": [ { \"y\": 508, \"x\": 235 }, { \"y\": 196, \"x\": 317 }, { \"y\": 470, \"x\": 239 }, { \"y\": 430, \"x\": 343 }, { \"y\": 128, \"x\": 361 }, { \"y\": 299, \"x\": 261 }, { \"y\": 328, \"x\": 282 }, { \"y\": 192, \"x\": 352 }, { \"y\": 89, \"x\": 303 }, { \"y\": 379, \"x\": 261 }, { \"y\": 277, \"x\": 185 }, { \"y\": 93, \"x\": 346 }, { \"y\": 638, \"x\": 124 }, { \"y\": 325, \"x\": 358 }, { \"y\": 576, \"x\": 171 }, { \"y\": 642, \"x\": 350 }, { \"y\": 382, \"x\": 350 }, { \"y\": 174, \"x\": 185 }, { \"y\": 352, \"x\": 219 }, { \"y\": 162, \"x\": 362 }, { \"y\": 426, \"x\": 237 }, { \"y\": 277, \"x\": 362 }, { \"y\": 607, \"x\": 144 }, { \"y\": 533, \"x\": 204 }, { \"y\": 108, \"x\": 245 }, { \"y\": 124, \"x\": 218 }, { \"y\": 240, \"x\": 337 }, { \"y\": 334, \"x\": 194 }, { \"y\": 604, \"x\": 348 }, { \"y\": 204, \"x\": 172 }, { \"y\": 235, \"x\": 168 }, { \"y\": 144, \"x\": 195 }, { \"y\": 468, \"x\": 345 }, { \"y\": 529, \"x\": 347 }, { \"y\": 563, \"x\": 346 }, { \"y\": 96, \"x\": 273 } ] }, \"left\": { \"md5\": \"0e5bccf98ad13f863470caa8507d3c3c\", \"filepath\": \"\\/storage\\/emulated\\/0\\/Pictures\\/MADN3SController\\/scumbag-robin\\/IMG_0_left_20150412_173711.jpg\", \"points\": [ { \"y\": 379, \"x\": 183 }, { \"y\": 499, \"x\": 134 }, { \"y\": 243, \"x\": 185 }, { \"y\": 483, \"x\": 309 }, { \"y\": 634, \"x\": 306 }, { \"y\": 116, \"x\": 300 }, { \"y\": 420, \"x\": 152 }, { \"y\": 82, \"x\": 216 }, { \"y\": 369, \"x\": 292 }, { \"y\": 136, \"x\": 123 }, { \"y\": 191, \"x\": 213 }, { \"y\": 265, \"x\": 122 }, { \"y\": 337, \"x\": 121 }, { \"y\": 43, \"x\": 333 }, { \"y\": 134, \"x\": 180 }, { \"y\": 382, \"x\": 134 }, { \"y\": 52, \"x\": 211 }, { \"y\": 639, \"x\": 147 }, { \"y\": 357, \"x\": 160 }, { \"y\": 96, \"x\": 155 }, { \"y\": 594, \"x\": 309 }, { \"y\": 536, \"x\": 309 }, { \"y\": 164, \"x\": 197 }, { \"y\": 171, \"x\": 345 }, { \"y\": 314, \"x\": 342 }, { \"y\": 138, \"x\": 323 }, { \"y\": 82, \"x\": 327 }, { \"y\": 21, \"x\": 206 }, { \"y\": 597, \"x\": 141 }, { \"y\": 449, \"x\": 306 }, { \"y\": 236, \"x\": 359 }, { \"y\": 272, \"x\": 355 }, { \"y\": 465, \"x\": 135 }, { \"y\": 86, \"x\": 186 }, { \"y\": 536, \"x\": 137 }, { \"y\": 202, \"x\": 357 }, { \"y\": 402, \"x\": 303 } ] } }, { \"right\": { \"md5\": \"fd6f1da8fb3981124c490203c8a929e7\", \"filepath\": \"\\/storage\\/emulated\\/0\\/Pictures\\/MADN3SController\\/scumbag-robin\\/IMG_1_right_20150412_173851.jpg\", \"points\": [ { \"y\": 95, \"x\": 318 }, { \"y\": 247, \"x\": 314 }, { \"y\": 262, \"x\": 353 }, { \"y\": 370, \"x\": 305 }, { \"y\": 43, \"x\": 360 }, { \"y\": 365, \"x\": 357 }, { \"y\": 325, \"x\": 359 }, { \"y\": 260, \"x\": 232 }, { \"y\": 114, \"x\": 235 }, { \"y\": 218, \"x\": 327 }, { \"y\": 143, \"x\": 220 }, { \"y\": 339, \"x\": 246 }, { \"y\": 64, \"x\": 276 }, { \"y\": 181, \"x\": 203 }, { \"y\": 132, \"x\": 340 }, { \"y\": 291, \"x\": 340 }, { \"y\": 170, \"x\": 351 }, { \"y\": 231, \"x\": 218 }, { \"y\": 71, \"x\": 348 }, { \"y\": 79, \"x\": 248 }, { \"y\": 52, \"x\": 304 }, { \"y\": 312, \"x\": 231 }, { \"y\": 355, \"x\": 274 } ] }, \"left\": { \"md5\": \"aaebe80f0c4b9c082d7b244f85ec26bd\", \"filepath\": \"\\/storage\\/emulated\\/0\\/Pictures\\/MADN3SController\\/scumbag-robin\\/IMG_1_left_20150412_174031.jpg\", \"points\": [ { \"y\": 256, \"x\": 337 }, { \"y\": 568, \"x\": 197 }, { \"y\": 163, \"x\": 355 }, { \"y\": 319, \"x\": 178 }, { \"y\": 354, \"x\": 301 }, { \"y\": 317, \"x\": 131 }, { \"y\": 96, \"x\": 279 }, { \"y\": 82, \"x\": 167 }, { \"y\": 185, \"x\": 121 }, { \"y\": 373, \"x\": 353 }, { \"y\": 75, \"x\": 353 }, { \"y\": 230, \"x\": 352 }, { \"y\": 272, \"x\": 297 }, { \"y\": 431, \"x\": 309 }, { \"y\": 431, \"x\": 351 }, { \"y\": 430, \"x\": 203 }, { \"y\": 318, \"x\": 316 }, { \"y\": 211, \"x\": 168 }, { \"y\": 108, \"x\": 189 }, { \"y\": 179, \"x\": 151 }, { \"y\": 160, \"x\": 322 }, { \"y\": 247, \"x\": 191 }, { \"y\": 374, \"x\": 180 }, { \"y\": 68, \"x\": 318 }, { \"y\": 61, \"x\": 200 }, { \"y\": 49, \"x\": 294 }, { \"y\": 626, \"x\": 205 }, { \"y\": 463, \"x\": 195 }, { \"y\": 524, \"x\": 197 }, { \"y\": 120, \"x\": 297 }, { \"y\": 140, \"x\": 168 }, { \"y\": 502, \"x\": 359 }, { \"y\": 347, \"x\": 147 }, { \"y\": 278, \"x\": 186 }, { \"y\": 49, \"x\": 237 }, { \"y\": 575, \"x\": 361 }, { \"y\": 610, \"x\": 362 }, { \"y\": 537, \"x\": 361 } ] } }, { \"right\": { \"md5\": \"9c6c606469c325ab953b1fed5fe10152\", \"filepath\": \"\\/storage\\/emulated\\/0\\/Pictures\\/MADN3SController\\/scumbag-robin\\/IMG_2_right_20150412_174402.jpg\", \"points\": [ { \"y\": 108, \"x\": 220 }, { \"y\": 117, \"x\": 178 }, { \"y\": 107, \"x\": 254 }, { \"y\": 336, \"x\": 131 }, { \"y\": 62, \"x\": 144 }, { \"y\": 411, \"x\": 218 }, { \"y\": 18, \"x\": 183 }, { \"y\": 242, \"x\": 340 }, { \"y\": 269, \"x\": 125 }, { \"y\": 334, \"x\": 298 }, { \"y\": 3, \"x\": 323 }, { \"y\": 40, \"x\": 244 }, { \"y\": 21, \"x\": 124 }, { \"y\": 634, \"x\": 343 }, { \"y\": 529, \"x\": 174 }, { \"y\": 473, \"x\": 179 }, { \"y\": 642, \"x\": 179 }, { \"y\": 606, \"x\": 179 }, { \"y\": 226, \"x\": 242 }, { \"y\": 130, \"x\": 132 }, { \"y\": 489, \"x\": 328 }, { \"y\": 285, \"x\": 273 }, { \"y\": 160, \"x\": 276 }, { \"y\": 400, \"x\": 175 }, { \"y\": 14, \"x\": 291 }, { \"y\": 223, \"x\": 124 }, { \"y\": 184, \"x\": 122 }, { \"y\": 124, \"x\": 297 }, { \"y\": 67, \"x\": 261 }, { \"y\": 89, \"x\": 127 }, { \"y\": 192, \"x\": 258 }, { \"y\": 255, \"x\": 255 }, { \"y\": 356, \"x\": 270 }, { \"y\": 378, \"x\": 241 }, { \"y\": 365, \"x\": 121 }, { \"y\": 385, \"x\": 145 }, { \"y\": 575, \"x\": 177 }, { \"y\": 486, \"x\": 222 }, { \"y\": 600, \"x\": 339 }, { \"y\": 557, \"x\": 337 }, { \"y\": 489, \"x\": 263 } ] }, \"left\": { \"md5\": \"e411d4b713821296ff3e2f7b0f05f994\", \"filepath\": \"\\/storage\\/emulated\\/0\\/Pictures\\/MADN3SController\\/scumbag-robin\\/IMG_2_left_20150412_174504.jpg\", \"points\": [ { \"y\": 81, \"x\": 200 }, { \"y\": 228, \"x\": 349 }, { \"y\": 9, \"x\": 305 }, { \"y\": 108, \"x\": 242 }, { \"y\": 166, \"x\": 213 }, { \"y\": 90, \"x\": 162 }, { \"y\": 374, \"x\": 325 }, { \"y\": 34, \"x\": 257 }, { \"y\": 393, \"x\": 151 }, { \"y\": 329, \"x\": 125 }, { \"y\": 324, \"x\": 304 }, { \"y\": 199, \"x\": 126 }, { \"y\": 109, \"x\": 272 }, { \"y\": 77, \"x\": 232 }, { \"y\": 399, \"x\": 197 }, { \"y\": 40, \"x\": 150 }, { \"y\": 139, \"x\": 295 }, { \"y\": 564, \"x\": 126 }, { \"y\": 220, \"x\": 268 }, { \"y\": 116, \"x\": 139 }, { \"y\": 243, \"x\": 317 }, { \"y\": 24, \"x\": 121 }, { \"y\": 377, \"x\": 226 }, { \"y\": 347, \"x\": 276 }, { \"y\": 361, \"x\": 172 }, { \"y\": 597, \"x\": 129 }, { \"y\": 271, \"x\": 288 }, { \"y\": 120, \"x\": 319 }, { \"y\": 420, \"x\": 123 }, { \"y\": 440, \"x\": 362 }, { \"y\": 517, \"x\": 123 }, { \"y\": 243, \"x\": 142 }, { \"y\": 388, \"x\": 287 }, { \"y\": 84, \"x\": 294 }, { \"y\": 132, \"x\": 213 }, { \"y\": 173, \"x\": 292 }, { \"y\": 74, \"x\": 122 }, { \"y\": 146, \"x\": 126 }, { \"y\": 403, \"x\": 355 }, { \"y\": 143, \"x\": 184 }, { \"y\": 273, \"x\": 133 }, { \"y\": 3, \"x\": 147 }, { \"y\": 480, \"x\": 120 }, { \"y\": 489, \"x\": 360 }, { \"y\": 524, \"x\": 361 }, { \"y\": 635, \"x\": 129 }, { \"y\": 394, \"x\": 253 } ] } }, { \"right\": { \"md5\": \"e1bc2970db0055f60242e5f1508fcab9\", \"filepath\": \"\\/storage\\/emulated\\/0\\/Pictures\\/MADN3SController\\/scumbag-robin\\/IMG_3_right_20150412_174711.jpg\", \"points\": [ { \"y\": 53, \"x\": 240 }, { \"y\": 136, \"x\": 156 }, { \"y\": 169, \"x\": 151 }, { \"y\": 38, \"x\": 209 }, { \"y\": 41, \"x\": 178 }, { \"y\": 87, \"x\": 127 }, { \"y\": 92, \"x\": 277 }, { \"y\": 159, \"x\": 266 }, { \"y\": 59, \"x\": 138 }, { \"y\": 255, \"x\": 341 }, { \"y\": 252, \"x\": 309 }, { \"y\": 334, \"x\": 260 }, { \"y\": 339, \"x\": 162 }, { \"y\": 130, \"x\": 322 }, { \"y\": 280, \"x\": 129 }, { \"y\": 295, \"x\": 287 }, { \"y\": 180, \"x\": 341 }, { \"y\": 199, \"x\": 135 }, { \"y\": 308, \"x\": 145 }, { \"y\": 229, \"x\": 120 }, { \"y\": 129, \"x\": 272 }, { \"y\": 338, \"x\": 205 }, { \"y\": 217, \"x\": 346 }, { \"y\": 154, \"x\": 216 }, { \"y\": 153, \"x\": 183 } ] }, \"left\": { \"md5\": \"fa78e35715532e3b26fa9da2ada24b16\", \"filepath\": \"\\/storage\\/emulated\\/0\\/Pictures\\/MADN3SController\\/scumbag-robin\\/IMG_3_left_20150412_174746.jpg\", \"points\": [ { \"y\": 94, \"x\": 142 }, { \"y\": 37, \"x\": 305 }, { \"y\": 35, \"x\": 129 }, { \"y\": 615, \"x\": 348 }, { \"y\": 224, \"x\": 123 }, { \"y\": 541, \"x\": 291 }, { \"y\": 35, \"x\": 350 }, { \"y\": 19, \"x\": 165 }, { \"y\": 138, \"x\": 149 }, { \"y\": 42, \"x\": 240 }, { \"y\": 6, \"x\": 248 }, { \"y\": 65, \"x\": 131 }, { \"y\": 9, \"x\": 294 }, { \"y\": 88, \"x\": 253 }, { \"y\": 239, \"x\": 287 }, { \"y\": 552, \"x\": 319 }, { \"y\": 331, \"x\": 158 }, { \"y\": 327, \"x\": 241 }, { \"y\": 538, \"x\": 215 }, { \"y\": 620, \"x\": 220 }, { \"y\": 300, \"x\": 147 }, { \"y\": 128, \"x\": 270 }, { \"y\": 296, \"x\": 261 }, { \"y\": 158, \"x\": 281 }, { \"y\": 584, \"x\": 332 }, { \"y\": 260, \"x\": 132 }, { \"y\": 190, \"x\": 287 }, { \"y\": 185, \"x\": 147 }, { \"y\": 543, \"x\": 251 }, { \"y\": 4, \"x\": 137 }, { \"y\": 42, \"x\": 271 }, { \"y\": 644, \"x\": 358 }, { \"y\": 330, \"x\": 195 }, { \"y\": 573, \"x\": 215 }, { \"y\": 267, \"x\": 274 } ] } }, { \"right\": { \"md5\": \"20d6b602305076f74852d5ffb2ec8c61\", \"filepath\": \"\\/storage\\/emulated\\/0\\/Pictures\\/MADN3SController\\/scumbag-robin\\/IMG_4_right_20150412_174945.jpg\", \"points\": [ { \"y\": 553, \"x\": 216 }, { \"y\": 493, \"x\": 333 }, { \"y\": 500, \"x\": 210 }, { \"y\": 557, \"x\": 330 }, { \"y\": 275, \"x\": 271 }, { \"y\": 440, \"x\": 288 }, { \"y\": 138, \"x\": 127 }, { \"y\": 106, \"x\": 162 }, { \"y\": 556, \"x\": 279 }, { \"y\": 206, \"x\": 229 }, { \"y\": 434, \"x\": 208 }, { \"y\": 464, \"x\": 318 }, { \"y\": 200, \"x\": 166 }, { \"y\": 173, \"x\": 150 }, { \"y\": 190, \"x\": 259 }, { \"y\": 117, \"x\": 190 }, { \"y\": 137, \"x\": 217 }, { \"y\": 241, \"x\": 245 }, { \"y\": 523, \"x\": 334 }, { \"y\": 220, \"x\": 268 }, { \"y\": 464, \"x\": 206 }, { \"y\": 433, \"x\": 238 } ] }, \"left\": { \"md5\": \"c77afe48a6010a2ad585d85fc5331124\", \"filepath\": \"\\/storage\\/emulated\\/0\\/Pictures\\/MADN3SController\\/scumbag-robin\\/IMG_4_left_20150412_175200.jpg\", \"points\": [ { \"y\": 546, \"x\": 328 }, { \"y\": 549, \"x\": 142 }, { \"y\": 393, \"x\": 283 }, { \"y\": 446, \"x\": 123 }, { \"y\": 476, \"x\": 121 }, { \"y\": 514, \"x\": 133 }, { \"y\": 506, \"x\": 336 }, { \"y\": 262, \"x\": 183 }, { \"y\": 328, \"x\": 151 }, { \"y\": 541, \"x\": 258 }, { \"y\": 5, \"x\": 355 }, { \"y\": 107, \"x\": 121 }, { \"y\": 167, \"x\": 357 }, { \"y\": 67, \"x\": 361 }, { \"y\": 451, \"x\": 352 }, { \"y\": 420, \"x\": 327 }, { \"y\": 375, \"x\": 125 }, { \"y\": 368, \"x\": 330 }, { \"y\": 185, \"x\": 174 }, { \"y\": 258, \"x\": 359 }, { \"y\": 292, \"x\": 171 }, { \"y\": 302, \"x\": 360 }, { \"y\": 408, \"x\": 232 }, { \"y\": 16, \"x\": 130 }, { \"y\": 146, \"x\": 157 }, { \"y\": 534, \"x\": 180 }, { \"y\": 342, \"x\": 357 }, { \"y\": 553, \"x\": 296 }, { \"y\": 531, \"x\": 227 }, { \"y\": 70, \"x\": 129 }, { \"y\": 197, \"x\": 357 }, { \"y\": 35, \"x\": 358 }, { \"y\": 218, \"x\": 181 }, { \"y\": 227, \"x\": 359 } ] } }, { \"right\": { \"md5\": \"5859af46ec8b924507479c4692c2ed93\", \"filepath\": \"\\/storage\\/emulated\\/0\\/Pictures\\/MADN3SController\\/scumbag-robin\\/IMG_5_right_20150412_175504.jpg\", \"points\": [ { \"y\": 330, \"x\": 174 }, { \"y\": 143, \"x\": 121 }, { \"y\": 270, \"x\": 195 }, { \"y\": 416, \"x\": 128 }, { \"y\": 601, \"x\": 124 }, { \"y\": 644, \"x\": 243 }, { \"y\": 636, \"x\": 157 }, { \"y\": 160, \"x\": 183 }, { \"y\": 214, \"x\": 158 }, { \"y\": 340, \"x\": 310 }, { \"y\": 376, \"x\": 148 }, { \"y\": 365, \"x\": 287 }, { \"y\": 628, \"x\": 271 }, { \"y\": 312, \"x\": 322 }, { \"y\": 125, \"x\": 154 }, { \"y\": 228, \"x\": 337 }, { \"y\": 520, \"x\": 284 }, { \"y\": 534, \"x\": 135 }, { \"y\": 564, \"x\": 135 }, { \"y\": 125, \"x\": 256 }, { \"y\": 169, \"x\": 311 }, { \"y\": 145, \"x\": 285 }, { \"y\": 404, \"x\": 284 }, { \"y\": 578, \"x\": 290 }, { \"y\": 274, \"x\": 339 }, { \"y\": 241, \"x\": 176 }, { \"y\": 114, \"x\": 222 }, { \"y\": 115, \"x\": 187 }, { \"y\": 299, \"x\": 184 }, { \"y\": 451, \"x\": 130 }, { \"y\": 452, \"x\": 285 }, { \"y\": 482, \"x\": 286 }, { \"y\": 486, \"x\": 131 }, { \"y\": 198, \"x\": 327 } ] }, \"left\": { \"md5\": \"23369cea16f2fb2d6139f01c1c756762\", \"filepath\": \"\\/storage\\/emulated\\/0\\/Pictures\\/MADN3SController\\/scumbag-robin\\/IMG_5_left_20150412_175621.jpg\", \"points\": [ { \"y\": 180, \"x\": 222 }, { \"y\": 201, \"x\": 200 }, { \"y\": 98, \"x\": 122 }, { \"y\": 415, \"x\": 131 }, { \"y\": 265, \"x\": 234 }, { \"y\": 226, \"x\": 234 }, { \"y\": 300, \"x\": 225 }, { \"y\": 341, \"x\": 361 }, { \"y\": 150, \"x\": 171 }, { \"y\": 314, \"x\": 192 }, { \"y\": 115, \"x\": 157 }, { \"y\": 144, \"x\": 358 }, { \"y\": 32, \"x\": 122 }, { \"y\": 577, \"x\": 338 }, { \"y\": 625, \"x\": 297 }, { \"y\": 261, \"x\": 182 }, { \"y\": 620, \"x\": 169 }, { \"y\": 349, \"x\": 189 }, { \"y\": 405, \"x\": 168 }, { \"y\": 372, \"x\": 348 }, { \"y\": 480, \"x\": 356 }, { \"y\": 214, \"x\": 361 }, { \"y\": 48, \"x\": 357 }, { \"y\": 534, \"x\": 142 }, { \"y\": 488, \"x\": 136 }, { \"y\": 4, \"x\": 360 }, { \"y\": 580, \"x\": 156 }, { \"y\": 249, \"x\": 359 }, { \"y\": 520, \"x\": 360 }, { \"y\": 438, \"x\": 361 }, { \"y\": 374, \"x\": 172 }, { \"y\": 153, \"x\": 202 }, { \"y\": 602, \"x\": 317 }, { \"y\": 405, \"x\": 358 }, { \"y\": 551, \"x\": 361 }, { \"y\": 642, \"x\": 145 }, { \"y\": 279, \"x\": 359 }, { \"y\": 233, \"x\": 195 }, { \"y\": 62, \"x\": 122 }, { \"y\": 176, \"x\": 360 }, { \"y\": 103, \"x\": 361 } ] } } ]}";
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
