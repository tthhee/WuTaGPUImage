package com.wuta.demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import com.wuta.gpuimage.convert.GPUImageConvertor;
import com.wuta.gpuimage.exfilters.GPUImageDrawFilter;
import com.wuta.gpuimage.exfilters.GPUImageSampleFilter;
import com.wuta.gpuimage.exfilters.GPUImageSwirlFilter;

public class MainActivity extends AppCompatActivity
{

//    private GPUImage mGPUImage;
    private ICameraLoader mCameraLoader;
    private GPUImageFilter mFilter;
    private GPUImageDrawFilter mDrawFilter;

    private IGPUImage mIGPUImage;
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN ,
//                WindowManager.LayoutParams. FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        mIGPUImage = new GPUImageImpl(this, (GLSurfaceView) findViewById(R.id.surfaceView)); //, GPUImageConvertor.ConvertType.RAW_NV21_TO_RGBA);
//        mFilter = new GPUImageSampleFilter();
//        mGPUImage = new GPUImage(this, (GLSurfaceView) findViewById(R.id.surfaceView));
//        mGPUImage.setFilter(mFilter);
        mFilter = new GPUImageSwirlFilter();

        mDrawFilter = new GPUImageDrawFilter();
        mIGPUImage.setFilter(mFilter);
        mIGPUImage.setDrawFilter(mDrawFilter);

        Bitmap picture = BitmapFactory.decodeResource(getResources(), R.mipmap.testpic);
        mIGPUImage.setDrawPicture(picture);

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
