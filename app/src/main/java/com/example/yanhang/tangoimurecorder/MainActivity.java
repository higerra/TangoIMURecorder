package com.example.yanhang.tangoimurecorder;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;

import android.hardware.SensorEventListener;
import android.hardware.display.DisplayManager;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.Scene;
import android.util.Log;
import android.hardware.SensorEvent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.net.wifi.WifiManager;

import com.example.yanhang.tangoimurecorder.rajawali.ScenePoseCalculator;
import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoAreaDescriptionMetaData;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPoseData;

// import com.projecttango.tangosupport.ux.TangoUx;
import com.google.tango.ux.TangoUx;
import com.google.tango.ux.UxExceptionEvent;
import com.google.tango.ux.UxExceptionEventListener;
import com.google.tango.support.TangoSupport;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.rajawali3d.surface.RajawaliSurfaceView;
import org.rajawali3d.scene.ASceneFrameCallback;
import org.rajawali3d.math.Matrix4;


public class MainActivity extends AppCompatActivity
        implements SensorEventListener, SetAdfNameDialog.CallbackListener, SaveAdfTask.SaveAdfListener {

    private static final String LOG_TAG = MainActivity.class.getName();
    public static final String INTENT_EXTRA_CONFIG = "config";
    public static final String INTENT_EXTRA_ADF_NAME = "adf_name";
    public static final String INTENT_EXTRA_ADF_UUID = "adf_uuid";

    private static final int REQUEST_CODE_WRITE_EXTERNAL = 1001;
    private static final int REQUEST_CODE_CAMERA = 1002;
    private static final int REQUEST_CODE_ACCESS_WIFI = 1003;
    private static final int REQUEST_CODE_CHANGE_WIFI = 1004;
    private static final int REQUEST_CODE_COARSE_LOCATION = 1005;
    private static final int REQUEST_CODE_AREA_LEARNING = 1006;

    private static final int RESULT_CODE_PICK_ADF = 2001;

    private static final int INVALID_TEXTURE_ID = 0;
//    int mRenderedTexture = TangoCameraIntrinsics.TANGO_CAMERA_FISHEYE;

    int mRenderedTexture = -1;

    private Tango mTango;
    private TangoConfig mTangoConfig;
    private TangoUx mTangoUx;

    private ArrayList<String> mAdfNames = new ArrayList<>();
    private ArrayList<String> mAdfUuids = new ArrayList<>();

    private UxExceptionEventListener mUxExceptionEventListener = new UxExceptionEventListener() {
        @Override
        public void onUxExceptionEvent(UxExceptionEvent uxExceptionEvent) {
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_LYING_ON_SURFACE) {
                Log.i(LOG_TAG, "Device lying on surface ");
                showToast("Device lying on surface");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FEW_DEPTH_POINTS) {
                Log.i(LOG_TAG, "Very few depth points in mPoint cloud ");
                showToast("Very few depth points in the point cloud");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FEW_FEATURES) {
                Log.i(LOG_TAG, "Too few features");
                showToast("Too few features");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_MOTION_TRACK_INVALID) {
                Log.i(LOG_TAG, "Invalid poses in MotionTracking");
                showToast("Invalid poses in MotionTracking");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_MOVING_TOO_FAST) {
                Log.i(LOG_TAG, "Moving to fast");
                showToast("Moving too fast");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FISHEYE_CAMERA_OVER_EXPOSED) {
                Log.i(LOG_TAG, "Fisheye Camera Over Exposed");
                showToast("Fisheye Camera Over Exposed");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FISHEYE_CAMERA_UNDER_EXPOSED) {
                Log.i(LOG_TAG, "Fisheye camera under exposed");
                showToast("Fisheye camera under exposed");
            }
        }
    };

    private PoseIMURecorder mRecorder;
    private OutputDirectoryManager mOutputDirectoryManager;
    private MotionRajawaliRenderer mRenderer;
    private org.rajawali3d.surface.RajawaliSurfaceView mSurfaceView;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private Sensor mGravity;
    private Sensor mLinearAcce;
    private Sensor mOrientation;
    private Sensor mMagnetometer;
    private Sensor mStepCounter;

    private float mInitialStepCount = -1;

    private PeriodicScan wifi_scanner_;
    private WifiManager mWifiMangerRef;
    private BroadcastReceiver mWifiScanReceiverRef;

    // Gyroscope
    private TextView mLabelRx;
    private TextView mLabelRy;
    private TextView mLabelRz;
    // Accelerometer
    private TextView mLabelAx;
    private TextView mLabelAy;
    private TextView mLabelAz;
    // Linear acceleration
    private TextView mLabelLx;
    private TextView mLabelLy;
    private TextView mLabelLz;
    // Magnetometer
    private TextView mLabelMx;
    private TextView mLabelMy;
    private TextView mLabelMz;

    private TextView mLabelScanTimes;
    private TextView mLabelWifiNums;

    private TextView mLabelStepCount;

    private TextView mLabelInfoPose;
    private TextView mLabelInfoADF;
    private TextView mLabelInfoAL;
    private TextView mLabelInfoWifi;
    private TextView mLabelInfoWifiInterval;
    private TextView mLabelInfoRedun;
    private TextView mLabelInfoFile;
    private TextView mLabelInfoPrefix;
    private TextView mLabelSS2AL;
    private TextView mLabelD2SS;
    private TextView mLabelD2AL;
    private TextView mLabelRenderPose;

    static final int ROTATION_SENSOR = Sensor.TYPE_GAME_ROTATION_VECTOR;

    private Button mStartStopButton;
    private Button mScanButton;

    private int mCameraToDisplayRotation = 0;

    TangoIMUConfig mConfig = new TangoIMUConfig();

    private AtomicBoolean mIsConnected = new AtomicBoolean(false);
    private AtomicBoolean mIsTangoInitialized = new AtomicBoolean(false);
    private AtomicBoolean mIsRecording = new AtomicBoolean(false);
    private AtomicBoolean mIsLocalizedToADF = new AtomicBoolean(false);
    private Matrix4 mInitialTransform = new Matrix4();

    private boolean mStoragePermissionGranted = false;
    private boolean mCameraPermissionGranted = false;
    private boolean mAccessWifiPermissionGranted = false;
    private boolean mChangeWifiPermissionGranted = false;
    private boolean mCoarseLocationPermissionGranted = false;

    SaveAdfTask mSaveADFTask = null;
    private AtomicInteger mLocalizeCounter = new AtomicInteger(0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTangoUx = setupTangoUx();
        mTango = new Tango(MainActivity.this, new Runnable() {
            @Override
            public void run() {
                synchronized (MainActivity.this) {
                    try {
                        TangoSupport.initialize(mTango);
                        mIsTangoInitialized.set(true);
                    } catch (TangoOutOfDateException e) {
                        Log.e(LOG_TAG, "Out of date");
                    } catch (TangoErrorException e) {
                        Log.e(LOG_TAG, "Tango error");
                    } catch (TangoInvalidException e) {
                        Log.e(LOG_TAG, "Tango exception");
                    }
                }
            }
        });
        mSurfaceView = (RajawaliSurfaceView) findViewById(R.id.gl_surface_view);
        mRenderer = new MotionRajawaliRenderer(this);

        setupRenderer();

        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if (displayManager != null) {
            displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {

                }

                @Override
                public void onDisplayRemoved(int displayId) {
                    synchronized (this) {
                        setAndroidOrientation();
                    }
                }

                @Override
                public void onDisplayChanged(int displayId) {

                }
            }, null);
        }

        // initialize IMU sensor
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mLinearAcce = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mOrientation = mSensorManager.getDefaultSensor(ROTATION_SENSOR);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mStepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        // initialize UI widgets
        mLabelRx = (TextView) findViewById(R.id.label_rx);
        mLabelRy = (TextView) findViewById(R.id.label_ry);
        mLabelRz = (TextView) findViewById(R.id.label_rz);
        mLabelAx = (TextView) findViewById(R.id.label_ax);
        mLabelAy = (TextView) findViewById(R.id.label_ay);
        mLabelAz = (TextView) findViewById(R.id.label_az);
        mLabelLx = (TextView) findViewById(R.id.label_lx);
        mLabelLy = (TextView) findViewById(R.id.label_ly);
        mLabelLz = (TextView) findViewById(R.id.label_lz);
        mLabelMx = (TextView) findViewById(R.id.label_mx);
        mLabelMy = (TextView) findViewById(R.id.label_my);
        mLabelMz = (TextView) findViewById(R.id.label_mz);

        mLabelScanTimes = (TextView) findViewById(R.id.label_wifi_record_num);
        mLabelWifiNums = (TextView) findViewById(R.id.label_wifi_beacon_num);

        mLabelStepCount = (TextView) findViewById(R.id.label_step_count);

        mLabelInfoPose = (TextView) findViewById(R.id.label_info_pose);
        mLabelInfoADF = (TextView) findViewById(R.id.label_info_adf);
        mLabelInfoAL = (TextView) findViewById(R.id.label_info_al);
        mLabelInfoWifi = (TextView) findViewById(R.id.label_info_wifi);
        mLabelInfoRedun = (TextView) findViewById(R.id.label_info_redun);
        mLabelInfoWifiInterval = (TextView) findViewById(R.id.label_info_wifi_interval);
        mLabelInfoFile = (TextView) findViewById(R.id.label_info_file);
        mLabelInfoPrefix = (TextView) findViewById(R.id.label_info_prefix);
        mLabelSS2AL = (TextView) findViewById(R.id.label_ss_to_al);
        mLabelD2SS = (TextView) findViewById(R.id.label_device_to_ss);
        mLabelD2AL = (TextView) findViewById(R.id.label_device_to_al);
        mLabelRenderPose = (TextView) findViewById(R.id.label_render_pose);

        mStartStopButton = (Button) findViewById(R.id.button_start_stop);
        mScanButton = (Button) findViewById(R.id.button_scan);
        mScanButton.setVisibility(View.GONE);

        updateConfig();

        Runnable wifi_callback = new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLabelScanTimes.setText(String.valueOf(wifi_scanner_.getRecordCount()));
                        mLabelWifiNums.setText(String.valueOf(wifi_scanner_.getLatestScanResult().size()));
                        if (mConfig.getWifiEnabled() && !mConfig.getContinuesWifiScan()) {
                            mScanButton.setEnabled(true);
                        }
                    }
                });
            }
        };

        wifi_scanner_ = new PeriodicScan(this, wifi_callback);
        mWifiScanReceiverRef = wifi_scanner_.getBroadcastReceiver();
        mWifiMangerRef = wifi_scanner_.getWifiManager();

        startActivityForResult(
                Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_ADF_LOAD_SAVE), REQUEST_CODE_AREA_LEARNING
        );
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_setting:
                if (mIsRecording.get()) {
                    break;
                }
                Intent intent = new Intent(this, PrefActivity.class);
                intent.putExtra(INTENT_EXTRA_ADF_NAME, mAdfNames);
                intent.putExtra(INTENT_EXTRA_ADF_UUID, mAdfUuids);
                startActivity(intent);
                break;
        }
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mIsRecording.get()) {
            menu.getItem(0).setEnabled(false);
        } else {
            menu.getItem(0).setEnabled(true);
        }
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mIsRecording.get()) {
            stopRecording();
        }
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mGyroscope);
        mSensorManager.unregisterListener(this, mGravity);
        mSensorManager.unregisterListener(this, mLinearAcce);
        mSensorManager.unregisterListener(this, mOrientation);
        mSensorManager.unregisterListener(this, mMagnetometer);
        mSensorManager.unregisterListener(this, mStepCounter);

        unregisterReceiver(mWifiScanReceiverRef);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mStoragePermissionGranted = checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_CODE_WRITE_EXTERNAL);
        mCameraPermissionGranted = checkPermission(Manifest.permission.CAMERA, REQUEST_CODE_CAMERA);
        mAccessWifiPermissionGranted = checkPermission(Manifest.permission.ACCESS_WIFI_STATE, REQUEST_CODE_ACCESS_WIFI);
        mChangeWifiPermissionGranted = checkPermission(Manifest.permission.CHANGE_WIFI_STATE, REQUEST_CODE_CHANGE_WIFI);
        mCoarseLocationPermissionGranted = checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, REQUEST_CODE_COARSE_LOCATION);

        updateConfig();
        if (mIsTangoInitialized.get()) {
            updateADFList();
        }

        mStartStopButton.setText(R.string.start_title);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mLinearAcce, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mStepCounter, SensorManager.SENSOR_DELAY_FASTEST);

        registerReceiver(mWifiScanReceiverRef, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        // prevent screen lock
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void updateConfig() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        mConfig.setPoseEnabled(pref.getBoolean("pref_pose_enabled", true));
        mConfig.setFileEnabled(pref.getBoolean("pref_file_enabled", true));
        mConfig.setFolderPrefix(pref.getString("pref_folder_prefix", ""));
        mConfig.setWifiEnabled(pref.getBoolean("pref_wifi_enabled", true));
        mConfig.setContinuesWifiScan(pref.getBoolean("pref_auto_wifi_enabled", false));
        mConfig.setADFEnabled(pref.getBoolean("pref_adf_enabled", false));
        mConfig.setAreaLearningMode(pref.getBoolean("pref_al_mode", false));
        mConfig.setADFUuid(pref.getString("pref_adf_uuid", ""));
        mConfig.setNumRequestsPerScan(Integer.valueOf(pref.getString("pref_num_requests", "1")));
        mConfig.setWifiScanInterval(Integer.valueOf(pref.getString("pref_scan_interval", "1")));

        int index = mAdfUuids.indexOf(mConfig.getADFUuid());
        if (index >= 0 && index < mAdfNames.size()) {
            mConfig.setADFName(mAdfNames.get(index));
        } else {
            mConfig.setADFName(mConfig.getADFUuid());
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mConfig.getPoseEnabled()) {
                    mLabelInfoPose.setText("ON");
                } else {
                    mLabelInfoPose.setText("OFF");
                }
                if (mConfig.getWifiEnabled()) {
                    mLabelInfoRedun.setText(String.valueOf(mConfig.getNumRequestsPerScan()));
                    if (mConfig.getContinuesWifiScan()) {
                        mScanButton.setVisibility(View.INVISIBLE);
                        mLabelInfoWifi.setText("AUTO");
                        mLabelInfoWifiInterval.setText(String.valueOf(mConfig.getWifiScanInterval()));
                    } else {
                        mScanButton.setVisibility(View.VISIBLE);
                        mLabelInfoWifi.setText("Manual");
                        mLabelInfoWifiInterval.setText("N/A");
                    }
                } else {
                    mLabelInfoRedun.setText("N/A");
                    mScanButton.setVisibility(View.INVISIBLE);
                    mLabelInfoWifi.setText("OFF");
                }

                if (mConfig.getAreaLearningMode()) {
                    mLabelInfoAL.setText("ON");
                } else {
                    mLabelInfoAL.setText("OFF");
                }

                if (mConfig.getADFEnabled()) {
                    mLabelInfoADF.setText(mConfig.getADFName());
                } else {
                    mLabelInfoADF.setText("OFF");
                }
                if (mConfig.getFileEnabled()){
                    mLabelInfoFile.setText("Enabled");
                    mLabelInfoPrefix.setText(mConfig.getFolderPrefix());
                } else {
                    mLabelInfoFile.setText("Disabled");
                    mLabelInfoPrefix.setText("N/A");
                }
            }
        });
    }

    private void updateADFList() {
        TangoAreaDescriptionMetaData metaData = new TangoAreaDescriptionMetaData();
        mAdfUuids = mTango.listAreaDescriptions();
        mAdfNames.clear();
        for (String uuid : mAdfUuids) {
            String name;
            try {
                metaData = mTango.loadAreaDescriptionMetaData(uuid);
            } catch (TangoErrorException e) {
                mAdfNames.add("Unknown");
            }
            name = new String(metaData.get(TangoAreaDescriptionMetaData.KEY_NAME));
            mAdfNames.add(name);
        }
        updateConfig();
    }

    private void showAlertAndStop(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(text)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                stopRecording();
                            }
                        }).show();
            }
        });
    }

    private void showToast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startNewRecording() {
        if (!mStoragePermissionGranted) {
            showAlertAndStop("Storage permission not granted");
            return;
        }
        if (!mCameraPermissionGranted) {
            showAlertAndStop("Camera permission not granted");
            return;
        }
        if (mConfig.getWifiEnabled() && (!mAccessWifiPermissionGranted || !mChangeWifiPermissionGranted)) {
            showAlertAndStop("Wifi permission not granted");
            return;
        }
        if (mConfig.getWifiEnabled() && !mCoarseLocationPermissionGranted) {
            showAlertAndStop("Location permission not granted");
            return;
        }

        // initialize Wifi
        if (mConfig.getWifiEnabled()) {
            if (!mWifiMangerRef.isWifiEnabled()) {
                showAlertAndStop("Turn on wifi first");
                return;
            }
            wifi_scanner_.reset();
            Log.i(LOG_TAG, "Configured redundancy: " + String.valueOf(mConfig.getNumRequestsPerScan()));
            Log.i(LOG_TAG, "Configured scan interval: " + String.valueOf(mConfig.getWifiScanInterval()));
            wifi_scanner_.setRedundancy(mConfig.getNumRequestsPerScan());
            wifi_scanner_.setScanInterval(mConfig.getWifiScanInterval() * 1000);
            if (mConfig.getContinuesWifiScan()) {
                wifi_scanner_.start();
            }
        }

        if (mConfig.getPoseEnabled()) {
            if (!mIsTangoInitialized.get()) {
                showAlertAndStop("Tango not initialized");
                return;
            }
            // initialize tango service
            synchronized (this) {
                TangoConfig config = setupTangoConfig(mTango);
                mTangoUx.start();
                mTango.connect(config);
                startupTango();
                mIsConnected.set(true);
            }
        }
        // initialize recorder
        if (mConfig.getFileEnabled()) {
            try {
                mOutputDirectoryManager = new OutputDirectoryManager(mConfig.getFolderPrefix());
                mRecorder = new PoseIMURecorder(mOutputDirectoryManager.getOutputDirectory(), this);
            } catch (FileNotFoundException e) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.alert_title)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                stopRecording();
                            }
                        }).show();
                e.printStackTrace();
            }
        }
        mInitialStepCount = -1.0f;
        mIsRecording.set(true);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStartStopButton.setText(R.string.stop_title);
            }
        });
    }

    private void stopRecording() {
        mIsRecording.set(false);
        updateADFList();
        if (mRecorder != null) {
            mRecorder.endFiles();
        }

        if (mConfig.getWifiEnabled()) {
            if (mConfig.getContinuesWifiScan()) {
                wifi_scanner_.terminate();
            }
            if (mConfig.getFileEnabled()) {
                wifi_scanner_.saveResultToFile(mRecorder.getOutputDir() + "/wifi.txt");
            }
        }

        if (mConfig.getPoseEnabled()) {
            synchronized (this) {
                try {
                    mTangoUx.stop();
                    if (mConfig.getAreaLearningMode()) {
                        showSetAdfNameDialog();
                    } else {
                        mTango.disconnect();
                        mIsConnected.set(false);
                        mIsLocalizedToADF.set(false);
                        mLocalizeCounter.set(0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        resetUI();
        showToast("Stopped");
    }

    public void startStopRecording(View view) {
        if (!mIsRecording.get()) {
            startNewRecording();
        } else {
            stopRecording();
        }
    }

    public void scanWifi(View view) {
        wifi_scanner_.singleScan();
        mScanButton.setEnabled(false);
    }

    private TangoConfig setupTangoConfig(Tango tango) {
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_MOTIONTRACKING, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_HIGH_RATE_POSE, true);
        if (mConfig.getAreaLearningMode()) {
            config.putBoolean(TangoConfig.KEY_BOOLEAN_LEARNINGMODE, true);
        }
        if (mConfig.getADFEnabled() && mConfig.getADFUuid() != null) {
            try {
                config.putString(TangoConfig.KEY_STRING_AREADESCRIPTION, mConfig.getADFUuid());
                Log.i(LOG_TAG, mConfig.getADFUuid() + " loaded");
            } catch (TangoErrorException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }
        return config;
    }

    private TangoUx setupTangoUx() {
        TangoUx tangoUx = new TangoUx(this);
        tangoUx.setUxExceptionEventListener(mUxExceptionEventListener);
        return tangoUx;
    }

    private void setupRenderer() {
        // motion renderer
        mSurfaceView.setEGLContextClientVersion(2);
        mRenderer.getCurrentScene().registerFrameCallback(new ASceneFrameCallback() {
            @Override
            public void onPreFrame(long sceneTime, double deltaTime) {
                synchronized (MainActivity.this) {
                    // Don't execute any tango API actions if we're not connected to the service
                    if (!mIsConnected.get()) {
                        return;
                    }

                    // Update current camera pose
                    try {
                        TangoPoseData lastFramePose = TangoSupport.getPoseAtTime(0,
                                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                                TangoPoseData.COORDINATE_FRAME_DEVICE,
                                TangoSupport.ENGINE_OPENGL,
                                TangoSupport.ENGINE_OPENGL, mCameraToDisplayRotation);
                        if (mConfig.getADFEnabled()) {
                            if (mIsLocalizedToADF.get()) {
                                Matrix4 pose_m = ScenePoseCalculator.tangoPoseToMatrix(lastFramePose);
                                pose_m.leftMultiply(mInitialTransform);
                                final TangoPoseData render_pose = ScenePoseCalculator.matrixToTangoPose(pose_m);
                                mRenderer.updateCameraPose(render_pose);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mLabelRenderPose.setText(tangoPoseToShortString(render_pose));
                                    }
                                });
                            }
                        } else {
                            mRenderer.updateCameraPose(lastFramePose);
                        }
                    } catch (TangoErrorException e) {
                        Log.e(LOG_TAG, "Could not get valid transform");
                    }
                }
            }

            @Override
            public boolean callPreFrame() {
                return true;
            }

            @Override
            public void onPreDraw(long sceneTime, double deltaTime) {

            }

            @Override
            public void onPostFrame(long sceneTime, double deltaTime) {

            }
        });

        mSurfaceView.setSurfaceRenderer(mRenderer);
    }

    private void resetUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStartStopButton.setText(R.string.start_title);
                mLabelSS2AL.setText("N/A");
                mLabelD2AL.setText("N/A");
                mLabelD2SS.setText("N/A");
                mLabelWifiNums.setText("N/A");
                mLabelScanTimes.setText("N/A");
                mLabelStepCount.setText("N/A");
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        mRenderer.onTouchEvent(motionEvent);
        return true;
    }

    private void setAndroidOrientation() {
        Display display = getWindowManager().getDefaultDisplay();
        Camera.CameraInfo depthCameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(1, depthCameraInfo);

        int depthCameraRotation = Surface.ROTATION_0;
        switch (depthCameraInfo.orientation) {
            case 90:
                depthCameraRotation = Surface.ROTATION_90;
                break;
            case 180:
                depthCameraRotation = Surface.ROTATION_180;
                break;
            case 270:
                depthCameraRotation = Surface.ROTATION_270;
                break;
        }

        mCameraToDisplayRotation = display.getRotation() - depthCameraRotation;
        if (mCameraToDisplayRotation < 0) {
            mCameraToDisplayRotation += 4;
        }
    }

    private String tangoPoseToShortString(final TangoPoseData pose){
        return String.format(Locale.US, "p:(%.3f %.3f %.3f) q:(%.3f %.3f %.3f %.3f)",
                pose.translation[0], pose.translation[1], pose.translation[2], pose.rotation[0],
                pose.rotation[1], pose.rotation[2], pose.rotation[3]);
    }

    private void startupTango() {
        ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<>();
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE
        ));
        if (mConfig.getADFEnabled()) {
            framePairs.add(new TangoCoordinateFramePair(
                    TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                    TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE
            ));
            framePairs.add(new TangoCoordinateFramePair(
                    TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                    TangoPoseData.COORDINATE_FRAME_DEVICE
            ));
        }

        mIsLocalizedToADF.set(false);
        mLocalizeCounter.set(0);

        mTango.connectListener(framePairs, new Tango.TangoUpdateCallback() {
            @Override
            public void onPoseAvailable(final TangoPoseData tangoPoseData) {
                if (mTangoUx != null) {
                    mTangoUx.updatePoseStatus(tangoPoseData.statusCode);
                }

                // For robustness we do not use frame_device to frame_area_description. Instead,
                // we obtain the transformation from frame_start_of_service to frame_area_description,
                // and apply this initial transformation to pose of frame_device to frame_start_of_service.
                if (mConfig.getADFEnabled()) {
                    if (tangoPoseData.baseFrame == TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
                            && tangoPoseData.targetFrame == TangoPoseData.COORDINATE_FRAME_DEVICE
                            && tangoPoseData.statusCode == TangoPoseData.POSE_VALID){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mLabelD2AL.setText(tangoPoseToShortString(tangoPoseData));
                            }
                        });
                    }
                   if (tangoPoseData.baseFrame == TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE
                            && tangoPoseData.targetFrame == TangoPoseData.COORDINATE_FRAME_DEVICE
                            && tangoPoseData.statusCode == TangoPoseData.POSE_VALID) {
                        if (mIsRecording.get() && mIsLocalizedToADF.get()) {
                            Matrix4 pose_m = ScenePoseCalculator.tangoPoseToMatrix(tangoPoseData);
                            pose_m.leftMultiply(mInitialTransform);

                            final TangoPoseData transformed = ScenePoseCalculator.matrixToTangoPose(pose_m);
                            transformed.timestamp = tangoPoseData.timestamp;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mLabelD2SS.setText(tangoPoseToShortString(transformed));
                                }
                            });

                            if (mConfig.getFileEnabled()) {
                                mRecorder.addPoseRecord(transformed);
                            }
                        }
                    }
                    if (tangoPoseData.baseFrame == TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
                            && tangoPoseData.targetFrame == TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE
                            && tangoPoseData.statusCode == TangoPoseData.POSE_VALID) {
                        mInitialTransform = ScenePoseCalculator.tangoPoseToMatrix(tangoPoseData);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mLabelSS2AL.setText(tangoPoseToShortString(tangoPoseData));
                            }
                        });
                        if (mLocalizeCounter.addAndGet(1) > 5 && !mIsLocalizedToADF.get()) {
                            mIsLocalizedToADF.set(true);
                            showToast("Localized to " + mConfig.getADFName());
                        }
                    }
                } else {
                    if (tangoPoseData.baseFrame == TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE
                            && tangoPoseData.targetFrame == TangoPoseData.COORDINATE_FRAME_DEVICE
                            && tangoPoseData.statusCode == TangoPoseData.POSE_VALID) {
                        if (mIsRecording.get() && mConfig.getFileEnabled()) {
                            mRecorder.addPoseRecord(tangoPoseData);
                        }
                    }
                }
            }

            @Override
            public void onTangoEvent(TangoEvent tangoEvent) {
                if (mTangoUx != null) {
                    mTangoUx.updateTangoEvent(tangoEvent);
                }
            }
        });
    }

    // receive IMU data
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        long timestamp = event.timestamp;
        float[] values = {0.0f, 0.0f, 0.0f, 0.0f};
        final Boolean mIsWriteFile = mConfig.getFileEnabled();
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLabelAx.setText(String.format(Locale.US, "%.6f", event.values[0]));
                    mLabelAy.setText(String.format(Locale.US, "%.6f", event.values[1]));
                    mLabelAz.setText(String.format(Locale.US, "%.6f", event.values[2]));
                }
            });
            if (mIsRecording.get() && mIsWriteFile) {
                System.arraycopy(event.values, 0, values, 0, 3);
                mRecorder.addIMURecord(timestamp, values, PoseIMURecorder.ACCELEROMETER);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLabelRx.setText(String.format(Locale.US, "%.6f", event.values[0]));
                    mLabelRy.setText(String.format(Locale.US, "%.6f", event.values[1]));
                    mLabelRz.setText(String.format(Locale.US, "%.6f", event.values[2]));
                }
            });
            if (mIsRecording.get() && mIsWriteFile) {
                System.arraycopy(event.values, 0, values, 0, 3);
                mRecorder.addIMURecord(timestamp, values, PoseIMURecorder.GYROSCOPE);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLabelLx.setText(String.format(Locale.US, "%.6f", event.values[0]));
                    mLabelLy.setText(String.format(Locale.US, "%.6f", event.values[1]));
                    mLabelLz.setText(String.format(Locale.US, "%.6f", event.values[2]));
                }
            });
            if (mIsRecording.get() && mIsWriteFile) {
                System.arraycopy(event.values, 0, values, 0, 3);
                mRecorder.addIMURecord(timestamp, values, PoseIMURecorder.LINEAR_ACCELERATION);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            if (mIsRecording.get() && mIsWriteFile) {
                System.arraycopy(event.values, 0, values, 0, 3);
                mRecorder.addIMURecord(timestamp, values, PoseIMURecorder.GRAVITY);
            }
        } else if (event.sensor.getType() == ROTATION_SENSOR) {
            if (mIsRecording.get() && mIsWriteFile) {
                System.arraycopy(event.values, 0, values, 0, 4);
                mRecorder.addIMURecord(timestamp, values, PoseIMURecorder.ROTATION_VECTOR);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLabelMx.setText(String.format(Locale.US, "%.6f", event.values[0]));
                    mLabelMy.setText(String.format(Locale.US, "%.6f", event.values[1]));
                    mLabelMz.setText(String.format(Locale.US, "%.6f", event.values[2]));
                }
            });
            if (mIsRecording.get() && mIsWriteFile) {
                System.arraycopy(event.values, 0, values, 0, 3);
                mRecorder.addIMURecord(timestamp, values, PoseIMURecorder.MAGNETOMETER);
            }
        } else if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if (mIsRecording.get()) {
                if (mInitialStepCount < 0) {
                    mInitialStepCount = event.values[0] - 1;
                }
                if (mIsRecording.get() && mIsWriteFile) {
                    mRecorder.addStepRecord(timestamp, (int) (event.values[0] - mInitialStepCount));
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int cur_step = (int) (event.values[0] - mInitialStepCount);
                        mLabelStepCount.setText(String.valueOf(cur_step));
                    }
                });
            }
        }
    }

    @Override
    public void onAdfNameOk(String name, String uuid) {
        showToast("Saving...");
        mSaveADFTask = new SaveAdfTask(this, this, mTango, name);
        mSaveADFTask.execute();
    }

    @Override
    public void onAdfNameCancelled() {
        showToast("Canceled!");
    }

    @Override
    public void onSaveAdfFailed(String adfName) {
        showToast("Save failed");
        mSaveADFTask = null;
        mTango.disconnect();
        mIsConnected.set(false);
    }

    @Override
    public void onSaveAdfSuccess(String adfName, String adfUuid) {
        showToast("Save succeed\n name: " + adfName + "\n uuid: " + adfUuid);
        mSaveADFTask = null;
        mTango.disconnect();
        mIsConnected.set(false);
    }

    private void showSetAdfNameDialog() {
        Bundle bundle = new Bundle();
        bundle.putString(TangoAreaDescriptionMetaData.KEY_NAME, "New ADF");
        bundle.putString(TangoAreaDescriptionMetaData.KEY_UUID, "");

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        SetAdfNameDialog setAdfNameDialog = new SetAdfNameDialog();
        setAdfNameDialog.setArguments(bundle);
        setAdfNameDialog.show(ft, "ADFNameDialog");
    }

    private boolean checkPermission(String permission, int request_code) {
        int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission},
                    request_code);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_WRITE_EXTERNAL:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mStoragePermissionGranted = true;
                }
                break;
            case REQUEST_CODE_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mCameraPermissionGranted = true;
                }
                break;
            case REQUEST_CODE_ACCESS_WIFI:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mAccessWifiPermissionGranted = true;
                }
                break;
            case REQUEST_CODE_CHANGE_WIFI:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mChangeWifiPermissionGranted = true;
                }
                break;
            case REQUEST_CODE_COARSE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mCoarseLocationPermissionGranted = true;
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_AREA_LEARNING) {
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Area learning permission required.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
