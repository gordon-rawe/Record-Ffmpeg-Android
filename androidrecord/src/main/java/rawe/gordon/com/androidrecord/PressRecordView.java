package rawe.gordon.com.androidrecord;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import rawe.gordon.com.androidrecord.utils.DensityUtils;

/**
 * Created by mcluo on 2016/8/5.
 */
public class PressRecordView extends ViewGroup {
    private float lastY = 0;
    private float downY = 0;
    private ImageView centerButton;
    private TextView textView, cancelHint;
    private int screenWidth;
    private float progressLength = screenWidth;
    private int selfHeight = 0;
    private int squareLength;
    private boolean drawText = false;
    private int dragDistance;
    private int textWidth = 0, textHeight = 0, hintWidth = 0, hintHeight = 0;
    private boolean lessThanFiveSeconds = true;
    private Handler handler = new Handler();
    private Runnable runnable;

    public PressRecordView(Context context) {
        super(context);
        init();
    }

    public PressRecordView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PressRecordView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        squareLength = DensityUtils.dp2px(110);
        dragDistance = DensityUtils.dp2px(50);
        screenWidth = DensityUtils.getScreenWidth();
        setBackgroundColor(Color.parseColor("#4c000000"));
        addCenterView();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChild(textView, widthMeasureSpec, heightMeasureSpec);
        textWidth = textView.getMeasuredWidth();
        textHeight = textView.getMeasuredHeight();
        measureChild(cancelHint, widthMeasureSpec, heightMeasureSpec);
        hintWidth = cancelHint.getMeasuredWidth();
        hintHeight = cancelHint.getMeasuredHeight();
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

    private void addCenterView() {
        centerButton = new ImageView(getContext());
        centerButton.setImageResource(R.drawable.icon_hold_shoot);
        ViewGroup.LayoutParams centerButtonLayout = new ViewGroup.LayoutParams(squareLength, squareLength);
        addView(centerButton, centerButtonLayout);
        textView = new TextView(getContext());
        textView.setText("按住拍摄");
        textView.setTextColor(Color.parseColor("#42baf8"));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        ViewGroup.LayoutParams tlp = new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        addView(textView, tlp);
        cancelHint = new TextView(getContext());
        cancelHint.setText("上滑取消");
        cancelHint.setTextColor(Color.WHITE);
        cancelHint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        ViewGroup.LayoutParams tlpHint = new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        cancelHint.setVisibility(GONE);
        addView(cancelHint, tlpHint);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        selfHeight = getMeasuredHeight();
        centerButton.layout((screenWidth - squareLength) / 2, (selfHeight - squareLength) / 2,
                (screenWidth + squareLength) / 2, (selfHeight + squareLength) / 2);
        textView.layout((screenWidth - textWidth) / 2, (selfHeight - textHeight) / 2,
                (screenWidth + textWidth) / 2, (selfHeight + textHeight) / 2);
        cancelHint.layout((screenWidth - hintWidth) / 2, (selfHeight - hintHeight) / 2 - DensityUtils.dp2px(100),
                (screenWidth + hintWidth) / 2, (selfHeight + hintHeight) / 2 - DensityUtils.dp2px(100));
    }

    private ValueAnimator valueAnimator;
    boolean flag = false;

    private void startCount() {
        lessThanFiveSeconds = true;
        flag = false;
        centerButton.setImageResource(R.drawable.icon_hold_shoot_grey);
        valueAnimator = ValueAnimator.ofFloat(screenWidth, 0);
        if (runnable != null) handler.removeCallbacks(runnable);
        handler.postDelayed(runnable = new Runnable() {
            @Override
            public void run() {
                lessThanFiveSeconds = false;
            }
        }, 5500);
        valueAnimator.setDuration(15500);
        if (listener != null) listener.onDown();
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                progressLength = (float) animation.getAnimatedValue();
                invalidate();
                if (progressLength == 0F && listener != null) {
                    listener.onTimeOut();
                    flag = true;
                }
            }
        });
        valueAnimator.start();
    }

    private void handleActionUp() {
        valueAnimator.cancel();
        progressLength = screenWidth;
        if (listener != null) {
            if (drawText) {
                listener.onCancel();
            } else {
                if (lessThanFiveSeconds) {
                    Toast.makeText(getContext(), "录制视频最短需5秒噢", Toast.LENGTH_SHORT).show();
                    listener.onLessThanFive();
                } else {
                    if (!flag) listener.onTimeOut();
                }
            }
        }
        drawText = false;
        cancelHint.setVisibility(drawText ? VISIBLE : GONE);
        centerButton.setImageResource(R.drawable.icon_hold_shoot);
        invalidate();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastY = downY = ev.getY();
                if (!within(ev.getX(), ev.getY())) return false;
                startCount();
                break;
            case MotionEvent.ACTION_MOVE:
                lastY = ev.getY();
                calcState();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handleActionUp();
                lastY = 0;
                break;
        }
        super.dispatchTouchEvent(ev);
        return true;
    }

    private void calcState() {
        drawText = downY - lastY > dragDistance;
        cancelHint.setVisibility(drawText ? VISIBLE : GONE);
        Log.d("drawText", String.valueOf(drawText));
        invalidate();
    }

    private Paint paint;

    {
        paint = new Paint();
        paint.setTextSize(14);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        paint.setColor(Color.parseColor("#42baf8"));
        canvas.drawRect((screenWidth - progressLength) / 2, 0, (screenWidth + progressLength) / 2, 10, paint);
        paint.setColor(Color.RED);
    }

    public void setListener(StateChangeListener listener) {
        this.listener = listener;
    }

    private StateChangeListener listener;

    public interface StateChangeListener {
        void onTimeOut();

        void onCancel();

        void onDown();

        void onLessThanFive();
    }

    private boolean within(float x, float y) {
        return x > (screenWidth - squareLength) / 2 && x < (screenWidth + squareLength) / 2 && y < (selfHeight + squareLength) / 2 && y > (selfHeight - squareLength) / 2;
    }
}
