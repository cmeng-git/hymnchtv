package org.cog.hymnchtv;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.cog.hymnchtv.utils.LocaleHelper;
import org.cog.hymnchtv.utils.ThemeHelper;

/**
 * BaseActivity implements the support of user set Theme and locale.
 * All app activities must extend BaseActivity inorder to support Theme and locale.
 */
public class BaseActivity extends AppCompatActivity {

    /**
     * Override AppCompatActivity#onCreate() to support Theme setting
     * Must setTheme() before super.onCreate(), otherwise user selected Theme is not working
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Always call setTheme() method in base class and before super.onCreate()
        ThemeHelper.setTheme(this);
        super.onCreate(savedInstanceState);
    }

    /**
     * Override AppCompatActivity#attachBaseContext() to support Locale setting.
     * Language value is already initialized in Hymnchtv Application class.
     *
     */
    @Override
    protected void attachBaseContext(Context base) {
        LocaleHelper context = LocaleHelper.setLocale(base);
        super.attachBaseContext(context);
    }
}
