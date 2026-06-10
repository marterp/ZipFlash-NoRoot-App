package com.zipflash.mrp.util

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

object CustomTabsHelper {

    fun openUrl(context: Context, url: String) {
        try {
            val intent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                .build()
            intent.launchUrl(context, Uri.parse(url))
        } catch (e: Exception) {
            val fallback = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
            fallback.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fallback)
        }
    }
}
