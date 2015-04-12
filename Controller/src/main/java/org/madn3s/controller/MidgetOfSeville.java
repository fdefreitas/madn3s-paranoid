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
		String jsonBakString = "{\"frames\":[{\"right\":{\"md5\":\"d7069b22785ebd62a4bc807c52384a56\",\"filepath\":\"/storage/emulated/0/Pictures/MADN3SController/football2/IMG_0_right_20141214_132234.jpg\",\"points\":[{\"y\":241,\"x\":221},{\"y\":116,\"x\":340},{\"y\":197,\"x\":235},{\"y\":63,\"x\":173},{\"y\":135,\"x\":176},{\"y\":68,\"x\":265},{\"y\":255,\"x\":194},{\"y\":53,\"x\":292},{\"y\":328,\"x\":121},{\"y\":255,\"x\":349},{\"y\":265,\"x\":123},{\"y\":108,\"x\":295},{\"y\":170,\"x\":198},{\"y\":284,\"x\":358},{\"y\":199,\"x\":358},{\"y\":105,\"x\":172},{\"y\":225,\"x\":343},{\"y\":210,\"x\":189},{\"y\":46,\"x\":220},{\"y\":255,\"x\":154},{\"y\":364,\"x\":121},{\"y\":410,\"x\":362},{\"y\":466,\"x\":120},{\"y\":574,\"x\":362},{\"y\":590,\"x\":121},{\"y\":609,\"x\":361},{\"y\":643,\"x\":362},{\"y\":327,\"x\":361}]},\"left\":{\"md5\":\"ff63bc1ef984fe77bebb516371fda9b9\",\"filepath\":\"/storage/emulated/0/Pictures/MADN3SController/football2/IMG_0_left_20141214_132222.jpg\",\"points\":[{\"y\":455,\"x\":131},{\"y\":162,\"x\":143},{\"y\":291,\"x\":134},{\"y\":322,\"x\":325},{\"y\":236,\"x\":122},{\"y\":79,\"x\":299},{\"y\":89,\"x\":184},{\"y\":210,\"x\":196},{\"y\":280,\"x\":166},{\"y\":146,\"x\":191},{\"y\":95,\"x\":215},{\"y\":442,\"x\":333},{\"y\":356,\"x\":144},{\"y\":268,\"x\":361},{\"y\":352,\"x\":328},{\"y\":606,\"x\":342},{\"y\":136,\"x\":340},{\"y\":511,\"x\":124},{\"y\":110,\"x\":302},{\"y\":549,\"x\":126},{\"y\":640,\"x\":351},{\"y\":552,\"x\":339},{\"y\":282,\"x\":330},{\"y\":380,\"x\":121},{\"y\":406,\"x\":324},{\"y\":417,\"x\":120},{\"y\":71,\"x\":267},{\"y\":71,\"x\":237},{\"y\":190,\"x\":120}]}},{\"right\":{\"md5\":\"1cceb1682c9dea92af3f6141b4052f36\",\"filepath\":\"/storage/emulated/0/Pictures/MADN3SController/football2/IMG_1_right_20141214_132629.jpg\",\"points\":[{\"y\":171,\"x\":204},{\"y\":245,\"x\":137},{\"y\":248,\"x\":170},{\"y\":181,\"x\":148},{\"y\":148,\"x\":343},{\"y\":102,\"x\":319},{\"y\":80,\"x\":231},{\"y\":142,\"x\":168},{\"y\":191,\"x\":246},{\"y\":264,\"x\":360},{\"y\":198,\"x\":190},{\"y\":194,\"x\":302},{\"y\":255,\"x\":209},{\"y\":260,\"x\":318},{\"y\":371,\"x\":134},{\"y\":112,\"x\":178},{\"y\":222,\"x\":229},{\"y\":224,\"x\":314},{\"y\":424,\"x\":126},{\"y\":130,\"x\":223},{\"y\":322,\"x\":360},{\"y\":628,\"x\":347},{\"y\":418,\"x\":349},{\"y\":554,\"x\":124},{\"y\":356,\"x\":355},{\"y\":219,\"x\":160},{\"y\":576,\"x\":353},{\"y\":232,\"x\":361},{\"y\":503,\"x\":124},{\"y\":78,\"x\":294},{\"y\":587,\"x\":123},{\"y\":619,\"x\":123},{\"y\":284,\"x\":133},{\"y\":337,\"x\":134},{\"y\":456,\"x\":127}]},\"left\":{\"md5\":\"ad18f3d2ecd0cf7d83d808cd2309f889\",\"filepath\":\"/storage/emulated/0/Pictures/MADN3SController/football2/IMG_1_left_20141214_132643.jpg\",\"points\":[{\"y\":134,\"x\":170},{\"y\":165,\"x\":154},{\"y\":254,\"x\":360},{\"y\":442,\"x\":124},{\"y\":269,\"x\":183},{\"y\":384,\"x\":124},{\"y\":210,\"x\":334},{\"y\":203,\"x\":180},{\"y\":92,\"x\":343},{\"y\":227,\"x\":254},{\"y\":548,\"x\":123},{\"y\":219,\"x\":143},{\"y\":308,\"x\":140},{\"y\":196,\"x\":269},{\"y\":201,\"x\":217},{\"y\":351,\"x\":137},{\"y\":93,\"x\":209},{\"y\":89,\"x\":265},{\"y\":598,\"x\":129},{\"y\":276,\"x\":149},{\"y\":132,\"x\":238},{\"y\":642,\"x\":165},{\"y\":148,\"x\":360},{\"y\":237,\"x\":180},{\"y\":80,\"x\":310},{\"y\":493,\"x\":123},{\"y\":139,\"x\":203},{\"y\":298,\"x\":362},{\"y\":524,\"x\":361},{\"y\":606,\"x\":357}]}},{\"right\":{\"md5\":\"2bbf35b97ac1a7404a41f5f5e190507d\",\"filepath\":\"/storage/emulated/0/Pictures/MADN3SController/football2/IMG_2_right_20141214_133022.jpg\",\"points\":[{\"y\":232,\"x\":145},{\"y\":150,\"x\":246},{\"y\":97,\"x\":224},{\"y\":93,\"x\":264},{\"y\":212,\"x\":268},{\"y\":263,\"x\":141},{\"y\":111,\"x\":359},{\"y\":250,\"x\":183},{\"y\":233,\"x\":330},{\"y\":217,\"x\":298},{\"y\":646,\"x\":132},{\"y\":295,\"x\":130},{\"y\":151,\"x\":355},{\"y\":169,\"x\":150},{\"y\":341,\"x\":326},{\"y\":386,\"x\":123},{\"y\":187,\"x\":359},{\"y\":82,\"x\":309},{\"y\":128,\"x\":169},{\"y\":107,\"x\":191},{\"y\":293,\"x\":324},{\"y\":484,\"x\":323},{\"y\":601,\"x\":121},{\"y\":503,\"x\":120},{\"y\":518,\"x\":317},{\"y\":592,\"x\":315},{\"y\":384,\"x\":321},{\"y\":419,\"x\":320},{\"y\":449,\"x\":319}]},\"left\":{\"md5\":\"a741af42de0eb1dedbb0af2b35d6c4c1\",\"filepath\":\"/storage/emulated/0/Pictures/MADN3SController/football2/IMG_2_left_20141214_133031.jpg\",\"points\":[{\"y\":214,\"x\":331},{\"y\":247,\"x\":275},{\"y\":184,\"x\":282},{\"y\":83,\"x\":260},{\"y\":266,\"x\":156},{\"y\":152,\"x\":207},{\"y\":121,\"x\":355},{\"y\":214,\"x\":181},{\"y\":259,\"x\":234},{\"y\":110,\"x\":298},{\"y\":632,\"x\":133},{\"y\":211,\"x\":296},{\"y\":237,\"x\":352},{\"y\":108,\"x\":206},{\"y\":152,\"x\":269},{\"y\":429,\"x\":124},{\"y\":292,\"x\":127},{\"y\":346,\"x\":122},{\"y\":167,\"x\":149},{\"y\":510,\"x\":362},{\"y\":460,\"x\":123},{\"y\":138,\"x\":163},{\"y\":89,\"x\":326},{\"y\":162,\"x\":345},{\"y\":600,\"x\":128},{\"y\":380,\"x\":123},{\"y\":469,\"x\":362},{\"y\":237,\"x\":148},{\"y\":76,\"x\":290},{\"y\":85,\"x\":229},{\"y\":244,\"x\":307},{\"y\":378,\"x\":362},{\"y\":512,\"x\":121},{\"y\":561,\"x\":361}]}},{\"right\":{\"md5\":\"e0c9399b98ee4a631c410efc7c9f989e\",\"filepath\":\"/storage/emulated/0/Pictures/MADN3SController/football2/IMG_3_right_20141214_133453.jpg\",\"points\":[{\"y\":272,\"x\":202},{\"y\":247,\"x\":227},{\"y\":228,\"x\":341},{\"y\":214,\"x\":141},{\"y\":115,\"x\":349},{\"y\":72,\"x\":227},{\"y\":121,\"x\":158},{\"y\":86,\"x\":193},{\"y\":250,\"x\":264},{\"y\":468,\"x\":334},{\"y\":253,\"x\":177},{\"y\":181,\"x\":268},{\"y\":66,\"x\":277},{\"y\":507,\"x\":341},{\"y\":222,\"x\":285},{\"y\":118,\"x\":284},{\"y\":149,\"x\":360},{\"y\":304,\"x\":182},{\"y\":410,\"x\":345},{\"y\":373,\"x\":356},{\"y\":273,\"x\":354},{\"y\":52,\"x\":305},{\"y\":398,\"x\":123},{\"y\":364,\"x\":131},{\"y\":10,\"x\":123},{\"y\":556,\"x\":361},{\"y\":454,\"x\":123},{\"y\":85,\"x\":338},{\"y\":197,\"x\":360},{\"y\":86,\"x\":123},{\"y\":330,\"x\":156},{\"y\":529,\"x\":121},{\"y\":165,\"x\":135},{\"y\":631,\"x\":121},{\"y\":304,\"x\":361},{\"y\":56,\"x\":122},{\"y\":484,\"x\":122},{\"y\":219,\"x\":172}]},\"left\":{\"md5\":\"28f9d1413c21651577cc7965d8fb0e1a\",\"filepath\":\"/storage/emulated/0/Pictures/MADN3SController/football2/IMG_3_left_20141214_133500.jpg\",\"points\":[{\"y\":64,\"x\":291},{\"y\":268,\"x\":242},{\"y\":248,\"x\":359},{\"y\":240,\"x\":194},{\"y\":220,\"x\":146},{\"y\":79,\"x\":201},{\"y\":258,\"x\":309},{\"y\":158,\"x\":269},{\"y\":210,\"x\":307},{\"y\":81,\"x\":260},{\"y\":353,\"x\":140},{\"y\":65,\"x\":233},{\"y\":129,\"x\":207},{\"y\":277,\"x\":190},{\"y\":96,\"x\":176},{\"y\":257,\"x\":272},{\"y\":123,\"x\":155},{\"y\":97,\"x\":354},{\"y\":254,\"x\":149},{\"y\":179,\"x\":291},{\"y\":1,\"x\":283},{\"y\":284,\"x\":358},{\"y\":482,\"x\":129},{\"y\":315,\"x\":359},{\"y\":160,\"x\":138},{\"y\":386,\"x\":141},{\"y\":75,\"x\":320},{\"y\":88,\"x\":122},{\"y\":513,\"x\":120},{\"y\":131,\"x\":362},{\"y\":285,\"x\":151},{\"y\":315,\"x\":146},{\"y\":350,\"x\":361},{\"y\":420,\"x\":361}]}},{\"right\":{\"md5\":\"6c6e4b62a7285fc4dc01f3d32d22dff4\",\"filepath\":\"/storage/emulated/0/Pictures/MADN3SController/football2/IMG_4_right_20141214_133711.jpg\",\"points\":[{\"y\":72,\"x\":255},{\"y\":500,\"x\":205},{\"y\":493,\"x\":272},{\"y\":261,\"x\":125},{\"y\":499,\"x\":238},{\"y\":423,\"x\":274},{\"y\":80,\"x\":309},{\"y\":291,\"x\":269},{\"y\":293,\"x\":327},{\"y\":287,\"x\":209},{\"y\":270,\"x\":159},{\"y\":241,\"x\":206},{\"y\":98,\"x\":345},{\"y\":356,\"x\":184},{\"y\":441,\"x\":223},{\"y\":452,\"x\":255},{\"y\":135,\"x\":363},{\"y\":432,\"x\":322},{\"y\":493,\"x\":155},{\"y\":531,\"x\":208},{\"y\":188,\"x\":300},{\"y\":105,\"x\":279},{\"y\":249,\"x\":301},{\"y\":142,\"x\":292},{\"y\":134,\"x\":164},{\"y\":189,\"x\":145},{\"y\":83,\"x\":222},{\"y\":101,\"x\":190},{\"y\":466,\"x\":310},{\"y\":413,\"x\":234},{\"y\":463,\"x\":165},{\"y\":216,\"x\":315},{\"y\":228,\"x\":157}]},\"left\":{\"md5\":\"c53e6a2f909c1e4876d424365d405904\",\"filepath\":\"/storage/emulated/0/Pictures/MADN3SController/football2/IMG_4_left_20141214_133724.jpg\",\"points\":[{\"y\":513,\"x\":235},{\"y\":214,\"x\":360},{\"y\":510,\"x\":193},{\"y\":288,\"x\":130},{\"y\":435,\"x\":305},{\"y\":431,\"x\":346},{\"y\":502,\"x\":270},{\"y\":240,\"x\":219},{\"y\":289,\"x\":327},{\"y\":365,\"x\":226},{\"y\":449,\"x\":253},{\"y\":190,\"x\":142},{\"y\":281,\"x\":244},{\"y\":489,\"x\":313},{\"y\":418,\"x\":263},{\"y\":120,\"x\":162},{\"y\":238,\"x\":138},{\"y\":151,\"x\":156},{\"y\":93,\"x\":195},{\"y\":120,\"x\":355},{\"y\":263,\"x\":312},{\"y\":87,\"x\":320},{\"y\":464,\"x\":203},{\"y\":70,\"x\":268},{\"y\":73,\"x\":237},{\"y\":224,\"x\":330},{\"y\":274,\"x\":283},{\"y\":462,\"x\":341},{\"y\":285,\"x\":203},{\"y\":285,\"x\":160}]}},{\"right\":{\"md5\":\"b1b21136199736c3d08c9ed4cd562d98\",\"filepath\":\"/storage/emulated/0/Pictures/MADN3SController/football2/IMG_5_right_20141214_134124.jpg\",\"points\":[{\"y\":201,\"x\":262},{\"y\":254,\"x\":342},{\"y\":203,\"x\":360},{\"y\":277,\"x\":160},{\"y\":434,\"x\":165},{\"y\":216,\"x\":323},{\"y\":439,\"x\":134},{\"y\":284,\"x\":299},{\"y\":478,\"x\":176},{\"y\":351,\"x\":326},{\"y\":433,\"x\":205},{\"y\":354,\"x\":154},{\"y\":489,\"x\":353},{\"y\":457,\"x\":326},{\"y\":314,\"x\":355},{\"y\":406,\"x\":185},{\"y\":285,\"x\":340},{\"y\":537,\"x\":360},{\"y\":409,\"x\":322},{\"y\":395,\"x\":155},{\"y\":202,\"x\":296},{\"y\":493,\"x\":135},{\"y\":148,\"x\":171},{\"y\":116,\"x\":362},{\"y\":628,\"x\":361},{\"y\":279,\"x\":190},{\"y\":91,\"x\":325},{\"y\":102,\"x\":199},{\"y\":192,\"x\":145},{\"y\":642,\"x\":147},{\"y\":615,\"x\":121},{\"y\":589,\"x\":361},{\"y\":247,\"x\":151},{\"y\":306,\"x\":143},{\"y\":78,\"x\":262},{\"y\":89,\"x\":227},{\"y\":82,\"x\":295},{\"y\":571,\"x\":121}]},\"left\":{\"md5\":\"70dcc27fc522e44454305ecae6dabd6a\",\"filepath\":\"/storage/emulated/0/Pictures/MADN3SController/football2/IMG_5_left_20141214_134118.jpg\",\"points\":[{\"y\":214,\"x\":270},{\"y\":430,\"x\":201},{\"y\":218,\"x\":313},{\"y\":202,\"x\":360},{\"y\":481,\"x\":161},{\"y\":150,\"x\":321},{\"y\":441,\"x\":173},{\"y\":486,\"x\":202},{\"y\":276,\"x\":343},{\"y\":628,\"x\":329},{\"y\":532,\"x\":354},{\"y\":118,\"x\":235},{\"y\":475,\"x\":125},{\"y\":120,\"x\":172},{\"y\":272,\"x\":151},{\"y\":290,\"x\":127},{\"y\":155,\"x\":265},{\"y\":580,\"x\":351},{\"y\":109,\"x\":323},{\"y\":100,\"x\":197},{\"y\":446,\"x\":139},{\"y\":172,\"x\":163},{\"y\":631,\"x\":138},{\"y\":368,\"x\":361},{\"y\":95,\"x\":290},{\"y\":126,\"x\":362},{\"y\":482,\"x\":362},{\"y\":510,\"x\":150},{\"y\":234,\"x\":345},{\"y\":336,\"x\":352},{\"y\":356,\"x\":144},{\"y\":407,\"x\":170},{\"y\":330,\"x\":121},{\"y\":203,\"x\":138},{\"y\":537,\"x\":130},{\"y\":80,\"x\":255},{\"y\":243,\"x\":141},{\"y\":460,\"x\":218},{\"y\":87,\"x\":225},{\"y\":581,\"x\":127}]}}]}";
		try {
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
