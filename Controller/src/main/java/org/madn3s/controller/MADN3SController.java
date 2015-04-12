package org.madn3s.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.madn3s.controller.Consts.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.madn3s.controller.components.NXTTalker;
import org.madn3s.controller.fragments.ScannerFragment;
import org.madn3s.controller.fragments.SettingsFragment;
import org.madn3s.controller.io.BraveHeartMidgetService;
import org.madn3s.controller.io.HiddenMidgetReader;
import org.madn3s.controller.io.UniversalComms;
import org.madn3s.controller.vtk.Madn3sNative;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.xmlpull.v1.XmlSerializer;

import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

/**
 * Created by inaki on 1/11/14.
 */
public class MADN3SController extends Application {
	private static final String tag = MADN3SController.class.getSimpleName();
	public static Context appContext;
	public static final String MODEL_MESSAGE = "MODEL";
	public static final String SERVICE_NAME = "MADN3S";
	public static final UUID APP_UUID = UUID
			.fromString("65da7fe0-8b80-11e3-baa8-0800200c9a66");

	public static final String defaultJSONObjectString = "{}";
	public static final String defaultJSONArrayString = "[]";

	public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final int MEDIA_TYPE_JSON = 3;
    public static final int MEDIA_TYPE_VTU = 4;

    private static File appDirectory;

	public static SharedPreferences sharedPreferences;
	public static Editor sharedPreferencesEditor;
	public static BluetoothDevice nxt;
	public static BluetoothDevice rightCamera;
	public static BluetoothDevice leftCamera;

	private Handler mBluetoothHandler;
	private Handler.Callback mBluetoothHandlerCallback = null;

	public static WeakReference<BluetoothSocket> rightCameraWeakReference = null;
	public static WeakReference<BluetoothSocket> leftCameraWeakReference = null;

	public static AtomicBoolean isPictureTaken;
	public static AtomicBoolean isRunning;

	public static AtomicBoolean readRightCamera;
	public static AtomicBoolean readLeftCamera;

	public static NXTTalker talker;
	public static boolean isOpenCvLoaded;

	public static enum Mode {
		SCANNER("SCANNER", 0), CONTROLLER("CONTROLLER", 1), SCAN("SCAN", 2);

		private String strVal;
		private int intVal;

		Mode(String strVal, int intVal) {
			this.strVal = strVal;
			this.intVal = intVal;
		}

		public int getValue() {
			return intVal;
		}

		@Override
		public String toString() {
			return this.strVal;
		}
	}

	public static enum Device {
		NXT("NXT", 0), RIGHT_CAMERA("RIGHT_CAMERA", 1), LEFT_CAMERA("LEFT_CAMERA", 2);

		private String strVal;
		private int intVal;

		Device(String strVal, int intVal) {
			this.strVal = strVal;
			this.intVal = intVal;
		}

		public int getValue() {
			return intVal;
		}

		@Override
		public String toString() {
			return this.strVal;
		}

		public static Device setDevice(int device) {
			switch (device) {
			case 0:
				return NXT;
			case 1:
				return RIGHT_CAMERA;
			default:
			case 2:
				return LEFT_CAMERA;
			}
		}
	}

	public static enum State {
		CONNECTED(0), CONNECTING(1), FAILED(2);

		private int state;

		State(int state) {
			this.state = state;
		}

		public int getState() {
			return state;
		}

		@Override
		public String toString() {
			return "state: " + this.state;
		}

		public static State setState(int state) {
			switch (state) {
			case 0:
				return CONNECTED;
			case 1:
				return CONNECTING;
			default:
			case 2:
				return FAILED;
			}
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		appContext = super.getBaseContext();
		appDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
				, appContext.getString(R.string.app_name));
		setSharedPreferences();
		setUpBridges();
		Log.d(tag, "onCreate. ");

		MADN3SController.isPictureTaken = new AtomicBoolean(true);
        MADN3SController.isRunning = new AtomicBoolean(true);
        MADN3SController.readRightCamera = new AtomicBoolean(false);
        MADN3SController.readLeftCamera = new AtomicBoolean(false);

		mBluetoothHandler = new Handler() {
			public void handleMessage(Message msg) {
				if (mBluetoothHandlerCallback != null) {
					mBluetoothHandlerCallback.handleMessage(msg);
				}
			};
		};

	}

