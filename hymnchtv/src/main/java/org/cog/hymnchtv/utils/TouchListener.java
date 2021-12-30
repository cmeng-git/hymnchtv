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

import android.content.Context;
import android.view.*;

import timber.log.Timber;

/**
 * Class supports SingleTap, DoubleTap and LongPress for the given touched view
 *
 * @author Eng Chong Meng
 */
public abstract class TouchListener implements View.OnTouchListener
{
    private GestureDetector gestureDetector;
    private View mView;

    public TouchListener(Context context)
    {
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener()
        {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e)
            {
                return TouchListener.this.onSingleTap(mView);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e)
            {
                return TouchListener.this.onDoubleTap(mView);
            }

            @Override
            public void onLongPress(MotionEvent e)
            {
                TouchListener.this.onLongPress(mView);
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        mView = v;
        gestureDetector.onTouchEvent(event);
        return true;
    }

    public abstract boolean onSingleTap(View v);

    public abstract boolean onDoubleTap(View v);

    public abstract void onLongPress(View v);
}
