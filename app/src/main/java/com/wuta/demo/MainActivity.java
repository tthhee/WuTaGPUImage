package com.wuta.demo;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import com.wuta.demo.camera.CameraLoaderImpl;
import com.wuta.demo.camera.ICameraLoader;
import com.wuta.gpuimage.GPUImage;
import com.wuta.gpuimage.GPUImageFilter;
import com.wuta.gpuimage.GPUImageImpl;
import com.wuta.gpuimage.IGPUImage;
import com.wuta.gpuimage.exfilters.GPUImageSampleFilter;
import com.wuta.gpuimage.exfilters.GPUImageSwirlFilter;

public class MainActivity extends AppCompatActivity
{

//    private GPUImage mGPUImage;
    private ICameraLoader mCameraLoader;
    private GPUImageFilter mFilter;

    private IGPUImage mIGPUImage;
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN ,
//                WindowManager.LayoutParams. FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        mIGPUImage = new GPUImageImpl(this, (GLSurfaceView) findViewById(R.id.surfaceView));
//        mFilter = new GPUImageSampleFilter();
//        mGPUImage = new GPUImage(this, (GLSurfaceView) findViewById(R.id.surfaceView));
//        mGPUImage.setFilter(mFilter);
        mFilter = new GPUImageSwirlFilter();
        mIGPUImage.setFilter(mFilter);

        mCameraLoader = CameraLoaderImpl.getInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraLoader.onResume(this, mIGPUImage);
    }

    @Override
    protected void onPause() {
        mCameraLoader.onPause();
        super.onPause();
    }
}
