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
package org.cog.hymnchtv.mediaconfig;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.apache.commons.text.StringEscapeUtils;
import org.cog.hymnchtv.HymnsApp;
import org.cog.hymnchtv.persistance.DatabaseBackend;

import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * The class provides retrieval and saving of user selected English lyrics.
 *
 * @author Eng Chong Meng
 */
public class LyricsEnglishRecord
{
    // SQL database parameters for LyricsEnglishRecord
    public static final String TABLE_NAME = "lyrics_eng";
    public static final String HYMN_NO_ENG = "hymnNoEng";
    public static final String LYRICS_ENG = "lyricsEng";

    /**
     * Default CSS styles used to format the retrieved english lyrics for table display.
     */
    public static final String DEFAULT_CSS = "\n" +
            " h1 { margin-bottom: 20px; text-align: center; font-size: 1.2em; }\n" +
            " table { margin: 0;  height: auto; width: 100%; font-size: 1.2em }\n" +
            " table tr { vertical-align: text-top }\n" +
            " table tr td: first-child { padding-right: 15px }\n" +
            " table tr td { padding-bottom: 20px }\n" +
            " table tr td.verse-num { cursor: pointer }\n" +
            " table tr td.verse-num div { background: #ddd; color: #444 }\n" +
            " table tr td.verse-num.highlight div { background: #6363fe; color: #fff }\n" +
            " table tr td.chorus {padding-left: 15px }\n" +
            " table tr td.note { width: 320px; text-align: center; color: #707070; font-style: italic}\n" +
            " table tr td.copyright { width: 320px; padding-top: 10px; text-align: center; color:#707070 }\n" +
            " table tr.stanza-num { left: -2 em; width: 1.5 em; padding: 2px; text-align: center; color: #444; background: #ddd }\n";

    public static String HYMNAL_LINK_MAIN = "https://www.hymnal.net/en/hymn/h/";

    private static final WeakHashMap<Context, LyricsEnglishRecord> INSTANCES = new WeakHashMap<>();
    private final Context mContext;
    private EnglishLyricsListener mListener = null;
    private String mLyricsEnglish = null;

    public static synchronized LyricsEnglishRecord getInstanceFor(Context context)
    {
        LyricsEnglishRecord lyricsEnglishRecord = INSTANCES.get(context);
        if (lyricsEnglishRecord == null) {
            lyricsEnglishRecord = new LyricsEnglishRecord(context);
            INSTANCES.put(context, lyricsEnglishRecord);
        }
        return lyricsEnglishRecord;
    }

    public LyricsEnglishRecord(Context context)
    {
        mContext = context;
    }

    /**
     * Get the English lyrics from SQL DB if found, else retrieve it online; save to DB available.
     * mLyricsEnglish may only contain a link for external access. Currently the link is http i.e.
     * not secure ssl and will be blocked by android if proceed to access its content.
     */
    public void fetchLyrics(int hymnNoEng)
    {
        DatabaseBackend mDB = DatabaseBackend.getInstance(HymnsApp.getGlobalContext());
        String webUrl = HYMNAL_LINK_MAIN + hymnNoEng;

        mLyricsEnglish = mDB.getLyricsEnglish(hymnNoEng);
        if (mLyricsEnglish != null) {
            showLyrics(mLyricsEnglish);
            return;
        }

        getURLSource(webUrl, data -> {
            if (extraEnglishLyrics(data)) {
                mDB.storeLyricsEng(hymnNoEng, mLyricsEnglish);
                showLyrics(mLyricsEnglish);
            }
            else {
                showLyrics(null);
            }
        });
    }

    /**
     * Call the mListener if not null to display the English lyrics.
     *
     * @param lyrics English lyrics content; may just contains a href link
     */
    private void showLyrics(String lyrics)
    {
        if (mListener != null) {
            lyrics = (lyrics != null) ? toHtml(lyrics) : null;
            mListener.showLyricsEnglish(lyrics);
        }
    }

