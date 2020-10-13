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
package org.cog.hymnchtv;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import org.cog.hymnchtv.service.androidupdate.UpdateServiceImpl;

import de.cketti.library.changelog.ChangeLog;

/**
 * About info on hymnchtv
 *
 * @author Eng Chong Meng
 */
public class About extends FragmentActivity implements View.OnClickListener
{
    private static String[][] USED_LIBRARIES = new String[][]{
            new String[]{"Android Support Library", "https://developer.android.com/topic/libraries/support-library/index.html"},
            new String[]{"annotations-java5", "https://mvnrepository.com/artifact/org.jetbrains/annotations"},
            new String[]{"ckChangeLog", "https://github.com/cketti/ckChangeLog"},
            new String[]{"commons-lang", "https://commons.apache.org/proper/commons-lang/"},
            new String[]{"glide", "https://github.com/bumptech/glide"},
            new String[]{"Timber", "https://github.com/JakeWharton/timber"},
    };

    /**
     * Default CSS styles used to format the change log.
     */
    public static final String DEFAULT_CSS =
            "h1 { margin-left: 0px; font-size: 1.2em; }" + "\n" +
                    "li { margin-left: 0px; font-size: 0.9em;}" + "\n" +
                    "ul { padding-left: 2em; }";

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        // crash if enabled under FragmentActivity
        // requestWindowFeature(Window.FEATURE_LEFT_ICON);
        // setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, android.R.drawable.ic_dialog_info);
        setTitle(getString(R.string.gui_about));

        View hymnchtvkUrl = findViewById(R.id.hymnchtv_link);
        hymnchtvkUrl.setOnClickListener(this);

        TextView hymnchtvHelp = findViewById(R.id.hymnchtv_help);
        hymnchtvHelp.setTextColor(Color.CYAN);
        hymnchtvHelp.setOnClickListener(this);

        TextView copyRight = findViewById(R.id.copyRight);
        copyRight.setMovementMethod(LinkMovementMethod.getInstance());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            copyRight.setText(Html.fromHtml(getString(R.string.gui_copyright) , Html.FROM_HTML_MODE_LEGACY));
        } else {
            copyRight.setText(Html.fromHtml(getString(R.string.gui_copyright)));
        }


        findViewById(R.id.ok_button).setOnClickListener(this);
        findViewById(R.id.check_new_version).setOnClickListener(this);
        findViewById(R.id.history_log).setOnClickListener(this);

        // View btn_submitLogs = findViewById(R.id.submit_logs);
        // btn_submitLogs.setOnClickListener(this);

        String aboutInfo = getAboutInfo();
        WebView wv = findViewById(R.id.AboutDialog_Info);
        wv.loadDataWithBaseURL("file:///android_res/drawable/", aboutInfo, "text/html", "utf-8", null);

        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);

            TextView textView = findViewById(R.id.AboutDialog_Version);
            textView.setText(String.format(getString(R.string.gui_version), pi.versionName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId()) {
            case R.id.ok_button:
                finish();
                break;

            case R.id.check_new_version:
                new Thread()
                {
                    @Override
                    public void run()
                    {
                        UpdateServiceImpl.getInstance().checkForUpdates(true);
                    }
                }.start();
                break;

            case R.id.history_log:
                ChangeLog cl = new ChangeLog(this, DEFAULT_CSS);
                cl.getFullLogDialog().show();
                break;
            case R.id.hymnchtv_help:
            case R.id.hymnchtv_link:
                // hymnUrlAccess(this, getString(R.string.gui_help_link));
                break;

            default:
                finish();
                break;
        }
    }

    public static void hymnUrlAccess(Context context, String url)
    {
        if (url == null)
            url = context.getString(R.string.gui_help_link);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        context.startActivity(intent);
    }

    private String getAboutInfo()
    {
        StringBuilder html = new StringBuilder()
                .append("<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\"/>")
                .append("<html><head><style type=\"text/css\">")
                .append(DEFAULT_CSS)
                .append("</style></head><body>");

        StringBuilder libs = new StringBuilder().append("<ul>");
        for (String[] library : USED_LIBRARIES) {
            libs.append("<li><a href=\"")
                    .append(library[1])
                    .append("\">")
                    .append(library[0])
                    .append("</a></li>");
        }
        libs.append("</ul>");

        html.append(getString(R.string.content_about))
                .append("</p><hr/><p>");

        html.append(getString(R.string.content_help))
                .append("</p><hr/><p>");

        html.append(String.format(getString(R.string.gui_app_libraries), libs.toString()))
                .append("</p><hr/><p>");
        html.append("</body></html>");

        return html.toString();
    }
}
