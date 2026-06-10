package com.zipflash.mrp.manager

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import org.json.JSONObject

class ModuleManager(private val context: Context) {

    fun getModulesDir(): File {
        val dir = File(context.filesDir, "modules")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun listModules(): Array<File>? = getModulesDir().listFiles()

    fun addModule(zipFile: File) {
        val dest = File(getModulesDir(), zipFile.name)
        copyFile(zipFile, dest)
        saveStatus(dest.name, true, false)
    }

    private fun saveStatus(module: String, enabled: Boolean, synced: Boolean) {
        try {
            val obj = JSONObject()
            obj.put("enabled", enabled)
            obj.put("synced", synced)
            val fw = FileWriter(File(getModulesDir(), "$module.json"))
            fw.write(obj.toString())
            fw.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun copyFile(src: File, dst: File) {
        val `in` = FileInputStream(src)
        val out = FileOutputStream(dst)
        val buf = ByteArray(4096)
        var len: Int
        while (`in`.read(buf).also { len = it } > 0) out.write(buf, 0, len)
        `in`.close()
        out.close()
    }
}