    /**
     * Extra and phrase the retrieved English title and lyrics or the href link.
     *
     * @param lyricsContent the remote raw content containing the required info
     * @return true if lyrics is found and not empty
     */
    private boolean extraEnglishLyrics(String lyricsContent)
    {
        if (TextUtils.isEmpty(lyricsContent))
            return false;

        Pattern pattern = Pattern.compile("(<h1 id=\"song-title.+?</h1>).+?<div class=\"row hymn-content\">.+?(<table data-end=\"\" class=\"js-stanzas\">.+?table>).*?");
        Pattern patternExt = Pattern.compile("(<h1 id=\"song-title.+?</h1>).+?<div class=\"alert.+?(<a href=\".+?</a>).+?");
        mLyricsEnglish = null;

        Matcher matcher = pattern.matcher(lyricsContent);
        if (matcher.find()) {
            mLyricsEnglish = matcher.group(1) + matcher.group(2);
        }
        else {
            Matcher matcherExt = patternExt.matcher(lyricsContent);
            if (matcherExt.find()) {
                mLyricsEnglish = matcherExt.group(1) + matcherExt.group(2);
            }
        }
        return (!TextUtils.isEmpty(mLyricsEnglish));
    }

    @SuppressLint("SetJavaScriptEnabled")
    private WebView initWebView(Context context)
    {
        WebView webView = new WebView(context);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        return webView;
    }

    /**
     * Get the url source and return the required lyrics content.
     *
     * @param urlToLoad link to load the content
     * @param valueCallback callback to deliver the result
     */
    private void getURLSource(String urlToLoad, final ValueCallback<String> valueCallback)
    {
        Pattern pattern = Pattern.compile("<div class=\"row main-content\">(.+?</table>).+?");

        WebView webView = initWebView(mContext);
        webView.loadUrl(urlToLoad);
        webView.setWebViewClient(new WebViewClient()
        {
            @Override
            public void onPageFinished(WebView view, String url)
            {
                super.onPageFinished(view, url);
                if (webView.getProgress() == 100) {
                    try {
                        webView.evaluateJavascript("document.documentElement.outerHTML", data -> {
                            data = cleanUpHtml(data);
                            Matcher matcher = pattern.matcher(data);
                            if (matcher.find()) {
                                valueCallback.onReceiveValue(matcher.group(1));
                            }
                            else {
                                Timber.w("Web scrapping failed: %s", urlToLoad);
                                valueCallback.onReceiveValue(null);
                            }
                        });
                    } catch (RuntimeException e) {
                        Timber.e("Web scrapping failed: %s", e.getMessage());
                        valueCallback.onReceiveValue(null);
                    }
                }
            }
        });
    }

    /**
     * Cleanup the stray content before any match.
     *
     * @param content url source content
     * @return clean up html content
     */
    private String cleanUpHtml(String content)
    {
        return StringEscapeUtils.unescapeJava(content)
                .replaceAll("[\n]", "")
                .replaceAll(" {2,}", " ")
                .replaceAll("\u003C", "<")
                .replaceAll("\u003E", ">")
                .replaceAll("> +<", "><")
                .replaceAll("http:", "https:");
    }

    /**
     * Generate the full html formatted content with the given content
     *
     * @param content The lyrics table content
     * @return the html formatted string
     */
    private String toHtml(String content)
    {
        StringBuilder html = new StringBuilder()
                .append("<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\"/>")
                .append("<html><head>\n<style type=\"text/css\">\n")
                .append(DEFAULT_CSS)
                .append("</style></head><body>\n")
                .append(content)
                .append("\n</body></html>");
        return html.toString();
    }

    /***
     * @param listener for update display the English lyrics.
     */
    public void registerLyricsListener(EnglishLyricsListener listener)
    {
        mListener = listener;
    }

    /**
     * The listener that will be be to show the lyrics.
     */
    public interface EnglishLyricsListener
    {
        /**
         * Show the lyrics content.
         *
         * @param lyrics The English lyrics or just link
         */
        void showLyricsEnglish(String lyrics);
    }
}
