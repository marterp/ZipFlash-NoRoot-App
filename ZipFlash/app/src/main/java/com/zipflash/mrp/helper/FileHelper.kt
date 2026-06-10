package com.zipflash.mrp.helper

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

object FileHelper {
    fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null

        if ("content" == uri.scheme) {
            val cursor = context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) result = it.getString(0)
            }
        }

        if (result == null) {
            val path = uri.path
            result = if (path != null) {
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else path
            } else {
                "script.sh"
            }
        }

        return result
    }

    fun getMRPDir(): File = File("/data/local/tmp/MRP")
}
