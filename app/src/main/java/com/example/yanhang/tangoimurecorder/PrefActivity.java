package com.example.yanhang.tangoimurecorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
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


    public static class PrefFragment extends PreferenceFragment {

        final static String LOG_TAG = PrefFragment.class.getSimpleName();

        CharSequence[] mAdfNames = null;
        CharSequence[] mAdfUuids = null;

        public final static String KEY_NAME = "adf_name";
        public final static String KEY_UUID = "adf_uuid";

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
            try {
                ArrayList<String> names = getArguments().getStringArrayList(KEY_NAME);
                ArrayList<String> uuids = getArguments().getStringArrayList(KEY_UUID);
                mAdfNames = new CharSequence[names.size()];
                mAdfUuids = new CharSequence[names.size()];
                for(int i=0; i<names.size(); ++i){
                    mAdfNames[i] = names.get(i);
                    mAdfUuids[i] = uuids.get(i);
                }

                ListPreference list = (ListPreference)findPreference("pref_adf_uuid");
                list.setEntries(mAdfNames);
                list.setEntryValues(mAdfUuids);

            }catch (NullPointerException e){
                e.printStackTrace();
            }
        }
    }

}
