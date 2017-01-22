package rawe.gordon.com.record_ffmpeg_android;

import android.app.Application;

import rawe.gordon.com.androidrecord.utils.DensityUtils;

/**
 * Created by gordon on 22/01/2017.
 */

public class RealApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DensityUtils.init(this);
    }
}
