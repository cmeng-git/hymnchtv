package org.cog.hymnchtv.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

/**
 * Text view with pinch-to-zoom and 2-points double-tap fixed zoom-in/zoom-out scale factor ability.
 * Ref: https://github.com/lecho/android_samples/blob/master/zoomtextview/src/lecho/sample/zoomtextview/view/ZoomTextView.java
 *
 * @author Eng Chong Meng
 */
public class ZoomTextView extends AppCompatTextView
{
    private static final float MIN_SCALE_FACTOR = 1.0f;
    public static final float MAX_SCALE_FACTOR = 2.0f;

    public static final float STEP_SCALE_FACTOR = 0.25f;
    private static final float MIN_CHANGE = 0.05f;

    /**
     * Defines the minimum duration in milliseconds between the first tap's up event,
     * and the second tap's down event for an interaction to be considered a double-tap.
     */
    private static final int DOUBLE_TAP_MIN_TIME = 40;
    private static final int DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();

    private final ScaleGestureDetector mScaleGestureDetector;
    private MotionEvent mCurrentDownEvent;
    private MotionEvent mPreviousUpEvent;

    private float mScaleFactor = MIN_SCALE_FACTOR;
    private static float maxScaleFactor = MAX_SCALE_FACTOR;
    private final float defaultSize;

    public ZoomTextView(Context context)
    {
        this(context, null, 0);
    }

    public ZoomTextView(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public ZoomTextView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        mScaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        defaultSize = getTextSize();
    }

    /***
     * @param scaleFactor
     * Default value is 3, 3 means text can zoom 3 times the default size
     */
    public static void setMaxScaleFactor(float scaleFactor)
    {
        maxScaleFactor = scaleFactor;
    }

    /**
     * Implement onTouchEvent with detection for 2-points double tab to perform fixed scale zoomIn/zoomOut
     */
    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(@NonNull MotionEvent event)
    {
        super.onTouchEvent(event);
        mScaleGestureDetector.onTouchEvent(event);

        // 2-points touch detected
        if (event.getPointerCount() == 2) {
            int action = event.getAction() & MotionEvent.ACTION_MASK;
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                if ((mCurrentDownEvent != null) && (mPreviousUpEvent != null)
                        && isConsideredDoubleTap(mPreviousUpEvent, event)) {
                    // This is a second tap
                    onDouble2PointTap();
                }

                if (mCurrentDownEvent != null) {
                    mCurrentDownEvent.recycle();
                }
                mCurrentDownEvent = MotionEvent.obtain(event);
            }
            else if ((action == MotionEvent.ACTION_POINTER_UP)) {
                if (mPreviousUpEvent != null) {
                    mPreviousUpEvent.recycle();
                }
                // Hold the event we obtained above - listeners may have changed the original.
                mPreviousUpEvent = MotionEvent.obtain(event);
            }
        }
        return true;
    }

    /**
     * Scale Gesture listener class, mScaleFactor is getting the scaling value
     * and mScaleFactor is mapped between 1.0 and zoomLimit that is MAX_SCALE_FACTOR by default.
     * You can also change it. Note: 2.0 means text can zoom to 2 times the default value.
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener
    {
        @Override
        public boolean onScale(ScaleGestureDetector detector)
        {
            mScaleFactor *= detector.getScaleFactor();
            if (mScaleFactor < MIN_CHANGE)
                return false;

            // Don't let the object get too small or too large (user setting + 1).
            mScaleFactor = Math.max(MIN_SCALE_FACTOR, Math.min(mScaleFactor, maxScaleFactor + 1));

            // Timber.d("Set TextView font size scale to: %.3f (%s)", mScaleFactor, defaultSize * mScaleFactor);
            setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultSize * mScaleFactor);
            return true;
        }
    }

    /**
     * 2-point double touch handler implementation
     */
    private void onDouble2PointTap()
    {
        if (mScaleFactor > MIN_SCALE_FACTOR + MIN_CHANGE) {
            mScaleFactor = MIN_SCALE_FACTOR;
        }
        else {
            mScaleFactor = maxScaleFactor;
        }
        setTextSize(TypedValue.COMPLEX_UNIT_PX, mScaleFactor * defaultSize);
    }

    /**
     * Routine to detect double touch event
     *
     * @param firstUp previous ACTION_POINTER_UP MotionEvent
     * @param secondDown new ACTION_POINTER_DOWN MotionEvent
     * @return true id fouble 2-point touch detected
     */
    private boolean isConsideredDoubleTap(MotionEvent firstUp, MotionEvent secondDown)
    {
        final long deltaTime = secondDown.getEventTime() - firstUp.getEventTime();
        return deltaTime <= DOUBLE_TAP_TIMEOUT && deltaTime >= DOUBLE_TAP_MIN_TIME;
    }
}
