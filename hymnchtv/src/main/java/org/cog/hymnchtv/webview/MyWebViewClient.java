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
package org.cog.hymnchtv.webview;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.*;
import android.widget.CheckBox;
import android.widget.EditText;

import org.cog.hymnchtv.About;
import org.cog.hymnchtv.R;
import org.cog.hymnchtv.utils.ViewUtil;

import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * The class implements the WebViewClient for App internal web access
 *
 * @author Eng Chong Meng
 */
public class MyWebViewClient extends WebViewClient
{
    // Domain match pattern for last two segments of host
    private final Pattern pattern = Pattern.compile("^.*?[.](.*?[.].+?)$");

    private final WebViewFragment viewFragment;
    private final Context mContext;

    private EditText mPasswordField;

    public MyWebViewClient(WebViewFragment viewFragment)
    {
        this.viewFragment = viewFragment;
        mContext = viewFragment.getContext();
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView webView, String url)
    {
        Timber.d("shouldOverrideUrlLoading for url: %s", url);
        // This is my website, so do not override; let my WebView load the page
        if (isDomainMatch(url)) {
            viewFragment.addUrl(url);
            return false;
        }

        // Otherwise, the link is not for a page on my site, so launch another Activity that handle it
        // Use android default browser for youtube.com/google.com string search; to allow link share with hymnchtv
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            viewFragment.startActivity(intent);
        } catch (Exception e) {
            // catch ActivityNotFoundException for xmpp:info@example.com. so let own webView load and display the error
            Timber.w("Failed to load url '%s' : %s", url, e.getMessage());
            String origin = Uri.parse(webView.getUrl()).getHost();
            String originDomain = pattern.matcher(origin).replaceAll("$1");
            if (url.contains(originDomain))
                return false;
        }
        return true;
    }

    /**
     * If you click on any link inside the webpage of the WebView, that page will not be loaded inside your WebView.
     * In order to do that you need to extend your class from WebViewClient and override the method below.
     *
     * @param view The WebView that is initiating the callback.
     * @param request Object containing the details of the request.
     * @return {@code true} to cancel the current load, otherwise return {@code false}.
     */
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request)
    {
        String url = request.getUrl().toString();
        return shouldOverrideUrlLoading(view, url);
    }

    @Override
    public void onReceivedHttpAuthRequest(final WebView view, final HttpAuthHandler handler, final String host,
            final String realm)
    {
        final String[] httpAuth = new String[2];
        final String[] viewAuth = view.getHttpAuthUsernamePassword(host, realm);

        httpAuth[0] = (viewAuth != null) ? viewAuth[0] : "";
        httpAuth[1] = (viewAuth != null) ? viewAuth[1] : "";

        if (handler.useHttpAuthUsernamePassword()) {
            handler.proceed(httpAuth[0], httpAuth[1]);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(mContext);
        View authView = inflater.inflate(R.layout.http_login_dialog, view, false);

        final EditText usernameInput = authView.findViewById(R.id.username);
        usernameInput.setText(httpAuth[0]);

        mPasswordField = authView.findViewById(R.id.password_label);
        mPasswordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        mPasswordField.setText(httpAuth[1]);

        CheckBox showPasswordCheckBox = authView.findViewById(R.id.show_password);
        showPasswordCheckBox.setOnCheckedChangeListener((buttonView, isChecked)
                -> ViewUtil.showPassword(mPasswordField, isChecked));

        AlertDialog.Builder authDialog = new AlertDialog.Builder(mContext)
                .setTitle(R.string.gui_USER_LOGIN)
                .setView(authView)
                .setCancelable(false);
        final AlertDialog dialog = authDialog.show();

        authView.findViewById(R.id.button_signin).setOnClickListener(v -> {
            httpAuth[0] = ViewUtil.toString(usernameInput);
            httpAuth[1] = ViewUtil.toString(mPasswordField);
            view.setHttpAuthUsernamePassword(host, realm, httpAuth[0], httpAuth[1]);
            handler.proceed(httpAuth[0], httpAuth[1]);
            dialog.dismiss();
        });

        authView.findViewById(R.id.button_cancel).setOnClickListener(v -> {
            handler.cancel();
            dialog.dismiss();
        });
    }

    /**
     * Match non-case sensitive for whole or at least last two segment of host between:
     * a. Hymnchtv host
     * b. requested url host
     *
     * @param url to be loaded
     * @return true if match with hymnchtv host
     */
    public boolean isDomainMatch(String url)
    {
        // return false if aim contains no host string i.e. not a url e.g. mailto:info[at]example.com
        String aim = Uri.parse(url).getHost();
        if (TextUtils.isEmpty(aim)) {
            return false;
        }

        String origin = Uri.parse(About.HYMNCHTV_LINK).getHost();
        if (origin.equalsIgnoreCase(aim)) {
            return true;
        }

        String originDomain = pattern.matcher(origin).replaceAll("$1");
        String aimDomain = pattern.matcher(aim).replaceAll("$1");
        return originDomain.equalsIgnoreCase(aimDomain);
    }
}
