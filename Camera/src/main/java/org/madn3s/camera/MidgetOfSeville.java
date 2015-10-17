package org.madn3s.camera;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.madn3s.camera.Consts.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.util.Base64;
import android.util.Log;

public class MidgetOfSeville {

	private static final String tag = MidgetOfSeville.class.getSimpleName();
	private static final Scalar ZERO_SCALAR = new Scalar(0);
	private int iterCount = 1;
    private Mat map1;
    private Mat map2;

    private static MidgetOfSeville me;

    public static MidgetOfSeville getInstance(){
        if(me == null){
            me = new MidgetOfSeville();
        }

        return me;
    }

//    public JSONObject shapeUp(Bitmap imgBitmap, JSONObject config) throws JSONException {
//        int height = imgBitmap.getHeight();
//        int width = imgBitmap.getWidth();
//
//        Mat imgMat = new Mat(height, width, CvType.CV_8UC3);
//        Utils.bitmapToMat(imgBitmap, imgMat);
//        return shapeUp(imgMat, config);
//    }

//    public JSONObject shapeUp(String filePath, JSONObject configs) throws JSONException{
//        Bitmap imgBitmap = loadBitmap(filePath);
//        return shapeUp(imgBitmap, configs);
//    }

	public JSONObject shapeUp(Mat imgMat, JSONObject config) throws JSONException {
		Log.d(tag, "shapeUp.");
        String savePath;
        Imgproc.resize(imgMat, imgMat, new Size(486, 648));
        int height = imgMat.rows();
        int width = imgMat.cols();

		Imgproc.cvtColor(imgMat, imgMat, Imgproc.COLOR_RGBA2RGB);

//        Log.d(tag, "shapeUp. imgMat type:" + imgMat.type());
//        Log.d(tag, "shapeUp. imgMat channels:" + imgMat.channels());

        Bitmap renderFrameBitmap = Bitmap.createBitmap(imgMat.cols(), imgMat.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(imgMat, renderFrameBitmap);
//        savePath = MADN3SCamera.saveBitmapAsJpeg(renderFrameBitmap, "rendered-frame");
        MADN3SCamera.saveBitmapAsJpeg(renderFrameBitmap, "rendered-frame");

		Mat maskMat = new Mat(height, width, CvType.CV_8UC1, ZERO_SCALAR);
//        Log.d(tag, "shapeUp. maskMat type:" + maskMat.type());
//        Log.d(tag, "shapeUp. maskMat channels:" + maskMat.channels());

        Bitmap exportedBitmap;
        String edgeDetectionsAlgorithm = "canny";
        boolean isOnValidationMode = false;
	    double x1 = width / 4;
	    double y1 = 0;
	    double x2 =  x1 * 3;
	    double y2 = height;
	    double iCannyLowerThreshold = 35;
	    double iCannyUpperThreshold = 75;
	    int maxCorners = 50;
	    double qualityLevel = 0.01;
	    double minDistance = 30;

	    int dDepth = 0;
        int dx = 0;
        int dy = 0;

        double r = 255, g = 0, b = 0;
        int radius = 10;

		if (config != null) {
            Log.d(tag, "config:" + config.toString());
            // Sets Validation Mode (send image with circles or without)
            if(config.has("validate")){
                isOnValidationMode = config.getBoolean("validate");
            }

			//grabCut el rectangulo q tenemos default en la tablet hace q explote el grabcut
//			if (config.has("grab_cut")) {
//				JSONObject grabCut = config.getJSONObject("grab_cut");
//				if (grabCut.has("rectangle")) {
//					JSONObject points = grabCut.getJSONObject("rectangle");
//					JSONObject point1 = points.getJSONObject("point_1");
//					JSONObject point2 = points.getJSONObject("point_2");
//					x1 = point1.getDouble("x");
//					y1 = point1.getDouble("y");
//					x2 = point2.getDouble("x");
//					y2 = point2.getDouble("y");
//				}
//				if (grabCut.has("iterations")) {
//					iterCount = grabCut.getInt("iterations");
//				}
//			}

			//goodFeaturesToTrack
			if (config.has("good_features")) {
				JSONObject goodFeaturesToTrack = config.getJSONObject("good_features");
				if (goodFeaturesToTrack.has("max_corners")) {
					maxCorners = goodFeaturesToTrack.getInt("max_corners");
				}
				if (goodFeaturesToTrack.has("quality_level")) {
					qualityLevel = goodFeaturesToTrack.getDouble("quality_level");
				}
				if (goodFeaturesToTrack.has("min_distance")) {
					minDistance = goodFeaturesToTrack.getInt("min_distance");
				}
			}

			//edge detection
			if (config.has("edge_detection")) {
				JSONObject edgeDetection = config.getJSONObject("edge_detection");
				if (edgeDetection.has("algorithm")) {
					edgeDetectionsAlgorithm = edgeDetection.getString("algorithm");
					if (edgeDetectionsAlgorithm.equalsIgnoreCase("Canny")) {//Canny
						JSONObject cannyConfig = edgeDetection.getJSONObject("canny_config");
						if (cannyConfig.has("lower_threshold")) {
							iCannyLowerThreshold = cannyConfig.getDouble("lower_threshold");
						}
						if (cannyConfig.has("upper_threshold")) {
							iCannyUpperThreshold = cannyConfig.getDouble("upper_threshold");
						}
					} else if (edgeDetectionsAlgorithm.equalsIgnoreCase("Sobel")) {//Sobel
						JSONObject sobelConfig = config.getJSONObject("sobel_config");
						if (sobelConfig.has("d_depth")) {
							dDepth = sobelConfig.getInt("d_depth");
						}
						if (sobelConfig.has("d_x")) {
							dx = sobelConfig.getInt("d_x");
						}
						if (sobelConfig.has("d_y")) {
							dy = sobelConfig.getInt("d_y");
						}
					} else {
                        Log.w(tag, "Unknown Config Algorithm");
					}
				}
			}

			//extras
			if (config.has("extras")) {
				JSONObject extras = config.getJSONObject("extras");
	        	if (extras.has("r")) {
	        		r = extras.getDouble("r");
	        		r = (r<0?0:(r>255?255:r));
	        	}
	        	if (extras.has("g")) {
	        		g = extras.getDouble("g");
	        		g = (g<0?0:(g>255?255:g));
	        	}
	        	if (extras.has("b")) {
	        		b = extras.getDouble("b");
	        		b = (b<0?0:(b>255?255:b));
	        	}
	        	if (extras.has("radius")) {
	        		radius = extras.getInt("radius");
	        	}
			}
		}

		Point p1 = new Point(x1, y1);
		Point p2 = new Point(x2, y2);

		Rect rect = new Rect(p1, p2);
//		Log.d(tag, "rect: " + rect.toString());

		Mat bgdModel = new Mat();
		Mat fgdModel = new Mat();
        String fgdSavepath;
        String gfttSavePath;

        try {

            Log.d(tag, "isOnValidationMode = " + (isOnValidationMode));

            Log.d(tag, "shapeUp. grabCut. begin");

            Imgproc.grabCut(imgMat, maskMat, rect, bgdModel, fgdModel, iterCount, Imgproc.GC_INIT_WITH_RECT);

            Log.d(tag, "shapeUp. grabCut. done");

            Core.compare(maskMat, new Scalar(Imgproc.GC_PR_FGD), maskMat, Core.CMP_EQ);

            Mat tempMask = new Mat();
            maskMat.copyTo(tempMask);

//            Imgproc.remap(tempMask, maskMat, map1, map2, Imgproc.INTER_CUBIC);
            Log.d(tag, "tempMask null?: " + (tempMask == null) );
            Log.d(tag, "maskMat null?: " + (maskMat == null) );
            Log.d(tag, "map1 null?: " + (map1 == null) );
            Log.d(tag, "map2 null?: " + (map2 == null) );
//            maskMat.copyTo(tempMask);
            //Smooth a maskMat
//            Imgproc.GaussianBlur(tempMask, maskMat, new Size(3, 3), 0);

            Mat foregroundMat = new Mat(imgMat.size(), CvType.CV_8UC3, new Scalar(255, 255, 255));
            imgMat.copyTo(foregroundMat, maskMat);

            String edgeAlgString;
            Mat edgifiedMat = new Mat(height, width, CvType.CV_8UC3, ZERO_SCALAR);

            if (edgeDetectionsAlgorithm.equalsIgnoreCase("Canny")) {
                edgeAlgString = "Canny";
                Imgproc.Canny(maskMat, edgifiedMat, iCannyLowerThreshold, iCannyUpperThreshold);
            } else if (edgeDetectionsAlgorithm.equalsIgnoreCase("Sobel")) {
                edgeAlgString = "Sobel";
                Imgproc.Sobel(maskMat, edgifiedMat, dDepth, dx, dy);
            } else {
                edgeAlgString = "Canny";
                Imgproc.Canny(maskMat, edgifiedMat, iCannyLowerThreshold, iCannyUpperThreshold);
            }

            //	    Log.d(tag, edgeAlgString + " done,moving on");

            MatOfPoint cornersMop = new MatOfPoint();

            Imgproc.goodFeaturesToTrack(edgifiedMat, cornersMop, maxCorners, qualityLevel, minDistance);

            //		Log.d(tag, "goodFeatures done,moving on");

            List<Point> corners = cornersMop.toList();
            Scalar color = new Scalar(r, g, b);

            //	    Log.d(tag, "starting point printing for");
            Mat goodFeaturesHighlight = new Mat(imgMat.size(), CvType.CV_8UC3, new Scalar(255, 255, 255));
            foregroundMat.copyTo(goodFeaturesHighlight);

            JSONObject actual;
            JSONArray pointsJsonArray = new JSONArray();
            for (Point point : corners) {
                actual = new JSONObject();
                actual.put("x", point.x);
                actual.put("y", point.y);
                pointsJsonArray.put(actual);
                Core.circle(goodFeaturesHighlight, point, radius, color);
            }

            //	    Log.d(tag, "finished point printing, point count: " + pointsJsonArray.length());
            //	    Log.d(tag, "result " + result.toString(1));

            Bitmap maskBitmap = Bitmap.createBitmap(maskMat.cols(), maskMat.rows(),
                    Bitmap.Config.RGB_565);
            Utils.matToBitmap(maskMat, maskBitmap);
            savePath = MADN3SCamera.saveBitmapAsJpeg(maskBitmap, "mask");
            Log.d(tag, "mask saved to " + savePath);

            Bitmap edgeBitmap = Bitmap.createBitmap(edgifiedMat.cols(), edgifiedMat.rows(),
                    Bitmap.Config.RGB_565);
            Utils.matToBitmap(edgifiedMat, edgeBitmap);
            savePath = MADN3SCamera.saveBitmapAsJpeg(edgeBitmap, edgeAlgString);
            Log.d(tag, edgeAlgString + " saved to " + savePath);

            Bitmap goodFeaturesBitmap = Bitmap.createBitmap(goodFeaturesHighlight.cols(),
                    goodFeaturesHighlight.rows(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(goodFeaturesHighlight, goodFeaturesBitmap);
            gfttSavePath = MADN3SCamera.saveBitmapAsJpeg(goodFeaturesBitmap, "good_features");
            Log.d(tag, "goodFeatures saved to " + gfttSavePath);

            Bitmap fgdBitmap = Bitmap.createBitmap(foregroundMat.cols(), foregroundMat.rows(),
                    Bitmap.Config.RGB_565);
            Utils.matToBitmap(foregroundMat, fgdBitmap);
            fgdSavepath = MADN3SCamera.saveBitmapAsJpeg(fgdBitmap, "fgd");
            Log.d(tag, "foreground saved to " + fgdSavepath);

            if(isOnValidationMode){
                exportedBitmap = goodFeaturesBitmap;
                savePath = gfttSavePath;
            } else {
                exportedBitmap = fgdBitmap;
                savePath = fgdSavepath;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            exportedBitmap.compress(Consts.BITMAP_COMPRESS_FORMAT, Consts.COMPRESSION_QUALITY, baos);
            byte[] bytes = baos.toByteArray();

            bytes = Base64.encode(bytes, Base64.DEFAULT);
            String md5Hex = MADN3SCamera.getMD5EncryptedString(bytes);
            Log.d(tag, "shapeUp. MD5 : " + md5Hex);

            JSONObject resultJsonObject = new JSONObject();
            resultJsonObject.put(KEY_MD5, md5Hex);
            resultJsonObject.put(KEY_FILE_PATH, savePath);
            resultJsonObject.put(KEY_POINTS, pointsJsonArray);

            imgMat.release();
            maskMat.release();
            fgdModel.release();
            bgdModel.release();
            foregroundMat.release();
            edgifiedMat.release();
            cornersMop.release();

            maskBitmap.recycle();
            fgdBitmap.recycle();
            edgeBitmap.recycle();

            Log.d(tag, "shapeUp. done");

            return resultJsonObject;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
	}

//	private Bitmap loadBitmap(String filePath){
//		Log.d(tag, "loadBitmap. filePath desde MidgetOfSeville: " + filePath);
//		Options options = new Options();
//		options.inPreferredConfig = Config.RGB_565;
//		options.inDither = true;
//		Bitmap imgBitmap = BitmapFactory.decodeFile(filePath, options);
//		Log.d(tag, "imgBitmap config: " + imgBitmap.getConfig().toString() + " hasAlpha: " + imgBitmap.hasAlpha());
//		return imgBitmap;
//	}

//    public Mat getMap1() {
//        return map1;
//    }

    public void setMap1(Mat map1) {
        this.map1 = map1;
    }

//    public Mat getMap2() {
//        return map2;
//    }

    public void setMap2(Mat map2) {
        this.map2 = map2;
    }
}
