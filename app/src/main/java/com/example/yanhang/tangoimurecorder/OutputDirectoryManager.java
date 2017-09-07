package com.example.yanhang.tangoimurecorder;

/**
 * Created by yanhang on 9/6/17.
 */
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

// This class
public class OutputDirectoryManager {
    private final static String LOG_TAG = OutputDirectoryManager.class.getSimpleName();

    private String mOutputDirectory;

    OutputDirectoryManager(final String prefix, final String suffix) throws FileNotFoundException{
        update(prefix, suffix);
    }

    OutputDirectoryManager(final String prefix) throws FileNotFoundException{
        update(prefix);
    }

    OutputDirectoryManager() throws FileNotFoundException{
        update();
    }

    public void update(final String prefix, final String suffix) throws FileNotFoundException{
        Calendar current_time = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddhhmmss");
        File external_dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String folder_name = formatter.format(current_time.getTime());
        if (prefix != null){
            folder_name = prefix + folder_name;
        }
        if (suffix != null){
            folder_name = folder_name + suffix;
        }
        File output_dir = new File(external_dir.getAbsolutePath() + "/" + folder_name);
        if (!output_dir.exists()) {
            if (!output_dir.mkdir()) {
                Log.e(LOG_TAG, "Can not create output directory");
                throw new FileNotFoundException();
            }
        }
        mOutputDirectory = output_dir.getAbsolutePath();
        Log.i(LOG_TAG, "Output directory: " + output_dir.getAbsolutePath());
    }

    public void update(final String prefix) throws FileNotFoundException{
        update(prefix, null);
    }

    public void update() throws FileNotFoundException{
        update(null, null);
    }

    public String getOutputDirectory(){
        return mOutputDirectory;
    }
}
