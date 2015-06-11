package org.madn3s.controller;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

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

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.madn3s.controller.Consts.KEY_FILE_PATH;
import static org.madn3s.controller.Consts.KEY_MD5;
import static org.madn3s.controller.Consts.KEY_POINTS;
import static org.madn3s.controller.Consts.KEY_PROJECT_NAME;
import static org.madn3s.controller.Consts.SIDE_LEFT;
import static org.madn3s.controller.Consts.SIDE_RIGHT;

/**
 * Created by fernando on 09/06/15.
 */
public class CameraMidget {
    private static final String tag = MidgetOfSeville.class.getSimpleName();
    private static final Scalar ZERO_SCALAR = new Scalar(0);
    private int iterCount = 1;
    private Mat map1Left;
    private Mat map2Left;
    private Mat map1Right;
    private Mat map2Right;

    private static CameraMidget me;

    public static CameraMidget getInstance(){
        if(me == null){
            me = new CameraMidget();
        }

        return me;
    }

    public JSONObject shapeUp(Bitmap imgBitmap, String side, JSONObject config) throws JSONException {
        int height = imgBitmap.getHeight();
        int width = imgBitmap.getWidth();

        Mat imgMat = new Mat(height, width, CvType.CV_8UC3);
        Utils.bitmapToMat(imgBitmap, imgMat);
        return shapeUp(imgMat, side, config);
    }

    public JSONObject shapeUp(String filename, String side, JSONObject configs) throws JSONException{
        Bitmap imgBitmap = loadBitmap(filename);
        return shapeUp(imgBitmap, side, configs);
    }

