package com.wuta.gpuimage.exfilters;

import android.graphics.Bitmap;
import android.opengl.GLES20;

import com.wuta.gpuimage.GPUImageFilter;
import com.wuta.gpuimage.util.OpenGlUtils;

import java.nio.FloatBuffer;

/**
 * Created by binarymelody on 16/5/12.
 */
public class GPUImageDrawFilter extends GPUImageFilter {

    protected int mPictureTexture = OpenGlUtils.NO_TEXTURE;
    protected Bitmap mPicture;

    public GPUImageDrawFilter() {
        super();
    }

    public void setPicture(final Bitmap picture) {
        if (picture != null && picture.isRecycled()) {
            return;
        }
        mPicture = picture;
        if (mPicture == null) {
            return;
        }

        if (mPictureTexture == OpenGlUtils.NO_TEXTURE) {
            if (picture == null || picture.isRecycled()) {
                return;
            }
            GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
            mPictureTexture = OpenGlUtils.loadTexture(picture, OpenGlUtils.NO_TEXTURE, false);
        }
    }

    private int getPictureTexture() { return mPictureTexture; }

    public void onDrawPicture(final FloatBuffer triangleBuffer, final FloatBuffer textureBuffer,
                              int shapes) {
        GLES20.glUseProgram(getProgram());
        runPendingOnDrawTasks();
        if (!mIsInitialized) {
            return;
        }

        triangleBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, triangleBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);

        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);

        if (getPictureTexture() != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, getPictureTexture());
            GLES20.glUniform1i(mGLUniformTexture, 4);
        }

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);


        onDrawArraysPre();

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, shapes * 3);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }
}
