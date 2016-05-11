package com.wuta.gpuimage;

import android.content.Context;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;

import com.wuta.gpuimage.util.OpenGlUtils;

/**
 * Created by kejin
 * on 2016/5/11.
 */
public interface IGPUImage extends GLSurfaceView.Renderer, Camera.PreviewCallback
{
    int NO_IMAGE = OpenGlUtils.NO_TEXTURE;

    void setupCamera(Camera camera);

    void setupCamera(Camera camera, int degrees, boolean flipHor, boolean flipVer);

    void setPreviewSize(int width, int height);

    void setRotation(Rotation rotation);

    void setRotation(Rotation rotation, boolean flipHor, boolean flipVer);

    void setFilter(GPUImageFilter filter);

    void setGLSurfaceView(GLSurfaceView surfaceView);

    void setBackgroundColor(float red, float green, float blue);

    void requestRender();

    void destroy();
}
