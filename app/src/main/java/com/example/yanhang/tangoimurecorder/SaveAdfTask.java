package com.example.yanhang.tangoimurecorder;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.ProgressBar;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoAreaDescriptionMetaData;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoInvalidException;

/**
 * Created by yanhang on 4/19/17.
 */

public class SaveAdfTask extends AsyncTask<Void, Integer, String> {
    public interface SaveAdfListener{
        void onSaveAdfFailed(String adfName);
        void onSaveAdfSuccess(String adfName, String adfUuid);
    }

    Context mContext;
    SaveAdfListener mCallbackListener;
    Tango mTango;
    String mAdfName;

    private ProgressDialog mProgressBar;

    SaveAdfTask(Context context, SaveAdfListener callbackListener, Tango tango, String adfName){
        mContext = context;
        mCallbackListener = callbackListener;
        mTango = tango;
        mAdfName = adfName;
        mProgressBar = new ProgressDialog(context);
    }

    @Override
    protected void onPreExecute(){
        if(mProgressBar != null){
            mProgressBar.setMessage("Saving");;
            mProgressBar.show();
        }
    }

    @Override
    protected String doInBackground(Void... params) {
        String adfUuid = null;
        try{
            adfUuid = mTango.saveAreaDescription();
            TangoAreaDescriptionMetaData metadata = mTango.loadAreaDescriptionMetaData(adfUuid);
            metadata.set(TangoAreaDescriptionMetaData.KEY_NAME, mAdfName.getBytes());
            mTango.saveAreaDescriptionMetadata(adfUuid, metadata);
        }catch (TangoErrorException e){
            adfUuid = null;
        }catch(TangoInvalidException e){
            adfUuid = null;
        }
        return adfUuid;
    }

    @Override
    protected void onProgressUpdate(Integer... progress){
    }

    @Override
    protected void onPostExecute(String adfUuid){
        if(mCallbackListener != null){
            if(adfUuid == null){
                mCallbackListener.onSaveAdfFailed(mAdfName);
            }else{
                mCallbackListener.onSaveAdfSuccess(mAdfName, adfUuid);
            }
        }
        if(mProgressBar.isShowing()){
            mProgressBar.dismiss();
        }
    }
}
