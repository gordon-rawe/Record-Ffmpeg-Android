package rawe.gordon.com.androidrecord.recorder;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import org.bytedeco.javacv.FFmpegFrameFilter;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameFilter;
import org.bytedeco.javacv.FrameRecorder;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import rawe.gordon.com.androidrecord.camera.CameraHelper;
import rawe.gordon.com.androidrecord.utils.DensityUtils;
import rawe.gordon.com.androidrecord.widget.CameraPreviewView;

/**
 * Wechat look like video recorder
 */
public class LivePushRecorder implements Camera.PreviewCallback, CameraPreviewView.PreviewEventListener {

    private static final String TAG = LivePushRecorder.class.getCanonicalName();

    // specify the rtmp url
    private String live_url;
    // specify the picture width and height
    private int imageWidth;
    private int imageHeight;

    // specify the output width and height
    private int outputWidth;
    private int outputHeight;

    private AudioRecordRunnable audioRecordRunnable;
    private Thread audioThread;
    private volatile boolean runAudioThread = true;

    private volatile FFmpegFrameRecorder recorder;

    /**
     * 录制开始时间
     */
    private long startTime;

    private boolean recording;

    /* The number of seconds in the continuous loop (or 0 to disable loop). */
    private Frame yuvImage = null;

    // filter in this project is to transpose clock
    private FFmpegFrameFilter mFrameFilter;
    // preview window
    private CameraPreviewView mCameraPreviewView;

    /**
     * 帧数据处理配置
     */
    private String mFilters;

    public LivePushRecorder(String live_url) {
        this.live_url = live_url;
    }

    public boolean isRecording() {
        return recording;
    }

    /**
     * 设置图片帧的大小
     *
     * @param width
     * @param height
     */
    private void setFrameSize(int width, int height) {
        imageWidth = width;
        imageHeight = height;
    }

    /**
     * 设置输出视频大小
     *
     * @param width
     * @param height
     */
    private void setOutputSize(float width, float height) {
        outputWidth = (int) width;
        outputHeight = (int) height;
    }

    /**
     * initialize ffmpeg_recorder
     */
    private void initRecorder() {
        Log.w(TAG, "init recorder");
        Log.i(TAG, "create yuvImage");
        yuvImage = new Frame(imageWidth, imageHeight, Frame.DEPTH_UBYTE, 2);
        // 初始化时设置录像机的目标视频大小
        recorder = new FFmpegFrameRecorder(live_url, outputWidth, outputHeight, 1);
        if (Build.VERSION.SDK_INT > 10) {
            recorder.setFormat(Constants.OUTPUT_FORMAT);
            recorder.setVideoCodec(Constants.VIDEO_CODEC);
        } else {
            recorder.setFormat(Constants.OUTPUT_FORMAT_OLD);
        }
        recorder.setSampleRate(Constants.SAMPLE_AUDIO_RATE_IN_HZ);
        recorder.setVideoBitrate(Constants.VIDEO_BIT_RATE);
        recorder.setFrameRate(Constants.FRAME_RATE);

        recorder.setVideoOption(Constants.KEY_PRESET, Constants.PRESET);
        recorder.setVideoOption(Constants.KEY_CRF, Constants.CRF);
        recorder.setVideoOption(Constants.KEY_TUNE, Constants.TUNE);

        Log.i(TAG, "recorder initialize success");
        audioRecordRunnable = new AudioRecordRunnable();
        audioThread = new Thread(audioRecordRunnable);
        runAudioThread = true;
    }

    /**
     * 设置帧图像数据处理参数
     *
     * @param filters
     */
    public void setFilters(String filters) {
        mFilters = filters;
    }

    /**
     * 初始化帧过滤器
     */
    private void initFrameFilter() {
        if (TextUtils.isEmpty(mFilters)) {
            mFilters = Constants.generateFilters((int) (imageHeight / Constants.REAL_OUTPUT_RATIO), imageHeight, 0, 0, "clock");
        }

        mFrameFilter = new FFmpegFrameFilter(mFilters, imageWidth, imageHeight);
        mFrameFilter.setPixelFormat(org.bytedeco.javacpp.avutil.AV_PIX_FMT_NV21); // default camera format on Android
    }

    /**
     * 释放帧过滤器
     */
    private void releaseFrameFilter() {
        if (null != mFrameFilter) {
            try {
                mFrameFilter.release();
            } catch (FrameFilter.Exception e) {
                e.printStackTrace();
            }
        }
        mFrameFilter = null;
    }

    public void setLiveUrl(String live_url) {
        this.live_url = live_url;
    }

    public String getLiveUrl() {
        return live_url;
    }

    /**
     * 开始录制
     *
     * @return
     */
    public boolean startRecording() {
        if(live_url==null) return false;
        boolean started = true;
        initRecorder();
        initFrameFilter();
        try {
            recorder.start();
            mFrameFilter.start();
            startTime = System.currentTimeMillis();
            recording = true;
            audioThread.start();
        } catch (Exception e) {
            e.printStackTrace();
            started = false;
        }

        return started;
    }

