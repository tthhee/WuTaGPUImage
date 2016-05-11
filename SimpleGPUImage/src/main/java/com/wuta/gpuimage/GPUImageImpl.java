package com.wuta.gpuimage;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.wuta.gpuimage.convert.GPUImageConvertor;
import com.wuta.gpuimage.util.FPSMeter;
import com.wuta.gpuimage.util.OpenGlUtils;
import com.wuta.gpuimage.util.TextureRotationUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.wuta.gpuimage.util.TextureRotationUtil.TEXTURE_NO_ROTATION;

/**
 * Created by kejin
 * on 2016/5/11.
 */
public class GPUImageImpl implements IGPUImage
{
    public final static String TAG = "GPUImage";

    public final Object mSurfaceChangedWaiter = new Object();

    public final static float [] CUBE = OpenGlUtils.VERTEX_CUBE;

    protected Context mContext;
    protected GLSurfaceView mGLSurfaceView;
    protected GPUImageFilter mImageFilter;
    protected ScaleType mScaleType = ScaleType.CENTER_CROP;

    protected Camera mCamera;

    /**
     * has converted texture handle id
     */
    private int mConvertedTextureId = NO_IMAGE;

    /**
     * surfacetexture's texture handle id
     */
    private int mSurfaceTextureId = NO_IMAGE;
    private SurfaceTexture mSurfaceTexture = null;

    private final GPUImageConvertor mImageConvertor;

    /**
     * runnable queue
     */
    private final Queue<Runnable> mRunOnDraw;
    private final Queue<Runnable> mRunOnDrawEnd;

    private final FloatBuffer mGLCubeBuffer;
    private final FloatBuffer mGLTextureBuffer;

    private int mOutputWidth;
    private int mOutputHeight;
    private int mImageWidth;
    private int mImageHeight;

    private Rotation mRotation;
    private boolean mFlipHorizontal;
    private boolean mFlipVertical;

    private float mBackgroundRed = 0;
    private float mBackgroundGreen = 0;
    private float mBackgroundBlue = 0;

    public GPUImageImpl(Context context, GLSurfaceView view)
    {
        this(context, view, GPUImageConvertor.ConvertType.SURFACE_TEXTURE);
    }

    public GPUImageImpl(Context context, GLSurfaceView view, GPUImageConvertor.ConvertType convertType)
    {
        this(context, convertType);
        setGLSurfaceView(view);
    }

