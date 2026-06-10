package com.zipflash.mrp.models

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val activityName: String?,
    val icon: Drawable
) {
    constructor(name: String, packageName: String, icon: Drawable) : this(name, packageName, null, icon)

    fun isActivity(): Boolean = activityName != null
}
