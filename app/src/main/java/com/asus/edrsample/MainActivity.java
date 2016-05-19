package com.asus.edrsample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.asus.EdrEngine.SnapCallBack;
import com.asus.EdrEngine.AlgoCallBack;
import com.asus.EdrEngine.VideoCallBack;
import com.asus.EdrEngine.VideoEngine;

import com.asus.edrsample.kneron.adas;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/*
public class MainActivity extends AppCompatActivity {

    private Button mButtonSnap;

    private Button mButtonT1;
    private Button mButtonT2;
    private Button mButtonT3;
    private Button mButtonT4;

    private TextView logText;

    private VideoEngine mVideoEngine;
    private adas mADAS = new adas();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v("EDR","onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButtonSnap = (Button) findViewById(R.id.button);
        mButtonT1 = (Button) findViewById(R.id.buttonT1);
        mButtonT2 = (Button) findViewById(R.id.buttonT2);
        mButtonT3 = (Button) findViewById(R.id.buttonT3);
        mButtonT4 = (Button) findViewById(R.id.buttonT4);

        logText = (TextView) findViewById(R.id.textViewLog);
        mADAS.Init();


        mButtonSnap.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                SnapCallBack snapCB = new SnapCallBack() {
                    @Override
                    public void run(byte[] bytes,int flag) {
                        logText.append("SnapCallBack with bytes \n");
                        File mFile;
                        if(flag==IMAGE_FMT_JPEG)
                            mFile = new File("/sdcard/", "pic.jpg");
                        else
                            mFile = new File("/sdcard/", "pic.y");
                        FileOutputStream output = null;
                        try {
                            output = new FileOutputStream(mFile);
                            output.write(bytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (null != output) {
                                try {
                                    output.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    @Override
                    public void run(String path) {
                        logText.append("SnapCallBack with path %s" + path + "\n");

                    }

                };
                mVideoEngine.TakePicture(snapCB);
            }
        });


        mButtonT1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AlgoCallBack algoCB = new AlgoCallBack() {
                    @Override
                    public void run(byte[] bytes,int flag) {
                        logText.append("AlgoCallBack with bytes \n");
                        mADAS.process(bytes , 1920, 1080);

                    }

                    @Override
                    public void run(String path) {
                        logText.append("AlgoCallBack with path %s" + path + "\n");

                    }

                };
                mVideoEngine.setAlgoCallBack(algoCB);
            }
        });

        mButtonT2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                VideoCallBack videoCB = new VideoCallBack() {
                    @Override
                    public void run(byte[] bytes,int flag) {
                        logText.append("VideoCallBack with bytes \n");
                        mADAS.process(bytes , 1920, 1080);

                    }


                    @Override
                    public void run(String path) {
                        logText.append("VideoCallBack with path %s" + path + "\n");

                    }

                };
                mVideoEngine.CaptureVideo(videoCB);
            }
        });

        mButtonT3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {


            }
        });

        mButtonT4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {


            }
        });

        mVideoEngine = new VideoEngine();
        Log.d("EDR", "VideoEngine: version:"+mVideoEngine.getVersion());
        mVideoEngine.setParentContext(this.getApplication());
        mVideoEngine.setConfig();
        mVideoEngine.prepare();

    }

    @Override
    protected  void onStart()
    {
        super.onStart();

        Log.v("EDR","onStart");

        //mVideoEngine.startLoopRecording();
        mVideoEngine.startNonLoopRecording();

        //do video snapshot sample

    }

    @Override
    protected  void onResume()
    {
        super.onResume();
        Log.v("EDR","onResume");
    }

    @Override
    protected  void onPause()
    {
        super.onPause();

        Log.v("EDR","onPause");

       //mVideoEngine.stopRecordingVideo();
    }
    @Override
    protected  void onStop()
    {
        super.onStop();
        mADAS.DeInit();
        Log.v("EDR","onStop");

        //mVideoEngine.stopLoopRecording();
        mVideoEngine.stopNonLoopRecording();

    }
    @Override
    protected  void onDestroy()
    {
        super.onDestroy();
        Log.v("EDR","onDestroy");
        mVideoEngine.reset();
        mVideoEngine.release();
        mVideoEngine=null;
    }
}
*/

import android.graphics.Bitmap;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;


import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView = (ImageView) findViewById(R.id.imageView);

        setupDecode("/carTest1.mp4");

    }

    private void setupDecode(String fileName) {

        String mp4FilePath = Environment.getExternalStorageDirectory() + fileName;
        Uri videoUri = Uri.fromFile(new File(mp4FilePath));

        /* Initialize and set the datasource of the media extractor */
        MediaExtractor extractor = new MediaExtractor();

        try {
            extractor.setDataSource(this, videoUri, null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
         * Iterate over the available tracks and choose the video track. Choose
		 * the codec by type and configure the codec
		 */
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);

            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {

                extractor.selectTrack(i);
                int inputWidth = format.getInteger(MediaFormat.KEY_WIDTH);
                int inputHeight = format.getInteger(MediaFormat.KEY_HEIGHT);

                //Bitmap outputBitmap = Bitmap.createBitmap(inputWidth, inputHeight, Bitmap.Config.ARGB_8888);
                Bitmap outputBitmap = Bitmap.createBitmap(inputWidth, inputHeight, Bitmap.Config.RGB_565);
                mImageView.setImageBitmap(outputBitmap);

                //format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);

                Log.d(TAG, "Found a video track.");

                //VideoDecodeAsync videoDecodeScore = new VideoDecodeAsync(this, videoUri, outputBitmap, mImageView, inputWidth, inputHeight, format, extractor, mime);
                VideoDecodeSync videoDecodeScore = new VideoDecodeSync(this, videoUri, outputBitmap, mImageView, inputWidth, inputHeight, format, extractor, mime);
                Thread decodeScore = new Thread(videoDecodeScore);
                decodeScore.start();

                break;
            }
        }
    }

}
