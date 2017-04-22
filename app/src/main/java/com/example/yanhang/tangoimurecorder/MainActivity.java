package com.example.yanhang.tangoimurecorder;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;

import android.hardware.SensorEventListener;
import android.hardware.display.DisplayManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

import com.google.atap.tango.ux.TangoUxLayout;
import com.google.atap.tango.ux.UxExceptionEvent;
import com.google.atap.tango.ux.UxExceptionEventListener;
import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.Tango.OnTangoUpdateListener;
import com.google.atap.tangoservice.TangoAreaDescriptionMetaData;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoXyzIjData;

import com.google.atap.tango.ux.TangoUx;
import com.google.atap.tango.ux.TangoUx.StartParams;

import com.projecttango.tangosupport.TangoSupport;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.rajawali3d.surface.RajawaliSurfaceView;
import org.rajawali3d.scene.ASceneFrameCallback;


public class MainActivity extends AppCompatActivity
        implements SensorEventListener, SetAdfNameDialog.CallbackListener, SaveAdfTask.SaveAdfListener{

    private static final String LOG_TAG = MainActivity.class.getName();
    public static final String INTENT_EXTRA_CONFIG = "config";

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

    private UxExceptionEventListener mUxExceptionEventListener = new UxExceptionEventListener() {
        @Override
        public void onUxExceptionEvent(UxExceptionEvent uxExceptionEvent) {
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_LYING_ON_SURFACE) {
                Log.i(LOG_TAG, "Device lying on surface ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FEW_DEPTH_POINTS) {
                Log.i(LOG_TAG, "Very few depth points in mPoint cloud ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FEW_FEATURES) {
                Log.i(LOG_TAG, "Invalid poses in MotionTracking ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_INCOMPATIBLE_VM) {
                Log.i(LOG_TAG, "Device not running on ART");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_MOTION_TRACK_INVALID) {
                Log.i(LOG_TAG, "Invalid poses in MotionTracking ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_MOVING_TOO_FAST) {
                Log.i(LOG_TAG, "Invalid poses in MotionTracking ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FISHEYE_CAMERA_OVER_EXPOSED) {
                Log.i(LOG_TAG, "Fisheye Camera Over Exposed");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_FISHEYE_CAMERA_UNDER_EXPOSED) {
                Log.i(LOG_TAG, "Fisheye Camera Under Exposed ");
            }
            if (uxExceptionEvent.getType() == UxExceptionEvent.TYPE_TANGO_SERVICE_NOT_RESPONDING) {
                Log.i(LOG_TAG, "TangoService is not responding ");
            }
        }
    };

    private PoseIMURecorder mRecorder;
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
    // Gravity
    private TextView mLabelGx;
    private TextView mLabelGy;
    private TextView mLabelGz;
    // Orientation
    private TextView mLabelOw;
    private TextView mLabelOx;
    private TextView mLabelOy;
    private TextView mLabelOz;
    // Magnetometer
    private TextView mLabelMx;
    private TextView mLabelMy;
    private TextView mLabelMz;

    private TextView mLabelScanTimes;
    private TextView mLabelWifiNums;

    private TextView mLabelStepCount;
    private TextView mLabelInfo;

    static final int ROTATION_SENSOR = Sensor.TYPE_GAME_ROTATION_VECTOR;

    private Button mStartStopButton;
    private Button mScanButton;

//    private GLSurfaceView mVideoSurfaceView;
//    private TangoVideoRenderer mVideoRenderer;

    private int mCameraToDisplayRotation = 0;

    TangoIMUConfig mConfig = new TangoIMUConfig();

    private AtomicBoolean mIsConnected = new AtomicBoolean(false);
    private AtomicBoolean mIsTangoInitialized = new AtomicBoolean(false);
    private AtomicBoolean mIsRecording = new AtomicBoolean(false);
    private AtomicBoolean mIsLocalizedToADF = new AtomicBoolean(false);

    private boolean mStoragePermissionGranted = false;
    private boolean mCameraPermissionGranted = false;
    private boolean mAccessWifiPermissionGranted = false;
    private boolean mChangeWifiPermissionGranted = false;
    private boolean mCoarseLocationPermissionGranted = false;

    private AtomicBoolean mIsFrameAvailableTangoThread = new AtomicBoolean(false);
    private int mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;

    SaveAdfTask mSaveADFTask = null;

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
                        TangoSupport.initialize();
                        mIsTangoInitialized.set(true);
                    }catch(TangoOutOfDateException e){
                        Log.e(LOG_TAG, "Out of date");
                    }catch(TangoErrorException e){
                        Log.e(LOG_TAG, "Tango error");
                    }catch(TangoInvalidException e){
                        Log.e(LOG_TAG,"Tango exception");
                    }
                }
            }
        });
        mSurfaceView = (RajawaliSurfaceView) findViewById(R.id.gl_surface_view);
        mRenderer = new MotionRajawaliRenderer(this);