	/**
	 * Sets up OpenCV Init Callback and all <code>UniversalComms</code> Bridges and Callbacks
	 */
	private void setUpBridges() {

		MainActivity.mLoaderCallback = new BaseLoaderCallback(getBaseContext()) {
		       @Override
		       public void onManagerConnected(int status) {
		           switch (status) {
		               case LoaderCallbackInterface.SUCCESS:
		                   Log.i(tag, "OpenCV loaded successfully");
		                   MADN3SController.isOpenCvLoaded = true;
		                   break;
		               default:
		                   super.onManagerConnected(status);
		                   break;
		           }
		       }
		   };

		HiddenMidgetReader.bridge = new UniversalComms() {
			@Override
			public void callback(Object msg) {
				Log.d(tag, "HiddenMidgetReader.bridge. EXTRA_CALLBACK_MSG");
				Intent williamWallaceIntent = new Intent(getBaseContext(), BraveHeartMidgetService.class);
				williamWallaceIntent.putExtra(EXTRA_CALLBACK_MSG, (String) msg);
				startService(williamWallaceIntent);
			}
		};

		HiddenMidgetReader.calibrationBridge = new UniversalComms() {
			@Override
			public void callback(Object msg) {
				Log.d(tag, "HiddenMidgetReader.bridge. EXTRA_CALLBACK_MSG");
				Intent williamWallaceIntent = new Intent(getBaseContext(), BraveHeartMidgetService.class);
				williamWallaceIntent.putExtra(EXTRA_CALLBACK_CALIBRATION_RESULT, (String) msg);
				startService(williamWallaceIntent);
			}
		};

		HiddenMidgetReader.pictureBridge = new UniversalComms() {
			@Override
			public void callback(Object msg) {
				Log.d(tag, "HiddenMidgetReader.pictureBridge. EXTRA_CALLBACK_PICTURE");
				Intent williamWallaceIntent = new Intent(getBaseContext(), BraveHeartMidgetService.class);
				williamWallaceIntent.putExtra(EXTRA_CALLBACK_PICTURE, (String) msg);
				startService(williamWallaceIntent);
			}
		};

		ScannerFragment.bridge = new UniversalComms() {
			@Override
			public void callback(Object msg) {
				Log.d(tag, "ScannerFragment.bridge. EXTRA_CALLBACK_SEND");
				Intent williamWallaceIntent = new Intent(getBaseContext(), BraveHeartMidgetService.class);
				williamWallaceIntent.putExtra(EXTRA_CALLBACK_SEND, (String)msg);
				startService(williamWallaceIntent);
			}
		};

		NXTTalker.bridge = new UniversalComms() {
			@Override
			public void callback(Object msg) {
				Log.d(tag, "NXTTalker.bridge. EXTRA_CALLBACK_NXT_MESSAGE");
				Intent williamWallaceIntent = new Intent(getBaseContext(), BraveHeartMidgetService.class);
				williamWallaceIntent.putExtra(EXTRA_CALLBACK_NXT_MESSAGE, (String)msg);
				startService(williamWallaceIntent);
			}
		};

		SettingsFragment.bridge = new UniversalComms() {
			@Override
			public void callback(Object msg) {
				Log.d(tag, "SettingsFragment.bridge. EXTRA_CALLBACK_SEND");
				Intent williamWallaceIntent = new Intent(getBaseContext(), BraveHeartMidgetService.class);
				williamWallaceIntent.putExtra(EXTRA_CALLBACK_SEND, (String)msg);
				startService(williamWallaceIntent);
			}
		};
	}

	/**
	 * Sets SharedPreferences and SharedPreferences Editor for later use with methods defined further
	 */
	private void setSharedPreferences() {
		sharedPreferences = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
		sharedPreferencesEditor = MADN3SController.sharedPreferences.edit();
	}

	public static void clearSharedPreferences() {
		sharedPreferencesEditor.clear().apply();
	}

	public static void removeKeyFromSharedPreferences(String key) {
		sharedPreferencesEditor.remove(key).apply();
	}

	public static void sharedPrefsPutJSONArray(String key, JSONArray value){
		sharedPreferencesEditor.putString(key, value.toString()).apply();
	}

	public static JSONArray sharedPrefsGetJSONArray(String key){
		String jsonString = sharedPreferences.getString(key, defaultJSONArrayString);
		try {
			return new JSONArray(jsonString);
		} catch (Exception e) {
			e.printStackTrace();
			return new JSONArray();
		}
	}

	public static void sharedPrefsPutJSONObject(String key, JSONObject value){
		sharedPreferencesEditor.putString(key, value.toString()).apply();
	}

	public static JSONObject sharedPrefsGetJSONObject(String key){
		String jsonString = sharedPreferences.getString(key, defaultJSONObjectString);
		try {
			return new JSONObject(jsonString);
		} catch (Exception e) {
			e.printStackTrace();
			return new JSONObject();
		}
	}

	public static void sharedPrefsPutString(String key, String value){
		sharedPreferencesEditor.putString(key, value).apply();
	}

	public static String sharedPrefsGetString(String key){
		return sharedPreferences.getString(key, "");
	}

	public static void sharedPrefsPutBoolean(String key, Boolean value){
		sharedPreferencesEditor.putBoolean(key, value).apply();
	}

	public static Boolean sharedPrefsGetBoolean(String key){
		return sharedPreferences.getBoolean(key, false);
	}

	public static void sharedPrefsPutInt(String key, int value){
		sharedPreferencesEditor.putInt(key, value).apply();
	}

	public static int sharedPrefsGetInt(String key){
		return sharedPreferences.getInt(key, 0);
	}

	public static void sharedPrefsPutLong(String key, Long value){
		sharedPreferencesEditor.putLong(key, value).apply();
	}

	public static Long sharedPrefsGetLong(String key){
		return sharedPreferences.getLong(key, (long) 0);
	}

