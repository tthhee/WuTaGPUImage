package com.wuta.demo;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import com.wuta.demo.camera.CameraLoaderImpl;
import com.wuta.demo.camera.ICameraLoader;
import com.wuta.gpuimage.GPUImage;
import com.wuta.gpuimage.GPUImageFilter;

public class MainActivity extends AppCompatActivity
{

    private GPUImage mGPUImage;
    private ICameraLoader mCameraLoader;
    private GPUImageFilter mFilter;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN ,
                WindowManager.LayoutParams. FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        mGPUImage = new GPUImage(this, (GLSurfaceView) findViewById(R.id.surfaceView));

        mCameraLoader = CameraLoaderImpl.getInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraLoader.onResume(this, mGPUImage);
    }

    @Override
    protected void onPause() {
        mCameraLoader.onPause();
        super.onPause();
    }
}