    public JSONObject shapeUp(Mat imgMat, String side, JSONObject config) throws JSONException {
        Log.d(tag, "shapeUp.");
        String savePath;
        Log.d(tag, "imgMat size before resize: w:" + (imgMat.cols()) + " h:" + (imgMat.rows()));
        // TODO Comentado para pruebas manuales
//        Imgproc.resize(imgMat, imgMat, new Size(486, 648));
        Log.d(tag, "imgMat size after resize: w:" + (imgMat.cols()) + " h:" + (imgMat.rows()));

        int height = imgMat.rows();
        int width = imgMat.cols();
        Log.d(tag, "width:" + width + " height:" + height);

        Imgproc.cvtColor(imgMat, imgMat, Imgproc.COLOR_RGBA2RGB);

        Mat maskMat = new Mat(height, width, CvType.CV_8UC1, ZERO_SCALAR);

        double x1 = width / 4;
        double y1 = 0;
        double x2 =  x1 * 3;
        double y2 = height;
        double iCannyLowerThreshold = 35;
        double iCannyUpperThreshold = 75;
        String edgeDetectionsAlgorithm = "canny";
        int maxCorners = 50;
        double qualityLevel = 0.01;
        double minDistance = 30;

        int ddepth = 0;
        int dx = 0;
        int dy = 0;

        double r = 255, g = 0, b = 0;
        int radius = 10;

        if (config != null) {
            //grabCut
            if (config.has("grab_cut")) {
                JSONObject grabCut = config.getJSONObject("grab_cut");
                if (grabCut.has("rectangle")) {
                    JSONObject points = grabCut.getJSONObject("rectangle");
                    JSONObject point1 = points.getJSONObject("point_1");
                    JSONObject point2 = points.getJSONObject("point_2");
                    x1 = point1.getDouble("x");
                    y1 = point1.getDouble("y");
                    x2 = point2.getDouble("x");
                    y2 = point2.getDouble("y");
                }
                if (grabCut.has("iterations")) {
                    iterCount = grabCut.getInt("iterations");
                }
            }

            //goodFeaturesToTrack
            if (config.has("good_features")) {
                JSONObject goodFeaturesToTrack = config.getJSONObject("good_features");
                if (goodFeaturesToTrack.has("max_corners")) {
                    maxCorners = config.getInt("max_corners");
                }
                if (goodFeaturesToTrack.has("quality_level")) {
                    qualityLevel = config.getDouble("quality_level");
                }
                if (goodFeaturesToTrack.has("min_distance")) {
                    minDistance = config.getInt("min_distance");
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
                            ddepth = sobelConfig.getInt("d_depth");
                        }
                        if (sobelConfig.has("d_x")) {
                            dx = sobelConfig.getInt("d_x");
                        }
                        if (sobelConfig.has("d_y")) {
                            dy = sobelConfig.getInt("d_y");
                        }
                    } else {
                        //Mandamos un algoritmo q no sabemos
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

        try {

            Log.d(tag, "imgMat size before grabcut: w:" + (imgMat.cols()) + " h:" + (imgMat.rows()));
            Log.d(tag, "shapeUp. grabcut. begin");

            Imgproc.grabCut(imgMat, maskMat, rect, bgdModel, fgdModel, iterCount, Imgproc.GC_INIT_WITH_RECT);

            Log.d(tag, "shapeUp. grabcut. done");
            Log.d(tag, "imgMat size after grabcut: w:" + (imgMat.cols()) + " h:" + (imgMat.rows()));

            Core.compare(maskMat, new Scalar(Imgproc.GC_PR_FGD), maskMat, Core.CMP_EQ);

//            Mat tempMask = maskMat.clone();
//            if(side.equalsIgnoreCase(SIDE_LEFT)){
//                Imgproc.remap(tempMask, maskMat, map1Left, map2Left, Imgproc.INTER_CUBIC);
//            } else if(side.equalsIgnoreCase(SIDE_RIGHT)){
//                Imgproc.remap(tempMask, maskMat, map1Right, map2Right, Imgproc.INTER_CUBIC);
//            } else {
//                Log.wtf(tag, "No side specified for remap");
//                return null;
//            }
//            Log.d(tag, "tempMask size: w:" + (tempMask.size().width) + " h:" + (tempMask.size().height));
//            Log.d(tag, "maskMat size: w:" + (maskMat.size().width) + " h:" + (maskMat.size().height));
//            // TODO comentado para ver resultado final
////            maskMat.copyTo(tempMask);
//            //Smooth a maskMat
//            Imgproc.GaussianBlur(tempMask, maskMat, new Size(3, 3), 0);

            Mat foregroundMat = new Mat(height, width, CvType.CV_8UC3, new Scalar(255, 255, 255));
            imgMat.copyTo(foregroundMat, maskMat);

            String edgeAlgString;
            Mat edgifiedMat = new Mat(height, width, CvType.CV_8UC3, ZERO_SCALAR);

            if (edgeDetectionsAlgorithm.equalsIgnoreCase("Canny")) {
                edgeAlgString = "Canny";
                Imgproc.Canny(maskMat, edgifiedMat, iCannyLowerThreshold, iCannyUpperThreshold);
            } else if (edgeDetectionsAlgorithm.equalsIgnoreCase("Sobel")) {
                edgeAlgString = "Sobel";
                Imgproc.Sobel(maskMat, edgifiedMat, ddepth, dx, dy);
            } else {
                edgeAlgString = "Canny";
                Imgproc.Canny(maskMat, edgifiedMat, iCannyLowerThreshold, iCannyUpperThreshold);
            }

            MatOfPoint cornersMop = new MatOfPoint();

            Imgproc.goodFeaturesToTrack(edgifiedMat, cornersMop, maxCorners, qualityLevel, minDistance);

            List<Point> corners = cornersMop.toList();
            Scalar color = new Scalar(r, g, b);

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

            Bitmap maskBitmap = Bitmap.createBitmap(maskMat.cols(), maskMat.rows(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(maskMat, maskBitmap);
            savePath = MADN3SController.saveBitmapAsJpeg(maskBitmap, side + "_" +"mask");
            //		Log.d(tag, "mask saved to " + savePath);

            Bitmap edgeBitmap = Bitmap.createBitmap(edgifiedMat.cols(), edgifiedMat.rows(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(edgifiedMat, edgeBitmap);
            savePath = MADN3SController.saveBitmapAsJpeg(edgeBitmap, side + "_" + edgeAlgString);
            //		Log.d(tag, edgeAlgString + " saved to " + savePath);

            Bitmap goodFeaturesBitmap = Bitmap.createBitmap(goodFeaturesHighlight.cols(), goodFeaturesHighlight.rows(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(goodFeaturesHighlight, goodFeaturesBitmap);
            savePath = MADN3SController.saveBitmapAsJpeg(goodFeaturesBitmap, side + "_" + "good_features");
            Log.d(tag, "goodFeatures saved to " + savePath);

            Bitmap fgdBitmap = Bitmap.createBitmap(foregroundMat.cols(), foregroundMat.rows(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(foregroundMat, fgdBitmap);
            savePath = MADN3SController.saveBitmapAsJpeg(fgdBitmap, side + "_" + "fgd");
            Log.d(tag, "foreground saved to " + savePath);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            fgdBitmap.compress(Consts.BITMAP_COMPRESS_FORMAT, Consts.COMPRESSION_QUALITY, baos);
            byte[] bytes = baos.toByteArray();

            bytes = Base64.encode(bytes, Base64.DEFAULT);
            String md5Hex = MADN3SController.getMD5EncryptedString(bytes);
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

    private Bitmap loadBitmap(String filename){
        String projectName = MADN3SController.sharedPrefsGetString(KEY_PROJECT_NAME);
        String path = MADN3SController.getAppDirectory() + "/" + projectName + "/" + filename;
        Log.d(tag, "loadBitmap. filename desde CameraMidget: " + path);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inDither = true;
        Bitmap imgBitmap = BitmapFactory.decodeFile(path, options);
        Log.d(tag, "imgBitmap config: " + imgBitmap.getConfig().toString() + " hasAlpha: " + imgBitmap.hasAlpha());
        return imgBitmap;
    }

    public void setMap1Left(Mat map1Left) {
        this.map1Left = map1Left;
        Log.d(tag, "setMap1Left.type == CV_32FC1? " + (map1Left.type() == CvType.CV_32FC1));
    }

    public void setMap2Left(Mat map2Left) {
        this.map2Left = map2Left;
    }

    public void setMap1Right(Mat map1Right) {
        this.map1Right = map1Right;
    }

    public void setMap2Right(Mat map2Right) {
        this.map2Right = map2Right;
    }
}
