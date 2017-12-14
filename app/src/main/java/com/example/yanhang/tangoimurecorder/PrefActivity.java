package com.example.yanhang.tangoimurecorder;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.atap.tangoservice.Tango;

import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.security.AlgorithmConstraints;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by yanhang on 4/21/17.
 */

public class PrefActivity extends Activity{

    final static String LOG_TAG = PrefActivity.class.getSimpleName();
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        ArrayList<String> names = intent.getStringArrayListExtra(MainActivity.INTENT_EXTRA_ADF_NAME);
        ArrayList<String> uuids = intent.getStringArrayListExtra(MainActivity.INTENT_EXTRA_ADF_UUID);
        getFragmentManager().beginTransaction().replace(android.R.id.content, PrefFragment.newInstance(names, uuids)).commit();
    }

    @Override
    protected void onResume(){
        super.onResume();

    }

    @Override
    protected void onPause(){
        super.onPause();
    }


    public static class PrefFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener{

        final static String LOG_TAG = PrefFragment.class.getSimpleName();

        CharSequence[] mAdfNames = null;
        CharSequence[] mAdfUuids = null;

        public final static String KEY_NAME = "adf_name";
        public final static String KEY_UUID = "adf_uuid";

        ListPreference mListPreference;
        ListPreference mListRequestPreference;
        ListPreference mListIntervalPreference;
        EditTextPreference mEditTextFolderPrefix;

        public static final PrefFragment newInstance(ArrayList<String> names,
                                                     ArrayList<String> uuids){
            PrefFragment fragment = new PrefFragment();
            final Bundle args = new Bundle(2);
            args.putStringArrayList(KEY_NAME, names);
            args.putStringArrayList(KEY_UUID, uuids);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference);
            mListPreference = (ListPreference)findPreference("pref_adf_uuid");
            mListRequestPreference = (ListPreference)findPreference("pref_num_requests");
            mListIntervalPreference = (ListPreference)findPreference("pref_scan_interval");
            mEditTextFolderPrefix = (EditTextPreference)findPreference("pref_folder_prefix");

            try {
                ArrayList<String> names = getArguments().getStringArrayList(KEY_NAME);
                ArrayList<String> uuids = getArguments().getStringArrayList(KEY_UUID);
                mAdfNames = new CharSequence[names.size()];
                mAdfUuids = new CharSequence[names.size()];
                for(int i=0; i<names.size(); ++i){
                    mAdfNames[i] = names.get(i);
                    mAdfUuids[i] = uuids.get(i);
                }

                mListPreference.setEntries(mAdfNames);
                mListPreference.setEntryValues(mAdfUuids);

                if(getPreferenceManager().getSharedPreferences().getBoolean("pref_adf_enabled", false)){
                    mListPreference.setSummary("Please select the area description file");
                }else{
                    mListPreference.setSummary("Area description file disabled");
                }

                String num_req = getPreferenceManager().getSharedPreferences().getString("pref_num_requests", "1");
                mListRequestPreference.setSummary("Requests per scan: " + num_req);

                String scan_interval = getPreferenceManager().getSharedPreferences().getString("pref_scan_interval", "1");
                mListIntervalPreference.setSummary("Scan interval: " + scan_interval + " sec");
            }catch (NullPointerException e){
                e.printStackTrace();
            }
        }

        @Override
        public void onResume(){
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            String uuid = getPreferenceManager().getSharedPreferences().getString("pref_adf_uuid", "unknwon");
            String folder_prefix = getPreferenceManager().getSharedPreferences().getString("pref_folder_prefix", "Not set");
            int index = mListPreference.findIndexOfValue(uuid);
            if(index >= 0) {
                mListPreference.setSummary(mAdfNames[index] + " (" + uuid + ")");
            }else{
                mListPreference.setSummary("Invalid ADF: " + uuid);
            }
            mEditTextFolderPrefix.setSummary(folder_prefix);
        }

        @Override
        public void onPause(){
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                              String key){
            if(key.equals("pref_adf_enabled")){
                boolean adf_enabled = sharedPreferences.getBoolean(key, false);
                if(adf_enabled){
                    mListPreference.setSummary("Please select the area description file");
                }else{
                    mListPreference.setSummary("Area description file disabled");
                }
            }else if(key.equals("pref_adf_uuid")){
                String uuid = sharedPreferences.getString("pref_adf_uuid", "unknwon");
                int index = mListPreference.findIndexOfValue(uuid);
                if(index >= 0) {
                    mListPreference.setSummary(mAdfNames[index] + " (" + uuid + ")");
                }else{
                    mListPreference.setSummary("Invalid ADF: " + uuid);
                }
            }else if(key.equals("pref_num_requests")){
                String value = sharedPreferences.getString("pref_num_requests", "1");
                mListRequestPreference.setSummary("Requests per scan: " + value);
            }else if(key.equals("pref_scan_interval")){
                String value = sharedPreferences.getString("pref_scan_interval", "1");
                mListIntervalPreference.setSummary("Scan interval: " + value + " sec");
            }else if(key.equals("pref_folder_prefix")){
                mEditTextFolderPrefix.setSummary(sharedPreferences.getString(
                        "pref_folder_prefix", "Not set"));
            }
        }
    }

}
