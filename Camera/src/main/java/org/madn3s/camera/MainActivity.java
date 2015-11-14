package org.madn3s.camera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.madn3s.camera.io.BraveheartMidgetService;
import org.madn3s.camera.io.HiddenMidgetReader;
import org.madn3s.camera.io.UniversalComms;
import org.madn3s.camera.utils.Chron;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.madn3s.camera.Consts.*;

public class MainActivity extends Activity  implements CameraBridgeViewBase.CvCameraViewListener2,
        View.OnTouchListener {

    public static final int DIALOG_CALIBRATING = 0;
    public static final int DIALOG_PROCESSING = 1;

    private CameraBridgeViewBase mOpenCvCameraView;
    public Chron chron;

    private static final String tag = MainActivity.class.getSimpleName();
    private TextView configTextView;
    private ImageView calibrationButtonImageView;
    private MainActivity mActivity;
    private MidgetOfSeville figaro;

    public static AtomicBoolean isCapturing;
    public static AtomicBoolean isCalibrating;
    public static AtomicBoolean isManual;

    private CameraCalibrator mCalibrator;
    private OnCameraFrameRender previewRender;
    private OnCameraFrameRender calibrationRender;
    private OnCameraFrameRender undistortionRender;
    private int mWidth;
    private int mHeight;

    public static JSONObject config;

    private BaseLoaderCallback mLoaderCallback;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mActivity = this;
        BraveheartMidgetService.mActivity = this;
        figaro = MidgetOfSeville.getInstance();

        //Ambos en true para arrancar listo para calibrar
        isCalibrating = new AtomicBoolean(true);
        isCapturing = new AtomicBoolean(true);
        isManual = new AtomicBoolean(true);

        MADN3SCamera.isPictureTaken = new AtomicBoolean(true);
        MADN3SCamera.isRunning = new AtomicBoolean(true);

        setDiscoverableBt();
        setUpBridges();

        chron = new Chron(this);

        Intent williamWallaceIntent = new Intent(this, BraveheartMidgetService.class);
        startService(williamWallaceIntent);

        configTextView = (TextView) findViewById(R.id.configs_text_view);

        calibrationButtonImageView = (ImageView) findViewById(R.id.calibration_button_imageView);
        calibrationButtonImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonClick();
            }
        });

        Button calibrateButton = (Button) findViewById(R.id.manual_calibration_button);
        calibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCalibrateButtonClick();
            }
        });

        Button resetIterButton = (Button) findViewById(R.id.reset_iterations_button);
        resetIterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetIterations();
            }
        });

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_calibration_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (!MADN3SCamera.isOpenCvLoaded) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_5, this, mLoaderCallback);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:

                break;
            case R.id.action_toggle_mode:
                setCaptureMode(isCalibrating.get(), !isCapturing.get());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

        /*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.calibration:
                mOnCameraFrameRender =
                        new OnCameraFrameRender(new CalibrationFrameRender(mCalibrator));
                item.setChecked(true);
                return true;

            case R.id.undistortion:
                mOnCameraFrameRender =
                        new OnCameraFrameRender(new UndistortionFrameRender(mCalibrator));
                item.setChecked(true);
                return true;

            case R.id.calibrate:
                Log.d(tag, "switch a calibrate");
                final Resources res = getResources();
                if (mCalibrator.getCornersBufferSize() < 2) {
                    (Toast.makeText(this, res.getString(R.string.more_samples), Toast.LENGTH_SHORT)).show();
                    return true;
                }
                Log.d(tag, "vale a calibrate");

                mOnCameraFrameRender = new OnCameraFrameRender(new PreviewFrameRender(mCalibrator.mWidth, mCalibrator.mHeight));

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }*/

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    /**
     * Relaunch activity with request to set Device discoverable over Bluetooth
     */
	private void setDiscoverableBt() {
		Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, MADN3SCamera.DISCOVERABLE_TIME);
		startActivity(discoverableIntent);
	}

    /**
	 * Sets up OpenCV Init Callback <code>UniversalComms</code> Bridges and Camera Callbacks
	 */
    private void setUpBridges() {
    	mLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS:
                    {
                        Log.i(tag, "OpenCV loaded successfully");
                        MADN3SCamera.isOpenCvLoaded = true;
                        mOpenCvCameraView.enableView();
                        mOpenCvCameraView.setOnTouchListener(MainActivity.this);

                        /* Load configuration data */
                        loadConfigData();
                    }
                    break;
                    default: {
                        super.onManagerConnected(status);
                    } break;
                }
            }
        };

        //Received Message Callback
    	HiddenMidgetReader.bridge = new UniversalComms() {
			@Override
			public void callback(Object msg) {
				final String msgFinal = (String) msg;
				mActivity.getWindow().getDecorView().post(
					new Runnable() {
						public void run() {
							configTextView.setText(msgFinal);
						}
					});
				Intent williamWallaceIntent = new Intent(getBaseContext(), BraveheartMidgetService.class);
				williamWallaceIntent.putExtra(Consts.EXTRA_CALLBACK_MSG, (String) msg);
				startService(williamWallaceIntent);
			}
		};

        /**
         * Punto de entrada desde el Service
         */
		BraveheartMidgetService.cameraCallback = new UniversalComms() {
			@Override
			public void callback(Object msg) {
                setCaptureMode(false, true);
                config = (JSONObject) msg;
				Log.d(tag, "takePhoto. config == null? " + (config == null));
			}
		};
	}

	/**
	 * Updates layout to reflect the application is working on processing the picture
     * @param mode Dialog mode, Calibrating or Processing
     */
    private void setWorking(int mode) {
        calibrationButtonImageView.setClickable(false);
        calibrationButtonImageView.setEnabled(false);

        progressDialog = new ProgressDialog(mActivity);

        switch(mode){
            case DIALOG_CALIBRATING:
                progressDialog.setTitle(mActivity.getString(R.string.calibrating));
                break;
            case DIALOG_PROCESSING:
                progressDialog.setTitle(mActivity.getString(R.string.processing));
                break;
        }

        progressDialog.setMessage(mActivity.getString(R.string.please_wait));
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.show();
    }

	/**
	 * Updates layout to reflect the application is done working on processing the picture
	 */
	private void unsetWorking(){
		calibrationButtonImageView.setClickable(true);
		calibrationButtonImageView.setEnabled(true);
        progressDialog.dismiss();
	}

