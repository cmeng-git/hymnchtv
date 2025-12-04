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
import android.graphics.Color;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.cog.hymnchtv.ContentHandler;
import org.cog.hymnchtv.HymnsApp;
import org.cog.hymnchtv.R;

import timber.log.Timber;

/**
 * The class displays the content accessed via given web link
 * <a href="https://developer.android.com/guide/webapps/webview">...</a>
 *
 * @author Eng Chong Meng
 */
@SuppressLint("SetJavaScriptEnabled")
public class WebViewLyrics extends WebView {
    /*
     * Default CSS styles used to format the change log.
     * Ensures the body aligns to the left edge of the viewport
     */
    public static final String DEFAULT_CSS = "h1 { margin-left: 0px; font-size: 1.2em; }\n" +
            "body {margin-left:10;padding-left:0;text-align:left; margin-right:0;margin-top:0;margin-bottom:0; }";

    public static final String bodyTextLight =
            "\n body { color: white }";
    public static final String bodyTextDark =
            "\n body { color: black }";

    // Limit the scale factors when when double taps to change the values
    private static final float MIN_SCALE_FACTOR = 1.0f;
    public static final float MAX_SCALE_FACTOR = 5.0f;

    public static final float STEP_SCALE_FACTOR = 0.25f;
    private float mScaleFactor = MIN_SCALE_FACTOR;
    private float mDefaultSize;

    private final WebViewLyrics webview;
    private final WebSettings mWebSettings;
    private ZoomTextListener mListener = null;

    private ContentHandler mContentHandler;

    public WebViewLyrics(Context context) {
        this(context, null, 0);
    }

    public WebViewLyrics(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WebViewLyrics(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        webview = this;
        webview.setBackgroundColor(Color.TRANSPARENT);

        mWebSettings = getSettings();
        mWebSettings.setBuiltInZoomControls(true);
        mWebSettings.setSupportZoom(true);
//        mWebSettings.setUseWideViewPort(true);
//        mWebSettings.setLoadWithOverviewMode(true);

        setWebViewClient(new WebViewClient() {
            @Override
            public void onScaleChanged(WebView view, float oldScale, float newScale) {
                super.onScaleChanged(view, oldScale, newScale);
                // This is your "callback" for when the scale changes
                Timber.d("Zoom scale changed from: %s to: %s", oldScale, newScale);

                mScaleFactor = newScale;
                Timber.d("Set Text scale web on change: %s (%s)", mScaleFactor, mDefaultSize);
                if (mListener != null)
                    mListener.updateTextScale(mScaleFactor);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        // setup keyPress listener - must re-enable every time on resume
        webview.setFocusableInTouchMode(true);
        webview.requestFocus();
    }

    public void setText(String lyrics) {
        webview.loadData(toHtml(lyrics), "text/html", "utf-8");
    }

    /**
     * Generate the full html formatted content with the given content
     *
     * @param content The lyrics table content
     *
     * @return the html formatted string
     */
    public static String toHtml(String content) {
        StringBuilder html = new StringBuilder()
                .append("<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\"/>")
                .append("<html><head>\n<style type=\"text/css\">\n")
                .append(DEFAULT_CSS);

        // Change text and ulr according to app theme
        if (ThemeHelper.isAppTheme(ThemeHelper.Theme.DARK)) {
            html.append(bodyTextLight);
        }
        else {
            html.append(bodyTextDark);
        }

        html.append("</style></head><body>\n")
                .append(content)
                .append("\n</body></html>");
        return html.toString();
    }

    /***
     * @param defaultSize ZoomTextView default text size
     * @param scaleFactor text size scale factor
     */
    public void scaleTextSize(int defaultSize, float scaleFactor) {
        mDefaultSize = defaultSize;
        mScaleFactor = scaleFactor;
        mWebSettings.setDefaultFontSize(Math.round(mScaleFactor * mDefaultSize));
        // setLyricsTextSize(mScaleFactor);
    }

    /**
     * onTextSize change via menu implementation;
     * with different STEP_SCALE_FACTOR
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

    private void setLyricsTextSize(float tmpScale) {
        // Limit the text size change to within range.
        mScaleFactor = Math.max(MIN_SCALE_FACTOR, Math.min(tmpScale, MAX_SCALE_FACTOR));
        if (mScaleFactor != tmpScale) {
            Timber.d("Set Text scale to: %.3f (%.3f); defaultSize: %s", mScaleFactor, tmpScale, mDefaultSize);
            HymnsApp.showToastMessage(R.string.lyrics_text_size_limits);
        }
        // Both TextView and WebView use the same font size unit; no conversion is required.
        mWebSettings.setDefaultFontSize(Math.round(mScaleFactor * mDefaultSize));
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