package rawe.gordon.com.androidrecord.recorder;

import static org.bytedeco.javacpp.avcodec.AV_CODEC_ID_H264;

public class Constants {

    public static final float TARGET_RATIO = 4 / 3F;

    public static final String MEDIA_FILE_DIR = "/Media";
    public final static String VIDEO_FILE_PREFIX = "VID_";
    public final static String IMAGE_FILE_PREFIX = "IMAGE_";
    public final static String OUTPUT_VIDEO_EXTENSION = ".flv";
    public final static String OUTPUT_IMAGE_EXTENSION = ".jpg";
    public final static int VIDEO_CODEC = AV_CODEC_ID_H264;
    public final static String OUTPUT_FORMAT = "flv";
    public final static String OUTPUT_FORMAT_OLD = "3gp";
    public static final int FRAME_RATE = 28;
    public static final int SAMPLE_AUDIO_RATE_IN_HZ = 44100;
    public static final int VIDEO_BIT_RATE = 2000000;
    public static final String PRESET = "faster";
    public static final String CRF = "14";
    public static final String TUNE = "zerolatency";

    public static final String KEY_PRESET = "preset";
    public static final String KEY_CRF = "crf";
    public static final String KEY_TUNE = "tune";

    public static final float SCALE_RATIO = 2F;

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
}