//    private void playSound(String title, String msg){
//        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//
//        Notification mNotification = new Notification.Builder(this)
//                .setContentTitle(title)
//                .setContentText(msg)
//                .setSmallIcon(R.drawable.ic_launcher)
////                .setContentIntent(pIntent)
//                .setSound(soundUri)
//                .build();
//
//        NotificationManager mNotificationManager =
//                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        mNotificationManager.notify(0, mNotification);
//    }

    //Metodos de CvCameraViewListener2
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat resultMat;

        synchronized (this){
            if (isCapturing.get()) {
                if (isCalibrating.get()) {
                    resultMat = calibrationRender.render(inputFrame);
                } else {
                    setCaptureMode(false, false);
                    resultMat = undistortionRender.render(inputFrame);
                    final Mat undistortionResult = resultMat.clone();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
//                            processFrameCallback(undistortionResult);
                            capturePhoto(undistortionResult);
                        }
                    });
                }
            } else {
                if(isManual.get()){
                    resultMat = undistortionRender.render(inputFrame);
                } else {
                    resultMat = previewRender.render(inputFrame);
                }
            }
            return resultMat;
        }
    }

    private void capturePhoto(final Mat resultMat){
        new AsyncTask<Void, Void, JSONObject>() {

            @Override
            protected void onPreExecute() {
                setWorking(DIALOG_PROCESSING);
                chron.restartChron();
            }

            @Override
            protected JSONObject doInBackground(Void... params) {
                JSONObject result = new JSONObject();

                try {
                    JSONArray pointsJson = null;
                    JSONObject resultJson = figaro.shapeUp(resultMat, config, isManual.get());

                    if (resultJson != null && resultJson.has(KEY_POINTS)) {
                        pointsJson = resultJson.getJSONArray(KEY_POINTS);
                    }

                    if (pointsJson != null && pointsJson.length() > 0) {
                        result.put(KEY_MD5, resultJson.get(KEY_MD5));
                        result.put(Consts.KEY_ERROR, false);
                        result.put(Consts.KEY_POINTS, pointsJson);
                        MADN3SCamera.sharedPrefsPutString(KEY_FILE_PATH, resultJson.getString(KEY_FILE_PATH));
                    } else {
                        result.put(Consts.KEY_ERROR, true);
                    }
                } catch (JSONException e) {
                    Log.e(tag, "Couldn't execute MidgetOfSeville.shapeUp to Camera Frame");
                    e.printStackTrace();
                    result = new JSONObject();
                }

                return result;
            }

            @Override
            protected void onPostExecute(JSONObject resultJson) {
                try {
                    int iteration = MADN3SCamera.sharedPrefsGetInt(KEY_ITERATION);
                    chron.stopChron("processing image");
                    if (!resultJson.getBoolean(KEY_ERROR)) {
                        MADN3SCamera.saveJsonToExternal(resultJson.toString(), "iteration-" + iteration, true, true);
                        MADN3SCamera.sharedPrefsPutInt(KEY_ITERATION, iteration + 1);
                    } else {
                        Toast.makeText(mActivity, "Error procesando foto, intenta de nuevo",
                                Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(tag, "Failure on shapeUp in doInBackground. resultJson null.");
                } finally {
                    unsetWorking();
                    chron.stopChron("Processing Image");
                    calibrationButtonImageView.setEnabled(true);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void processFrameCallback(final Mat resultMat) {
        new AsyncTask<Void, Void, JSONObject>() {

            @Override
            protected void onPreExecute() {
                setWorking(DIALOG_PROCESSING);
                chron.restartChron();
            }

            @Override
            protected JSONObject doInBackground(Void... params) {
                JSONObject result = new JSONObject();

                try {
                    JSONArray pointsJson = null;
                    JSONObject resultJson = figaro.shapeUp(resultMat, config, isManual.get());

                    if (resultJson != null && resultJson.has(KEY_POINTS)) {
                        pointsJson = resultJson.getJSONArray(KEY_POINTS);
                    }

                    if (pointsJson != null && pointsJson.length() > 0) {
                        result.put(KEY_MD5, resultJson.get(KEY_MD5));
                        result.put(Consts.KEY_ERROR, false);
                        result.put(Consts.KEY_POINTS, pointsJson);
                        MADN3SCamera.sharedPrefsPutString(KEY_FILE_PATH, resultJson.getString(KEY_FILE_PATH));
                    } else {
                        result.put(Consts.KEY_ERROR, true);
                    }
                } catch (JSONException e) {
                    Log.e(tag, "Couldn't execute MidgetOfSeville.shapeUp to Camera Frame");
                    e.printStackTrace();
                    result = new JSONObject();
                }

                return result;
            }

            @Override
            protected void onPostExecute(JSONObject resultJson) {
                try {
                    int iteration = MADN3SCamera.sharedPrefsGetInt(KEY_ITERATION);
                    chron.stopChron("processing image");
                    if (!resultJson.getBoolean(KEY_ERROR)) {
                        MADN3SCamera.saveJsonToExternal(resultJson.toString(), "iteration-" + iteration, true, true);
                        MADN3SCamera.sharedPrefsPutInt(KEY_ITERATION, iteration + 1);


                        /// Envío Agregado
                        Intent williamWallaceIntent = new Intent(mActivity, BraveheartMidgetService.class);
                        williamWallaceIntent.putExtra(Consts.EXTRA_RESULT, resultJson.toString());
                        startService(williamWallaceIntent);
                        /// Envío Agregado

                    } else {
                        Toast.makeText(mActivity, "Error procesando foto, intenta de nuevo",
                                Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(tag, "Failure on shapeUp in doInBackground. resultJson null.");
                } finally {
                    unsetWorking();
                    chron.stopChron("Processing Image");
                    calibrationButtonImageView.setEnabled(true);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onCameraViewStopped() {
        Log.d(tag, "onCameraViewStopped");
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.d(tag, "onCameraViewStarted");
        if (mWidth != width || mHeight != height) {
            mWidth = width;
            mHeight = height;
            mCalibrator = new CameraCalibrator(mWidth, mHeight);
            if (CalibrationResult.tryLoad(this, mCalibrator.getCameraMatrix(),
                    mCalibrator.getDistortionCoefficients())) {
                mCalibrator.setCalibrated();
                setCaptureMode(false, false);
            }

            previewRender = new OnCameraFrameRender(new PreviewFrameRender(mCalibrator.mWidth, mCalibrator.mHeight));
            calibrationRender = new OnCameraFrameRender(new CalibrationFrameRender(mCalibrator));
            undistortionRender = new OnCameraFrameRender(new UndistortionFrameRender(mCalibrator));
        }
    }

    public void setCaptureMode(final boolean calibrate, final boolean capture) {
        synchronized (this){
            isCalibrating.set(calibrate);
            isCapturing.set(capture);
        }
    }

    /* Metodos de OnTouchListener */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mCalibrator.addCorners();
        return false;
    }

    private void onCalibrateButtonClick(){
        calibrateCamera();
    }

    private void calibrateCamera() {
        try {

            // Warning si no hay suficientes puntos
            if (mCalibrator.getCornersBufferSize() < 2) {
                Toast.makeText(mActivity, mActivity.getString(R.string.more_samples),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            new AsyncTask<Void, Void, Bundle>() {

                @Override
                protected void onPreExecute() {
                    setWorking(DIALOG_CALIBRATING);
                    chron.restartChron();
//                    setCaptureMode(false, false);
                }

                @Override
                protected Bundle doInBackground(Void... params) {
                    Bundle result = null;
                    try {
                        result = mCalibrator.calibrate();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return result;
                }

                @Override
                protected void onPostExecute(Bundle result) {
                    mCalibrator.clearCorners();
                    chron.stopChron("calibration");

                    String resultMessage = (mCalibrator.isCalibrated()) ?
                            mActivity.getString(R.string.calibration_successful)
                                    + " " + mCalibrator.getAvgReprojectionError() :
                            mActivity.getString(R.string.calibration_unsuccessful);
                    (Toast.makeText(MainActivity.this, resultMessage, Toast.LENGTH_LONG)).show();

                    JSONObject resultForIntent = new JSONObject();
                    JSONObject calibPayload = new JSONObject();

                    /* Se guarda el resultado de la calibración en SharedPrefs */
                    if (mCalibrator.isCalibrated()) {
                        CalibrationResult.save(MainActivity.this,
                                mCalibrator.getCameraMatrix(), mCalibrator.getDistortionCoefficients());
                    }
                    /* Fin - Se guarda el resultado de la calibración en SharedPrefs */

                    try {
                        Log.i(tag, "Result: ");
                        Log.i(tag, "Calibration Coefficients: " + result.getString(Consts.KEY_CALIB_DISTORTION_COEFFICIENTS));
                        Log.i(tag, "Camera Matrix: " + result.getString(Consts.KEY_CALIB_CAMERA_MATRIX));
                        Log.i(tag, "Image Points: " + result.getString(Consts.KEY_CALIB_IMAGE_POINTS));

                        /* Set Calib Payload */
                        calibPayload.put(Consts.KEY_CALIB_DISTORTION_COEFFICIENTS, result.getString(Consts.KEY_CALIB_DISTORTION_COEFFICIENTS));
                        calibPayload.put(Consts.KEY_CALIB_CAMERA_MATRIX, result.getString(Consts.KEY_CALIB_CAMERA_MATRIX));
                        calibPayload.put(Consts.KEY_CALIB_IMAGE_POINTS, result.getString(Consts.KEY_CALIB_IMAGE_POINTS));
                        calibPayload.put(Consts.KEY_ACTION, Consts.ACTION_CALIBRATION_RESULT);
                        /* End - Set Calib Payload */

                        /* Guardar Sistema de Archivos */
                        MADN3SCamera.saveJsonToExternal(calibPayload.toString(), "camera-calibration", false, false);

                        /* Enviar a Controller */
                        resultForIntent.put(Consts.KEY_ACTION, Consts.ACTION_SEND_CALIBRATION_RESULT);
                        resultForIntent.put(Consts.KEY_CALIBRATION_RESULT, calibPayload.toString());

                        Intent williamWallaceIntent = new Intent(getBaseContext(), BraveheartMidgetService.class);
                        williamWallaceIntent.putExtra(Consts.EXTRA_CALLBACK_MSG, resultForIntent.toString());
                        startService(williamWallaceIntent);

                        Log.i(tag, "Result Ok");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    unsetWorking();
                }

            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (Exception e) {
            Log.e(tag, "Exception instantiating CameraCalibrator. ", e);
        }
    }

    /**
     * Carga datos de configuracion de existir el archivo. Ya {@link CalibrationResult} se encarga de
     * cargar los datos de calibración de la cámara desde SharedPreferences en <code>tryLoad</code>
     */
    private void loadConfigData(){
        String filename = "config.json";
        JSONObject configJson;

        MADN3SCamera.sharedPrefsPutString(KEY_PROJECT_NAME, "graduation");

        try {
            configJson = MADN3SCamera.getInputJson(filename);
            if(configJson.has(KEY_SIDE)){
                MADN3SCamera.sharedPrefsPutString(KEY_SIDE, configJson.getString(KEY_SIDE));
            }
        } catch (JSONException e){
            Log.e(tag, "Error Parsing config JSONObject");
            e.printStackTrace();
        }

        loadCalibration();
    }

    /**
     * Carga datos de calibración estereo de existir el archivo. Ya {@link CalibrationResult} se encarga de
     * cargar los datos de calibración de la cámara desde SharedPreferences en <code>tryLoad</code>
     */
    private void loadCalibration(){
        try {

            String filename = "calibration-stereo.json";
            String side = MADN3SCamera.sharedPrefsGetString(KEY_SIDE);
            JSONObject calibJson = MADN3SCamera.getInputJson(filename);
            calibJson = new JSONObject(calibJson.getString(KEY_CALIBRATION_RESULT));

            // Parsear Maps
            String map1Str = calibJson.getJSONObject(side).getString(Consts.KEY_CALIB_MAP_1);
            Log.d(tag, "map1: " + map1Str);
            String map2Str = calibJson.getJSONObject(side).getString(Consts.KEY_CALIB_MAP_2);
            Log.d(tag, "map2: " + map2Str);
            Mat map1 = MADN3SCamera.getMatFromString(map1Str);
            Mat map2 = MADN3SCamera.getMatFromString(map2Str);

            MidgetOfSeville figaro = MidgetOfSeville.getInstance();
            figaro.setMap1(map1);
            figaro.setMap2(map2);

//            // Setear Camera Matrix y DistCoeffs en MidgetOfSeville
//            String cameraMatrixStr = calibJson.getJSONObject(side).getString(Consts.KEY_CALIB_CAMERA_MATRIX);
//            String distCoeffsStr = calibJson.getJSONObject(side).getString(Consts.KEY_CALIB_DISTORTION_COEFFICIENTS);
//            Mat cameraMatrix = MADN3SCamera.getMatFromString(cameraMatrixStr);
//            Mat distCoeffs = MADN3SCamera.getMatFromString(distCoeffsStr);
//            Log.d(tag, "Obteniendo calib_camera_matrix:" + cameraMatrixStr);

        } catch (JSONException e){
            Log.e(tag, "Couldn't load calibration JSON");
            e.printStackTrace();
        }
    }

    /**
     * Returns <code>KEY_ITERATION</code> value on {@link android.content.SharedPreferences } to <code>0</code>
     */
    private void resetIterations(){
        MADN3SCamera.sharedPrefsPutInt(KEY_ITERATION, 0);
    }

    private void onButtonClick() {
        setCaptureMode(false, true);
    }

}