	public static void sharedPrefsPutFloat(String key, Float value){
		sharedPreferencesEditor.putFloat(key, value).apply();
	}

	public static Float sharedPrefsGetFloat(String key){
		return sharedPreferences.getFloat(key, 0);
	}

	public static boolean isToyDevice(BluetoothDevice device) {
		return device.getBluetoothClass() != null
				&& device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.TOY_ROBOT;
	}

	public static boolean isCameraDevice(BluetoothDevice device) {
		return device.getBluetoothClass() != null
				&& (device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.PHONE_SMART || device
						.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.Major.MISC);
	}

	public static boolean isRightCamera(String macAddress) {
		if (macAddress != null && rightCamera != null
				&& rightCamera.getAddress() != null) {
			return macAddress.equalsIgnoreCase(rightCamera.getAddress());
		}
		return false;
	}

	public static boolean isLeftCamera(String macAddress) {
		if (macAddress != null && leftCamera != null
				&& leftCamera.getAddress() != null) {
			return macAddress.equalsIgnoreCase(leftCamera.getAddress());
		}
		return false;
	}

	public Handler getBluetoothHandler() {
		return mBluetoothHandler;
	}

	public void setBluetoothHandlerCallBack(Handler.Callback callback) {
		this.mBluetoothHandlerCallback = callback;
	}

