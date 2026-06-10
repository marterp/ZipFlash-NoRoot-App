package com.zipflash.mrp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.viewModels
import com.zipflash.mrp.databinding.ActivitySettingsBinding
import com.zipflash.mrp.viewmodel.SettingsViewModel
import rikka.shizuku.Shizuku

class SettingsFragment : BaseFragment() {

    private var binding: ActivitySettingsBinding? = null
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var settingsHelper: SettingsHelper
    private lateinit var switchAnyFile: SwitchCompat
    private lateinit var switchShowActivities: SwitchCompat
    private lateinit var switchShowSystem: SwitchCompat
    private lateinit var switchOptimize: SwitchCompat
    private lateinit var radioFont: RadioGroup
    private lateinit var radioTheme: RadioGroup
    private lateinit var spinnerLanguage: Spinner

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = ActivitySettingsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar(view, "Settings")

        settingsHelper = viewModel.getSettingsHelper(requireContext())

        switchAnyFile = binding!!.switchAnyFile
        switchShowActivities = binding!!.switchShowActivities
        switchShowSystem = binding!!.switchShowSystem
        switchOptimize = binding!!.optimizeSwitch
        radioFont = binding!!.radioFont
        radioTheme = binding!!.radioTheme
        spinnerLanguage = binding!!.spinnerLanguage

