package com.asus.edrsample;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import com.asus.edrsample.kneron.adas;

import java.io.IOException;
import java.nio.ByteBuffer;


public class VideoDecodeSync implements Runnable {

    private static final String TAG = VideoDecodeSync.class.getSimpleName();

    private Object decodeScoreTaskObject = new Object();

    /* Pulls encoded AV from source */
    private MediaExtractor extractor;
    /* Used in this case to decode video frame returned by the MediaExtractor */
    private MediaCodec codec;
    /* Path to the encoded AV source */
    private Uri videoUri;
    private Context context;

    private ImageView mImageView;

    private int inputWidth;
    private int inputHeight;
    private MediaFormat format;
    private String mime;

    private int outputFrameCnt;
    private Bitmap outputBitmap;

    private long currentTimeMs;
    private int uvOffset;
    private int stride;

    private final Canvas canvas;
    private final Paint paint;
    private final Paint paint_txt;

    private adas mADAS = new adas();
    private static final boolean VERBOSE = true;

    public VideoDecodeSync(Context context, Uri videoUri, Bitmap outputBitmap, ImageView mImageView, int inputWidth, int inputHeight, MediaFormat format, MediaExtractor extractor, String mime) {

        this.context = context;
        this.videoUri = videoUri;

        this.outputBitmap = outputBitmap;
        this.mImageView = mImageView;

        this.inputWidth = inputWidth;
        this.inputHeight = inputHeight;
        this.format = format;
        this.mime = mime;
        this.extractor = extractor;

        canvas = new Canvas(this.outputBitmap);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.rgb(255, 0, 0));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);

        paint_txt = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint_txt.setColor(Color.rgb(0, 255, 0));
        paint_txt.setStyle(Paint.Style.STROKE);
        paint_txt.setStrokeWidth(2);

        paint_txt.setTextSize(30);
    }

    @Override
    public void run() {

        final int TIMEOUT_USEC = 10000;


        mADAS.Init();

        try {
            codec = MediaCodec.createDecoderByType(mime);
        } catch (IOException e) {
            e.printStackTrace();
        }

        codec.configure(format, null, null, 0);
        codec.start();

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        boolean inputDone = false;
        boolean decoderDone = false;

        while (!decoderDone) {

            ////////////////////////////////
            // Feed more data to the decoder.
            ////////////////////////////////
            if (!inputDone) {

                int decoderInputStatusOrIndex = codec.dequeueInputBuffer(TIMEOUT_USEC);
                if (decoderInputStatusOrIndex >= 0) {

                    ByteBuffer buffer = codec.getInputBuffer(decoderInputStatusOrIndex);
                    int sampleSize = extractor.readSampleData(buffer, 0);

                    if (sampleSize > 0) {
                        long sampleTime = extractor.getSampleTime();
                        codec.queueInputBuffer(decoderInputStatusOrIndex, 0, sampleSize,
                                sampleTime, 0);
                        if (VERBOSE) Log.d(TAG, "decoder input: queued frame for sampleTime = " + sampleTime);
                        extractor.advance();
                    } else {
                        // End of stream -- send empty frame with EOS flag set.
                        codec.queueInputBuffer(decoderInputStatusOrIndex, 0, 0, 0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                        if (VERBOSE) Log.d(TAG, "sent input EOS (with zero-length frame)");
                    }
                } else {
                    if (VERBOSE) Log.d(TAG, "input buffer not available");
                }
            }

            boolean decoderOutputAvailable = true;
            while (decoderOutputAvailable) {

                int decoderOutputStatusOrIndex = codec.dequeueOutputBuffer(info, TIMEOUT_USEC);
                if (decoderOutputStatusOrIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (VERBOSE) Log.d(TAG, "decoder output: no output available");
                    decoderOutputAvailable = false;
                } else if (decoderOutputStatusOrIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // expected before first buffer of data
                    if (VERBOSE) Log.d(TAG, "decoder output: format changed: " + codec.getOutputFormat());
                } else if (decoderOutputStatusOrIndex >= 0) {

                    if (VERBOSE) Log.d(TAG, "decoder output: frame available: sampleTime = " + info.presentationTimeUs);

                    decoderOutputAvailable = true;
                    ByteBuffer outputFrame = codec.getOutputBuffer(decoderOutputStatusOrIndex);

                    if (stride == 0 && info.size != 0) //not found yet
                    {
                        MediaFormat format = codec.getOutputFormat();
                        stride = (format.containsKey("stride") ?
                                format.getInteger("stride") : inputWidth);
                        uvOffset = (format.containsKey("slice-height") ?
                                format.getInteger("slice-height") : inputHeight) * stride;
                    }

                    if (info.size != 0) {

                        long newTimeMs = System.currentTimeMillis();
                        Log.d(TAG, "[" + (newTimeMs - currentTimeMs) + " ms from previous output frame] frame count = " + outputFrameCnt);
                        currentTimeMs = System.currentTimeMillis();
                        outputFrameCnt++;

                        mADAS.process(outputFrame,inputWidth,inputHeight);
                        int number_bounding_box = mADAS.getNumBoundingBox();
                        int[] bounding_box = mADAS.getBoundingBox();

                        //for display test
                        mADAS.YUV420ByteBufferToRGB8888Bitmap(outputFrame, uvOffset, inputWidth, inputHeight, stride, stride/2, outputBitmap);
                        //FastCVJNILib.YUV420PlanarByteBufferToRGB565Bitmap(outputFrame, uvOffset, inputWidth, inputHeight, stride, stride / 2, outputBitmap);

                        codec.releaseOutputBuffer(decoderOutputStatusOrIndex, false);

                        for (int i = 0; i < number_bounding_box; i++) {
                            int leftEdge = bounding_box[i * 5 + 0];
                            int topEdge = bounding_box[i * 5 + 1];
                            int width = bounding_box[i * 5 + 2];
                            int height = bounding_box[i * 5 + 3];
                            int distance = bounding_box[i * 5 + 4];
                            Log.d(TAG,  "x = " + leftEdge + " y = " + topEdge + " width = " + width + "height =" + height);
                            canvas.drawRect(leftEdge, topEdge, leftEdge + width, topEdge + height, paint);
                            //canvas.drawText(String.valueOf(distance), leftEdge + 10, topEdge + 10, paint_txt);
                        }

                        //Log.d(TAG, "detecting the bounding box " + number_bounding_box);
                        mImageView.postInvalidate();
                    }

                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (VERBOSE) Log.d(TAG, "decoder got EOS");
                        decoderOutputAvailable = false;
                        releaseDecodingResources();
                        mADAS.DeInit();
                        decoderDone = true;
                    }
                }
            }
        }
    }


    private void releaseDecodingResources() {

        if (codec != null) {
            codec.stop();
            codec.release();
            codec = null;
        }
        if (extractor != null) {
            extractor.release();
            extractor = null;
        }

        Log.d(TAG, "Task released resources.");
    }


    /*void yourFunction(byte[] data, int mWidth, int mHeight)
    {

        int[] mIntArray = new int[mWidth*mHeight];

// Decode Yuv data to integer array
        decodeYUV420SP(mIntArray, data, mWidth, mHeight);

//Initialize the bitmap, with the replaced color
        Bitmap bmp = Bitmap.createBitmap(mIntArray, mWidth, mHeight, Bitmap.Config.ARGB_8888);

// Draw the bitmap with the replaced color
        iv.setImageBitmap(bmp);

    }

    static public void decodeYUV420SP(int[] rgba, ByteBuffer yuv420sp, int width,
                                      int height) {
        final int frameSize = width * height;

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp.getChar(yp))) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp.getChar(uvp++)) - 128;
                    u = (0xff & yuv420sp.getChar(uvp++)) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0)
                    r = 0;
                else if (r > 262143)
                    r = 262143;
                if (g < 0)
                    g = 0;
                else if (g > 262143)
                    g = 262143;
                if (b < 0)
                    b = 0;
                else if (b > 262143)
                    b = 262143;

                // rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) &
                // 0xff00) | ((b >> 10) & 0xff);
                // rgba, divide 2^10 ( >> 10)
                rgba[yp] = ((r << 14) & 0xff000000) | ((g << 6) & 0xff0000)
                        | ((b >> 2) | 0xff00);
            }
        }
    }*/
}
