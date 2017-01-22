package rawe.gordon.com.androidrecord.activities;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.List;

import rawe.gordon.com.androidrecord.PressRecordView;
import rawe.gordon.com.androidrecord.R;
import rawe.gordon.com.androidrecord.camera.CameraHelper;
import rawe.gordon.com.androidrecord.recorder.GordonVideoRecorder;
import rawe.gordon.com.androidrecord.utils.DensityUtils;
import rawe.gordon.com.androidrecord.utils.FileUtil;
import rawe.gordon.com.androidrecord.widget.CameraPreviewView;

/**
 * 视频录制页面
 *
 * @author Gordon Rawe
 */
public class NewRecordVideoActivity extends Activity implements View.OnClickListener {

    private static final String TAG = NewRecordVideoActivity.class.getCanonicalName();
    //摄像机
    private Camera camera;
    //一个控件
    private GordonVideoRecorder mRecorder;
    //按钮控件
    private PressRecordView pressRecordView;
    //相机标号
    int cameraId;

    private View back, flash, switcher;
    private ImageView flashIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraId = CameraHelper.getDefaultCameraID();
        // Create an instance of Camera
        camera = CameraHelper.getCameraInstance(cameraId);
        if (null == camera) {
            Toast.makeText(this, "打开相机失败！", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // 初始化录像机
        setContentView(R.layout.activity_new_recorder);
        initCamera();
        pressRecordView = (PressRecordView) findViewById(R.id.press_record_view);
        back = findViewById(R.id.cancel_btn);
        flash = findViewById(R.id.flash_switcher);
        flashIcon = (ImageView) findViewById(R.id.flashIcon);
        switcher = findViewById(R.id.camera_switcher);
        back.setOnClickListener(this);
        flash.setOnClickListener(this);
        switcher.setOnClickListener(this);
        final Callback callback = new Callback() {
            @Override
            public void onSuccess(String path) {
                Toast.makeText(NewRecordVideoActivity.this, path, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFail() {

            }
        };
        pressRecordView.setListener(new PressRecordView.StateChangeListener() {
            @Override
            public void onTimeOut() {
                stopRecord(callback, true);
            }

            @Override
            public void onCancel() {
                stopRecord(callback, false);
            }

            @Override
            public void onDown() {
                startRecord();
            }

            @Override
            public void onLessThanFive() {
                stopRecord(callback, false);
            }
        });
    }

    private void initCamera() {
        mRecorder = new GordonVideoRecorder(FileUtil.MEDIA_FILE_DIR);
        Pair<Integer, Integer> size = getSuggestedSize();
        mRecorder.setOutputSize(size.first, size.second);
        CameraPreviewView preview = (CameraPreviewView) findViewById(R.id.camera_preview);
        preview.setCamera(camera, cameraId);
        mRecorder.setCameraPreviewView(preview);
    }

    private Pair<Integer, Integer> getSuggestedSize() {
        int suggestedWidth = DensityUtils.getScreenWidth();
        List<Camera.Size> sizes = camera.getParameters().getSupportedPictureSizes();
        Pair<Integer, Integer> best = null;
        for (Camera.Size size : sizes) {
            if (size.width <= suggestedWidth) {
                if (best == null)
                    best = new Pair<>(size.width, size.height);
                else if (best.first < size.width)
                    best = new Pair<>(size.width, size.height);
            }
        }
        if (best == null)
            return new Pair<>(suggestedWidth, suggestedWidth * 3 / 4);
        else
            return best;
    }

    private void switchCamera() {
        if (CameraHelper.getAvailableCamerasCount() < 2) return;
        if (mRecorder != null) {
            boolean recording = mRecorder.isRecording();
            // 页面不可见就要停止录制
            mRecorder.stopRecording();
            // 录制时退出，直接舍弃视频
            if (recording) {
                FileUtil.deleteFile(mRecorder.getFilePath());
            }
        }
        // release the camera immediately on pause event
        releaseCamera();
        if (cameraId == CameraHelper.getDefaultCameraID()) {
            cameraId = CameraHelper.getFrontCameraID();
        } else {
            cameraId = CameraHelper.getDefaultCameraID();
        }
        camera = CameraHelper.getCameraInstance(cameraId);
        initCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mRecorder != null) {
            boolean recording = mRecorder.isRecording();
            // 页面不可见就要停止录制
            mRecorder.stopRecording();
            // 录制时退出，直接舍弃视频
            if (recording) {
                FileUtil.deleteFile(mRecorder.getFilePath());
            }
        }
        // release the camera immediately on pause event
        releaseCamera();
        finish();
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            // stop preview before release
            camera.stopPreview();
            // release the camera for other applications
            camera.release();
            camera = null;
        }
    }

    /**
     * start recording
     */
    private void startRecord() {
        if (mRecorder.isRecording()) {
            return;
        }
        // initialize video camera
        if (prepareVideoRecorder()) {
            // start recording
            if (!mRecorder.startRecording())
                Toast.makeText(this, "录制失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * prepare work
     */
    private boolean prepareVideoRecorder() {
        if (!FileUtil.isSDCardMounted()) {
            Toast.makeText(this, "SD卡不可用", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * stop recording
     */
    private void stopRecord(Callback callback, boolean isValid) {
        mRecorder.stopRecording();
        String videoPath = mRecorder.getFilePath();
        // no video is recorded
        if (null == videoPath) {
            callback.onFail();
            return;
        }
        // if canceled we delete it and call back the path if com.ctrip.gs.video.record.record finished
        if (!isValid) {
            FileUtil.deleteFile(videoPath);
        } else {
            // call back the required path.
            callback.onSuccess(videoPath);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.cancel_btn) {
            if (mRecorder != null) {
                boolean recording = mRecorder.isRecording();
                // 页面不可见就要停止录制
                mRecorder.stopRecording();
                // 录制时退出，直接舍弃视频
                if (recording) {
                    FileUtil.deleteFile(mRecorder.getFilePath());
                }
            }
            // release the camera immediately on pause event
            releaseCamera();
            finish();
        } else if (v.getId() == R.id.flash_switcher) {
            String flashMode = onSwitchFlash();
            if (flashMode.equals(Camera.Parameters.FLASH_MODE_AUTO))
                flashIcon.setImageResource(R.drawable.icon_flash_auto);
            else if (flashMode.equals(Camera.Parameters.FLASH_MODE_OFF)) {
                flashIcon.setImageResource(R.drawable.icon_flash_close);
            } else if (flashMode.equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                flashIcon.setImageResource(R.drawable.icon_flash_open);
            }
        } else if (v.getId() == R.id.camera_switcher) {
            switchCamera();
        }
    }

    public String onSwitchFlash() {
        if (camera == null) return "";
        Camera.Parameters p = camera.getParameters();
        if (Camera.Parameters.FLASH_MODE_OFF.equals(p.getFlashMode())) {
            p.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
            camera.setParameters(p);
            return Camera.Parameters.FLASH_MODE_AUTO;
        } else if (Camera.Parameters.FLASH_MODE_AUTO.equals(p.getFlashMode())) {
            p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);//持续的亮灯
            camera.setParameters(p);
            return Camera.Parameters.FLASH_MODE_TORCH;
        } else if (Camera.Parameters.FLASH_MODE_TORCH.equals(p.getFlashMode())) {
            p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(p);
            return Camera.Parameters.FLASH_MODE_OFF;
        }
        return "";
    }

    private interface Callback {
        void onSuccess(String path);

        void onFail();
    }
}
