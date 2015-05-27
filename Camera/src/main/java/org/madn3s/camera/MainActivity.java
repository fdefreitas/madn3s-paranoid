package org.madn3s.camera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.madn3s.camera.io.BraveheartMidgetService;
import org.madn3s.camera.io.HiddenMidgetReader;
import org.madn3s.camera.io.UniversalComms;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.madn3s.camera.Consts.*;

public class MainActivity extends Activity  implements CameraBridgeViewBase.CvCameraViewListener2,
        View.OnTouchListener {

    private CameraBridgeViewBase mOpenCvCameraView;

	private static final String tag = MainActivity.class.getSimpleName();
    private Camera mCamera;
    private CameraPreview mPreview;
    private FrameLayout cameraPreviewFrameLayout;
    private Context mContext;
    private TextView configTextView;
    private RelativeLayout workingLayout;
    private ImageView takePictureImageView;
    private Chronometer elapsedChronometer;
    private MainActivity mActivity;
    private MidgetOfSeville figaro;

    public static AtomicBoolean isCapturing;
    public static AtomicBoolean isCalibrating;

    private CameraCalibrator mCalibrator;
    private OnCameraFrameRender mOnCameraFrameRender;
    private int mWidth;
    private int mHeight;

    public JSONObject config, result;

    private BaseLoaderCallback mLoaderCallback;
    private ProgressDialog calibrationProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mActivity = this;
		mContext = this;
		BraveheartMidgetService.mActivity = this;
        figaro = MidgetOfSeville.getInstance();
        isCapturing = new AtomicBoolean(false);
        isCalibrating = new AtomicBoolean(false);

		setDiscoverableBt();
		setUpBridges();

		Intent williamWallaceIntent = new Intent(this, BraveheartMidgetService.class);
		startService(williamWallaceIntent);

		workingLayout = (RelativeLayout) findViewById(R.id.working_layout);
		workingLayout.setVisibility(View.GONE);
		configTextView = (TextView) findViewById(R.id.configs_text_view);

		elapsedChronometer = (Chronometer) findViewById(R.id.elapsed_chronometer);
		resetChron();

		takePictureImageView = (ImageView) findViewById(R.id.take_picture_imageView);
		takePictureImageView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
                onButtonClick();
			}
		});

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_calibration_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

		MADN3SCamera.isPictureTaken = new AtomicBoolean(true);
		MADN3SCamera.isRunning = new AtomicBoolean(true);
    }

    @Override
    public void onResume(){
    	Log.d(tag, "onResume");
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(!MADN3SCamera.isOpenCvLoaded) {
        	OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_5, this, mLoaderCallback);
        }

        if(MADN3SCamera.hasReceivedCalibration) {
        	try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        	startCamera();
        	MADN3SCamera.hasReceivedCalibration = false;
        }
    }

    @Override
    protected void onPause() {
    	Log.d(tag, "onPause");
    	if(MADN3SCamera.hasInvokedCalibration) {
//    		releaseCamera();
    		MADN3SCamera.hasInvokedCalibration = false;
        }
        super.onPause();
    }

    /**
     * Sets up {@link Camera} instance and the {@link CameraPreview} associated with it
     */
    protected void startCamera() {
    	Log.d(tag, "startCamera");
		mCamera = MADN3SCamera.getCameraInstance();
		mPreview = new CameraPreview(this, mCamera);
        startCameraPreview();
	}

    protected void startCameraPreview(){
    	if(mCamera != null){
            cameraPreviewFrameLayout.removeAllViews();
            cameraPreviewFrameLayout.addView(mPreview);
        	mCamera.startPreview();
        }else {
        	Toast.makeText(this,  "Couldn't start Camera Preview", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Releases {@link Camera} instance
     */
    protected void releaseCamera(){
    	Log.d(tag, "releaseCamera");
    	mPreview.getHolder().removeCallback(mPreview);
    	mCamera = MADN3SCamera.getCameraInstance();
    	mCamera.setPreviewCallback(null);
        stopCameraPreview();
        mCamera.release();
        mCamera = null;
    }

    protected void stopCameraPreview(){
    	if (mCamera != null){
        	mCamera.stopPreview();
        } else {
        	Toast.makeText(this,  "Couldn't stop Camera Preview", Toast.LENGTH_SHORT).show();
        }
    }

    public void showElapsedTime(String msg) {
        long elapsedMillis = SystemClock.elapsedRealtime() - elapsedChronometer.getBase();
        Toast.makeText(this, (msg == null? "" : msg) + " : " + elapsedMillis,
                Toast.LENGTH_SHORT).show();
    }

    public void startChron(){
    	elapsedChronometer.start();
    }

    public void stopChron(){
    	elapsedChronometer.stop();
    }

    public void resetChron(){
    	elapsedChronometer.setBase(SystemClock.elapsedRealtime());
    	showElapsedTime("resetChron");
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
                //instanciar new renderer
                isCapturing.set(!isCapturing.get());

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
//        menu.findItem(R.id.preview_mode).setEnabled(true);
//        if (!mCalibrator.isCalibrated())
//            menu.findItem(R.id.preview_mode).setEnabled(false);
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
                    } break;
                    default:
                    {
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
                isCapturing.set(true);
				config = (JSONObject) msg;
				Log.d(tag, "takePhoto. config == null? " + (config == null));
			}
		};
	}

	/**
	 * Updates layout to reflect the application is working on processing the picture
	 */
	private void setWorking(){
		takePictureImageView.setClickable(false);
		takePictureImageView.setEnabled(false);
		workingLayout.setVisibility(View.VISIBLE);
	}

	/**
	 * Updates layout to reflect the application is done working on processing the picture
	 */
	private void unsetWorking(){
		takePictureImageView.setClickable(true);
		takePictureImageView.setEnabled(true);
		workingLayout.setVisibility(View.GONE);
	}

    private void playSound(String title, String msg){
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Notification mNotification = new Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(msg)
                .setSmallIcon(R.drawable.ic_launcher)
//                .setContentIntent(pIntent)
                .setSound(soundUri)
                .build();

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, mNotification);
    }

    //Metodos de CvCameraViewListener2
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat resultMat = mOnCameraFrameRender.render(inputFrame);
        JSONObject result = new JSONObject();
        Log.d(tag, "width: " + resultMat.cols() + " height: " + resultMat.rows());
        if(isCapturing.get()){
            if(isCalibrating.get()){
                isCalibrating.set(false);
                setCaptureMode(false);
            } else {
                isCapturing.set(false);
                processFrameCallback(resultMat, result);
            }
        }

        return resultMat;
    }

    private void processFrameCallback(final Mat resultMat, final JSONObject result) {
        new AsyncTask<Void, Void, JSONObject>() {

            @Override
            protected void onPreExecute() {
//                setWorking();
//                resetChron();
//                startChron();
            }

            @Override
            protected JSONObject doInBackground(Void... params) {
                try {
                    JSONObject resultJsonObject = figaro.shapeUp(resultMat, config);
                    JSONArray pointsJson = resultJsonObject.getJSONArray(KEY_POINTS);

                    SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.app_name)
                            , MODE_PRIVATE);
                    SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
                    sharedPreferencesEditor.putString(KEY_FILE_PATH, resultJsonObject.getString(KEY_FILE_PATH));
                    sharedPreferencesEditor.apply();

                    if(pointsJson != null && pointsJson.length() > 0){
                        result.put(KEY_MD5, resultJsonObject.get(KEY_MD5));
                        result.put(Consts.KEY_ERROR, false);
                        result.put(Consts.KEY_POINTS, pointsJson);
                        Log.d(tag, "pointsJson: " + pointsJson.toString(1));
                    } else {
                        result.put(Consts.KEY_ERROR, true);
                    }
                    Log.d(tag, "mPictureCallback. result: " + result.toString(1));
                } catch (JSONException e) {
                    Log.e(tag, "Couldn't execute MidgetOfSeville.shapeUp to Camera Frame");
                    e.printStackTrace();
                }

                return result;
            }

            @Override
            protected void onPostExecute(JSONObject jsonObject) {
//                stopChron();
//                showElapsedTime("Processing Image");

                if(result != null){
                    Intent williamWallaceIntent = new Intent(mContext, BraveheartMidgetService.class);
                    williamWallaceIntent.putExtra(Consts.EXTRA_RESULT, result.toString());
                    startService(williamWallaceIntent);
                }
//                unsetWorking();
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
            }

            mOnCameraFrameRender = new OnCameraFrameRender(new CalibrationFrameRender(mCalibrator));
        }
    }

    public void setCaptureMode(boolean calibrate){
        if(calibrate) {
            isCalibrating.set(true);
            mOnCameraFrameRender = new OnCameraFrameRender(new CalibrationFrameRender(mCalibrator));
            takePictureImageView.setEnabled(true);
        } else {
            mOnCameraFrameRender = new OnCameraFrameRender(new UndistortionFrameRender(mCalibrator));
        }
    }

    /* Metodos de OnTouchListener */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d(tag, "onTouch invoked");
        mCalibrator.addCorners();
        return false;
    }

