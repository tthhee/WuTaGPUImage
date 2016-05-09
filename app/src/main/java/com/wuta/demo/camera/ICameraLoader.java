package com.wuta.demo.camera;

import android.app.Activity;

import com.wuta.gpuimage.GPUImage;

/**
 * Created by kejin
 * on 2016/5/9.
 */
public interface ICameraLoader
{
    void onResume(Activity activity, GPUImage image);

    void onPause();

    void switchCamera(Activity activity, GPUImage image);
}
