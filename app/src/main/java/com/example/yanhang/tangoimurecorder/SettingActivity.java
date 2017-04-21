package com.example.yanhang.tangoimurecorder;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoAreaDescriptionMetaData;
import com.google.atap.tangoservice.TangoErrorException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yanhang on 4/18/17.
 */

public class SettingActivity extends AppCompatActivity {

    class ADFEntry{
        String name;
        String uuid;

        public ADFEntry(String n, String u){
            name = n;
            uuid = u;
        }
    }

    class ADFAdapter extends ArrayAdapter<ADFEntry>{
        private List<ADFEntry> mADFList;

        public ADFAdapter(Context context, ArrayList<ADFEntry> adfList){
            super(context, R.layout.adf_list_row);
            mADFList = adfList;
        }

        public void setList(List<ADFEntry> new_list){
            mADFList = new_list;
        }
        @Override
        public int getCount(){
            return mADFList.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            View row;
            if(convertView == null){
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.adf_list_row, parent, false);
            }else{
                row = convertView;
            }

            TextView uuid = (TextView) row.findViewById(R.id.adf_uuid);
            TextView name = (TextView) row.findViewById(R.id.adf_name);

            if(mADFList == null){
                name.setText("No data to show");
            }else{
                name.setText(mADFList.get(position).name);
                uuid.setText(mADFList.get(position).uuid);
            }
            return row;
        }

        @Override
        public ADFEntry getItem(int position){
            return mADFList.get(position);
        }
    }


    ArrayList<ADFEntry> mADFList;
    ListView mADFListView;
    ADFAdapter mListAdapter;

    Switch mFileSwitch;
    Switch mPoseSwitch;
    Switch mWifiSwitch;
    Switch mAutoWifiSwitch;
    Switch mADFSwitch;

    TextView mLabelADFInfo;

    TangoIMUConfig mConfig;

    private Tango mTango;

    final static String LOG_TAG = SettingActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_layout);
        mADFList = new ArrayList<>();
        mADFListView = (ListView)findViewById(R.id.ad_list);
        mListAdapter = new ADFAdapter(SettingActivity.this, mADFList);
        mADFListView.setAdapter(mListAdapter);

        mADFSwitch = (Switch)findViewById(R.id.switch_adf);
        mPoseSwitch = (Switch)findViewById(R.id.switch_pose);
        mWifiSwitch = (Switch)findViewById(R.id.switch_wifi);
        mAutoWifiSwitch = (Switch)findViewById(R.id.switch_auto_wifi);
        mFileSwitch = (Switch)findViewById(R.id.switch_file);

        mLabelADFInfo = (TextView)findViewById(R.id.txt_adf);

        mADFListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ADFEntry selected = (ADFEntry)parent.getItemAtPosition(position);
                mConfig.setADFName(selected.name);
                mConfig.setADFUuid(selected.uuid);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLabelADFInfo.setText(mConfig.getADFName() + "(" + mConfig.getADFUuid() + ") selected");
                    }
                });
                //Log.i(LOG_TAG, "Position: " + String.valueOf(position) + " count: " +  String.valueOf(parent.getCount()));
            }
        });


        Intent intent = getIntent();
        mConfig = (TangoIMUConfig)intent.getSerializableExtra(MainActivity.INTENT_EXTRA_CONFIG);
        mADFSwitch.setChecked(mConfig.getADFEnabled());

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onPause(){
        super.onPause();
        synchronized (SettingActivity.this){
            mTango.disconnect();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        mTango = new Tango(SettingActivity.this, new Runnable() {
            @Override
            public void run() {
                synchronized (SettingActivity.this) {
                    if(mADFSwitch.isChecked()) {
                        updateList();
                    }
                }
            }
        });
    }

    private void updateList(){
        TangoAreaDescriptionMetaData metaData = new TangoAreaDescriptionMetaData();
        final ArrayList<String> adflist = mTango.listAreaDescriptions();
        final ArrayList<String> namelist = new ArrayList<>();

        for(String uuid: adflist){
            String name;
            try {
                metaData = mTango.loadAreaDescriptionMetaData(uuid);
            }catch (TangoErrorException e){
                Toast.makeText(SettingActivity.this, "Tango exception", Toast.LENGTH_SHORT).show();
                namelist.add("Unknown");
            }
            name = new String(metaData.get(TangoAreaDescriptionMetaData.KEY_NAME));
            namelist.add(name);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mADFList.clear();
                for(int i=0; i<namelist.size(); ++i) {
                    mADFList.add(new ADFEntry(namelist.get(i), adflist.get(i)));
                }
                mListAdapter.setList(mADFList);
                mListAdapter.notifyDataSetChanged();
            }
        });
    }

    public void onADFSwitchClicked(View view){
        if(mADFSwitch.isChecked()){
            updateList();
        }else{
            mADFList.clear();
            mListAdapter.notifyDataSetChanged();
            mLabelADFInfo.setText("ADF not enabled");
        }
    }

    public void onPoseSwitchClicked(View view){
        mConfig.setPoseEnabled(mPoseSwitch.isChecked());
    }

    public void onFileSwitchClicked(View view){
        mConfig.setFileEnabled(mFileSwitch.isChecked());
    }
    public void onWifiSwitchClicked(View view){
        mConfig.setWifiEnabled(mWifiSwitch.isChecked());
    }
    public void onAutoWifiSwitchClicked(View view){
        mConfig.setContinuesWifiScan(mAutoWifiSwitch.isChecked());
    }

    private void prepareBackIntent(){
        Intent return_intent = new Intent();
        return_intent.putExtra(MainActivity.INTENT_EXTRA_CONFIG, mConfig);
        setResult(RESULT_OK, return_intent);
    }

    @Override
    public void onBackPressed(){
        prepareBackIntent();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(item.getItemId() == android.R.id.home){
            prepareBackIntent();
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
