package com.wuta.gpuimage;

/**
 * Created by kejin
 * on 2016/5/7.
 */

import android.opengl.GLES20;
import android.util.Log;

/**
 * 表示一个 FBO
 */
public class FrameBuffer
{
    public final static String TAG = "FrameBuffer";

    private int mFrameBufferId = 0;
    private int mFrameBufferTextureId = 0;
    private int mDepthRenderBufferId = 0;

    private int mWidth = 0;
    private int mHeight = 0;

    public boolean create(int width, int height)
    {
        int [] frameBuffer = new int[1];
        int [] frameBufferTexture = new int[1];
        int [] depthRenderBuffer = new int[1];

        // generate frame buffer
        GLES20.glGenFramebuffers(1, frameBuffer, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);

        // generate depth buffer
        GLES20.glGenRenderbuffers(1, depthRenderBuffer, 0);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthRenderBuffer[0]);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT, width, height);

        // attach frame buffer
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER,
                GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, depthRenderBuffer[0]);

        // generate texture
        GLES20.glGenTextures(1, frameBufferTexture, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTexture[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        // set texture as colour attachment
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, frameBufferTexture[0], 0);

        // unbind
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        mFrameBufferId = frameBuffer[0];
        mFrameBufferTextureId = frameBufferTexture[0];
        mDepthRenderBufferId = depthRenderBuffer[0];

        mWidth = width;
        mHeight = height;

        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "create framebuffer failed");
            return false;
        }
        Log.e(TAG, "create framebuffer success!");
        return true;
    }

    public void beginDrawToFrameBuffer() {
//        create(mWidth, mHeight);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferId);

//        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mWidth, mHeight, 0,
//                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
//        GLES20.glViewport(0, 0, mWidth, mHeight);
        GLES20.glClearColor(0, 0, 0, 0);
    }

    public void showFrameBuffer() {
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
//        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void drawToFrameBuffer(Runnable runnable)
    {
        beginDrawToFrameBuffer();
        runnable.run();
        showFrameBuffer();
    }

    public int getFrameBufferId()
    {
        return mFrameBufferId;
    }

    public int getFrameBufferTextureId()
    {
        return mFrameBufferTextureId;
    }

    public void destroy()
    {
        GLES20.glDeleteTextures(1, new int[]{mFrameBufferTextureId}, 0);
        GLES20.glDeleteRenderbuffers(1, new int[]{mDepthRenderBufferId}, 0);
        GLES20.glDeleteBuffers(1, new int[]{mFrameBufferId}, 0);
    }
}