	public static void pointsTest(){
		int points = MADN3SController.sharedPrefsGetInt("points");
		JSONArray framesJson = new JSONArray();
		JSONObject pointsJson = new JSONObject();
		for(int i = 0; i < points; i++){
			JSONObject frame = MADN3SController.sharedPrefsGetJSONObject("frame-"+i);
			framesJson.put(frame);
//			Log.d(tag, "frame-"+i + " = " + frame.toString());
		}

		try {
			pointsJson.put("name", MADN3SController.sharedPrefsGetString("project_name"));
			pointsJson.put("pictures", framesJson);
			Log.d(tag, "pointTest.pointsJson String length: " + pointsJson.toString().length());

            String test = "[{\"z\":0.9829001270984955,\"y\":0.18251926688416867,\"x\":-0.024373292059682437},{\"z\":0.9887029992251164,\"y\":0.14988519847857568,\"x\":8.981092904919008E-4},{\"z\":0.9878322144053591,\"y\":0.15407694537414202,\"x\":-0.021161547372029594},{\"z\":0.9889167243461322,\"y\":0.14842244884699243,\"x\":0.003806440171350145},{\"z\":0.9905847378202053,\"y\":0.1323547109633897,\"x\":-0.034987250298825066},{\"z\":0.9926882817956175,\"y\":0.11955045722643562,\"x\":0.01666323385825262},{\"z\":0.9939891895984784,\"y\":0.1077204022272441,\"x\":-0.019539854281982315},{\"z\":0.9887468906920934,\"y\":0.14958645983088475,\"x\":0.001864720349184559},{\"z\":0.990771711601218,\"y\":0.1303485703751508,\"x\":-0.037157310074162155},{\"z\":0.9888366016110137,\"y\":0.14852320423416457,\"x\":0.011959645412701933},{\"z\":0.9950855336658402,\"y\":0.09516549404894473,\"x\":-0.027355245043503746},{\"z\":0.9806674767356264,\"y\":0.19355799328561482,\"x\":0.028750709699513835},{\"z\":0.9868578203812269,\"y\":0.15942414511187156,\"x\":-0.026373932353043663},{\"z\":0.9838829527945745,\"y\":0.1726988254277695,\"x\":0.046362170959713095},{\"z\":0.981294347096251,\"y\":0.18833573256776948,\"x\":0.039888045766925485},{\"z\":0.9885984136941259,\"y\":0.14525746552828858,\"x\":0.03966667555714456},{\"z\":0.9817939053301161,\"y\":0.18724936182602142,\"x\":-0.03191244196840207},{\"z\":0.9931188920333166,\"y\":0.11680246228437904,\"x\":0.008488291396011325},{\"z\":0.9814025444678087,\"y\":0.18809436242162697,\"x\":-0.03833479538633386},{\"z\":0.9913883766503716,\"y\":0.13083807607438142,\"x\":-0.005521276274133482},{\"z\":0.9808418708545122,\"y\":0.1948035711501315,\"x\":8.905311762046238E-4},{\"z\":0.9879060425493416,\"y\":0.15491807048512118,\"x\":-0.006484021257311357},{\"z\":0.9794938345675092,\"y\":0.2014735044425175,\"x\":5.050266208642846E-4},{\"z\":0.9901627819648162,\"y\":0.13681779789290355,\"x\":0.029301115873507883},{\"z\":0.99034199304512,\"y\":0.13667763533132654,\"x\":0.023280051796681068},{\"z\":0.992775364126121,\"y\":0.11825988545432473,\"x\":-0.02028979735182692},{\"z\":0.9931211405574529,\"y\":0.11127979637994828,\"x\":-0.03643085362026835},{\"z\":0.9908082789154709,\"y\":0.1349012246274668,\"x\":0.01003065434418245},{\"z\":0.9852359359156713,\"y\":0.16647949986540658,\"x\":-0.0399340294102159},{\"z\":0.9853327798719647,\"y\":0.17039575408427668,\"x\":0.009197820385108572},{\"z\":0.9822491636712974,\"y\":0.18745711948901753,\"x\":-0.0068124019197897765},{\"z\":0.990294648880214,\"y\":0.1379645473154187,\"x\":0.016801550025674815},{\"z\":0.9918912383030393,\"y\":0.12454240591147954,\"x\":-0.025317197859230846},{\"z\":0.9927353115276146,\"y\":0.11837727542143482,\"x\":0.02152723646841977},{\"z\":0.9858380621594626,\"y\":0.16564021401669302,\"x\":0.026203715350687228},{\"z\":0.9881634747262261,\"y\":0.15326829081531623,\"x\":-0.006463609482547582},{\"z\":0.9880063691719151,\"y\":0.1523698538961005,\"x\":-0.02503681488149273},{\"z\":0.9881826092087708,\"y\":0.15183196952715214,\"x\":0.021028168889660564},{\"z\":0.9927406630705546,\"y\":0.11769730549891072,\"x\":-0.024765301624071468},{\"z\":0.9874546538850593,\"y\":0.15470465549426043,\"x\":-0.03161923606192873},{\"z\":0.9933034928099644,\"y\":0.11526686379221861,\"x\":-0.007856289393291648},{\"z\":0.9911999355958254,\"y\":0.1309193710858332,\"x\":0.01956542739940438},{\"z\":0.9886309374559709,\"y\":0.1489454960571352,\"x\":-0.020593899806072187},{\"z\":0.98872558499307,\"y\":0.14792856307792665,\"x\":0.023213310961849913},{\"z\":0.9878840269844706,\"y\":0.14862604178030525,\"x\":0.04467044810234501},{\"z\":0.9918696254510473,\"y\":0.12266547952783766,\"x\":-0.03387958441015762},{\"z\":0.9898946785278705,\"y\":0.13708698534855884,\"x\":0.036272356833380544},{\"z\":0.9832204609887345,\"y\":0.17846982071878525,\"x\":-0.03776305318304377},{\"z\":0.9888786918682221,\"y\":0.1479592288186578,\"x\":0.015066498477828838},{\"z\":0.9827544057284057,\"y\":0.17882406879985513,\"x\":0.04707154596221627},{\"z\":0.9839691682780296,\"y\":0.17833401055334785,\"x\":0.0012870735026177824},{\"z\":0.9740307242013406,\"y\":0.22399228336707272,\"x\":0.03303945071905409},{\"z\":0.9924732157370877,\"y\":0.12246102635334206,\"x\":-4.61593945465474E-4},{\"z\":0.9889833641739579,\"y\":0.14779642868795106,\"x\":0.008253547979393726},{\"z\":0.9912798268480807,\"y\":0.13166452877825363,\"x\":0.005362531644622607},{\"z\":0.9927191745319088,\"y\":0.12012415852385079,\"x\":0.00887846020560352},{\"z\":0.9877627052334388,\"y\":0.1556090913810446,\"x\":-0.010518974735420399},{\"z\":0.993651748917775,\"y\":0.1096029538274365,\"x\":-0.025365219986199304},{\"z\":0.9883053132394193,\"y\":0.1454833193043869,\"x\":0.045686011282566746},{\"z\":0.9913288766194552,\"y\":0.12951580509797103,\"x\":-0.022197175726507486},{\"z\":0.9859934442711423,\"y\":0.16596219689816039,\"x\":0.016537141683684607},{\"z\":0.9910156269039688,\"y\":0.13177952031308013,\"x\":0.022851373223247765},{\"z\":0.992844413284658,\"y\":0.11897932158160067,\"x\":-0.010192744744405265},{\"z\":0.9850736167564793,\"y\":0.16638513787247342,\"x\":0.04411298522506748},{\"z\":0.9944090462610935,\"y\":0.10287327549869982,\"x\":-0.02382725125294131},{\"z\":0.9891593220096072,\"y\":0.14509476818903733,\"x\":0.022612915019167748},{\"z\":0.9862080605357,\"y\":0.15932411609097455,\"x\":-0.04482730603374455},{\"z\":0.9840520444838262,\"y\":0.17787468925955893,\"x\":0.0014726398122483747},{\"z\":0.9839021311136341,\"y\":0.17847440529256295,\"x\":-0.009136905686022666},{\"z\":0.9925460342256544,\"y\":0.11781884863153992,\"x\":-0.03116229853627948},{\"z\":0.9821262282738943,\"y\":0.187670382490552,\"x\":0.014418712575820787},{\"z\":0.9792513978419232,\"y\":0.20255955256019414,\"x\":-0.00602722915223464},{\"z\":0.9898599917135579,\"y\":0.14203873443301224,\"x\":0.0014814605979021538},{\"z\":0.9955746937524965,\"y\":0.09377376028852363,\"x\":-0.006124625782298865},{\"z\":0.9825263375208305,\"y\":0.18596104635805605,\"x\":-0.007777230568762301},{\"z\":0.9783384944283225,\"y\":0.20326117130915458,\"x\":0.03922609536715713},{\"z\":0.9870998018231443,\"y\":0.16009357557664128,\"x\":-0.0020070624791528224},{\"z\":0.9912661160053631,\"y\":0.12940627550507947,\"x\":0.025406753423948582},{\"z\":0.9945176432714028,\"y\":0.10453004138408331,\"x\":0.0028509068971831046},{\"z\":0.9889578990280398,\"y\":0.14818563016860692,\"x\":-0.0018146519166149216},{\"z\":0.9852314346781911,\"y\":0.16580254409094422,\"x\":0.04276139023609631},{\"z\":0.9915759675361461,\"y\":0.12649811019689391,\"x\":-0.02784472519831934},{\"z\":0.9867065620710269,\"y\":0.16248015089743964,\"x\":0.0032188399028488545},{\"z\":0.9936365080362415,\"y\":0.1122065702479393,\"x\":-0.009806910356403466},{\"z\":0.9910692723695825,\"y\":0.1333297298147943,\"x\":-0.002209188163120423},{\"z\":0.9926741914975074,\"y\":0.12078005202688018,\"x\":-0.003182540990187678},{\"z\":0.9893861807867119,\"y\":0.14419771867232198,\"x\":0.01794444755298066},{\"z\":0.9908227228478591,\"y\":0.13328411521743677,\"x\":0.022487252367948265},{\"z\":0.9874363971354266,\"y\":0.15489963234330556,\"x\":-0.03123244326204265},{\"z\":0.9930650430942378,\"y\":0.11748364120127779,\"x\":0.0044061586817879085},{\"z\":0.9913301765549885,\"y\":0.1313898236023836,\"x\":0.0010933001372630164},{\"z\":0.9851329464545115,\"y\":0.17149302264325159,\"x\":0.010159773350504544},{\"z\":0.9919341116653366,\"y\":0.12670949783333063,\"x\":-0.003379537471972094},{\"z\":0.9875075456502854,\"y\":0.15685539472515897,\"x\":0.015007745645631296},{\"z\":0.9891963523279838,\"y\":0.14642367672093898,\"x\":0.007119230052016284},{\"z\":0.9941881697052685,\"y\":0.1070687909186361,\"x\":0.01123197352692573},{\"z\":0.9857725218384493,\"y\":0.1680577519255731,\"x\":0.0030211266088190623},{\"z\":0.9872407831417966,\"y\":0.1558545942773715,\"x\":0.03263405497647535},{\"z\":0.9920418179933614,\"y\":0.12204345258671453,\"x\":0.030958472719766605},{\"z\":0.9855131617787632,\"y\":0.16581511198321774,\"x\":0.035625224193238166},{\"z\":0.9845496147420645,\"y\":0.16952856427375668,\"x\":-0.04384201188963937},{\"z\":0.9902447665120104,\"y\":0.13932045085830752,\"x\":0.002261496896300166},{\"z\":0.9932944782221568,\"y\":0.10760650496349768,\"x\":-0.042271972072693696},{\"z\":0.9806608766580924,\"y\":0.19566193931749853,\"x\":0.00454428153812391},{\"z\":0.9957862918590008,\"y\":0.09144376098540168,\"x\":-0.006906484094358372},{\"z\":0.9907550637335943,\"y\":0.13561563700615697,\"x\":-0.0035780840760975506},{\"z\":0.9757161857192591,\"y\":0.21903841597042653,\"x\":3.1185674034242664E-4},{\"z\":0.9947492823552,\"y\":0.1023337286106566,\"x\":0.001293538733615339},{\"z\":0.9898836110743844,\"y\":0.13793532353684299,\"x\":-0.033230754537377},{\"z\":0.9897141733740951,\"y\":0.1370538150080936,\"x\":-0.04101349551256466},{\"z\":0.9891592542914815,\"y\":0.14680678210235806,\"x\":0.003426131677385805},{\"z\":0.9874842285616142,\"y\":0.15771620402837055,\"x\":7.052155378756249E-4},{\"z\":0.9798601881213727,\"y\":0.1996722612096719,\"x\":0.0022360317897194538},{\"z\":0.9908601652955934,\"y\":0.13384034425526062,\"x\":0.016819485129539156},{\"z\":0.979335846778063,\"y\":0.20222437024074374,\"x\":-0.0025696879633323173},{\"z\":0.9902213143379385,\"y\":0.13260072587728766,\"x\":-0.04334508193165587},{\"z\":0.983242839118854,\"y\":0.1818743069301677,\"x\":0.01246016854485694},{\"z\":0.9818514098014348,\"y\":0.18768341634067534,\"x\":0.02725333560552497},{\"z\":0.9780774157007973,\"y\":0.2073457484521674,\"x\":0.019295323134383768},{\"z\":0.9885560721019899,\"y\":0.15084236714984384,\"x\":0.0018634867633573074},{\"z\":0.9847110303676386,\"y\":0.17087358096068891,\"x\":0.03385861783910152},{\"z\":0.9865536325488218,\"y\":0.1632226417613638,\"x\":-0.008384469044943273},{\"z\":0.9853564121319109,\"y\":0.17002256679492178,\"x\":-0.012847873403571214},{\"z\":0.9912785550595331,\"y\":0.12933105164760808,\"x\":-0.025304255744990907},{\"z\":0.9847112519089897,\"y\":0.17288278991409906,\"x\":0.02133755645212664},{\"z\":0.9832322189627879,\"y\":0.18232235639702948,\"x\":0.0036002710101928578},{\"z\":0.9844088334894947,\"y\":0.17588940055458324,\"x\":0.0014721821903472623},{\"z\":0.992027436328466,\"y\":0.12545603689202908,\"x\":-0.011930983987798283},{\"z\":0.9856831083783373,\"y\":0.16855162586276798,\"x\":0.004377131096579307},{\"z\":0.9895332560570094,\"y\":0.14430498930111518,\"x\":-7.224969464848137E-5},{\"z\":0.9945778216040286,\"y\":0.10094525437150581,\"x\":-0.025000247863903736},{\"z\":0.9930595812893749,\"y\":0.11645331849403481,\"x\":0.01647096296816294},{\"z\":0.9866219998303953,\"y\":0.16096827723594542,\"x\":0.02581168677873089},{\"z\":0.9920344193578701,\"y\":0.12376300340458483,\"x\":0.023461240324628586},{\"z\":0.9800077678977281,\"y\":0.1988983042810608,\"x\":0.004923353961604467},{\"z\":0.9922712598011014,\"y\":0.12306650570194823,\"x\":0.015886539807245935},{\"z\":0.9949856640749493,\"y\":0.09887442872693677,\"x\":-0.015078979715293008},{\"z\":0.9922341831140536,\"y\":0.12438205440562289,\"x\":-6.560501681625774E-4},{\"z\":0.9889142254503753,\"y\":0.14839833710416203,\"x\":0.005156379214518166},{\"z\":0.983922758906575,\"y\":0.1783045036103661,\"x\":0.01017391261683357},{\"z\":0.9859182110895338,\"y\":0.16709449754650463,\"x\":0.006686548563662646},{\"z\":0.9881316767120759,\"y\":0.1527391321031212,\"x\":-0.016326267257602323},{\"z\":0.989438654541267,\"y\":0.1445794714408165,\"x\":0.01038871202132004},{\"z\":0.9796938322802639,\"y\":0.20011507714368507,\"x\":0.012407694861939616},{\"z\":0.9857686997498706,\"y\":0.1638504977916846,\"x\":0.03759102242380033},{\"z\":0.9923682763589751,\"y\":0.12304304917524848,\"x\":0.008100131231906414},{\"z\":0.983746440862644,\"y\":0.17713102342808953,\"x\":-0.029454042666464703},{\"z\":0.9890436934202457,\"y\":0.1440895795093813,\"x\":0.03210553818968089},{\"z\":0.9859432575091098,\"y\":0.16374949137578887,\"x\":-0.03319634086002751},{\"z\":0.9792430226097247,\"y\":0.20264309560565114,\"x\":0.004344936539100204},{\"z\":0.9900902092109205,\"y\":0.14021121883086948,\"x\":0.00788617389090908},{\"z\":0.971728310409746,\"y\":0.23368267282646832,\"x\":0.03371200333558202},{\"z\":0.9942569464951266,\"y\":0.10559510502906343,\"x\":-0.017401095945024054},{\"z\":0.9775495218136471,\"y\":0.21070578374678342,\"x\":-7.139722320495115E-5},{\"z\":0.9902549094914651,\"y\":0.13921445227342458,\"x\":0.003814512584875807},{\"z\":0.9848511916073691,\"y\":0.1733195156822201,\"x\":0.005336278968098685},{\"z\":0.9889964840603026,\"y\":0.14438567026866378,\"x\":-0.03222937693204934},{\"z\":0.988630828445931,\"y\":0.14834640218132414,\"x\":0.024544449600032516},{\"z\":0.9901555692496222,\"y\":0.13618050850148664,\"x\":0.032354563638425336},{\"z\":0.9944499373314672,\"y\":0.10229280879674865,\"x\":-0.024606978886335597},{\"z\":0.9808233620208656,\"y\":0.1947201454010666,\"x\":-0.00834251095734542},{\"z\":0.9930609898164046,\"y\":0.1122498254525251,\"x\":-0.035069177217901974},{\"z\":0.9778190476301832,\"y\":0.20688672847868733,\"x\":-0.03267708173915116},{\"z\":0.9802767635370477,\"y\":0.19197274822451907,\"x\":0.04694604145672299},{\"z\":0.9894284822922499,\"y\":0.14109150103169102,\"x\":0.0335330697294329},{\"z\":0.9933576560986223,\"y\":0.11417103380158214,\"x\":0.014336739898921938},{\"z\":0.9816787439126196,\"y\":0.18890242105052585,\"x\":-0.024954339730611515},{\"z\":0.991985501273787,\"y\":0.12633149845513922,\"x\":0.002262246819580061},{\"z\":0.9892291202785306,\"y\":0.14632052228624942,\"x\":0.0040065385114549665},{\"z\":0.9885751824403674,\"y\":0.1449369754999585,\"x\":-0.04138093517453093},{\"z\":0.9798538915864538,\"y\":0.19945964012980352,\"x\":0.010109554993742945},{\"z\":0.9885377593977157,\"y\":0.1455819434906074,\"x\":-0.03998744771102152},{\"z\":0.9920673236991173,\"y\":0.11971891182081534,\"x\":-0.03833806725580887},{\"z\":0.978507074808677,\"y\":0.2055438495914698,\"x\":-0.016602121686262197},{\"z\":0.9936623562067195,\"y\":0.11240383879839821,\"x\":7.063151522154939E-4},{\"z\":0.9876524457594147,\"y\":0.15644613861164053,\"x\":-0.008200737707912395},{\"z\":0.985909325230156,\"y\":0.16508305242079754,\"x\":-0.02702569569227065}]";
            String testPath = "/storage/emulated/legacy/Pictures/MADN3SController/";

        } catch (JSONException e) {
			e.printStackTrace();
			Log.e(tag, "generateModelButton.OnClick. Error composing points JSONObject");
		}
	}

