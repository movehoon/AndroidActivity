package cc.sici.sici3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

public class SiCi2Activity extends UnityPlayerActivity implements
        TextToSpeech.OnInitListener, TextToSpeech.OnUtteranceCompletedListener {
    final String CONSUMER_KEY = "kiU3qSvu5f1NVhLRbAAOg";
    final String CONSUMER_SECRET = "3lXSfJstEoqHGH5clD5FoSxdGL1o4mUK4uJfLfvg";

    private boolean isSpeechRecognitionFound = false;
    private static final String TAG = "SiCi";
    private static final boolean D = true;
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 12150111;

    private BluetoothAdapter mBluetoothAdapter = null;
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final String DEVICE_NAME = "SiCiBluetooth";
    public static final String UnityObjectName = "AndroidManager";
    private BTManager mBTManager = null;

    public enum ROBOT_MODE {
        ROBOTIS, ROBOROBO, UCR,
    }

    public ROBOT_MODE mRobotMode = ROBOT_MODE.ROBOTIS;

    private SensorManager sensorManager = null;
    private SiCiSensorManager mSensorManager = null;

    private TextToSpeech mTts;

    private ImageView imageView;

    private SpeechRecognizer mRecognizer = null;
    Intent recognizerIntent = new Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    int Time = 15000;
    int LimitTime = 0;
    private Handler mhandle = new Handler();
    boolean SpeechReset = false;
    boolean ResultReset = false;
    boolean TimerReset = false;
    boolean ErrorReset = false;

    public boolean CheckConnectWifi() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isWifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                .isConnectedOrConnecting();
        return isWifi;
    }

    public void CheckRecognizer() {
        if (mRecognizer == null) {
            UnityPlayer.UnitySendMessage(UnityObjectName, "OffRecognizer", "Off");
        } else {
            UnityPlayer
                    .UnitySendMessage(UnityObjectName, "OffRecognizer", "On");
        }

    }

    public void ConnectWifi() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isWifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                .isConnectedOrConnecting();
        if (isWifi) {
            UnityPlayer.UnitySendMessage(UnityObjectName, "OnWifi", "On");
        } else {
            UnityPlayer.UnitySendMessage(UnityObjectName, "OnWifi", "Off");
        }
    }

    private void LogMessage(String type, String message) {
        if (D)
            Log.e(TAG, message);

        if (type.equals("BT"))
            UnityPlayer.UnitySendMessage(UnityObjectName, "BTMessage", message);
        if (type.equals("TW"))
            UnityPlayer.UnitySendMessage(UnityObjectName, "TWMessage", message);
        if (type.equals("SP"))
            UnityPlayer.UnitySendMessage(UnityObjectName, "SPMessage", message);
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                "LANGUAGE_MODEL_WEB_SEARCH");
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);

        if (activities.size() == 0) {
            LogMessage("SP", "Recognizer isn't present!");
        } else {
            isSpeechRecognitionFound = true;
            LogMessage("SP", "Recognizer found");
        }

        mTts = new TextToSpeech(this, this);
        new BTChildThread().start();

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        addContentView(layout, new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT));

        imageView = new ImageView(this);
        layout.addView(imageView, new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mBTManager != null)
            mBTManager.stop();
        if (mSensorManager != null)
            mSensorManager.stop();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        super.onBackPressed();
    }

    public void UnitysendState(String msg) {
        UnityPlayer.UnitySendMessage(UnityObjectName, "SetState", msg);
    }

    public void UnitysendResult(ArrayList<String> resultList) {
        for (String s : resultList) {
            UnityPlayer.UnitySendMessage(UnityObjectName, "ReceiveResult", s);
        }
        UnityPlayer.UnitySendMessage(UnityObjectName, "SetResult",
                String.valueOf(resultList.size()));
    }

    private RecognitionListener listener = new RecognitionListener() {

        public void onBeginningOfSpeech() {
            UnitysendState("사용자말시작");
        }

        public void onBufferReceived(byte[] buffer) {

        }

        public void onEndOfSpeech() {
            UnitysendState("사용자말중지");
        }

        /*
         * (non-Javadoc)
         * 
         * @see android.speech.RecognitionListener#onError(int)
         */
        public void onError(int error) {
            String msg = null;

            switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                msg = "오디오 입력 중 오류가 발생했습니다.";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                msg = "단말에서 오류가 발생했습니다.";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                msg = "권한이 없습니다.";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                msg = "네트워크 오류가 발생했습니다.";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                msg = "일치하는 항목이 없습니다.";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                msg = "음성인식 서비스가 과부하 되었습니다.";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                msg = "서버에서 오류가 발생했습니다.";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                msg = "입력이 없습니다.";
                break;
            default:
                msg = "NULL";
                break;
            }
            Log.i("Speech!!", "ERROR!!" + msg);
            UnitysendState("에러" + msg);
            if (ErrorReset) {
                ResetSpeech();
            }

        }

        public void onEvent(int eventType, Bundle params) {

        }

        public void onPartialResults(Bundle partialResults) {

        }

        public void onReadyForSpeech(Bundle params) {
            UnitysendState("준비");
            Log.d("Speech", "ready");
        }

        public void onResults(Bundle results) {
            // Log.d("Speech", "onResults");

            ArrayList<String> strlist = results
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            for (int i = 0; i < strlist.size(); i++) {
                Log.d("Speech", "result=" + strlist.get(i));
            }
            UnitysendResult(strlist);
            Log.d("Speech", "resultDes");
            if (ResultReset) {
                ResetSpeech();
            }
        }

        public void onRmsChanged(float rmsdB) {
        }

    };

    public void SpeechRecordStart() {
        if (mRecognizer == null) {
            SpeechRecognizerCreate();
        }
        ResetSpeech();
        Log.d("Speech", "Start");
    }

    public void SpeechRecognizerCreate() {
        mhandle.post(new Runnable() {
            public void run() {
                mRecognizer = SpeechRecognizer
                        .createSpeechRecognizer(getApplicationContext()); // 음성인식
                                                                          // 객체
                mRecognizer.setRecognitionListener(listener); // 음성인식 리스너 등록
                Log.d("Speech", "Create");
            }
        });
    }

    public void ResetSpeech() {
        if (mRecognizer != null) {
            mhandle.post(new Runnable() {
                public void run() {
                    mRecognizer.cancel();
                    mRecognizer.startListening(recognizerIntent);
                }
            });
            Log.d("Speech", "Reset");
        }
    }

    public void DestroySpeech() {
        if (mRecognizer != null) {
            mhandle.post(new Runnable() {
                public void run() {
                    mRecognizer.cancel();
                    mRecognizer.destroy();
                }
            });
            Log.d("Speech", "Destroy");
        }
    }

    public void StopSpeech() {
        if (mRecognizer != null) {
            mhandle.post(new Runnable() {
                public void run() {
                    mRecognizer.cancel();
                    Log.d("Speech", "Stop");
                }
            });
            if (mRecognizer == null) {
                Log.d("Speech", "Null1!!");
            }

        }

        if (mRecognizer == null) {
            Log.d("Speech", "Null2!!");
        }

        UnitysendState("꺼짐");
    }

    public void ResetSwitch(int type) {
        switch (type) {
        case 1:
            TimerReset = true;
            break;

        case -1:
            TimerReset = false;
            break;

        case 2:
            ResultReset = true;
            break;

        case -2:
            ResultReset = false;
            break;

        case 3:
            ErrorReset = true;
            break;

        case -3:
            ErrorReset = false;
            break;

        default:

            break;
        }
    }

    public void SendMessage(String hexMessage) {
        // Check that we're actually connected before trying anything
        if (mBTManager.getState() != BTManager.STATE_CONNECTED) {
            LogMessage("BT", "Bluetooth is not connected!");
            return;
        }

        int num = hexMessage.length();
        byte[] message = new byte[num / 2];
        for (int i = 0; i < num; i += 2) {
            String hex = hexMessage.substring(i, i + 2);
            message[i / 2] = (byte) Integer.parseInt(hex, 16);
//            Log.d("Unity", hex + " -> " + message[i / 2]);
        }
//        Log.d("Unity", hexMessage + " => " + message[0] + ":" + message[1]
//                + ":" + message[2] + ":" + message[3] + ":" + message[4] + ":"
//                + message[5]);
        try {
            mBTManager.write(message);
        } catch (Exception e) {
            LogMessage("BT", "Write Error:" + e);
        }
    }

    public void SearchBluetoothDevice() {
        try {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
                    .getBondedDevices();

            if (pairedDevices.size() > 0) {
                for (BluetoothDevice bd : pairedDevices) {
                    String deviceName = bd.getName();
                    LogMessage("BT", deviceName + " => " + bd.getAddress());

                    UnityPlayer.UnitySendMessage(UnityObjectName,
                            "BluetoothDevice", deviceName);
                }
            } else {
                LogMessage("BT", "Paired devices not found!");
            }

            UnityPlayer.UnitySendMessage(UnityObjectName, "BluetoothDevice",
                    "END");

            LogMessage("BT", "SearchBluetoothDevice : " + "END");
        } catch (Exception e) {
            LogMessage("BT", "Error:" + e.getMessage());
        }
    }

    private void SetupBluetoothManager() {
        if (mBTManager == null)
            mBTManager = new BTManager(this, mHandler);
    }

    public void SetRobotMode(String robotMode) {
        if (robotMode.equalsIgnoreCase("ROBOTIS"))
            mRobotMode = ROBOT_MODE.ROBOTIS;
        else if (robotMode.equalsIgnoreCase("ROBOROBO"))
            mRobotMode = ROBOT_MODE.ROBOROBO;
        else if (robotMode.equalsIgnoreCase("UCR"))
            mRobotMode = ROBOT_MODE.UCR;
    }

    public void ConnectBluetooth(String deviceName) {
        LogMessage("BT", "ConnectBluetooth");
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
                .getBondedDevices();
        for (BluetoothDevice bd : pairedDevices) {
            if (bd.getName().equalsIgnoreCase(deviceName)) {
                SetupBluetoothManager();
                mBTManager.connect(bd);
                UnityPlayer.UnitySendMessage(UnityObjectName,
                        "BluetoothConnectState", "Success");
                return;
            }
        }

        UnityPlayer.UnitySendMessage(UnityObjectName, "BluetoothConnectState",
                "Fail");
    }

    public void DisconnectBluetooth() {
        try {
            LogMessage("BT", "Attempting to break BT connection");
            if (mBTManager != null) {
                mBTManager.stop();
                mBTManager = null;
            }
            UnityPlayer.UnitySendMessage(UnityObjectName,
                    "BluetoothConnectState", "Fail");
        } catch (Exception e) {
            LogMessage("BT", "Error in DoDisconnect [" + e.getMessage() + "]");
        }
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if (D)
                    Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BTManager.STATE_CONNECTED:
                    LogMessage("BT", "STATE_CONNECTED");
                    break;
                case BTManager.STATE_CONNECTING:
                    LogMessage("BT", "STATE_CONNECTING");
                    break;
                case BTManager.STATE_LISTEN:
                case BTManager.STATE_NONE:
                    LogMessage("BT", "STATE_NOTCONNECTED");
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                int[] writeData = new int[writeBuf.length];

                for (int i = 0; i < writeBuf.length; i++) {
                    if (writeBuf[i] < 0)
                        writeData[i] = (int) writeBuf[i] + 256;
                    else
                        writeData[i] = (int) writeBuf[i];
                }

                int writeValue = (int) ((writeData[4] << 8) | (writeData[2]));
                LogMessage("BT", "Write:" + Integer.toString(writeValue));
                break;
            case MESSAGE_READ:
                int len = (int) msg.arg1;
                byte[] readBuf = Arrays.copyOf((byte[]) msg.obj, len);
                UnityPlayer.UnitySendMessage(UnityObjectName,"BluetoothData", bytesToHex(readBuf));
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                LogMessage(
                        "BT",
                        "Connected Device:"
                                + msg.getData().getString(DEVICE_NAME));
                break;
            }
        }
    };
    
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int i = 0; i < bytes.length; i++ ) {
            v = bytes[i] & 0xFF;
            hexChars[i*2]     = hexArray[v >>> 4];
            hexChars[i*2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public void StartTalk(String message) {
        // sayHello();
        mTts.speak(message, TextToSpeech.QUEUE_FLUSH, null);
    }

    // Implements TextToSpeech.OnInitListener.
    public void onInit(int status) {
        // status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.
        if (status == TextToSpeech.SUCCESS) {
            int result = mTts.setLanguage(Locale.KOREA);
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                LogMessage("SP", "Language is not available.");
                result = mTts.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    LogMessage("SP", "No Language Supports.");
                }
            }
        } else {
            // Initialization failed.
            LogMessage("SP", "Could not initialize TextToSpeech.");
        }
    }

    public void onUtteranceCompleted(String utteranceId) {
        Log.i(TAG, utteranceId); // utteranceId == "SOME MESSAGE"
        UnityPlayer.UnitySendMessage(UnityObjectName, "Ttsend", "끝"
                + utteranceId);
    }

    public void StartRecognition() {
        if (!isSpeechRecognitionFound)
            return;

        startVoiceRecognitionActivity();
    }

    private void startVoiceRecognitionActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREA);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "수행할 동작을 말해보세요.");
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }

    public void Camera() {
        Intent cameraIntent = new Intent(
                android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, 1337);
    }

    public int MessageTest(int number) {
        return number + 10;
    }

    // ----- Android Sensor Manage Methods -----
    public void initSensorManager() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager == null)
            mSensorManager = new SiCiSensorManager(sensorManager);
    }

    final public static int TYPE_ACCELEROMETER = 1;
    final public static int TYPE_MAGNETICFIELD = 2;
    final public static int TYPE_ORIENTATION = 3;
    final public static int TYPE_GYROSCOPE = 4;
    final public static int TYPE_LIGHT = 5;
    final public static int TYPE_PRESSURE = 6;
    final public static int TYPE_TEMPERATURE = 7;
    final public static int TYPE_PROXIMITY = 8;
    final public static int TYPE_GRAVITY = 9;
    final public static int TYPE_LINEARACCELERATION = 10;
    final public static int TYPE_ROTATIONVECTOR = 11;
    final public static int TYPE_HUMIDITY = 12;
    final public static int TYPE_AMBIENTTEMPERATURE = 13;

    @SuppressLint("DefaultLocale")
    public void enableSensor(String type) {
        boolean retValue = mSensorManager.enable(Integer.parseInt(type), true);
        String message;
        if (retValue)
            message = String.format("%d:%s", Integer.parseInt(type), "true");
        else
            message = String.format("%d:%s", Integer.parseInt(type), "false");
        UnityPlayer.UnitySendMessage(UnityObjectName, "RetEnableSensor",
                message);
    }

    public void disableSensor(String type) {
        mSensorManager.enable(Integer.parseInt(type), false);
    }

    @SuppressLint("DefaultLocale")
    public void hasSensorValue(String type) {
        boolean hasValue = mSensorManager.hasValue(Integer.parseInt(type));
        String message = String.format("%d:%s", Integer.parseInt(type),
                (hasValue) ? "true" : "false");
        UnityPlayer.UnitySendMessage(UnityObjectName, "RetHasSensorValue",
                message);
    }

    @SuppressLint("DefaultLocale")
    public void getSensorValue(String type) {
        float value0 = mSensorManager.getValue(Integer.parseInt(type), 0);
        float value1 = mSensorManager.getValue(Integer.parseInt(type), 1);
        float value2 = mSensorManager.getValue(Integer.parseInt(type), 2);
        String stringSensorValue = String.format("%d:%f,%f,%f",
                Integer.parseInt(type), value0, value1, value2);
        UnityPlayer.UnitySendMessage(UnityObjectName, "RetGetSensor",
                stringSensorValue);
    }

    // ----- Android Mic Amplitude Methods -----
    private MediaRecorder mMediaRecorder = new MediaRecorder();
    int mMediaAmplitude = -1;
    Timer micTimer = null;

    public void enableMic(String filepath) {
        mMediaRecorder.reset();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        mMediaRecorder.setOutputFile(filepath);
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaRecorder.start();
        micTimer = new Timer();
        micTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        mMediaAmplitude = mMediaRecorder.getMaxAmplitude();
                        UnityPlayer.UnitySendMessage(UnityObjectName,
                                "RetMicAmplitude",
                                String.valueOf(mMediaAmplitude));
                    }
                });
            }
        }, 1000, 100);
    }

    public void disableMic() {
        if (micTimer != null) {
            micTimer.cancel();
            micTimer.purge();
            micTimer = null;
        }
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
        }
    }

    // ----- End of Android Mic Amplitude Methods -----

    /**
     * Handle the results from the recognition activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE
                && resultCode == RESULT_OK) {
            // Fill the list view with the strings the recognizer thought it
            // could have heard
            ArrayList<String> matches = data
                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            if (matches.size() > 0)
                UnityPlayer.UnitySendMessage(UnityObjectName,
                        "RecognizedMessage", matches.get(0));
            else
                LogMessage("SP", "적절한 단어를 찾지 못했습니다");
        } else if (requestCode == 1337) {
            if (data != null) {
                Bitmap rawThumbnail = (Bitmap) data.getExtras().get("data");
                Bitmap thumbnail = Bitmap.createScaledBitmap(rawThumbnail, 400,
                        300, true);

                if (thumbnail != null) {
                    UnityPlayer.UnitySendMessage(UnityObjectName, "Camera",
                            thumbnail.toString());
                    imageView.setImageBitmap(thumbnail);
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    class BTChildThread extends Thread {
        public void run() {
            Looper.prepare();

            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                LogMessage("BT", "Bluetooth is not available");
                return;
            }

            LogMessage("BT", "Bluetooth is available");

            // Looper.loop();
        }
    }
}