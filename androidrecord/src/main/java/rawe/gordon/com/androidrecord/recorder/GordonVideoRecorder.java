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

import rawe.gordon.com.androidrecord.utils.FileUtil;
import rawe.gordon.com.androidrecord.widget.CameraPreviewView;

import static org.bytedeco.javacpp.avcodec.AV_CODEC_ID_H264;

/**
 * 仿微信录像机
 */
public class GordonVideoRecorder implements Camera.PreviewCallback, CameraPreviewView.PreviewEventListener {

    private static final String TAG = GordonVideoRecorder.class.getCanonicalName();
    // 帧率
    private static final int FRAME_RATE = 28;
    // 声音采样率
    private static final int SAMPLE_AUDIO_RATE_IN_HZ = 44100;
    // 输出文件目录
    private final String mFolder;
    // 输出文件路径
    private String strFinalPath;
    // 图片帧宽、高
    private int imageWidth;//= 1280;
    private int imageHeight;// = 720;
    // 输出视频宽、高
    private int outputWidth;//= 1280;
    private int outputHeight;//= 720;

    /* audio data getting thread */
    private AudioRecord audioRecord;
    private AudioRecordRunnable audioRecordRunnable;
    private Thread audioThread;
    private volatile boolean runAudioThread = true;

    private volatile FFmpegFrameRecorder recorder;

    /**
     * 录制开始时间
     */
    private long startTime;

    private boolean recording;

    /* The number of seconds in the continuous com.ctrip.gs.video.record.record loop (or 0 to disable loop). */
    private Frame yuvImage = null;

    // 图片帧过滤器
    private FFmpegFrameFilter mFrameFilter;
    // 相机预览视图
    private CameraPreviewView mCameraPreviewView;

    /**
     * 帧数据处理配置
     */
    private String mFilters;

    public GordonVideoRecorder(String folder) {
        mFolder = folder;
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
    public void setFrameSize(int width, int height) {
        imageWidth = width;
        imageHeight = height;
    }

    /**
     * 设置输出视频大小
     *
     * @param width
     * @param height
     */
    public void setOutputSize(int width, int height) {
        outputWidth = width;
        outputHeight = height;
    }

    /**
     * initialize ffmpeg_recorder
     */
    private void initRecorder() {
        Log.w(TAG, "init recorder");

        yuvImage = new Frame(imageWidth, imageHeight, Frame.DEPTH_UBYTE, 2);
        Log.i(TAG, "create yuvImage");
        strFinalPath = FileUtil.createFilePath(mFolder, null, Long.toString(System.currentTimeMillis()));
        // 初始化时设置录像机的目标视频大小
        recorder = new FFmpegFrameRecorder(strFinalPath, outputWidth, outputHeight, 1);
        recorder.setFormat(Build.VERSION.SDK_INT > 10 ? "flv" : "3gp");
        recorder.setVideoCodec(AV_CODEC_ID_H264);
        recorder.setSampleRate(SAMPLE_AUDIO_RATE_IN_HZ);
        recorder.setVideoOption("preset", "faster");
        recorder.setVideoOption("crf", "14");
        recorder.setVideoOption("tune", "zerolatency");
        recorder.setVideoBitrate(2000000);
        // Set in the surface changed method
        recorder.setFrameRate(FRAME_RATE);
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
     * 生成处理配置
     *
     * @param w         裁切宽度
     * @param h         裁切高度
     * @param x         裁切起始x坐标
     * @param y         裁切起始y坐标
     * @param transpose 图像旋转参数
     * @return 帧图像数据处理参数
     */
    public static String generateFilters(int w, int h, int x, int y, String transpose) {
        return String.format("crop=w=%d:h=%d:x=%d:y=%d,transpose=%s", w, h, x, y, transpose);
    }

    /**
     * 初始化帧过滤器
     */
    private void initFrameFilter() {
        if (TextUtils.isEmpty(mFilters)) {
            mFilters = generateFilters((int) (1f * outputHeight / outputWidth * imageHeight), imageHeight, 0, 0, "clock");
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

    /**
     * 获取视频文件路径
     *
     * @return
     */
    public String getFilePath() {
        return strFinalPath;
    }

    /**
     * 开始录制
     *
     * @return
     */
    public boolean startRecording() {
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

    /**
     * 设置相机预览视图
     *
     * @param cameraPreviewView
     */
    public void setCameraPreviewView(CameraPreviewView cameraPreviewView) {
        mCameraPreviewView = cameraPreviewView;
        mCameraPreviewView.addPreviewEventListener(this);

    }

    @Override
    public void onPrePreviewStart() {
        Camera camera = mCameraPreviewView.getCamera();
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = parameters.getPreviewSize();
        // 设置Recorder处理的的图像帧大小
//        setFrameSize(size.width, size.height);
        setFrameSize(size.width, size.height);
        setOutputSize(size.width / 2, size.height / 2);
        mCameraPreviewView.setViewWHRatio(1F * size.width / size.height);
        mCameraPreviewView.requestLayout();
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
    class AudioRecordRunnable implements Runnable {

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            // Audio
            int bufferSize;
            ShortBuffer audioData;
            int bufferReadResult;

            bufferSize = AudioRecord.getMinBufferSize(SAMPLE_AUDIO_RATE_IN_HZ,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_AUDIO_RATE_IN_HZ,
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
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
                Log.v(TAG, "audioRecord released");
            }
        }
    }
}