	/**
	 * Crea un Mat desde un String
	 * @param str Matriz en forma de String
	 * @return Instancia de Mat con valores en Matriz recibida como String
	 */
	public static Mat getMatFromString(String str){
//		str = "[672.2618351846742, 0, 359.5; 0, 672.2618351846742, 239.5; 0, 0, 1]";
		int rows = 0;
		int cols = 0;
		double[] data;
		String[] colsStr = null;
		String rowStr = "";
		String colStr = "";
		str = str.replaceAll("^\\[|\\]$", "");
		String[] rowsStr = str.split(";");
		rows = rowsStr.length;
		//Por sacar cls
		rowStr = rowsStr[0];
		cols = rowStr.split(",").length;
		data = new double[rows*cols];

		for(int row = 0; row < rowsStr.length; ++row){
			rowStr = rowsStr[row];
			colsStr = rowStr.split(",");
			cols = colsStr.length;
//			Log.d(tag, "row[" + row + "]: " + rowStr);
			for(int col = 0; col < colsStr.length; ++col){
				colStr = colsStr[col];
				data[row*cols+col] = Double.valueOf(colStr);
//				Log.d(tag, "row[" + row + "]col[" + col + "]: " + colStr);
			}
		}
		int type = CvType.CV_64F;
		Mat mat = new Mat(rows, cols, type);
		mat.put(0, 0, data);
//		Log.d(tag, "getMatFromString. Result Mat: " + mat.dump());
		return mat;
	}

