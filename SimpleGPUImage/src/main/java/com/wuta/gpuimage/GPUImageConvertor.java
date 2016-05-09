package com.wuta.gpuimage;

import android.opengl.GLES20;

import java.nio.FloatBuffer;

/**
 * Created by kejin
 * on 2016/5/7.
 */
public class GPUImageConvertor
{
    public final static String TAG = "Convertor";

    protected final GPUImageRawFilter mConvertor;

    public enum Type {
        NV21_TO_RGBA,
        YV12_TO_RGBA
    }

    public GPUImageConvertor()
    {
        this(Type.NV21_TO_RGBA);
    }

    public GPUImageConvertor(Type type)
    {
        type = type == null ? Type.NV21_TO_RGBA : type;

        switch (type) {
            case YV12_TO_RGBA:
                mConvertor = new GPUImageYV12RawFilter();
                break;

            default:
                mConvertor = new GPUImageNV21RawFilter();
                break;
        }
    }

    public void initialize()
    {
        mConvertor.initialize();
    }

    public void onOutputSizeChanged(int width, int height)
    {
        GLES20.glUseProgram(mConvertor.getRawProgram());
        mConvertor.onOutputSizeChanged(width, height);
    }

    public int convert(byte [] data, int width, int height)
    {
        return mConvertor.convert(data, width, height);
    }

    public void destroy()
    {
        mConvertor.destroy();
    }
}