//        mVideoSurfaceView = (GLSurfaceView) findViewById(R.id.video_surface_view);
//        mVideoSurfaceView.setZOrderOnTop(true);

        setupRenderer();

        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if(displayManager != null){
            displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {

                }

                @Override
                public void onDisplayRemoved(int displayId) {
                    synchronized (this){
                        setAndroidOrientation();
                    }
                }

                @Override
                public void onDisplayChanged(int displayId) {

                }
            }, null);
        }

        // initialize IMU sensor
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mLinearAcce = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        //mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mOrientation = mSensorManager.getDefaultSensor(ROTATION_SENSOR);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mStepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        // initialize UI widgets
        mLabelRx = (TextView)findViewById(R.id.label_rx);
        mLabelRy = (TextView)findViewById(R.id.label_ry);
        mLabelRz = (TextView)findViewById(R.id.label_rz);
        mLabelAx = (TextView)findViewById(R.id.label_ax);
        mLabelAy = (TextView)findViewById(R.id.label_ay);
        mLabelAz = (TextView)findViewById(R.id.label_az);
        mLabelLx = (TextView)findViewById(R.id.label_lx);
        mLabelLy = (TextView)findViewById(R.id.label_ly);
        mLabelLz = (TextView)findViewById(R.id.label_lz);
        mLabelGx = (TextView)findViewById(R.id.label_gx);
        mLabelGy = (TextView)findViewById(R.id.label_gy);
        mLabelGz = (TextView)findViewById(R.id.label_gz);