    public GPUImageImpl(Context context, GPUImageConvertor.ConvertType convertType)
    {
        if (!OpenGlUtils.supportOpenGLES2(context)) {
            throw new IllegalStateException("OpenGL ES 2.0 is not supported on this phone.");
        }

        mContext = context;
        mImageFilter = new GPUImageFilter();
        mImageConvertor = new GPUImageConvertor(convertType);

        mRunOnDraw = new LinkedList<Runnable>();
        mRunOnDrawEnd = new LinkedList<Runnable>();

        mGLCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        setRotation(Rotation.NORMAL, false, false);
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(mBackgroundRed, mBackgroundGreen, mBackgroundBlue, 1);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        mImageFilter.init();
        mImageConvertor.initialize();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mOutputWidth = width;
        mOutputHeight = height;
        GLES20.glViewport(0, 0, width, height);

        mImageConvertor.onOutputSizeChanged(width, height);

        GLES20.glUseProgram(mImageFilter.getProgram());
        mImageFilter.onOutputSizeChanged(width, height);
        adjustImageScaling();

        synchronized (mSurfaceChangedWaiter) {
            mSurfaceChangedWaiter.notifyAll();
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        FPSMeter.meter("DrawFrame");
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        runAll(mRunOnDraw);
        switch (mImageConvertor.getConvertType()) {
            case SURFACE_TEXTURE:
                if (mSurfaceTexture != null) {
                    mSurfaceTexture.updateTexImage();
                    mConvertedTextureId = mImageConvertor.convert(mSurfaceTextureId);
                }
                break;
        }

        mImageFilter.onDraw(mConvertedTextureId, mGLCubeBuffer, mGLTextureBuffer);

        runAll(mRunOnDrawEnd);
    }

    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {

        if (data.length != mImageHeight*mImageWidth*3/2) {
            Camera.Size previewSize = camera.getParameters().getPreviewSize();
            mImageWidth = previewSize.width;
            mImageHeight = previewSize.height;
            runOnDraw(new Runnable() {
                @Override
                public void run() {
                    adjustImageScaling();
                }
            });
        }

        FPSMeter.meter("PreviewFrame");

        if (mRunOnDraw.isEmpty()) {
            runOnDraw(new Runnable() {
                @Override
                public void run() {
                    mConvertedTextureId = mImageConvertor.convert(data, mImageWidth, mImageHeight);
                    camera.addCallbackBuffer(data);
                }
            });
        }
        else {
            camera.addCallbackBuffer(data);
        }
    }

    @Override
    public void setPreviewSize(int width, int height) {
        if (mCamera == null) {
            return;
        }
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(width, height);
        Camera.Size size = mCamera.getParameters().getPreviewSize();
        mImageWidth = size.width;
        mImageHeight = size.height;

        adjustImageScaling();
    }

    @Override
    public void setupCamera(Camera camera) {
        setupCamera(camera, 0, false, false);
    }

    @Override
    public void setupCamera(final Camera camera, int degrees, boolean flipHor, boolean flipVer) {
        mCamera = camera;
        final Camera.Size size = camera.getParameters().getPreviewSize();
        mImageWidth = size.width;
        mImageHeight = size.height;

        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        runOnDraw(new Runnable() {
            @Override
            public void run() {

                if (mImageConvertor.getConvertType() == GPUImageConvertor.ConvertType.SURFACE_TEXTURE) {
                    if (mSurfaceTexture != null) {
                        mSurfaceTexture.release();
                    }
                    mSurfaceTextureId = createSurfaceTextureID();
                    mSurfaceTexture = new SurfaceTexture(mSurfaceTextureId);

                    try {
                        camera.setPreviewTexture(mSurfaceTexture);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    camera.addCallbackBuffer(new byte[size.width*size.height*3/2]);
                    camera.setPreviewCallbackWithBuffer(GPUImageImpl.this);
                }
                camera.startPreview();
            }
        });

        Rotation rotation = Rotation.NORMAL;
        switch (degrees) {
            case 90:
                rotation = Rotation.ROTATION_90;
                break;
            case 180:
                rotation = Rotation.ROTATION_180;
                break;
            case 270:
                rotation = Rotation.ROTATION_270;
                break;
        }
        setRotation(rotation, flipHor, flipVer);
    }

    @Override
    public void setRotation(Rotation rotation) {
        mRotation = rotation;
        adjustImageScaling();
    }

    @Override
    public void setRotation(Rotation rotation, boolean flipHor, boolean flipVer) {
        mFlipHorizontal = flipHor;
        mFlipVertical = flipVer;
        setRotation(rotation);
    }

    @Override
    public void setFilter(final GPUImageFilter filter) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                final GPUImageFilter oldFilter = mImageFilter;
                mImageFilter = filter;
                if (oldFilter != null) {
                    oldFilter.destroy();
                }
                mImageFilter.init();
                GLES20.glUseProgram(mImageFilter.getProgram());
                mImageFilter.onOutputSizeChanged(mOutputWidth, mOutputHeight);
            }
        });
    }

    @Override
    public void setGLSurfaceView(GLSurfaceView surfaceView) {
        mGLSurfaceView = surfaceView;
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mGLSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        mGLSurfaceView.setRenderer(this);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mGLSurfaceView.requestRender();
    }

    @Override
    public void setBackgroundColor(float red, float green, float blue) {
        mBackgroundRed = red;
        mBackgroundGreen = green;
        mBackgroundBlue = blue;
    }

    @Override
    public void requestRender() {
        if (mGLSurfaceView != null) {
            mGLSurfaceView.requestRender();
        }
    }

    @Override
    public void destroy() {
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
        }
        mImageConvertor.destroy();
        mImageFilter.destroy();
    }

    private void adjustImageScaling() {
        float outputWidth = mOutputWidth;
        float outputHeight = mOutputHeight;
        if (mRotation == Rotation.ROTATION_270 || mRotation == Rotation.ROTATION_90) {
            outputWidth = mOutputHeight;
            outputHeight = mOutputWidth;
        }

        float ratio1 = outputWidth / mImageWidth;
        float ratio2 = outputHeight / mImageHeight;
        float ratioMax = Math.max(ratio1, ratio2);
        int imageWidthNew = Math.round(mImageWidth * ratioMax);
        int imageHeightNew = Math.round(mImageHeight * ratioMax);

        float ratioWidth = imageWidthNew / outputWidth;
        float ratioHeight = imageHeightNew / outputHeight;

        float[] cube = CUBE;
        float[] textureCords = TextureRotationUtil.getRotation(mRotation, mFlipHorizontal, mFlipVertical);
        if (mScaleType == ScaleType.CENTER_CROP) {
            float distHorizontal = (1 - 1 / ratioWidth) / 2;
            float distVertical = (1 - 1 / ratioHeight) / 2;
            textureCords = new float[]{
                    addDistance(textureCords[0], distHorizontal), addDistance(textureCords[1], distVertical),
                    addDistance(textureCords[2], distHorizontal), addDistance(textureCords[3], distVertical),
                    addDistance(textureCords[4], distHorizontal), addDistance(textureCords[5], distVertical),
                    addDistance(textureCords[6], distHorizontal), addDistance(textureCords[7], distVertical),
            };
        } else {
            cube = new float[]{
                    CUBE[0] / ratioHeight, CUBE[1] / ratioWidth,
                    CUBE[2] / ratioHeight, CUBE[3] / ratioWidth,
                    CUBE[4] / ratioHeight, CUBE[5] / ratioWidth,
                    CUBE[6] / ratioHeight, CUBE[7] / ratioWidth,
            };
        }

        mGLCubeBuffer.clear();
        mGLCubeBuffer.put(cube).position(0);
        mGLTextureBuffer.clear();
        mGLTextureBuffer.put(textureCords).position(0);
    }

    private float addDistance(float coordinate, float distance) {
        return coordinate == 0.0f ? distance : 1 - distance;
    }

    private int createSurfaceTextureID()
    {
        int[] texture = new int[1];

        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER,GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        return texture[0];
    }

    private void runAll(Queue<Runnable> queue) {
        synchronized (queue) {
            while (!queue.isEmpty()) {
                queue.poll().run();
            }
        }
    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.add(runnable);
        }
    }

    protected void runOnDrawEnd(final Runnable runnable) {
        synchronized (mRunOnDrawEnd) {
            mRunOnDrawEnd.add(runnable);
        }
    }

    public enum ScaleType { CENTER_INSIDE, CENTER_CROP }
}
