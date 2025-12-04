package org.cog.hymnchtv.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

import org.cog.hymnchtv.HymnsApp;
import org.cog.hymnchtv.R;

import timber.log.Timber;

/**
 * Text view with pinch-to-zoom and user increment/decrement fixed zoom-in/zoom-out scale factor ability.
 * Ref: <a href="https://github.com/lecho/android_samples/blob/master/zoomtextview/src/lecho/sample/zoomtextview/view/ZoomTextView.java">...</a>
 *
 * @author Eng Chong Meng
 */
public class ZoomTextView extends AppCompatTextView {
    // Limit the scale factors when when double taps to change the values
    private static final float MIN_SCALE_FACTOR = 1.0f;
    public static final float MAX_SCALE_FACTOR = 5.0f;

    public static final float STEP_SCALE_FACTOR = 0.25f;

    private final ScaleGestureDetector mScaleGestureDetector;
    private ZoomTextListener mListener = null;

    private float mScaleFactor = MIN_SCALE_FACTOR;
    private float mDefaultSize;

    public ZoomTextView(Context context) {
        this(context, null, 0);
    }

    public ZoomTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureListener());
    }

    /***
     * @param defaultSize ZoomTextView default text size
     * @param scaleFactor text size scale factor
     */
    public void scaleTextSize(int defaultSize, float scaleFactor) {
        mDefaultSize = defaultSize;
        mScaleFactor = scaleFactor;
        setTextSize(mScaleFactor * mDefaultSize);
    }

    /**
     * onTextSize change via menu implementation, with different STEP_SCALE_FACTOR
     * return the permitted scaleFactor
     */
    public float onTextSizeChange(boolean stepInc) {
        float tmpScale;
        if (stepInc)
            tmpScale = mScaleFactor + STEP_SCALE_FACTOR * 1.5f;
        else
            tmpScale = mScaleFactor - STEP_SCALE_FACTOR;
        setLyricsTextSize(tmpScale);
        return mScaleFactor;
    }

    /**
     * Implement onTouchEvent with detection for 2-points double tab to perform scale zoomIn/zoomOut
     */
    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        mScaleGestureDetector.onTouchEvent(event);
        return true;
    }

    /**
     * Scale Gesture listener class, mScaleFactor is getting the scaling value
     * and mScaleFactor is mapped between 1.0 and zoomLimit that is MAX_SCALE_FACTOR by default.
     * You can also change it. Note: 2.0 means text can zoom to 2 times the default value.
     */
    private class ScaleGestureListener extends SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float tmpScale = mScaleFactor * detector.getScaleFactor();
            setLyricsTextSize(tmpScale);
            return true;
        }

        public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
            Timber.d("Set Text scale on end: %s (%s)", mScaleFactor, mListener);
            if (mListener != null)
                mListener.updateTextScale(mScaleFactor);
        }
    }

    private void setLyricsTextSize(float tmpScale) {
        // Limit the text size change to within range.
        mScaleFactor = Math.max(MIN_SCALE_FACTOR, Math.min(tmpScale, MAX_SCALE_FACTOR));
        if (mScaleFactor != tmpScale) {
            Timber.d("Set Text scale to: %.3f (%.3f); defaultSize: %s", mScaleFactor, tmpScale, mDefaultSize);
            HymnsApp.showToastMessage(R.string.lyrics_text_size_limits);
        }
        setTextSize(mScaleFactor * mDefaultSize);
    }

    /***
     * @param listener for update the user selected zoom scale to preference
     */
    public void registerZoomTextListener(ZoomTextListener listener) {
        mListener = listener;
    }

    /**
     * Listener for the change in zoom factor; save to preferences
     */
    public interface ZoomTextListener {
        void updateTextScale(Float mScale);
    }
}