        setupLanguageSpinner()
        setupFontRadioGroup()
        setupThemeRadioGroup()
        setupSwitches()
        setupButtons()
    }

    private fun setupLanguageSpinner() {
        val labels = arrayOf("System default", "English")
        val codes = arrayOf("system", "en")

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = adapter

        val current = settingsHelper.language
        var sel = 0
        for (i in codes.indices) {
            if (codes[i] == current) { sel = i; break }
        }
        spinnerLanguage.setSelection(sel, false)

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            private var initialized = false

            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (!initialized) { initialized = true; return }
                settingsHelper.setLanguage(codes[position])
                requireActivity().recreate()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupFontRadioGroup() {
        val font = settingsHelper.font
        setFontRadioButtonChecked(font)

        radioFont.setOnCheckedChangeListener { _, checkedId ->
            val selectedFont = getFontTypeFromRadioId(checkedId)
            settingsHelper.setFont(selectedFont)
            applySelectedFontToView(requireView())
            Toast.makeText(requireContext(), "Font changed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFontTypeFromRadioId(radioId: Int): SettingsHelper.FontType {
        return when (radioId) {
            R.id.radio_font_sans -> SettingsHelper.FontType.SANS
            R.id.radio_font_serif -> SettingsHelper.FontType.SERIF
            else -> SettingsHelper.FontType.MONOSPACE
        }
    }

    private fun setFontRadioButtonChecked(font: SettingsHelper.FontType) {
        val radioId = when (font) {
            SettingsHelper.FontType.SANS -> R.id.radio_font_sans
            SettingsHelper.FontType.SERIF -> R.id.radio_font_serif
            SettingsHelper.FontType.MONOSPACE -> R.id.radio_font_monospace
        }
        val rb = requireView().findViewById<RadioButton>(radioId)
        rb?.isChecked = true
    }

    private fun setupThemeRadioGroup() {
        val currentTheme = settingsHelper.themeMode
        setThemeRadioButtonChecked(currentTheme)

        radioTheme.setOnCheckedChangeListener { _, checkedId ->
            val selectedTheme = getThemeModeFromRadioId(checkedId)
            settingsHelper.setThemeMode(selectedTheme)
            applyThemeMode(selectedTheme)
            Toast.makeText(requireContext(), "Theme changed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getThemeModeFromRadioId(radioId: Int): SettingsHelper.ThemeMode {
        return when (radioId) {
            R.id.radio_theme_light -> SettingsHelper.ThemeMode.LIGHT
            R.id.radio_theme_dark -> SettingsHelper.ThemeMode.DARK
            else -> SettingsHelper.ThemeMode.FOLLOW_SYSTEM
        }
    }

    private fun setThemeRadioButtonChecked(theme: SettingsHelper.ThemeMode) {
        val radioId = when (theme) {
            SettingsHelper.ThemeMode.LIGHT -> R.id.radio_theme_light
            SettingsHelper.ThemeMode.DARK -> R.id.radio_theme_dark
            SettingsHelper.ThemeMode.FOLLOW_SYSTEM -> R.id.radio_theme_system
        }
        val btn = requireView().findViewById<RadioButton>(radioId)
        btn?.isChecked = true
    }

    private fun applyThemeMode(mode: SettingsHelper.ThemeMode) {
        when (mode) {
            SettingsHelper.ThemeMode.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            SettingsHelper.ThemeMode.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            SettingsHelper.ThemeMode.FOLLOW_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun applySelectedFontToView(root: View?) {
        if (root == null) return
        val tf = when (settingsHelper.font) {
            SettingsHelper.FontType.SANS -> android.graphics.Typeface.SANS_SERIF
            SettingsHelper.FontType.SERIF -> android.graphics.Typeface.SERIF
            SettingsHelper.FontType.MONOSPACE -> android.graphics.Typeface.MONOSPACE
        }
        applyTypefaceRecursively(root, tf)
    }

    private fun applyTypefaceRecursively(v: View, tf: android.graphics.Typeface) {
        when (v) {
            is ViewGroup -> {
                for (i in 0 until v.childCount) {
                    applyTypefaceRecursively(v.getChildAt(i), tf)
                }
            }
            is android.widget.TextView -> v.typeface = tf
        }
    }

    private fun setupSwitches() {
        setupAnyFileSwitch()
        setupShowActivitiesSwitch()
        setupShowSystemAppsSwitch()
        setupOptimizeScriptSwitch()
    }

    private fun setupAnyFileSwitch() {
        switchAnyFile.isChecked = settingsHelper.isAnyFileMode
        switchAnyFile.setOnCheckedChangeListener { _, isChecked -> settingsHelper.setAnyFileMode(isChecked) }
    }

    private fun setupShowActivitiesSwitch() {
        switchShowActivities.isChecked = settingsHelper.showActivities
        switchShowActivities.setOnCheckedChangeListener { _, isChecked -> settingsHelper.setShowActivities(isChecked) }
    }

    private fun setupShowSystemAppsSwitch() {
        switchShowSystem.isChecked = settingsHelper.showSystemApps
        switchShowSystem.setOnCheckedChangeListener { _, isChecked -> settingsHelper.setShowSystemApps(isChecked) }
    }

    private fun setupOptimizeScriptSwitch() {
        switchOptimize.isChecked = settingsHelper.isOptimizeScriptEnabled
        switchOptimize.setOnCheckedChangeListener { _, isChecked -> settingsHelper.setOptimizeScriptEnabled(isChecked) }
    }

    private fun setupButtons() {
        setupResetButton()
        setupDevOptionsButton()
        setupClearCacheButton()
        setupGrantShizukuButton()
        setupShowIntroButton()
    }

    private fun setupResetButton() {
        binding?.btnResetDefaults?.setOnClickListener { showResetDialog() }
    }

    private fun setupDevOptionsButton() {
        binding?.btnDevOptions?.setOnClickListener {
            try {
                startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
            } catch (e: Exception) {
                startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
            }
        }
    }

    private fun setupClearCacheButton() {
        val btnClearCache = binding?.btnClearCache ?: return
        btnClearCache.setOnClickListener { v ->
            v.isEnabled = false
            clearAppCache()
            Toast.makeText(requireContext(), "Cache cleared successfully.", Toast.LENGTH_SHORT).show()
            v.postDelayed({ v.isEnabled = true }, 2000)
        }
    }

    private fun clearAppCache() {
        try {
            val cacheDir = requireContext().cacheDir
            if (cacheDir != null && cacheDir.isDirectory) deleteDir(cacheDir)

            val extCache = requireContext().externalCacheDir
            if (extCache != null && extCache.isDirectory) deleteDir(extCache)

            try {
                requireContext().deleteDatabase("webview.db")
                requireContext().deleteDatabase("webviewCache.db")
            } catch (_: Throwable) {}
        } catch (_: Throwable) {}
    }

    private fun deleteDir(dir: java.io.File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            if (children != null) {
                for (child in children) {
                    if (!deleteDir(java.io.File(dir, child))) return false
                }
            }
        }
        return dir != null && dir.delete()
    }

    private fun setupGrantShizukuButton() {
        val btnGrantShizuku = binding?.btnGrantShizuku ?: return

        var isLimitedMode = false
        try {
            if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                isLimitedMode = true
            }
        } catch (e: Throwable) {
            isLimitedMode = true
        }

        if (isLimitedMode) {
            btnGrantShizuku.visibility = View.VISIBLE
            btnGrantShizuku.setOnClickListener {
                androidx.navigation.Navigation.findNavController(requireView())
                    .navigate(R.id.checkPermFragment)
            }
        } else {
            btnGrantShizuku.visibility = View.GONE
        }
    }

    private fun setupShowIntroButton() {
        binding?.btnShowIntro?.setOnClickListener {
            Toast.makeText(requireContext(), "Intro not implemented", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showResetDialog() {
        val customView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_reset, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setView(customView)
            .setCancelable(true)
            .create()

        val yes = customView.findViewById<View>(R.id.btnYes)
        val no = customView.findViewById<View>(R.id.btnNo)

        yes?.setOnClickListener {
            settingsHelper.resetDefaults()
            updateAllViews()
            Toast.makeText(requireContext(), "Settings reset to defaults", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        no?.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun updateAllViews() {
        setFontRadioButtonChecked(settingsHelper.font)
        setThemeRadioButtonChecked(settingsHelper.themeMode)
        updateSwitches()
    }

    private fun updateSwitches() {
        switchAnyFile.isChecked = settingsHelper.isAnyFileMode
        switchShowActivities.isChecked = settingsHelper.showActivities
        switchShowSystem.isChecked = settingsHelper.showSystemApps
        switchOptimize.isChecked = settingsHelper.isOptimizeScriptEnabled
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