	public static Mat getImagePointFromString(String str){
		int rows = 0;
		int cols = 1;
		float[] data;
		String[] colsStr = null;
		String rowStr = "";
		String colStr = "";
		str = str.replaceAll("^\\[|\\]$", "");
		String[] rowsStr = str.split(";");
		rows = rowsStr.length;
		int type = CvType.CV_64F;
		Mat mat = new Mat(rows, cols, type);
		mat.create(rows, 1, CvType.CV_32FC2);
		//Por sacar cls
		rowStr = rowsStr[0];
		cols = rowStr.split(",").length;
		data = new float[2];

		for(int row = 0; row < rowsStr.length; ++row){
			rowStr = rowsStr[row];
			colsStr = rowStr.split(",");
			for(int col = 0; col < colsStr.length; ++col){
				colStr = colsStr[col];
				data[col] = Float.valueOf(colStr);
//				Log.d(tag, "str = " + colStr + " float = " + Float.valueOf(colStr));
			}
//			Log.d(tag, "data[0] = " + data[0] + " data[1] = " + data[1]);
			mat.put(row, 0, data);

		}
		return mat;
	}

	/**
	 * Returns Public App folder
	 */
	public static File getAppDirectory(){
    	return appDirectory;
    }

    public static Uri getOutputMediaFileUri(int type, String position){
        return Uri.fromFile(getOutputMediaFile(type, position));
    }