//        mLabelOw = (TextView)findViewById(R.id.label_ow);
//        mLabelOx = (TextView)findViewById(R.id.label_ox);
//        mLabelOy = (TextView)findViewById(R.id.label_oy);
//        mLabelOz = (TextView)findViewById(R.id.label_oz);
        mLabelMx = (TextView)findViewById(R.id.label_mx);
        mLabelMy = (TextView)findViewById(R.id.label_my);
        mLabelMz = (TextView)findViewById(R.id.label_mz);

        mLabelScanTimes = (TextView)findViewById(R.id.label_wifi_record_num);
        mLabelWifiNums = (TextView)findViewById(R.id.label_wifi_beacon_num);

        mLabelStepCount = (TextView)findViewById(R.id.label_step_count);
        mLabelInfo = (TextView)findViewById(R.id.label_info);

        mStartStopButton = (Button)findViewById(R.id.button_start_stop);
        mScanButton = (Button)findViewById(R.id.button_scan);
        mScanButton.setVisibility(View.GONE);

        Runnable wifi_callback = new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLabelScanTimes.setText(String.valueOf(wifi_scanner_.getRecordCount()));
                        mLabelWifiNums.setText(String.valueOf(wifi_scanner_.getLatestScanResult().size()));
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
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.menu_setting:
                if(mIsRecording.get()){
                    break;
                }
                Intent intent = new Intent(this, SettingActivity.class);
                intent.putExtra(INTENT_EXTRA_CONFIG, mConfig);
                startActivityForResult(intent, RESULT_CODE_PICK_ADF);
                break;
        }
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        //getMenuInflater().inflate(R.menu.option_menu, menu);
        if(mIsRecording.get()){
            menu.getItem(0).setEnabled(false);
        }else {
            menu.getItem(0).setEnabled(true);
        }
        return true;
    }

    @Override
    protected void onPause(){
        super.onPause();
        if(mIsRecording.get()) {
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
    protected void onResume(){
        super.onResume();
        mStoragePermissionGranted = checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_CODE_WRITE_EXTERNAL);
        mCameraPermissionGranted = checkPermission(Manifest.permission.CAMERA, REQUEST_CODE_CAMERA);
        mAccessWifiPermissionGranted = checkPermission(Manifest.permission.ACCESS_WIFI_STATE, REQUEST_CODE_ACCESS_WIFI);
        mChangeWifiPermissionGranted = checkPermission(Manifest.permission.CHANGE_WIFI_STATE, REQUEST_CODE_CHANGE_WIFI);
        mCoarseLocationPermissionGranted = checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, REQUEST_CODE_COARSE_LOCATION);


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

    private void showAlertAndStop(final String text){
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

    private void showToast(final String text){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void startNewRecording(){
        if(!mStoragePermissionGranted){
            showAlertAndStop("Storage permission not granted");
            return;
        }
        if(!mCameraPermissionGranted){
            showAlertAndStop("Camera permission not granted");
            return;
        }
        if(mConfig.getWifiEnabled() && (!mAccessWifiPermissionGranted || !mChangeWifiPermissionGranted)){
            showAlertAndStop("Wifi permission not granted");
            return;
        }
        if(mConfig.getWifiEnabled() && !mCoarseLocationPermissionGranted){
            showAlertAndStop("Location permission not granted");
            return;
        }

        // initialize Wifi
        if(mConfig.getWifiEnabled()){
            if(!mWifiMangerRef.isWifiEnabled()){
                showAlertAndStop("Turn on wifi first");
            }
            wifi_scanner_.reset();
            wifi_scanner_.start();
        }

        if(mConfig.getPoseEnabled()) {
            if(!mIsTangoInitialized.get()){
                showAlertAndStop("Tango not initialized");
            }
            // initialize tango service
            synchronized (this) {
                TangoConfig config = setupTangoConfig(mTango);
                mTangoUx.start(new StartParams());
                mTango.connect(config);
                startupTango();
                mIsConnected.set(true);
            }
        }
        // initialize recorder
        if(mConfig.getFileEnabled()) {
            try {
                String output_dir = setupOutputFolder();
                mRecorder = new PoseIMURecorder(output_dir, this);
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
        mLabelInfo.setText("Not localized");
        mIsRecording.set(true);
    }

    private void stopRecording(){
        mIsRecording.set(false);
        if(mRecorder != null) {
            mRecorder.endFiles();
        }

        if(mConfig.getWifiEnabled()) {
            wifi_scanner_.terminate();
            if(mConfig.getFileEnabled()) {
                wifi_scanner_.saveResultToFile(mRecorder.getOutputDir() + "/wifi.txt");
            }
        }

        if (mConfig.getPoseEnabled()) {
            synchronized (this) {
                try {
//                    mTango.disconnectCamera(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
//                    mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
                    mTangoUx.stop();
                    if(mConfig.getAreaLearningMode()){
                        showSetAdfNameDialog();
                    }else{
                        mTango.disconnect();
                        mIsConnected.set(false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        mLabelInfo.setText("Stopped");
        showToast("Stopped");
    }

    public void startStopRecording(View view){
        if(!mIsRecording.get()){
            startNewRecording();
            mStartStopButton.setText(R.string.stop_title);
        }else{
            stopRecording();
            mStartStopButton.setText(R.string.start_title);
        }
    }

    public void scanWifi(View view){

    }

    private TangoConfig setupTangoConfig(Tango tango){
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_MOTIONTRACKING, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_HIGH_RATE_POSE, true);
        if(mConfig.getAreaLearningMode()){
            config.putBoolean(TangoConfig.KEY_BOOLEAN_LEARNINGMODE, true);
        }
        if(mConfig.getADFEnabled() && mConfig.getADFUuid() != null){
            try {
                config.putString(TangoConfig.KEY_STRING_AREADESCRIPTION, mConfig.getADFUuid());
                Log.i(LOG_TAG, mConfig.getADFUuid() + " loaded");
            }catch (TangoErrorException e){
                Log.e(LOG_TAG, e.getMessage());
            }
        }
        return config;
    }

    private TangoUx setupTangoUx(){
        TangoUx tangoUx = new TangoUx(this);
        tangoUx.setUxExceptionEventListener(mUxExceptionEventListener);
        TangoUxLayout uxLayout = (TangoUxLayout) findViewById(R.id.layout_tango);
        tangoUx.setLayout(uxLayout);
        return tangoUx;
    }

    private String setupOutputFolder() throws FileNotFoundException{
        Calendar current_time = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddhhmmss");
        File external_dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File output_dir = new File(external_dir.getAbsolutePath() + "/" + formatter.format(current_time.getTime()));
        if(!output_dir.exists()) {
            if (!output_dir.mkdir()) {
                Log.e(LOG_TAG, "Can not create output directory");
                throw new FileNotFoundException();
            }
        }
        Log.i(LOG_TAG, "Output directory: " + output_dir.getAbsolutePath());
        return output_dir.getAbsolutePath();
    }

    private void setupRenderer(){
        // motion renderer
        mSurfaceView.setEGLContextClientVersion(2);
        mRenderer.getCurrentScene().registerFrameCallback(new ASceneFrameCallback() {
            @Override
            public void onPreFrame(long sceneTime, double deltaTime) {
                synchronized (MainActivity.this){
                    // Don't execute any tango API actions if we're not connected to the service
                    if (!mIsConnected.get()){
                        return;
                    }

                    // Update current camera pose
                    try{
                        TangoPoseData lastFramePose;
                        if(mConfig.getADFEnabled()){
                            lastFramePose =TangoSupport.getPoseAtTime(0,
                                    TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                                    TangoPoseData.COORDINATE_FRAME_DEVICE,
                                    TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL, mCameraToDisplayRotation);
                        }else{
                            lastFramePose =TangoSupport.getPoseAtTime(0,
                                    TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                                    TangoPoseData.COORDINATE_FRAME_DEVICE,
                                    TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL, mCameraToDisplayRotation);
                        }
                        mRenderer.updateCameraPose(lastFramePose);
                    }catch (TangoErrorException e){
                        Log.e(LOG_TAG, "Could not get valid transform");
                    }
                }
            }

            @Override
            public boolean callPreFrame(){
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

//        // camera renderer
//        mVideoSurfaceView.setEGLContextClientVersion(2);
//        mVideoRenderer = new TangoVideoRenderer(new TangoVideoRenderer.RenderCallback() {
//
//            @Override
//            public void preRender() {
//                if(!mIsConnected.get()){
//                    return;
//                }
//
//                try{
//                    synchronized (MainActivity.this) {
//                        if (mConnectedTextureIdGlThread == INVALID_TEXTURE_ID) {
//                            mConnectedTextureIdGlThread = mVideoRenderer.getTextureId();
//                            mTango.connectTextureId(mRenderedTexture, mVideoRenderer.getTextureId());
//                        }
//
//                        if (mIsFrameAvailableTangoThread.compareAndSet(true, false)) {
//                            mTango.updateTexture(mRenderedTexture);
//                        }
//                    }
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//            }
//        });
//        mVideoSurfaceView.setRenderer(mVideoRenderer);

    }

    private void resetUI(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLabelWifiNums.setText("N/A");
                mLabelScanTimes.setText("N/A");
                mLabelStepCount.setText("N/A");
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
        mRenderer.onTouchEvent(motionEvent);
        return true;
    }

    private void setAndroidOrientation(){
        Display display = getWindowManager().getDefaultDisplay();
        Camera.CameraInfo depthCameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(1, depthCameraInfo);

        int depthCameraRotation = Surface.ROTATION_0;
        switch(depthCameraInfo.orientation){
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
        if(mCameraToDisplayRotation < 0){
            mCameraToDisplayRotation += 4;
        }
    }

    private void startupTango(){
        ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<>();
        if(mConfig.getADFEnabled()){
            framePairs.add(new TangoCoordinateFramePair(
                    TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                    TangoPoseData.COORDINATE_FRAME_DEVICE
            ));
            framePairs.add(new TangoCoordinateFramePair(
                    TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                    TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE
            ));
        }else {
            framePairs.add(new TangoCoordinateFramePair(
                    TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                    TangoPoseData.COORDINATE_FRAME_DEVICE
            ));
        }

        mIsLocalizedToADF.set(false);
        mTango.connectListener(framePairs, new OnTangoUpdateListener() {
            @Override
            public void onPoseAvailable(TangoPoseData tangoPoseData) {
                if(mTangoUx != null){
                    mTangoUx.updatePoseStatus(tangoPoseData.statusCode);
                }

                if(mConfig.getADFEnabled()){
                    if(tangoPoseData.baseFrame == TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
                            && tangoPoseData.targetFrame == TangoPoseData.COORDINATE_FRAME_DEVICE
                            && tangoPoseData.statusCode == TangoPoseData.POSE_VALID){
                        if(mIsRecording.get() && mConfig.getFileEnabled()) {
                            mRecorder.addPoseRecord(tangoPoseData);
                        }
                    }
                    if(tangoPoseData.baseFrame == TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
                            && tangoPoseData.targetFrame == TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE
                            && tangoPoseData.statusCode == TangoPoseData.POSE_VALID){
                        if(!mIsLocalizedToADF.get()) {
                            showToast("Localized to ADF");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mLabelInfo.setText("Localized to " + mConfig.getADFName());
                                }
                            });
                            mIsLocalizedToADF.set(true);
                        }

                    }
                }else{
                    if(tangoPoseData.baseFrame == TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE
                            && tangoPoseData.targetFrame == TangoPoseData.COORDINATE_FRAME_DEVICE
                            && tangoPoseData.statusCode == TangoPoseData.POSE_VALID){
                        if(mIsRecording.get() && mConfig.getFileEnabled()) {
                            mRecorder.addPoseRecord(tangoPoseData);
                        }
                    }
                }
            }

            @Override
            public void onFrameAvailable(int cameraID){
//                Log.i(LOG_TAG, "onFrameAvailable called");
//                if(cameraID == mRenderedTexture){
//                    if(mVideoSurfaceView.getRenderMode() != GLSurfaceView.RENDERMODE_WHEN_DIRTY){
//                        mVideoSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
//                    }
//
//                    mIsFrameAvailableTangoThread.set(true);
//                    mVideoSurfaceView.requestRender();
//                }
            }

            @Override
            public void onXyzIjAvailable(TangoXyzIjData xyzIj){

            }

            @Override
            public void onPointCloudAvailable(final TangoPointCloudData pointCloudData){

            }


            @Override
            public void onTangoEvent(TangoEvent tangoEvent) {
                if(mTangoUx != null){
                    mTangoUx.updateTangoEvent(tangoEvent);
                }
            }
        });
    }

    // receive IMU data
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){

    }

    @Override
    public void onSensorChanged(final SensorEvent event){
        long timestamp = event.timestamp;
        float[] values = {0.0f, 0.0f, 0.0f, 0.0f};
        final Boolean mIsWriteFile = mConfig.getFileEnabled();
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLabelAx.setText(String.format(Locale.US, "%.6f", event.values[0]));
                    mLabelAy.setText(String.format(Locale.US, "%.6f", event.values[1]));
                    mLabelAz.setText(String.format(Locale.US, "%.6f", event.values[2]));
                }
            });
            if(mIsRecording.get() && mIsWriteFile){
                System.arraycopy(event.values, 0, values, 0, 3);
                mRecorder.addIMURecord(timestamp, values, PoseIMURecorder.ACCELEROMETER);
            }
        }else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLabelRx.setText(String.format(Locale.US, "%.6f", event.values[0]));
                    mLabelRy.setText(String.format(Locale.US, "%.6f", event.values[1]));
                    mLabelRz.setText(String.format(Locale.US, "%.6f", event.values[2]));
                }
            });
            if(mIsRecording.get() && mIsWriteFile){
                System.arraycopy(event.values, 0, values, 0, 3);
                mRecorder.addIMURecord(timestamp, values, PoseIMURecorder.GYROSCOPE);
            }
        }
        else if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLabelLx.setText(String.format(Locale.US, "%.6f", event.values[0]));
                    mLabelLy.setText(String.format(Locale.US, "%.6f", event.values[1]));
                    mLabelLz.setText(String.format(Locale.US, "%.6f", event.values[2]));
                }
            });
            if(mIsRecording.get() && mIsWriteFile){
                System.arraycopy(event.values, 0, values, 0, 3);
                mRecorder.addIMURecord(timestamp, values, PoseIMURecorder.LINEAR_ACCELERATION);
            }
        }else if(event.sensor.getType() == Sensor.TYPE_GRAVITY){
//            mUIHandler.post(new Runnable() {
//                @Override
//                public void run() {
//                    mLabelGx.setText(String.format(Locale.US, "%.6f", event.values[0]));
//                    mLabelGy.setText(String.format(Locale.US, "%.6f", event.values[1]));
//                    mLabelGz.setText(String.format(Locale.US, "%.6f", event.values[2]));
//                }
//            });
            if(mIsRecording.get() && mIsWriteFile){
                System.arraycopy(event.values, 0, values, 0, 3);
                mRecorder.addIMURecord(timestamp, values, PoseIMURecorder.GRAVITY);
            }
        }else if(event.sensor.getType() == ROTATION_SENSOR){
//            mUIHandler.post(new Runnable() {
//                @Override
//                public void run() {
//                    mLabelOw.setText(String.format(Locale.US, "%.6f", event.values[3]));
//                    mLabelOx.setText(String.format(Locale.US, "%.6f", event.values[0]));
//                    mLabelOy.setText(String.format(Locale.US, "%.6f", event.values[1]));
//                    mLabelOz.setText(String.format(Locale.US, "%.6f", event.values[2]));
//                }
//            });
            if(mIsRecording.get() && mIsWriteFile){
                System.arraycopy(event.values, 0, values, 0, 4);
                mRecorder.addIMURecord(timestamp, values, PoseIMURecorder.ROTATION_VECTOR);
            }
        }else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