    public void stopRecording() {
        if (!recording)
            return;

        runAudioThread = false;
        try {
            audioThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        audioRecordRunnable = null;
        audioThread = null;

        if (recorder != null && recording) {
            recording = false;
            Log.v(TAG, "Finishing recording, calling stop and release on recorder");
            try {
                recorder.stop();
                recorder.release();
            } catch (FFmpegFrameRecorder.Exception e) {
                e.printStackTrace();
            }
            recorder = null;

            // 释放帧过滤器
            releaseFrameFilter();
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        try {
            // 去掉必须录制音频的限制，可以录制无声视频
//            if (audioRecord == null || audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
//                startTime = System.currentTimeMillis();
//                return;
//            }

            /* get video data */
            if (yuvImage != null && recording) {
                ((ByteBuffer) yuvImage.image[0].position(0)).put(data);
                try {
                    Log.v(TAG, "Writing Frame");
                    long pastTime = System.currentTimeMillis() - startTime;
                    long t = 1000 * pastTime;
                    if (t > recorder.getTimestamp()) {
                        recorder.setTimestamp(t);
                    }
                    recordFrame(yuvImage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            camera.addCallbackBuffer(data);
        }
    }

    /**
     * 录制帧
     *
     * @throws FrameRecorder.Exception
     */
    private void recordFrame(Frame frame) throws FrameRecorder.Exception, FrameFilter.Exception {
        mFrameFilter.push(frame);
        Frame filteredFrame;
        while ((filteredFrame = mFrameFilter.pull()) != null) {
            recorder.record(filteredFrame);
        }
//        recorder.record(frame);
    }


    public void optimizeCameraSize() {
        // set preview size and make any resize, rotate or
        // reformatting changes here
        Camera.Parameters parameters = mCameraPreviewView.getCamera().getParameters();
        Camera.Size size = null;
        if (Constants.USE_RECTANGLE) {
            size = CameraHelper.getRectanglePreviewSize(parameters.getSupportedPreviewSizes());
        }
        if (size == null)
            size = CameraHelper.getOptimalPreviewSize(parameters.getSupportedPreviewSizes(), (int) Math.min(DensityUtils.getScreenWidth(), DensityUtils.getScreenWidth() / Constants.TARGET_RATIO));
        Log.d(TAG, "OptimalPreviewSize w: " + size.width + "---h: " + size.height);
        Constants.REAL_OUTPUT_RATIO = 1F * size.width / size.height;
        parameters.setPreviewSize(size.width, size.height);
        mCameraPreviewView.getCamera().setParameters(parameters);
        setFrameSize(size.width, size.height);
        setOutputSize(size.width / Constants.SCALE_RATIO, size.height / Constants.SCALE_RATIO);
        mCameraPreviewView.setViewWHRatio(1F * size.width / size.height);
        // 预览尺寸改变，请求重新布局、计算宽高
        mCameraPreviewView.requestLayout();
    }

    /**
     * 设置相机预览视图
     *
     * @param cameraPreviewView
     */
    public void setCameraPreviewView(CameraPreviewView cameraPreviewView) {
        mCameraPreviewView = cameraPreviewView;
        mCameraPreviewView.addPreviewEventListener(this);
        optimizeCameraSize();
    }

    @Override
    public void onPrePreviewStart() {
        Camera camera = mCameraPreviewView.getCamera();
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = parameters.getPreviewSize();
//
//        setFrameSize(size.width, size.height);
//        setOutputSize(size.width / Constants.SCALE_RATIO, size.height / Constants.SCALE_RATIO);
//        mCameraPreviewView.setViewWHRatio(1F * size.width / size.height);
//        mCameraPreviewView.requestLayout();

        camera.setPreviewCallbackWithBuffer(this);
        camera.addCallbackBuffer(new byte[size.width * size.height * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / 8]);
    }

    @Override
    public void onPreviewStarted() {
    }

    @Override
    public void onPreviewFailed() {
    }

    @Override
    public void onAutoFocusComplete(boolean success) {
    }

    //---------------------------------------------
    // audio thread, gets and encodes audio data
    //---------------------------------------------
    private class AudioRecordRunnable implements Runnable {

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            // Audio
            int bufferSize;
            ShortBuffer audioData;
            int bufferReadResult;

            bufferSize = AudioRecord.getMinBufferSize(Constants.SAMPLE_AUDIO_RATE_IN_HZ,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, Constants.SAMPLE_AUDIO_RATE_IN_HZ,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            audioData = ShortBuffer.allocate(bufferSize);
            Log.d(TAG, "audioRecord.startRecording()");
            audioRecord.startRecording();

            /* ffmpeg_audio encoding loop */
            while (runAudioThread) {
                //Log.v(LOG_TAG,"recording? " + recording);
                bufferReadResult = audioRecord.read(audioData.array(), 0, audioData.capacity());
                audioData.limit(bufferReadResult);
                if (bufferReadResult > 0 && recording) {
                    Log.v(TAG, "bufferReadResult: " + bufferReadResult);
                    // If "recording" isn't true when start this thread, it never get's set according to this if statement...!!!
                    // Why?  Good question...
                    try {
                        recorder.recordSamples(audioData);
                        //Log.v(LOG_TAG,"recording " + 1024*i + " to " + 1024*i+1024);
                    } catch (FFmpegFrameRecorder.Exception e) {
                        Log.v(TAG, e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            Log.v(TAG, "AudioThread Finished, release audioRecord");

            /* encoding finish, release recorder */
            audioRecord.stop();
            audioRecord.release();
            Log.v(TAG, "audioRecord released");
        }
    }
}
