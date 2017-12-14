package com.example.yanhang.tangoimurecorder;

import android.hardware.camera2.params.BlackLevelPattern;

/**
 * Created by yanhang on 4/21/17.
 */

public class TangoIMUConfig implements java.io.Serializable {
    private Boolean mIsPoseEnabled = true;
    private Boolean mIsFileEnabled = true;
    private String mFolderPrefix = "";
    private Boolean mIsWifiEnabled = true;
    private Boolean mIsContinuesWifiScan = false;
    private int mWifiScanInterval = 1;
    private Boolean mIsADFEnabled = false;
    private Boolean mIsAreaLearningMode = false;
    private int mNumRequestsPerScan = 1;


    private String mADFuuid = "";
    private String mADFName = "";

    // getters and setters
    public Boolean getPoseEnabled(){
        return this.mIsPoseEnabled;
    }

    public Boolean getFileEnabled(){
        return this.mIsFileEnabled;
    }

    public Boolean getWifiEnabled(){
        return this.mIsWifiEnabled;
    }

    public Boolean getContinuesWifiScan(){
        return this.mIsContinuesWifiScan;
    }

    public int getWifiScanInterval() { return this.mWifiScanInterval; }
    public Boolean getADFEnabled(){
        return this.mIsADFEnabled;
    }

    public Boolean getAreaLearningMode(){
        return this.mIsAreaLearningMode;
    }

    public String getADFName(){
        return this.mADFName;
    }

    public String getADFUuid(){
        return this.mADFuuid;
    }
    public String getFolderPrefix() { return this.mFolderPrefix;}

    public int getNumRequestsPerScan(){
        return this.mNumRequestsPerScan;
    }

    public void setPoseEnabled(Boolean v){
        this.mIsPoseEnabled = v;
    }

    public void setFileEnabled(Boolean v){
        this.mIsFileEnabled = v;
    }

    public void setWifiEnabled(Boolean v){
        this.mIsWifiEnabled = v;
    }

    public void setContinuesWifiScan(Boolean v){
        this.mIsContinuesWifiScan = v;
    }

    public void setWifiScanInterval(int v) { this.mWifiScanInterval = v; }

    public void setADFEnabled(Boolean v) {
        this.mIsADFEnabled = v;
    }

    public void setAreaLearningMode(Boolean v){
        this.mIsAreaLearningMode = v;
    }

    public void setADFName(String v){
        this.mADFName = v;
    }

    public void setADFUuid(String v){
        this.mADFuuid = v;
    }

    public void setNumRequestsPerScan(int v){
        this.mNumRequestsPerScan = v;
    }

    public void setFolderPrefix(String v){this.mFolderPrefix = v;}
}