    public static Uri getOutputMediaFileUri(int type, String projectName, String position){
        return Uri.fromFile(getOutputMediaFile(type, projectName, position));
    }

    @SuppressLint("SimpleDateFormat")
	public static File getOutputMediaFile(int type, String name){
    	return getOutputMediaFile(type, sharedPrefsGetString(KEY_PROJECT_NAME), name);
    }

    @SuppressLint("SimpleDateFormat")
	public static File getOutputMediaFile(int type, String projectName, String name){
    	Log.d(tag, "getOutputMediaFile. projectName: " + projectName + " name: " + name);
        File mediaStorageDir = new File(getAppDirectory(), projectName);

        if (!mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d(tag, "getOutputMediaFile. failed to create directory");
                return null;
            }
        }

        if(name == null){
        	name = "";
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename;
        String iteration = String.valueOf(sharedPrefsGetInt(KEY_ITERATION));
        File mediaFile;

        if (type == MEDIA_TYPE_IMAGE){
            filename = "IMG_" + iteration + "_" + name + "_" + timeStamp + Consts.IMAGE_EXT;
        } else if(type == MEDIA_TYPE_JSON){
        	filename = name + "_" + timeStamp + Consts.JSON_EXT;
        } else if(type == MEDIA_TYPE_VTU){
        	filename = name + "_" + timeStamp + Consts.VTU_EXT;
        } else {
            return null;
        }

        mediaFile = new File(mediaStorageDir.getPath(), filename);

        return mediaFile;
    }

