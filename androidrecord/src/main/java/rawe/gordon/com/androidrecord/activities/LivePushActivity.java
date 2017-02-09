package rawe.gordon.com.androidrecord.activities;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import rawe.gordon.com.androidrecord.PressRecordView;
import rawe.gordon.com.androidrecord.R;
import rawe.gordon.com.androidrecord.camera.CameraHelper;
import rawe.gordon.com.androidrecord.recorder.LivePushRecorder;
import rawe.gordon.com.androidrecord.widget.CameraPreviewView;

/**
 * 视频录制页面
 *
 * @author Gordon Rawe
 */
public class LivePushActivity extends Activity implements View.OnClickListener {

    private static final String TAG = LivePushActivity.class.getCanonicalName();

    private String live_rtmp_url = "rtmp://10.32.64.130:1935/live/gordon";
    //摄像机
    private Camera camera;
    //一个控件
    private LivePushRecorder mRecorder;
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
        setContentView(R.layout.activity_live_push);
        initCamera();
        pressRecordView = (PressRecordView) findViewById(R.id.press_record_view);
        back = findViewById(R.id.cancel_btn);
        flash = findViewById(R.id.flash_switcher);
        flashIcon = (ImageView) findViewById(R.id.flashIcon);
        switcher = findViewById(R.id.camera_switcher);
        back.setOnClickListener(this);
        flash.setOnClickListener(this);
        switcher.setOnClickListener(this);

        pressRecordView.setListener(new PressRecordView.StateChangeListener() {
            @Override
            public void onTimeOut() {
                stopRecord();
            }

            @Override
            public void onCancel() {
                stopRecord();
            }

            @Override
            public void onDown() {
                startRecord();
            }

            @Override
            public void onLessThanFive() {
                stopRecord();
            }
        });
    }

    private void initCamera() {
        mRecorder = new LivePushRecorder(live_rtmp_url);
        CameraPreviewView preview = (CameraPreviewView) findViewById(R.id.camera_preview);
        preview.setCamera(camera, cameraId);
        mRecorder.setCameraPreviewView(preview);
    }

    private void switchCamera() {
        if (CameraHelper.getAvailableCamerasCount() < 2) return;
        if (mRecorder != null) {
            // 页面不可见就要停止录制
            mRecorder.stopRecording();
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
            // 页面不可见就要停止录制
            mRecorder.stopRecording();
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
        if (mRecorder.getLiveUrl() == null) {
            Toast.makeText(this, " 视频上传路径不能为空", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * stop recording
     */
    private void stopRecord() {
        mRecorder.stopRecording();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.cancel_btn) {
            if (mRecorder != null) {
                boolean recording = mRecorder.isRecording();
                // 页面不可见就要停止录制
                mRecorder.stopRecording();
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
}
