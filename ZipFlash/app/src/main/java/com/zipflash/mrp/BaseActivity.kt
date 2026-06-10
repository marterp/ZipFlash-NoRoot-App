package com.zipflash.mrp

import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.color.DynamicColors
import java.io.File
import java.util.Locale

abstract class BaseActivity : AppCompatActivity() {

    protected lateinit var settings: SettingsHelper

    override fun attachBaseContext(newBase: Context) {
        val code = newBase.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            .getString("language", "system") ?: "system"
        val wrapped = applyLocale(newBase, code)
        super.attachBaseContext(wrapped)
    }

    private fun applyLocale(base: Context, code: String): Context {
        if (code == "system") return base
        val locale = Locale(code)
        Locale.setDefault(locale)
        val res = base.resources
        val cfg = Configuration(res.configuration)
        return if (Build.VERSION.SDK_INT >= 24) {
            cfg.setLocale(locale)
            base.createConfigurationContext(cfg)
        } else {
            cfg.locale = locale
            res.updateConfiguration(cfg, res.displayMetrics)
            base
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        DynamicColors.applyToActivityIfAvailable(this)
        settings = SettingsHelper(this)
        applyStoredTheme()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        val wic = ViewCompat.getWindowInsetsController(window.decorView)
        wic?.let {
            it.isAppearanceLightStatusBars = false
            it.isAppearanceLightNavigationBars = false
        }
    }

    protected fun applyStoredTheme() {
        when (settings.themeMode) {
            SettingsHelper.ThemeMode.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            SettingsHelper.ThemeMode.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            SettingsHelper.ThemeMode.FOLLOW_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    protected fun isOnline(): Boolean {
        return try {
            val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val netInfo = cm.activeNetworkInfo
            netInfo != null && netInfo.isConnected
        } catch (e: Throwable) {
            false
        }
    }

    protected fun clearAppCache() {
        try {
            val cacheDir = cacheDir
            if (cacheDir != null && cacheDir.isDirectory) {
                deleteDir(cacheDir)
            }
            val extCache = externalCacheDir
            if (extCache != null && extCache.isDirectory) {
                deleteDir(extCache)
            }
            try {
                applicationContext.deleteDatabase("webview.db")
                applicationContext.deleteDatabase("webviewCache.db")
            } catch (_: Throwable) { }
        } catch (_: Throwable) { }
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            if (children != null) {
                for (child in children) {
                    if (!deleteDir(File(dir, child))) return false
                }
            }
        }
        return dir != null && dir.delete()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        applySelectedFontToView(window.decorView)
    }

    override fun setContentView(view: View) {
        super.setContentView(view)
        applySelectedFontToView(window.decorView)
    }

    protected fun applySelectedFontToView(root: View?) {
        if (root == null) return
        val tf = when (settings.font) {
            SettingsHelper.FontType.SANS -> Typeface.SANS_SERIF
            SettingsHelper.FontType.SERIF -> Typeface.SERIF
            SettingsHelper.FontType.MONOSPACE -> Typeface.MONOSPACE
        }
        applyTypefaceRecursively(root, tf)
    }

    private fun applyTypefaceRecursively(v: View, tf: Typeface) {
        when (v) {
            is ViewGroup -> {
                for (i in 0 until v.childCount) {
                    applyTypefaceRecursively(v.getChildAt(i), tf)
                }
            }
            is TextView -> v.typeface = tf
        }
    }

    protected open fun setupToolbar(title: String) {
        val toolbar = findViewById<Toolbar>(R.id.toolbar) ?: return
        setSupportActionBar(toolbar)
        toolbar.setTitleTextColor(getColor(R.color.text_primary))

        val actionBar = supportActionBar
        actionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
            it.title = title
        }

        toolbar.navigationIcon?.setTint(getColor(R.color.text_primary))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