    public static String saveJsonToExternal(String output, String fileName) throws JSONException {
		try {
            String projectName = MADN3SController.sharedPrefsGetString(KEY_PROJECT_NAME);
			File calibrationFile = getOutputMediaFile(MEDIA_TYPE_JSON, projectName, fileName);
			Log.i(MidgetOfSeville.tag, "saveJsonToExternal. filepath: " + calibrationFile.getAbsolutePath());
			FileOutputStream fos = new FileOutputStream(calibrationFile);
			fos.write(output.getBytes());
			fos.flush();
			fos.close();
			return calibrationFile.getAbsolutePath();
		} catch (FileNotFoundException e) {
			Log.e(tag, "saveJsonToExternal. " + fileName + " FileNotFoundException", e);
		} catch (IOException e) {
			Log.e(tag, "saveJsonToExternal. " + fileName + " IOException", e);
		}

		return null;
	}

    public static String createVtpFromPoints(String pointsData, int size, String fileName){
//    	StringBuffer connectivityData = null;
//    	if(pointsData != null){
//    		connectivityData = new StringBuffer();
//    		for(int i = 0; i < size; ++i){
//    			connectivityData.append(String.format("%02d ", i));
//    		}
//    	}
    	File newxmlfile = getOutputMediaFile(MEDIA_TYPE_VTU, fileName);
        try {
	        FileOutputStream fileos = new FileOutputStream(newxmlfile);

	        XmlSerializer serializer = Xml.newSerializer();
	        serializer.setOutput(fileos, "UTF-8");
	        serializer.startDocument(null, null);
	        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
	        serializer.startTag(null, "VTKFile");
	        serializer.attribute(null, "type", "PolyData");
	        serializer.attribute(null, "version", "0.1");
	        serializer.attribute(null, "byte_order", "LittleEndian");
	        serializer.attribute(null, "compressor", "vtkZLibDataCompressor");
		        serializer.startTag(null, "PolyData");
			        serializer.startTag(null, "Piece");
			        serializer.attribute(null, "NumberOfPoints", String.valueOf(size));
				        serializer.startTag(null, "PointData");
				        serializer.endTag(null, "PointData");
				        serializer.startTag(null, "CellData");
				        serializer.endTag(null, "CellData");
				        serializer.startTag(null, "Points");
					        serializer.startTag(null, "DataArray");
					        	serializer.attribute(null, "type", "Float32");
					        	serializer.attribute(null, "NumberOfComponents", "3");
					        	serializer.attribute(null, "format", "ascii");
					        	serializer.text(pointsData);
					        serializer.endTag(null, "DataArray");
				        serializer.endTag(null, "Points");
//				        serializer.startTag(null, "Cells");
//					        serializer.startTag(null, "DataArray");
//					        	serializer.attribute(null, "type", "Int32");
//					        	serializer.attribute(null, "Name", "connectivity");
//					        	serializer.attribute(null, "format", "ascii");
//					        	serializer.text(connectivityData.toString());
//					        serializer.endTag(null, "DataArray");
//				        serializer.endTag(null, "Cells");
			        serializer.endTag(null, "Piece");
		        serializer.endTag(null, "PolyData");
	        serializer.endTag(null,"VTKFile");
	        serializer.endDocument();
	        serializer.flush();
	        fileos.close();

        } catch(FileNotFoundException e) {
            Log.e("FileNotFoundException",e.toString());
        } catch (IOException e) {
            Log.e("IOException", "Exception in create new File(");
        } catch(Exception e) {
            Log.e("Exception","Exception occured in wroting");
        }

    	return null;
    }

    public static String saveBitmapAsPng(Bitmap bitmap, String position){
    	FileOutputStream out;
        try {
            final File imgFile = getOutputMediaFile(MEDIA_TYPE_IMAGE, sharedPrefsGetString(KEY_PROJECT_NAME), position);

            out = new FileOutputStream(imgFile.getAbsoluteFile());
            bitmap.compress(Consts.BITMAP_COMPRESS_FORMAT, Consts.COMPRESSION_QUALITY, out);

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                	Toast.makeText(appContext, imgFile.getName(), Toast.LENGTH_SHORT).show();
                }
              });

            return imgFile.getPath();

        } catch (FileNotFoundException e) {
            Log.e(position, "saveBitmapAsPng: No se pudo guardar el Bitmap", e);
            return null;
        }
    }

    public static String getMD5EncryptedString(byte[] bytes){
        MessageDigest mdEnc = null;
        try {
            mdEnc = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Exception while encrypting to md5");
            e.printStackTrace();
        }

        mdEnc.update(bytes, 0, bytes.length);
        String md5 = new BigInteger(1, mdEnc.digest()).toString(16);
        while ( md5.length() < 32 ) {
            md5 = "0"+md5;
        }
        return md5;
    }
}
