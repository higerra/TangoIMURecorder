package com.example.yanhang.tangoimurecorder;

/**
 * Created by yanhang on 4/15/17.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Calendar;

import java.io.BufferedWriter;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

public class PeriodicScan implements Runnable{
    static final String LOG_TAG = "PerodicScan";
    static final int DEFAULT_INTERVAL = 1000;
    static final int DEFAULT_REDUNDANCY = 1;

    private int scan_interval_ = DEFAULT_INTERVAL;
    private int redundancy_ = DEFAULT_REDUNDANCY;

    private final MainActivity parent_;
    private final Runnable receive_callback_;
    private Handler handler_ = new Handler();
    private AtomicBoolean is_running_ = new AtomicBoolean(false);
    private AtomicInteger redundant_counter_ = new AtomicInteger(0);

    private WifiManager wifi_manager_;
    ArrayList<ArrayList<String> > scan_results_ = new ArrayList<>();
    BroadcastReceiver scan_receiver_ = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(redundant_counter_.get() > 0){
                redundant_counter_.set(redundant_counter_.get() - 1);
                if(redundant_counter_.get() > 0) {
                    wifi_manager_.startScan();
                }
            }else {
                if (!is_running_.get()) {
                    return;
                }
            }
            List<ScanResult> results = wifi_manager_.getScanResults();
            ArrayList<String> current_record = new ArrayList<>();
            for(ScanResult res: results){
                String str = String.valueOf(res.timestamp) + '\t' + res.BSSID + '\t' + String.valueOf(res.level);
                current_record.add(str);
            }
            scan_results_.add(current_record);
            if (receive_callback_ != null) {
                parent_.runOnUiThread(receive_callback_);
            }
        }
    };

    PeriodicScan(@NonNull MainActivity parent, Runnable receive_callback, int interval, int redun){
        this.parent_ = parent;
        this.receive_callback_ = receive_callback;
        this.scan_interval_ = interval;
        this.redundancy_ = redun;
        wifi_manager_ = (WifiManager)parent.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    PeriodicScan(@NonNull MainActivity parent, Runnable receive_callback, int interval){
        this(parent, receive_callback, interval, DEFAULT_REDUNDANCY);
    }

    PeriodicScan(@NonNull MainActivity parent, Runnable receive_callback){
        this(parent, receive_callback, DEFAULT_INTERVAL, DEFAULT_REDUNDANCY);
    }

    PeriodicScan(@NonNull MainActivity parent){
        this(parent, null, DEFAULT_INTERVAL, DEFAULT_INTERVAL);
    }

    BroadcastReceiver getBroadcastReceiver(){
        return scan_receiver_;
    }

    WifiManager getWifiManager(){
        return wifi_manager_;
    }

    public void setRedundancy(int v){
        redundancy_ = v;
    }

    public void setScanInterval(int new_interval){
        scan_interval_ = new_interval;
    }

    public int getRecordCount(){
        synchronized (this) {
            return scan_results_.size();
        }
    }

    public ArrayList<String> getLatestScanResult(){
        if(scan_results_.isEmpty()){
            return new ArrayList<>();
        }
        return scan_results_.get(scan_results_.size() - 1);
    }

    public void saveResultToFile(String path){
        if(isRunning()) {
            terminate();
        }
        synchronized (this) {
            Calendar file_timestamp = Calendar.getInstance();
            String header = "# Created at " + file_timestamp.getTime().toString() + "\n";
            // create file
            try {
                File file = new File(path);
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                Intent scan_intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                scan_intent.setData(Uri.fromFile(file));
                parent_.sendBroadcast(scan_intent);
                writer.write(header);
                writer.write(String.valueOf(redundancy_) + '\n');
                writer.write(String.valueOf(scan_results_.size()) + '\n');
                for (ArrayList<String> record : scan_results_) {
                    writer.write(String.valueOf(record.size()) + '\n');
                    for (String v : record) {
                        writer.write(v + '\n');
                    }
                }

                writer.flush();
                writer.close();
                Log.i(LOG_TAG, "File written to " + path);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void reset(){
        synchronized (this) {
            terminate();
            scan_results_.clear();
        }
    }

    public void terminate(){
        handler_.removeCallbacks(null);
        is_running_.set(false);
        redundant_counter_.set(0);
    }

    public void start(){
        is_running_.set(true);
        redundant_counter_.set(0);
        run();
    }

    public void singleScan(){
        if(!wifi_manager_.isWifiEnabled()){
            wifi_manager_.setWifiEnabled(true);
        }
        redundant_counter_.set(redundancy_);
        if(wifi_manager_.startScan()){
            Log.i(LOG_TAG, "Scan request sent");
        }else{
            Log.i(LOG_TAG, "Scan request failed");
        }
    }

    @Override
    public void run(){
        if(!wifi_manager_.isWifiEnabled()){
            wifi_manager_.setWifiEnabled(true);
        }
        // wifi_manager_.startScan();
        singleScan();
        if(is_running_.get()) {
            handler_.postDelayed(this, scan_interval_);
        }
    }

    public boolean isRunning(){
        return is_running_.get();
    }
}
