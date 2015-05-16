package org.madn3s.camera;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.madn3s.camera.io.BraveheartMidgetService;

import static org.madn3s.camera.Consts.KEY_FILE_PATH;
import static org.madn3s.camera.Consts.KEY_MD5;
import static org.madn3s.camera.Consts.KEY_POINTS;

/**
 * Created by fernando on 14/05/15.
 */
public class nameToDefineAsyncTask extends AsyncTask<Void, Void, JSONObject> {

    private static final String tag = nameToDefineAsyncTask.class.getSimpleName();
        @Override
        protected void onPreExecute() {
//            setWorking();
//            resetChron();
//            startChron();
//            isCapturing.set(false);
//            takePictureImageView.setEnabled(false);
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            JSONObject result = new JSONObject();
//            Log.d(tag, "onPicureTaken. doInBackground.");
//            try {
//                Log.d(tag, "onPicureTaken. config: " + config.toString());
//                MADN3SCamera.position = config.getString(Consts.KEY_SIDE);
//                MADN3SCamera.projectName = config.getString(Consts.KEY_PROJECT_NAME);
//            } catch (Exception e) {
//                MADN3SCamera.position = Consts.VALUE_DEFAULT_POSITION;
//                MADN3SCamera.projectName = Consts.VALUE_DEFAULT_PROJECT_NAME;
//                Log.e(tag, "onPicureTaken. Error parsing JSONObject. Fallback to default config", e);
//            }
//
//            Log.d(tag, "onPicureTaken. doInBackground.");
//            MidgetOfSeville figaro = new MidgetOfSeville();
//            int orientation;
//            Bitmap bMap = BitmapFactory.decodeByteArray(mData, 0, mData.length
//                    , Consts.bitmapFactoryOptions);
//
//            if(bMap.getHeight() < bMap.getWidth()){
//                orientation = 90;
//            } else {
//                orientation = 0;
//            }
//
//            Bitmap bMapRotate;
//            if (orientation != 0) {
//                Matrix matrix = new Matrix();
//                matrix.postRotate(orientation);
//                bMapRotate = Bitmap.createBitmap(bMap, 0, 0, bMap.getWidth(), bMap.getHeight()
//                        , matrix, true);
//            } else {
//                bMapRotate = Bitmap.createScaledBitmap(bMap, bMap.getWidth(), bMap.getHeight(), true);
//            }
//
//            try {
//                String filePath = MADN3SCamera.saveBitmapAsJpeg(bMapRotate, MADN3SCamera.position, MADN3SCamera.iteration);
//
//                Log.d(tag, "filePath desde MainActivity: " + filePath);
//
//                mCalibrator.calibrate();
//
//                JSONObject resultJsonObject = figaro.shapeUp(filePath, config);
//
//                JSONArray pointsJson = resultJsonObject.getJSONArray(KEY_POINTS);
//
//                SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.app_name)
//                        , MODE_PRIVATE);
//                SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
//                sharedPreferencesEditor.putString(KEY_FILE_PATH, resultJsonObject.getString(KEY_FILE_PATH));
//                sharedPreferencesEditor.apply();
//
//                if(pointsJson != null && pointsJson.length() > 0){
//                    result.put(KEY_MD5, resultJsonObject.get(KEY_MD5));
//                    result.put(Consts.KEY_ERROR, false);
//                    result.put(Consts.KEY_POINTS, pointsJson);
//                } else {
//                    Log.d(tag, "pointsJson: " + pointsJson.toString(1));
//                    result.put(Consts.KEY_ERROR, true);
//                }
//                Log.d(tag, "mPictureCalback. result: " + result.toString(1));
//
//            } catch (JSONException e) {
//                e.printStackTrace();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
            return result;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
//            //No eliminar, mantiene preview para subsecuentes fotos
//            mCamera.startPreview();
//            stopChron();
//            showElapsedTime("processing image");
//
//
//            mCalibrator.clearCorners();
//            mOnCameraFrameRender = new OnCameraFrameRender(new CalibrationFrameRender(mCalibrator));
//            String resultMessage = (mCalibrator.isCalibrated()) ?
//                    mActivity.getString(R.string.calibration_successful)
//                            + " " + mCalibrator.getAvgReprojectionError() :
//                    mActivity.getString(R.string.calibration_unsuccessful);
//            (Toast.makeText(MainActivity.this, resultMessage, Toast.LENGTH_LONG)).show();
//
//                    /* Se guarda el resultado de la calibración en SharedPrefs */
//            if (mCalibrator.isCalibrated()) {
//                CalibrationResult.save(MainActivity.this,
//                        mCalibrator.getCameraMatrix(), mCalibrator.getDistortionCoefficients());
//            }
//                    /* Fin - Se guarda el resultado de la calibración en SharedPrefs */
//
//            if(result != null){
//                Intent williamWallaceIntent = new Intent(mContext, BraveheartMidgetService.class);
//                williamWallaceIntent.putExtra(Consts.EXTRA_RESULT, result.toString());
//                startService(williamWallaceIntent);
//            }
//            unsetWorking();
        }

//    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
}