//            mUIHandler.post(new Runnable() {
//                @Override
//                public void run() {
//                    mLabelMx.setText(String.format(Locale.US, "%.6f", event.values[0]));
//                    mLabelMy.setText(String.format(Locale.US, "%.6f", event.values[1]));
//                    mLabelMz.setText(String.format(Locale.US, "%.6f", event.values[2]));
//                }
//            });
            if(mIsRecording.get() && mIsWriteFile){
                System.arraycopy(event.values, 0, values, 0, 3);
                mRecorder.addIMURecord(timestamp, values, PoseIMURecorder.MAGNETOMETER);
            }
        }else if(event.sensor.getType() == Sensor.TYPE_STEP_COUNTER){
            if(mIsRecording.get()) {
                if (mInitialStepCount < 0) {
                    mInitialStepCount = event.values[0];
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
    public void onAdfNameOk(String name, String uuid){
        showToast("Saving...");
        mSaveADFTask = new SaveAdfTask(this, this, mTango, name);
        mSaveADFTask.execute();
    }

    @Override
    public void onAdfNameCancelled(){
        showToast("Canceled!");
    }

    @Override
    public void onSaveAdfFailed(String adfName){
        showToast("Save failed");
        mSaveADFTask = null;
        mTango.disconnect();
        mIsConnected.set(false);
    }

    @Override
    public void onSaveAdfSuccess(String adfName, String adfUuid){
        showToast("Save succeed\n name: "+adfName + "\n uuid: " + adfUuid);
        mSaveADFTask = null;
        mTango.disconnect();
        mIsConnected.set(false);
    }

    private void showSetAdfNameDialog(){
        Bundle bundle = new Bundle();
        bundle.putString(TangoAreaDescriptionMetaData.KEY_NAME, "New ADF");
        bundle.putString(TangoAreaDescriptionMetaData.KEY_UUID, "");

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        SetAdfNameDialog setAdfNameDialog = new SetAdfNameDialog();
        setAdfNameDialog.setArguments(bundle);
        setAdfNameDialog.show(ft, "ADFNameDialog");
    }

    private boolean checkPermission(String permission, int request_code){
        int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
        if(permissionCheck != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{permission},
                    request_code);
            return false;
        }else{
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        switch (requestCode){
            case REQUEST_CODE_WRITE_EXTERNAL:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mStoragePermissionGranted = true;
                }
                break;
            case REQUEST_CODE_CAMERA:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mCameraPermissionGranted = true;
                }
                break;
            case REQUEST_CODE_ACCESS_WIFI:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mAccessWifiPermissionGranted = true;
                }
                break;
            case REQUEST_CODE_CHANGE_WIFI:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mChangeWifiPermissionGranted = true;
                }
                break;
            case REQUEST_CODE_COARSE_LOCATION:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mCoarseLocationPermissionGranted = true;
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == REQUEST_CODE_AREA_LEARNING){
            if(resultCode == RESULT_CANCELED){
                Toast.makeText(this, "Area learning permission required.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }else if(requestCode == RESULT_CODE_PICK_ADF){
            if(resultCode == RESULT_OK && data != null){
                mConfig = (TangoIMUConfig)data.getSerializableExtra(INTENT_EXTRA_CONFIG);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(mConfig.getAreaLearningMode()){
                            showToast("Learning mode on");
                            mLabelInfo.setText("Learning mode");
                        }
                        if(mConfig.getWifiEnabled() && !mConfig.getContinuesWifiScan()){
                            mScanButton.setVisibility(View.VISIBLE);
                        }else{
                            mScanButton.setVisibility(View.GONE);
                        }
                    }
                });
            }else{
                showToast("resultCode: RESULT_CANCEL");
            }
        }
    }
}
