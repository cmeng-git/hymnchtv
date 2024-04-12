/*
 * hymnchtv: COG hymns' lyrics viewer and player client
 * Copyright 2020 Eng Chong Meng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cog.hymnchtv.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;

/**
 * Class supports SingleTap, DoubleTap, LongPress and onFling for the given touched view
 * The caller may implement any or all the actions.
 * https://stackoverflow.com/questions/20797099/swipe-listview-item-from-right-to-left-show-delete-button
 *
 * @author Eng Chong Meng
 */
public class TouchListener implements View.OnTouchListener {
    private static final int SWIPE_THRESHOLD_DISTANCE = 120;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    private final GestureDetector gestureDetector;
    private View mView;

    // ListView item index else -1 for View
    private int mIndex;

    public TouchListener(Context context) {
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return TouchListener.this.onSingleTap(mView, mIndex);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                return TouchListener.this.onDoubleTap(mView, mIndex);
            }

            @Override
            public void onLongPress(MotionEvent e) {
                TouchListener.this.onLongPress(mView, mIndex);
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                // "Left Swipe"
                if (e1.getX() - e2.getX() > SWIPE_THRESHOLD_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    return TouchListener.this.onSwipeLeft(mView, mIndex);
                }
                // "Right Swipe"
                else if (e2.getX() - e1.getX() > SWIPE_THRESHOLD_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    return TouchListener.this.onSwipeRight(mView, mIndex);
                }
                return false;
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mView = v;
        mIndex = (mView instanceof ListView) ? ((ListView) v).pointToPosition((int) event.getX(), (int) event.getY()) : -1;
        return gestureDetector.onTouchEvent(event);
    }

    // ===== Caller implementation for the required actions ===== //

    /**
     * Called when a view has been clicked.
     *
     * @param v The View or ListView where the click happened.
     * @param idx ListView item index else -1 for View
     *
     * @return true if the callback consumed the long click, false otherwise.
     */
    public boolean onSingleTap(View v, int idx) {
        return false;
    }

    /**
     * Called when a view has been clicked twice within 300ms
     *
     * @param v The View or ListView where the click happened.
     * @param idx ListView item index else -1 for View
     *
     * @return true if the callback consumed the double clicks, false otherwise.
     */
    public boolean onDoubleTap(View v, int idx) {
        return false;
    }

    /**
     * Called when a view has been clicked and held.
     *
     * @param v The View or ListView where the click happened.
     * @param idx ListView item index else -1 for View
     */
    public void onLongPress(View v, int idx) {
    }

    /**
     * Called when a swipe left is detected on the view
     *
     * @param v The ListView where the swipe happened.
     * @param idx ListView item index
     *
     * @return true if the callback consumed the action, false otherwise.
     */
    public boolean onSwipeLeft(View v, int idx) {
        return false;
    }

    /**
     * Called when a swipe right is detected on the view
     *
     * @param v The ListView where the swipe happened.
     * @param idx ListView item index
     *
     * @return true if the callback consumed the action, false otherwise.
     */
    public boolean onSwipeRight(View v, int idx) {
        return false;
    }
}
