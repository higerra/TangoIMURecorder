package com.example.yanhang.tangoimurecorder;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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

public class ADFActivity extends AppCompatActivity {

    class ADFEntry{
        String name;
        String uuid;

        public ADFEntry(String n, String u){
            name = n;
            uuid = u;
        }
    }

    class ADFAdapter extends ArrayAdapter<String>{
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
    }


    ArrayList<ADFEntry> mADFList;
    ListView mADFListView;
    ADFAdapter mListAdapter;
    private Tango mTango;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_al);
        mADFList = new ArrayList<>();
        mADFListView = (ListView)findViewById(R.id.ad_list);
        mListAdapter = new ADFAdapter(ADFActivity.this, mADFList);
        mADFListView.setAdapter(mListAdapter);
    }

    @Override
    protected void onPause(){
        super.onPause();
        synchronized (ADFActivity.this){
            mTango.disconnect();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        mTango = new Tango(ADFActivity.this, new Runnable() {
            @Override
            public void run() {
                synchronized (ADFActivity.this) {
                    TangoAreaDescriptionMetaData metaData = new TangoAreaDescriptionMetaData();
                    ArrayList<String> adflist = mTango.listAreaDescriptions();
                    mADFList.clear();
                    for(String uuid: adflist){
                        String name;
                        try {
                            metaData = mTango.loadAreaDescriptionMetaData(uuid);
                        }catch (TangoErrorException e){
                            Toast.makeText(ADFActivity.this, "Tango exception", Toast.LENGTH_SHORT).show();
                        }
                        name = new String(metaData.get(TangoAreaDescriptionMetaData.KEY_NAME));
                        mADFList.add(new ADFEntry(name, uuid));
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mListAdapter.setList(mADFList);
                            mListAdapter.notifyDataSetChanged();
                        }
                    });

                }
            }
        });
    }
}
