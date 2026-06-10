package com.zipflash.mrp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.zipflash.mrp.SettingsHelper

class SettingsViewModel : ViewModel() {

    fun getSettingsHelper(context: Context): SettingsHelper {
        return SettingsHelper(context)
    }
}
