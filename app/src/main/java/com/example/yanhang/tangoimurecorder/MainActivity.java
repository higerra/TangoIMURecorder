package com.example.yanhang.tangoimurecorder;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.PeriodicSync;
import android.content.pm.PackageManager;
import android.hardware.Camera;

import android.hardware.SensorEventListener;
import android.hardware.display.DisplayManager;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.BoolRes;
import android.support.v4.app.ActivityCompat;
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
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.net.wifi.WifiManager;

import com.google.atap.tango.ux.TangoUxLayout;
import com.google.atap.tango.ux.UxExceptionEvent;
import com.google.atap.tango.ux.UxExceptionEventListener;
import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.Tango.OnTangoUpdateListener;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
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


public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private static final String LOG_TAG = MainActivity.class.getName();
    private static final int REQUEST_CODE_WRITE_EXTERNAL = 1001;
    private static final int REQUEST_CODE_CAMERA = 1002;
    private static final int REQUEST_CODE_ACCESS_WIFI = 1003;
    private static final int REQUEST_CODE_CHANGE_WIFI = 1004;
    private static final int REQUEST_CODE_COARSE_LOCATION = 1005;

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

    static final int ROTATION_SENSOR = Sensor.TYPE_GAME_ROTATION_VECTOR;

    private Button mStartStopButton;
    private ToggleButton mToggleALButton;

//    private GLSurfaceView mVideoSurfaceView;
//    private TangoVideoRenderer mVideoRenderer;

    private int mCameraToDisplayRotation = 0;

    private boolean mIsRecordingPose = true;
    private boolean mIsWriteFile = true;
    private boolean mIsWifiEnabled = true;

    private AtomicBoolean mIsConnected = new AtomicBoolean(false);
    private AtomicBoolean mIsRecording = new AtomicBoolean(false);
    private boolean mStoragePermissionGranted = false;
    private boolean mCameraPermissionGranted = false;
    private boolean mAccessWifiPermissionGranted = false;
    private boolean mChangeWifiPermissionGranted = false;
    private boolean mCoarseLocationPermissionGranted = false;

    private AtomicBoolean mIsFrameAvailableTangoThread = new AtomicBoolean(false);
    private int mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTangoUx = setupTangoUx();
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
        mStartStopButton = (Button)findViewById(R.id.button_start_stop);

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
            case R.id.menu_toggle_pose:
                mIsRecordingPose = !mIsRecordingPose;
                item.setChecked(mIsRecordingPose);
                if(mIsRecordingPose){
                    showToast("Pose enabled");
                }else{
                    showToast("Pose disabled");
                }
                break;
            case R.id.menu_toggle_file:
                mIsWriteFile = !mIsWriteFile;
                item.setChecked(mIsWriteFile);
                if(mIsWriteFile){
                    showToast("File enabled");
                }else{
                    showToast("File disabled");
                }
                break;
            case R.id.menu_toggle_wifi:
                mIsWifiEnabled = !mIsWifiEnabled;
                item.setChecked(mIsWifiEnabled);
                if(mIsWifiEnabled){
                    showToast("Wifi enabled");
                }else{
                    showToast("Wifi disabled");
                }
                break;
            case R.id.menu_adl:
                Intent intent = new Intent(this, ADLActivity.class);
                startActivity(intent);
        }
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        //getMenuInflater().inflate(R.menu.option_menu, menu);
        if(mIsRecording.get()){
            menu.getItem(0).setEnabled(false);
            menu.getItem(1).setEnabled(false);
            menu.getItem(2).setEnabled(false);
        }else{
            menu.getItem(0).setEnabled(true);
            menu.getItem(1).setEnabled(true);
            menu.getItem(2).setEnabled(true);
        }
        return true;
    }

    public void onToggleALClicked(View view){

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
        if(mIsWifiEnabled && (!mAccessWifiPermissionGranted || !mChangeWifiPermissionGranted)){
            showAlertAndStop("Wifi permission not granted");
            return;
        }
        if(mIsWifiEnabled && !mCoarseLocationPermissionGranted){
            showAlertAndStop("Location permission not granted");
            return;
        }

        // initialize Wifi
        if(mIsWifiEnabled){
            if(!mWifiMangerRef.isWifiEnabled()){
                showAlertAndStop("Turn on wifi first");
            }
            wifi_scanner_.reset();
            wifi_scanner_.start();
        }

        if(mIsRecordingPose) {
            mTangoUx.start(new StartParams());
            // initialize tango service
            mTango = new Tango(MainActivity.this, new Runnable() {
                @Override
                public void run() {
                    synchronized (MainActivity.this) {
                        try {
                            TangoSupport.initialize();
                            mTangoConfig = setupTangoConfig(mTango);
                            mTango.connect(mTangoConfig);
                            startupTango();
                            mIsConnected.set(true);
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
        }
        // initialize recorder
        if(mIsWriteFile) {
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
        mIsRecording.set(true);
    }

    private void stopRecording(){
        mIsRecording.set(false);
        if(mRecorder != null) {
            mRecorder.endFiles();
        }

        if(mIsWifiEnabled) {
            wifi_scanner_.terminate();
            if(mIsWriteFile) {
                wifi_scanner_.saveResultToFile(mRecorder.getOutputDir() + "/wifi.txt");
            }
        }

        if (mIsRecordingPose) {
            synchronized (this) {
                try {
//                    mTango.disconnectCamera(TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
//                    mConnectedTextureIdGlThread = INVALID_TEXTURE_ID;
                    mTangoUx.stop();
                    mTango.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mIsConnected.set(false);
        }

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

    private TangoConfig setupTangoConfig(Tango tango){
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_MOTIONTRACKING, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_HIGH_RATE_POSE, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);
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
                        TangoPoseData lastFramePose = TangoSupport.getPoseAtTime(0,
                                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                                TangoPoseData.COORDINATE_FRAME_DEVICE,
                                TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL, mCameraToDisplayRotation);
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
        final ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<>();
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE
        ));

        mTango.connectListener(framePairs, new OnTangoUpdateListener() {
            @Override
            public void onPoseAvailable(TangoPoseData tangoPoseData) {
                if(mTangoUx != null){
                    mTangoUx.updatePoseStatus(tangoPoseData.statusCode);
                }
                if(mIsRecording.get() && mIsWriteFile) {
//                    final double nano_to_second = 1e09;
//                    long timestamp = (long)(tangoPoseData.timestamp * nano_to_second);
//                    float[] translation = tangoPoseData.getTranslationAsFloats();
//                    float[] orientation = tangoPoseData.getRotationAsFloats();
                    mRecorder.addPoseRecord(tangoPoseData);
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
        }
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
}
