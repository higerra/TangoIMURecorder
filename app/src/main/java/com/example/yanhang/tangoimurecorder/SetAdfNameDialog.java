package com.example.yanhang.tangoimurecorder;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.atap.tangoservice.TangoAreaDescriptionMetaData;

/**
 * Created by yanhang on 4/19/17.
 */

public class SetAdfNameDialog extends DialogFragment{
    EditText mNameEditText;
    TextView mUuidTextView;

    CallbackListener mCallbackListener;
    Button mOkButton;
    Button mCancelButton;

    interface CallbackListener{
        void onAdfNameOk(String name, String uuid);
        void onAdfNameCancelled();
    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        mCallbackListener = (CallbackListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View dialogView = inflater.inflate(R.layout.set_adfname_dialog, container, false);
        getDialog().setTitle("Set ADF name");
        mNameEditText = (EditText)dialogView.findViewById(R.id.name);
        mUuidTextView = (TextView)dialogView.findViewById(R.id.uuidDisplay);
        setCancelable(false);

        mOkButton = (Button)dialogView.findViewById(R.id.ok);
        mOkButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallbackListener.onAdfNameOk(
                        mNameEditText.getText().toString(),
                        mUuidTextView.getText().toString());
                dismiss();
            }
        });

        mCancelButton = (Button) dialogView.findViewById(R.id.cancel);
        mCancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallbackListener.onAdfNameCancelled();
                dismiss();
            }
        });

        String name = this.getArguments().getString(TangoAreaDescriptionMetaData.KEY_NAME);
        String id = this.getArguments().getString(TangoAreaDescriptionMetaData.KEY_UUID);
        if(name != null){
            mNameEditText.setText(name);
        }
        if(id != null){
            mUuidTextView.setText(id);
        }

        return dialogView;
    }
}
