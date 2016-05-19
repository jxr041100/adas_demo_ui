package com.asus.edrsample.kneron;

import android.graphics.Bitmap;
import android.util.Log;
import java.nio.ByteBuffer;

/**
 */
public class adas {

    public int[] mBoundingBox    = new int[100*5];

    static {
        System.loadLibrary("kneron_adas");
        System.loadLibrary("kneron_demo");
    };


    public void Init() {
        ObjectInit();
    }
    public void process(ByteBuffer data, int w, int h)
    {

        byte[] data_arr = new byte[data.remaining()];
        data.get(data_arr);
        update(data_arr, w, h);
    }


    public void process(byte[] data_arr, int w, int h)
    {

       // byte[] data_arr = new byte[data.remaining()];
      //  data.get(data_arr);
        update(data_arr, w, h);
    }

    public void DeInit()
    {
        cleanup();
    }
    public int[] getBoundingBox()
    {
       getObjectInfo(mBoundingBox);
        return mBoundingBox;
    }

    public int getNumBoundingBox()
    {
        return getNumObject();
    }

    private native void ObjectInit();
    private native void ObjectDeinit();
    private native int  getNumObject();
    private native void getObjectInfo(int[] faceRect);
    public 	native void update(byte[] data, int w, int h);
    private native void cleanup();
    public native boolean YUV420ByteBufferToRGB8888Bitmap(ByteBuffer src, int Coffset, int srcWidth,
                                                          int srcHeight, int srcYStride,
                                                          int srcCStride,
                                                          Bitmap outputBitmap);

}