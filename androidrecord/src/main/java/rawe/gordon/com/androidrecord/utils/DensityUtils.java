package rawe.gordon.com.androidrecord.utils;

import android.app.Application;
import android.util.TypedValue;

/**
 * Created by gordon on 22/01/2017.
 */

public class DensityUtils {

    private static Application application;

    public static void init(Application appl) {
        application = appl;
    }

    private DensityUtils() {
        /* cannot be instantiated */
        throw new UnsupportedOperationException("cannot be instantiated");
    }

    /**
     * dp转px
     */
    public static int dp2px(float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dpVal, application.getResources().getDisplayMetrics());
    }

    /**
     * sp转px
     */
    public static int sp2px(float spVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                spVal, application.getResources().getDisplayMetrics());
    }

    /**
     * px转dp
     */
    public static float px2dp(float pxVal) {
        final float scale = application.getResources().getDisplayMetrics().density;
        return (pxVal / scale);
    }

    /**
     * px转sp
     */
    public static float px2sp(float pxVal) {
        return (pxVal / application.getResources().getDisplayMetrics().scaledDensity);
    }

    /**
     * 获取屏幕的宽
     * <p>
     * description:
     * MethodsName:
     * author: rtzhoooou
     * data: Mar 17, 2015 4:55:23 PM
     */
    public static int getScreenWidth() {
        return application.getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * 获取屏幕的高
     * <p>
     * description:
     * MethodsName:
     * author: rtzhoooou
     * data: Mar 17, 2015 4:55:43 PM
     */
    public static int getScreenHeight() {
        return application.getResources().getDisplayMetrics().heightPixels;
    }
}
