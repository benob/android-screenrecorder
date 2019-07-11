package au.edu.qut.screenrecorder;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends ActionBarActivity {

    private static final int CAST_PERMISSION_CODE = 22;
    private DisplayMetrics mDisplayMetrics;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaRecorder mMediaRecorder;
    private MediaProjectionManager mProjectionManager;
    private String TAG = "REC";
    private boolean recording = false;

    private int maxVideoWidth = 800;
    private int videoFPS = 15;
    private int videoBitRate = 1500 * 1024;
    private int audioBitRate = 96 * 1024;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Context context = this;
        final Button buttonStart = (Button) findViewById(R.id.start_button);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "click start");
                if(recording) stopRecording();
                startActivityForResult(mProjectionManager.createScreenCaptureIntent(), CAST_PERMISSION_CODE);
            }
        });
        final Button buttonStop = (Button) findViewById(R.id.stop_button);
        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "click stop");
                stopRecording();
            }
        });
        mMediaRecorder = new MediaRecorder();

        mProjectionManager = (MediaProjectionManager) getSystemService
                (Context.MEDIA_PROJECTION_SERVICE);
        mDisplayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != CAST_PERMISSION_CODE) {
            // Where did we get this request from ? -_-
            Log.w(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Screen Cast Permission Denied :(", Toast.LENGTH_SHORT).show();
            return;
        }
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                if(recording) stopRecording();
            }
        }, null);

        startRecording();
    }

    private VirtualDisplay getVirtualDisplay() {
        int screenDensity = mDisplayMetrics.densityDpi;
        int width = mDisplayMetrics.widthPixels;
        int height = mDisplayMetrics.heightPixels;
        if(width > maxVideoWidth) {
            height = height * maxVideoWidth / width;
            screenDensity = screenDensity * maxVideoWidth / width;
            width = maxVideoWidth;
        }

        return mMediaProjection.createVirtualDisplay(this.getClass().getSimpleName(),
                width, height, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null /*Handler*/);
    }

    private void startRecording() {
        Log.d(TAG, "tentative start recording");
        prepareRecording();
        mVirtualDisplay = getVirtualDisplay();
        mMediaRecorder.start();
        Toast.makeText(this, "Recording screen", Toast.LENGTH_SHORT).show();
        recording = true;
        Log.d(TAG,"    recording=true");
    }

    private void stopRecording() {
        Log.d(TAG, "tentative stop recording");
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            Log.d(TAG, "    stop recording");
            Toast.makeText(this, "Stopped recording", Toast.LENGTH_SHORT).show();
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
        }
        recording = false;
    }

    public String getCurSysDate() {
        return new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
    }

    public String getFileName() {
        final String directory = Environment.getExternalStorageDirectory() + File.separator + "Recordings";
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Log.e(TAG, "Failed to get external storage");
            Toast.makeText(this, "Failed to get External Storage", Toast.LENGTH_SHORT).show();
            return null;
        }
        final File folder = new File(directory);
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdir();
        }
        String filePath;
        if (success) {
            String videoName = ("capture_" + getCurSysDate() + ".mp4");
            filePath = directory + File.separator + videoName;
        } else {
            Log.e(TAG, "Failed to create recording directory");
            Toast.makeText(this, "Failed to create Recordings directory", Toast.LENGTH_SHORT).show();
            return null;
        }
        Log.d(TAG, "filepath = " + filePath);
        return filePath;
    }

    private void prepareRecording() {


        int width = mDisplayMetrics.widthPixels;
        int height = mDisplayMetrics.heightPixels;
        if(width > maxVideoWidth) {
            height = height * maxVideoWidth / width;
            width = maxVideoWidth;
        }

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setCaptureRate(videoFPS);

        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        /*mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.WEBM);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.VP8);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.VORBIS);*/

        mMediaRecorder.setAudioEncodingBitRate(audioBitRate);
        mMediaRecorder.setVideoEncodingBitRate(videoBitRate);
        mMediaRecorder.setVideoFrameRate(videoFPS);
        mMediaRecorder.setVideoSize(width, height);
        mMediaRecorder.setOutputFile(getFileName());
        try {
            mMediaRecorder.prepare();
        } catch (Exception e) {
            Log.e(TAG, "mediarecorder.prepare() failed");
            e.printStackTrace();
        }

    }
}