//    public void setCalibrationValues(Mat cameraMatrix, Mat distCoeffs){
//        mCalibrator.setCameraMatrix(cameraMatrix);
//        mCalibrator.setDistortionCoefficients(distCoeffs);
//        setCaptureMode(false);
//    }

    private void onButtonClick() {
//        dummyCalibration();

        try {
            new AsyncTask<Void, Void, Bundle>() {

                @Override
                protected void onPreExecute() {
                    setWorking();
                    resetChron();
                    startChron();

                    // Warning si no hay suficientes puntos
                    if (mCalibrator.getCornersBufferSize() < 2) {
                        Toast.makeText(mActivity, mActivity.getString(R.string.more_samples),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Cambiar FrameRender a Preview
                    mOnCameraFrameRender = new OnCameraFrameRender(
                            new PreviewFrameRender(mCalibrator.mWidth, mCalibrator.mHeight)
                    );

                    // TODO mover a setCaptureMode
                    isCapturing.set(false);
                    isCalibrating.set(false);
                    takePictureImageView.setEnabled(false);

                    calibrationProgress = new ProgressDialog(mActivity);
                    calibrationProgress.setTitle(mActivity.getString(R.string.calibrating));
                    calibrationProgress.setMessage(mActivity.getString(R.string.please_wait));
                    calibrationProgress.setCancelable(false);
                    calibrationProgress.setIndeterminate(true);
                    calibrationProgress.show();
                    playSound("Calibration", "Starting");

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
                    playSound("Calibration", "Post Calibration");
                    try {
                        Process process = new ProcessBuilder()
                                .command("logcat", "-c")
                                .redirectErrorStream(true)
                                .start();
                        process.destroy();
                    } catch (Exception e){
                        e.printStackTrace();
                    }

                    calibrationProgress.dismiss();
                    mCalibrator.clearCorners();
                    stopChron();
                    showElapsedTime("processing image");

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

                        resultForIntent.put(Consts.KEY_ACTION, Consts.ACTION_SEND_CALIBRATION_RESULT);
                        resultForIntent.put(Consts.KEY_CALIBRATION_RESULT, calibPayload.toString());

                        Intent williamWallaceIntent = new Intent(getBaseContext(), BraveheartMidgetService.class);
                        williamWallaceIntent.putExtra(Consts.EXTRA_CALLBACK_MSG, resultForIntent.toString());
                        startService(williamWallaceIntent);

                        Log.i(tag, "Result Ok");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    playSound("Calibration", "Result Ok");

                    unsetWorking();
                }

            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (Exception e) {
            playSound("Calibration", "Result Ok");
            Log.e(tag, "Exception instantiating CameraCalibrator. ", e);
        }
    }

    private void dummyCalibration() {
        try {
            JSONObject calibPayloadCalib = new JSONObject("{\"right\":{\"calib_map_2\":\"[-2.8307974e+15, -2.8308148e+15, -2.8308323e+15, -2.8308497e+15;\\n  -2.8307974e+15, -2.8308148e+15, -2.8308323e+15, -2.8308497e+15;\\n  -2.8307974e+15, -2.8308148e+15, -2.8308323e+15, -2.8308497e+15;\\n  -2.8307974e+15, -2.8308148e+15, -2.8308323e+15, -2.8308497e+15;\\n  -2.8307974e+15, -2.8308148e+15, -2.8308323e+15, -2.8308497e+15;\\n  -2.8307974e+15, -2.8308148e+15, -2.8308323e+15, -2.8308497e+15;\\n  -2.8307974e+15, -2.8308148e+15, -2.8308323e+15, -2.8308497e+15;\\n  -2.8307974e+15, -2.8308148e+15, -2.8308323e+15, -2.8308497e+15;\\n  -2.8307974e+15, -2.8308148e+15, -2.8308323e+15, -2.8308497e+15;\\n  -2.8307974e+15, -2.8308148e+15, -2.8308323e+15, -2.8308497e+15;\\n  -2.8307974e+15, -2.8308148e+15, -2.8308323e+15, -2.8308497e+15]\",\"calib_map_1\":\"[8.041172e+14, 8.0412176e+14, 8.0412633e+14, 8.0413096e+14;\\n  8.041172e+14, 8.0412176e+14, 8.0412633e+14, 8.0413096e+14;\\n  8.041172e+14, 8.0412176e+14, 8.0412633e+14, 8.0413096e+14;\\n  8.041172e+14, 8.0412176e+14, 8.0412633e+14, 8.0413096e+14;\\n  8.041172e+14, 8.0412176e+14, 8.0412633e+14, 8.0413096e+14;\\n  8.041172e+14, 8.0412176e+14, 8.0412633e+14, 8.0413096e+14;\\n  8.041172e+14, 8.0412176e+14, 8.0412633e+14, 8.0413096e+14;\\n  8.041172e+14, 8.0412176e+14, 8.0412633e+14, 8.0413096e+14;\\n  8.041172e+14, 8.0412176e+14, 8.0412633e+14, 8.0413096e+14;\\n  8.041172e+14, 8.0412176e+14, 8.0412633e+14, 8.0413096e+14;\\n  8.041172e+14, 8.0412176e+14, 8.0412633e+14, 8.0413096e+14]\",\"calib_camera_matrix\":\"[1.0469388e-38, 0, 0;\\n  5.7453237e-44, 1.4012985e-45, 9.9950187e-12;\\n  2.1826625e-41, 1.4012985e-45, 0]\",\"calib_distortion_coefficients\":\"[0.04181658698748777;\\n  0.5143468128821382;\\n  0;\\n  0;\\n  -4.353305195069944]\"},\"calibration\":true,\"left\":{\"calib_map_2\":\"[2.2027946e+22, 2.2027946e+22, 2.2027946e+22, 2.2027946e+22;\\n  2.2027946e+22, 2.2027946e+22, 2.2027946e+22, 2.2027946e+22;\\n  2.2027946e+22, 2.2027946e+22, 2.2027946e+22, 2.2027946e+22;\\n  2.2027946e+22, 2.2027946e+22, 2.2027946e+22, 2.2027946e+22;\\n  2.2027946e+22, 2.2027946e+22, 2.2027946e+22, 2.2027946e+22;\\n  2.2027946e+22, 2.2027946e+22, 2.2027946e+22, 2.2027946e+22;\\n  2.2027946e+22, 2.2027946e+22, 2.2027946e+22, 2.2027946e+22;\\n  2.2027946e+22, 2.2027946e+22, 2.2027946e+22, 2.2027946e+22;\\n  2.2027946e+22, 2.2027946e+22, 2.2027946e+22, 2.2027946e+22;\\n  2.2027946e+22, 2.2027946e+22, 2.2027946e+22, 2.2027946e+22;\\n  2.2027946e+22, 2.2027946e+22, 2.2027946e+22, 2.2027946e+22]\",\"calib_map_1\":\"[-4.1828909e+20, -4.1828909e+20, -4.1828909e+20, -4.1828909e+20;\\n  -4.1828909e+20, -4.1828909e+20, -4.1828909e+20, -4.1828909e+20;\\n  -4.1828909e+20, -4.1828909e+20, -4.1828909e+20, -4.1828909e+20;\\n  -4.1828909e+20, -4.1828909e+20, -4.1828909e+20, -4.1828909e+20;\\n  -4.1828909e+20, -4.1828909e+20, -4.1828909e+20, -4.1828909e+20;\\n  -4.1828909e+20, -4.1828909e+20, -4.1828909e+20, -4.1828909e+20;\\n  -4.1828909e+20, -4.1828909e+20, -4.1828909e+20, -4.1828909e+20;\\n  -4.1828909e+20, -4.1828909e+20, -4.1828909e+20, -4.1828909e+20;\\n  -4.1828909e+20, -4.1828909e+20, -4.1828909e+20, -4.1828909e+20;\\n  -4.1828909e+20, -4.1828909e+20, -4.1828909e+20, -4.1828909e+20;\\n  -4.1828909e+20, -4.1828909e+20, -4.1828909e+20, -4.1828909e+20]\",\"calib_camera_matrix\":\"[48.562515, 2.4662853e-43, 0;\\n  1.4012985e-45, 0, 1.4012985e-45;\\n  6.1657132e-44, 0, 1.4587517e-42]\",\"calib_distortion_coefficients\":\"[0.114933779657224;\\n  -0.1920176879641323;\\n  0;\\n  0;\\n  -1.469845531523524]\"}}");

            String side = "right";
            String cameraMatrixStr = calibPayloadCalib.getJSONObject(side)
                    .getString(Consts.KEY_CALIB_CAMERA_MATRIX);
            String distCoeffsStr = calibPayloadCalib.getJSONObject(side)
                    .getString(Consts.KEY_CALIB_DISTORTION_COEFFICIENTS);
            Log.d(tag, "Obteniendo calib_camera_matrix:" + cameraMatrixStr);

            Mat cameraMatrix = MADN3SCamera.getMatFromString(cameraMatrixStr);
            Mat distCoeffs = MADN3SCamera.getMatFromString(distCoeffsStr);
            // Reemplazado por setCaptureMode
//        mActivity.setCalibrationValues(cameraMatrix, distCoeffs);
            mActivity.setCaptureMode(false);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

}
