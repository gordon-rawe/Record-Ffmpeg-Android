package rawe.gordon.com.androidrecord.utils;

import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import rawe.gordon.com.androidrecord.recorder.Constants;

/**
 * Created by Administrator on 2015/9/11.
 */
public class FileUtil {


    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    /**
     * 判断SD卡是否挂载
     *
     * @return
     */
    public static boolean isSDCardMounted() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     * Create a file Uri for saving an image or video
     */
    public static Uri getOutputMediaFileUri(int type) {
        File file = getOutputMediaFile(type);
        if (null == file)
            return null;
        return Uri.fromFile(file);
    }

    /**
     * Create a File for saving an image or video
     */
    public static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

//        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
//                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory() + Constants.MEDIA_FILE_DIR);
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    Constants.IMAGE_FILE_PREFIX + timeStamp + Constants.OUTPUT_IMAGE_EXTENSION);
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    Constants.VIDEO_FILE_PREFIX + timeStamp + Constants.OUTPUT_VIDEO_EXTENSION);
        } else {
            return null;
        }

        return mediaFile;
    }

    /**
     * 删除文件
     *
     * @param filePath
     * @return
     */
    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists())
            return file.delete();
        return true;
    }

    public static String createFilePath(String folder, String subfolder, String uniqueId) {
        File dir = new File(Environment.getExternalStorageDirectory(), folder);
        if (subfolder != null) {
            dir = new File(dir, subfolder);
        }
        dir.mkdirs();
        String fileName = Constants.VIDEO_FILE_PREFIX + uniqueId + Constants.OUTPUT_VIDEO_EXTENSION;
        return new File(dir, fileName).getAbsolutePath();
    }

}
