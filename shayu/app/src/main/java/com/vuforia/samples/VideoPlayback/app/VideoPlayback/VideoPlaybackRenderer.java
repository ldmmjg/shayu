/*===============================================================================
Copyright (c) 2016,2018 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.vuforia.samples.VideoPlayback.app.VideoPlayback;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.annotation.SuppressLint;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;

import com.vuforia.Device;
import com.vuforia.ImageTarget;
import com.vuforia.Matrix44F;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.TrackableResult;
import com.vuforia.VIDEO_BACKGROUND_REFLECTION;
import com.vuforia.Vec2F;
import com.vuforia.Vec3F;
import com.vuforia.Vuforia;
import com.vuforia.samples.AES;
import com.vuforia.samples.GsonUtil;
import com.vuforia.samples.SampleApplication.SampleApplicationSession;
import com.vuforia.samples.SampleApplication.SampleAppRenderer;
import com.vuforia.samples.SampleApplication.SampleAppRendererControl;
import com.vuforia.samples.SampleApplication.utils.SampleMath;
import com.vuforia.samples.SampleApplication.utils.SampleUtils;
import com.vuforia.samples.SampleApplication.utils.Texture;
import com.vuforia.samples.VideoPlayback.app.VideoPlayback.VideoPlayerHelper.MEDIA_STATE;
import com.vuforia.samples.VideoPlayback.app.VideoPlayback.VideoPlayerHelper.MEDIA_TYPE;


// The renderer class for the VideoPlayback sample.
public class VideoPlaybackRenderer implements GLSurfaceView.Renderer, SampleAppRendererControl
{
    private static final String LOGTAG = "VideoPlaybackRenderer";
    
    SampleApplicationSession vuforiaAppSession;
    SampleAppRenderer mSampleAppRenderer;

    // Video Playback Rendering Specific
    private int videoPlaybackShaderID = 0;
    private int videoPlaybackVertexHandle = 0;
    private int videoPlaybackTexCoordHandle = 0;
    private int videoPlaybackMVPMatrixHandle = 0;
    private int videoPlaybackTexSamplerOESHandle = 0;
    
    // Video Playback Textures for the two targets
    int videoPlaybackTextureID[] = new int[VideoPlayback.NUM_TARGETS];
    
    // Keyframe and icon rendering specific
    private int keyframeShaderID = 0;
    private int keyframeVertexHandle = 0;
    private int keyframeTexCoordHandle = 0;
    private int keyframeMVPMatrixHandle = 0;
    private int keyframeTexSampler2DHandle = 0;
    
    // We cannot use the default texture coordinates of the quad since these
    //    // will change depending on the video itself
    private float videoQuadTextureCoords[] = { 0.0f, 0.0f, 0.5f, 0.0f, 0.5f, 1.0f, 0.0f, 1.0f, };

    
    // This variable will hold the transformed coordinates (changes every frame)
    private float videoQuadTextureCoordsTransformedStones[] = { 0.0f, 0.0f,
            1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, };
    private final float mVideoTwoW[] = { 0.0f, 0.0f,
            1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, };
    private float videoQuadTextureCoordsTransformedChips[] = { 0.0f, 0.0f,
            1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, };
    
    // Trackable dimensions
    Vec3F targetPositiveDimensions[] = new Vec3F[VideoPlayback.NUM_TARGETS];
    
    static int NUM_QUAD_VERTEX = 4;
    static int NUM_QUAD_INDEX = 6;
    
    double quadVerticesArray[] = {
            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            1.0f, 1.0f, 0.0f,
            -1.0f, 1.0f, 0.0f };
    
    double quadTexCoordsArray[] = { 0.0f, 0.0f, 0.5f, 0.0f, 0.5f, 1.0f, 0.0f, 1.0f };
    
    double quadNormalsArray[] = { 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, };
    
    short quadIndicesArray[] = { 0, 1, 2, 2, 3, 0 };
    
    Buffer quadVertices, quadTexCoords, quadIndices, quadNormals;
    
    private boolean mIsActive = false;
    private Matrix44F tappingProjectionMatrix = null;
    
    private float[][] mTexCoordTransformationMatrix = null;

    private float[][] mVideoArrsyMatrix = null;

    float mWidth = 1080;
    float mHeight = 1920;

    private VideoPlayerHelper mVideoPlayerHelper[] = null;
    private String mMovieName[] = null;
    private MEDIA_TYPE mCanRequestType[] = null;
    private int mSeekPosition[] = null;
    private boolean mShouldPlayImmediately[] = null;
    private long mLostTrackingSince[] = null;
    private boolean mLoadRequested[] = null;


    private final float[] mMatrix44F =
            {1.0f,0.0f,0.0f,0.0f,
            0.0f,1.0f,0.0f,0.0f,
            0.0f,0.0f,1.0f,0.0f,
            0.0f,0.0f,0.0f,1.0f};
    VideoPlayback mActivity;

    // Needed to calculate whether a screen tap is inside the target
    Matrix44F modelViewMatrix[] = new Matrix44F[VideoPlayback.NUM_TARGETS];
    
//    private Vector<Texture> mTextures;
    
    boolean isTracking[] = new boolean[VideoPlayback.NUM_TARGETS];
    MEDIA_STATE currentStatus[] = new MEDIA_STATE[VideoPlayback.NUM_TARGETS];
    
    // These hold the aspect ratio of both the video and the
    // keyframe
    float videoQuadAspectRatio[] = new float[VideoPlayback.NUM_TARGETS];
    float keyframeQuadAspectRatio[] = new float[VideoPlayback.NUM_TARGETS];
    
    
    public VideoPlaybackRenderer(VideoPlayback activity,
        SampleApplicationSession session)
    {
        
        mActivity = activity;
        vuforiaAppSession = session;

        // SampleAppRenderer used to encapsulate the use of RenderingPrimitives setting
        // the device mode AR/VR and stereo mode
        mSampleAppRenderer = new SampleAppRenderer(this, mActivity, Device.MODE.MODE_AR, false, 0.01f, 5f);

        // Create an array of the size of the number of targets we have
        mVideoPlayerHelper = new VideoPlayerHelper[VideoPlayback.NUM_TARGETS];
        mMovieName = new String[VideoPlayback.NUM_TARGETS];
        mCanRequestType = new MEDIA_TYPE[VideoPlayback.NUM_TARGETS];
        mSeekPosition = new int[VideoPlayback.NUM_TARGETS];
        mShouldPlayImmediately = new boolean[VideoPlayback.NUM_TARGETS];
        mLostTrackingSince = new long[VideoPlayback.NUM_TARGETS];
        mLoadRequested = new boolean[VideoPlayback.NUM_TARGETS];
        mTexCoordTransformationMatrix = new float[VideoPlayback.NUM_TARGETS][16];
        mVideoArrsyMatrix = new float[VideoPlayback.NUM_TARGETS][];
        // Initialize the arrays to default values
        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++)
        {
            mVideoPlayerHelper[i] = null;
            mMovieName[i] = "";
            mCanRequestType[i] = MEDIA_TYPE.ON_TEXTURE_FULLSCREEN;
            mSeekPosition[i] = 0;
            mShouldPlayImmediately[i] = false;
            mLostTrackingSince[i] = -1;
            mLoadRequested[i] = false;
        }
        
        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++)
            targetPositiveDimensions[i] = new Vec3F();
        
        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++)
            modelViewMatrix[i] = new Matrix44F();
    }
    
    
    // Store the Player Helper object passed from the main activity
    public void setVideoPlayerHelper(int target,
        VideoPlayerHelper newVideoPlayerHelper)
    {
        mVideoPlayerHelper[target] = newVideoPlayerHelper;
    }
    
    
    public void requestLoad(int target, String movieName, int seekPosition,
        boolean playImmediately)
    {
        mMovieName[target] = movieName;
        mSeekPosition[target] = seekPosition;
        mShouldPlayImmediately[target] = playImmediately;
        mLoadRequested[target] = true;
    }
    
    
    // Called when the surface is created or recreated.
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        // Call function to initialize rendering:
        // The video texture is also created on this step
        initRendering();
        
        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        Vuforia.onSurfaceCreated();

        mSampleAppRenderer.onSurfaceCreated();

        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++)
        {
            
            if (mVideoPlayerHelper[i] != null)
            {
                // The VideoPlayerHelper needs to setup a surface texture given
                // the texture id
                // Here we inform the video player that we would like to play
                // the movie
                // both on texture and on full screen
                // Notice that this does not mean that the platform will be able
                // to do what we request
                // After the file has been loaded one must always check with
                // isPlayableOnTexture() whether
                // this can be played embedded in the AR scene
                if (!mVideoPlayerHelper[i]
                    .setupSurfaceTexture(videoPlaybackTextureID[i]))
                    mCanRequestType[i] = MEDIA_TYPE.FULLSCREEN;
                else
                    mCanRequestType[i] = MEDIA_TYPE.ON_TEXTURE_FULLSCREEN;
                
                // And now check if a load has been requested with the
                // parameters passed from the main activity
                if (mLoadRequested[i])
                {
                    mVideoPlayerHelper[i].load(mMovieName[i],
                        mCanRequestType[i], mShouldPlayImmediately[i],
                        mSeekPosition[i]);
                    mLoadRequested[i] = false;
                }
            }
        }
    }
    
    
    // Called when the surface changed size.
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        // Call Vuforia function to handle render surface size changes:
        Vuforia.onSurfaceChanged(width, height);
        Log.d("zhujian", "onSurfaceChanged width : "+width +"_height_"+height);
        mWidth = width;
        mHeight = height;
        // RenderingPrimitives to be updated when some rendering change is done
        mSampleAppRenderer.onConfigurationChanged(mIsActive);

        // Upon every on pause the movie had to be unloaded to release resources
        // Thus, upon every surface create or surface change this has to be
        // reloaded
        // See:
        // http://developer.android.com/reference/android/media/MediaPlayer.html#release()
        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++)
        {
            if (mLoadRequested[i] && mVideoPlayerHelper[i] != null)
            {
                mVideoPlayerHelper[i].load(mMovieName[i], mCanRequestType[i],
                    mShouldPlayImmediately[i], mSeekPosition[i]);
                mLoadRequested[i] = false;
            }
        }
    }
    
    
    // Called to draw the current frame.
    public void onDrawFrame(GL10 gl)
    {
        if (!mIsActive)
            return;
        
        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++)
        {
            if (mVideoPlayerHelper[i] != null)
            {
                if (mVideoPlayerHelper[i].isPlayableOnTexture())
                {
                    // First we need to update the video data. This is a built
                    // in Android call
                    // Here, the decoded data is uploaded to the OES texture
                    // We only need to do this if the movie is playing
                    if (mVideoPlayerHelper[i].getStatus() == MEDIA_STATE.PLAYING)
                    {
                        mVideoPlayerHelper[i].updateVideoData();
                    }
                    
                    // According to the Android API
                    // (http://developer.android.com/reference/android/graphics/SurfaceTexture.html)
                    // transforming the texture coordinates needs to happen
                    // every frame.
                    mVideoPlayerHelper[i].getSurfaceTextureTransformMatrix(mTexCoordTransformationMatrix[i]);
                    mVideoArrsyMatrix[i] = mVideoTwoW.clone();
                    setVideoDimensions(i,
                        mVideoPlayerHelper[i].getVideoWidth(),
                        mVideoPlayerHelper[i].getVideoHeight(),
                            mTexCoordTransformationMatrix[i]);
                }
                setStatus(i, mVideoPlayerHelper[i].getStatus().getNumericType());
            }
        }

        // Call our function to render content from SampleAppRenderer class
        mSampleAppRenderer.render();
        
        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++)
        {
            // Ask whether the target is currently being tracked and if so react
            //            // to it
            if (isTracking(i))
            {
                // If it is tracking reset the timestamp for lost tracking
                mLostTrackingSince[i] = -1;
            } else
            {
                // If it isn't tracking
                // check whether it just lost it or if it's been a while
//                if (mLostTrackingSince[i] < 0)
//                    mLostTrackingSince[i] = SystemClock.uptimeMillis();
//                else
//                {
//                    // If it's been more than 2 seconds then pause the player
//                    if ((SystemClock.uptimeMillis() - mLostTrackingSince[i]) > 2000)
//                    {
//                        if (mVideoPlayerHelper[i] != null)
//                            mVideoPlayerHelper[i].pause();
//                    }
//                }
            }
        }
        
        // If you would like the video to start playing as soon as it starts
        // tracking
        // and pause as soon as tracking is lost you can do that here by
        // commenting
        // the for-loop above and instead checking whether the isTracking()
        // value has
        // changed since the last frame. Notice that you need to be careful not
        // to
        // trigger automatic playback for fullscreen since that will be
        // inconvenient
        // for your users.
        
    }


    public void setActive(boolean active)
    {
        mIsActive = active;

        if(mIsActive)
            mSampleAppRenderer.configureVideoBackground();
    }


    @SuppressLint("InlinedApi")
    void initRendering()
    {
        Log.d(LOGTAG, "VideoPlayback VideoPlaybackRenderer initRendering" + Vuforia.requiresAlpha());
        // Define clear color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
            : 1.0f);

        // Now we create the texture for the video data from the movie
        // IMPORTANT:
        // Notice that the textures are not typical GL_TEXTURE_2D textures
        // but instead are GL_TEXTURE_EXTERNAL_OES extension textures
        // This is required by the Android SurfaceTexture
        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++)
        {
            GLES20.glGenTextures(1, videoPlaybackTextureID, i);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                videoPlaybackTextureID[i]);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        }
        String s  =  AES.decrypt(VideoPlaybackShaders.FRAGMENT_SHADER_EXT_TRANS);
        // The first shader is the one that will display the video data of the
        // movie
        // (it is aware of the GL_TEXTURE_EXTERNAL_OES extension)
        videoPlaybackShaderID = SampleUtils.createProgramFromShaderSrc(
            VideoPlaybackShaders.VIDEO_PLAYBACK_VERTEX_SHADER,
                AES.decrypt(VideoPlaybackShaders.FRAGMENT_SHADER_EXT_TRANS));
//            videoPlaybackShaderID = SampleUtils.createProgramFromShaderSrc(
//            VideoPlaybackShaders.VIDEO_PLAYBACK_VERTEX_SHADER,
//            VideoPlaybackShaders.VIDEO_PLAYBACK_FRAGMENT_SHADER);
        videoPlaybackVertexHandle = GLES20.glGetAttribLocation(
            videoPlaybackShaderID, "vertexPosition");
        videoPlaybackTexCoordHandle = GLES20.glGetAttribLocation(
            videoPlaybackShaderID, "vertexTexCoord");
        videoPlaybackMVPMatrixHandle = GLES20.glGetUniformLocation(
            videoPlaybackShaderID, "modelViewProjectionMatrix");
        videoPlaybackTexSamplerOESHandle = GLES20.glGetUniformLocation(
            videoPlaybackShaderID, "texSamplerOES");
        
        // This is a simpler shader with regular 2D textures
        keyframeShaderID = SampleUtils.createProgramFromShaderSrc(
            KeyFrameShaders.KEY_FRAME_VERTEX_SHADER,
            KeyFrameShaders.KEY_FRAME_FRAGMENT_SHADER);
        keyframeVertexHandle = GLES20.glGetAttribLocation(keyframeShaderID,
            "vertexPosition");
        keyframeTexCoordHandle = GLES20.glGetAttribLocation(keyframeShaderID,
            "vertexTexCoord");
        keyframeMVPMatrixHandle = GLES20.glGetUniformLocation(keyframeShaderID,
            "modelViewProjectionMatrix");
        keyframeTexSampler2DHandle = GLES20.glGetUniformLocation(
            keyframeShaderID, "texSampler2D");

        keyframeQuadAspectRatio[0] = 1.0f;
        keyframeQuadAspectRatio[1] = 1.0f;

        quadVertices = fillBuffer(quadVerticesArray);
        quadTexCoords = fillBuffer(quadTexCoordsArray);
        quadIndices = fillBuffer(quadIndicesArray);
        quadNormals = fillBuffer(quadNormalsArray);
        
    }
    
    
    private Buffer fillBuffer(double[] array)
    {
        // Convert to floats because OpenGL doesnt work on doubles, and manually
        // casting each input value would take too much time.
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length); // each float takes 4 bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (double d : array)
            bb.putFloat((float) d);
        bb.rewind();
        
        return bb;
        
    }
    
    
    private Buffer fillBuffer(short[] array)
    {
        ByteBuffer bb = ByteBuffer.allocateDirect(2 * array.length); // each
                                                                     // short
                                                                     // takes 2
                                                                     // bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (short s : array)
            bb.putShort(s);
        bb.rewind();
        
        return bb;
        
    }
    
    
    private Buffer fillBuffer(float[] array)
    {
        // Convert to floats because OpenGL doesnt work on doubles, and manually
        // casting each input value would take too much time.
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length); // each float takes 4 bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (float d : array)
            bb.putFloat(d);
        bb.rewind();
        
        return bb;
        
    }


    public void updateRenderingPrimitives()
    {
        mSampleAppRenderer.updateRenderingPrimitives();
    }

    boolean isplay = false;

    float[] modelViewMatrixVideo;
    int currentTarget;
    @SuppressLint("InlinedApi")
    // The render function called from SampleAppRendering by using RenderingPrimitives views.
    // The state is owned by SampleAppRenderer which is controlling it's lifecycle.
    // State should not be cached outside this method.
    public void renderFrame(State state, float[] projectionMatrix)
    {

        // Renders video background replacing Renderer.DrawVideoBackground()
        mSampleAppRenderer.renderVideoBackground(state);
        
//        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // We must detect if background reflection is active and adjust the
        // culling direction.
        // If the reflection is active, this means the post matrix has been
        // reflected as well,
        // therefore standard counter clockwise face culling will result in
        // "inside out" models.
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);

        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        if(tappingProjectionMatrix == null)
        {
            tappingProjectionMatrix = new Matrix44F();
            tappingProjectionMatrix.setData(projectionMatrix);
        }

        float temp[] = { 0.0f, 0.0f, 0.0f };
        for (int i = 0; i < VideoPlayback.NUM_TARGETS; i++)
        {
            isTracking[i] = false;
            targetPositiveDimensions[i].setData(temp);
        }

        if (isplay){


            GLES20.glUseProgram(videoPlaybackShaderID);

            // Prepare for rendering the keyframe
            GLES20.glVertexAttribPointer(videoPlaybackVertexHandle, 3,
                    GLES20.GL_FLOAT, false, 0, quadVertices);

            GLES20.glVertexAttribPointer(videoPlaybackTexCoordHandle,
                    2, GLES20.GL_FLOAT, false, 0,
                    fillBuffer(mVideoArrsyMatrix[currentTarget]));


            GLES20.glEnableVertexAttribArray(videoPlaybackVertexHandle);
            GLES20.glEnableVertexAttribArray(videoPlaybackTexCoordHandle);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

            // IMPORTANT:
            // Notice here that the texture that we are binding is not the
            // typical GL_TEXTURE_2D but instead the GL_TEXTURE_EXTERNAL_OES
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    videoPlaybackTextureID[currentTarget]);
            GLES20.glUniformMatrix4fv(videoPlaybackMVPMatrixHandle, 1,
                    false, modelViewMatrixVideo, 0);
            GLES20.glUniform1i(videoPlaybackTexSamplerOESHandle, 0);

            // Render
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, NUM_QUAD_INDEX,
                    GLES20.GL_UNSIGNED_SHORT, quadIndices);

            GLES20.glDisableVertexAttribArray(videoPlaybackVertexHandle);
            GLES20.glDisableVertexAttribArray(videoPlaybackTexCoordHandle);


            GLES20.glUseProgram(0);

            SampleUtils.checkGLError("VideoPlayback renderFrame");

            if (mVideoPlayerHelper[currentTarget].getStatus() == MEDIA_STATE.REACHED_END){
                mVideoPlayerHelper[currentTarget].pause();
                isplay = false;
                modelViewMatrixVideo = null;
                Log.d("zhujian", " mMatrix44F 赋值 renderFrame: " + GsonUtil.GsonString(mMatrix44F));
                Log.d("zhujian", "modelViewMatrixVideo renderFrame: " + GsonUtil.GsonString(modelViewMatrixVideo));
            }

        }else {

        // Did we find any trackables this frame?
         for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++)
        {
            // Get the trackable:
            TrackableResult trackableResult = state.getTrackableResult(tIdx);
            
            ImageTarget imageTarget = (ImageTarget) trackableResult
                .getTrackable();
            

            // We store the modelview matrix to be used later by the tap
            // calculation
            if (imageTarget.getName().compareTo("yuewanggoujian") == 0)
                currentTarget = 0;
            else
                currentTarget = 1;
            
            modelViewMatrix[currentTarget] = Tool
                .convertPose2GLMatrix(trackableResult.getPose());
            
            isTracking[currentTarget] = true;
            
            targetPositiveDimensions[currentTarget] = imageTarget.getSize();
            
            // The pose delivers the center of the target, thus the dimensions
            // go from -width/2 to width/2, same for height
            float w = mVideoPlayerHelper[currentTarget].getVideoWidth() / 2f;
            float h = mVideoPlayerHelper[currentTarget].getVideoHeight();
            temp[0] = 1.0f;

            temp[1] = proportion(w,h);
            targetPositiveDimensions[currentTarget].setData(temp);
            Log.d("zhujian", "赋值 temp: " + GsonUtil.GsonString(temp));
            if (modelViewMatrixVideo == null){
                modelViewMatrixVideo = mMatrix44F.clone();
                Log.d("zhujian", "赋值 renderFrame: " + GsonUtil.GsonString(modelViewMatrixVideo));
            }
//            if (temp[1] < 1.78f){
                Matrix.scaleM(modelViewMatrixVideo, 0, 1f,
                        temp[1],
                        1f);
//            }
            Log.d("zhujian", "renderFrame: " + GsonUtil.GsonString(modelViewMatrixVideo));
            GLES20.glUseProgram(videoPlaybackShaderID);


            // Prepare for rendering the keyframe
            GLES20.glVertexAttribPointer(videoPlaybackVertexHandle, 3,
                GLES20.GL_FLOAT, false, 0, quadVertices);


            GLES20.glVertexAttribPointer(videoPlaybackTexCoordHandle,
                    2, GLES20.GL_FLOAT, false, 0,
                    fillBuffer(mVideoArrsyMatrix[currentTarget]));

            GLES20.glEnableVertexAttribArray(videoPlaybackVertexHandle);
            GLES20.glEnableVertexAttribArray(videoPlaybackTexCoordHandle);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

            // IMPORTANT:
            // Notice here that the texture that we are binding is not the
            // typical GL_TEXTURE_2D but instead the GL_TEXTURE_EXTERNAL_OES
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                videoPlaybackTextureID[currentTarget]);
            GLES20.glUniformMatrix4fv(videoPlaybackMVPMatrixHandle, 1,
                false, modelViewMatrixVideo, 0);
            GLES20.glUniform1i(videoPlaybackTexSamplerOESHandle, 0);

            // Render
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, NUM_QUAD_INDEX,
                GLES20.GL_UNSIGNED_SHORT, quadIndices);

            GLES20.glDisableVertexAttribArray(videoPlaybackVertexHandle);
            GLES20.glDisableVertexAttribArray(videoPlaybackTexCoordHandle);


            GLES20.glUseProgram(0);


            SampleUtils.checkGLError("VideoPlayback renderFrame");
            if (!isplay){
                isplay = true;
                mVideoPlayerHelper[currentTarget].play(false,0);
            }
        }
        }

        GLES20.glDepthFunc(GLES20.GL_LESS);
//        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_BLEND);

        Renderer.getInstance().end();
        
    }
    
    private float proportion(float videoW,float videoH){
       return (mWidth / videoW * videoH )/ mHeight;
    }

    boolean isTapOnScreenInsideTarget(int target, float x, float y)
    {
        // Here we calculate that the touch event is inside the target
        Vec3F intersection;
        // Vec3F lineStart = new Vec3F();
        // Vec3F lineEnd = new Vec3F();
        
        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        intersection = SampleMath.getPointToPlaneIntersection(SampleMath
            .Matrix44FInverse(tappingProjectionMatrix),
            modelViewMatrix[target], metrics.widthPixels, metrics.heightPixels,
            new Vec2F(x, y), new Vec3F(0, 0, 0), new Vec3F(0, 0, 1));
        
        // The target returns as pose the center of the trackable. The following
        // if-statement simply checks that the tap is within this range
        if ((intersection.getData()[0] >= -(targetPositiveDimensions[target]
            .getData()[0]))
            && (intersection.getData()[0] <= (targetPositiveDimensions[target]
                .getData()[0]))
            && (intersection.getData()[1] >= -(targetPositiveDimensions[target]
                .getData()[1]))
            && (intersection.getData()[1] <= (targetPositiveDimensions[target]
                .getData()[1])))
            return true;
        else
            return false;
    }
    
    
    void setVideoDimensions(int target, float videoWidth, float videoHeight,
        float[] textureCoordMatrix)
    {
        // The quad originaly comes as a perfect square, however, the video
        // often has a different aspect ration such as 4:3 or 16:9,
        // To mitigate this we have two options:
        // 1) We can either scale the width (typically up)
        // 2) We can scale the height (typically down)
        // Which one to use is just a matter of preference. This example scales
        // the height down.    4 / 3;
        // (see the render call in renderFrame)
        float width = videoWidth / 2;
        videoQuadAspectRatio[target] = videoHeight / width;
        
        float mtx[] = textureCoordMatrix;
        float tempUVMultRes[] = new float[2];
        tempUVMultRes = uvMultMat4f(
                mVideoArrsyMatrix[target][0],
                mVideoArrsyMatrix[target][1],
            videoQuadTextureCoords[0], videoQuadTextureCoords[1], mtx);
        mVideoArrsyMatrix[target][0] = tempUVMultRes[0];
        mVideoArrsyMatrix[target][1] = tempUVMultRes[1];
        tempUVMultRes = uvMultMat4f(
                mVideoArrsyMatrix[target][2],
                mVideoArrsyMatrix[target][3],
            videoQuadTextureCoords[2], videoQuadTextureCoords[3], mtx);
        mVideoArrsyMatrix[target][2] = tempUVMultRes[0];
        mVideoArrsyMatrix[target][3] = tempUVMultRes[1];
        tempUVMultRes = uvMultMat4f(
                mVideoArrsyMatrix[target][4],
                mVideoArrsyMatrix[target][5],
            videoQuadTextureCoords[4], videoQuadTextureCoords[5], mtx);
        mVideoArrsyMatrix[target][4] = tempUVMultRes[0];
        mVideoArrsyMatrix[target][5] = tempUVMultRes[1];
        tempUVMultRes = uvMultMat4f(
                mVideoArrsyMatrix[target][6],
                mVideoArrsyMatrix[target][7],
            videoQuadTextureCoords[6], videoQuadTextureCoords[7], mtx);
        mVideoArrsyMatrix[target][6] = tempUVMultRes[0];
        mVideoArrsyMatrix[target][7] = tempUVMultRes[1];

//         textureCoordMatrix = mtx;
    }
    
    
    // Multiply the UV coordinates by the given transformation matrix
    float[] uvMultMat4f(float transformedU, float transformedV, float u,
        float v, float[] pMat)
    {
        float x = pMat[0] * u + pMat[4] * v /* + pMat[ 8]*0.f */+ pMat[12]
            * 1.f;
        float y = pMat[1] * u + pMat[5] * v /* + pMat[ 9]*0.f */+ pMat[13]
            * 1.f;
        // float z = pMat[2]*u + pMat[6]*v + pMat[10]*0.f + pMat[14]*1.f; // We
        // dont need z and w so we comment them out
        // float w = pMat[3]*u + pMat[7]*v + pMat[11]*0.f + pMat[15]*1.f;
        
        float result[] = new float[2];
        // transformedU = x;
        // transformedV = y;
        result[0] = x;
        result[1] = y;
        return result;
    }
    
    
    void setStatus(int target, int value)
    {
        // Transform the value passed from java to our own values
        switch (value)
        {
            case 0:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.REACHED_END;
                break;
            case 1:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.PAUSED;
                break;
            case 2:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.STOPPED;
                break;
            case 3:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.PLAYING;
                break;
            case 4:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.READY;
                break;
            case 5:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.NOT_READY;
                break;
            case 6:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.ERROR;
                break;
            default:
                currentStatus[target] = VideoPlayerHelper.MEDIA_STATE.NOT_READY;
                break;
        }
    }
    
    
    boolean isTracking(int target)
    {
        return isTracking[target];
    }
    
    
    public void setTextures(Vector<Texture> textures)
    {
//        mTextures = textures;
    }
    
}
