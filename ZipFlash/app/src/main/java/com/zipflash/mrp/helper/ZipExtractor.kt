package com.zipflash.mrp.helper

import android.content.Context
import android.net.Uri
import android.widget.TextView
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipInputStream

object ZipExtractor {
    fun extractToModules(context: Context, uri: Uri, outputView: TextView?): String {
        if (!CheckPermHelper.hasShizukuPermission()) {
            throw IOException("Shizuku permission required for extraction")
        }

        val modulesDir = File("/data/local/tmp/modules")
        if (!modulesDir.exists()) {
            val mkdir = Shizuku.newProcess(arrayOf("mkdir", "-p", modulesDir.absolutePath), null, null)
            mkdir.waitFor()
            if (mkdir.exitValue() != 0) {
                throw IOException("Failed to create directory: ${modulesDir.absolutePath}")
            }
            mkdir.destroy()
        }

        var fileName = FileHelper.getFileName(context, uri) ?: throw IOException("Invalid file name")
        if (fileName.endsWith(".zip")) {
            fileName = fileName.substring(0, fileName.length - 4)
        }

        val moduleDir = File(modulesDir, fileName)

        if (moduleDir.exists()) {
            deleteRecursive(moduleDir)
        }

        val mkdirModule = Shizuku.newProcess(arrayOf("mkdir", "-p", moduleDir.absolutePath), null, null)
        mkdirModule.waitFor()
        if (mkdirModule.exitValue() != 0) {
            throw IOException("Failed to create module directory: ${moduleDir.absolutePath}")
        }
        mkdirModule.destroy()

        val `is` = context.contentResolver.openInputStream(uri) ?: throw IOException("Unable to open ZIP input stream")

        val zis = ZipInputStream(`is`)
        val buffer = ByteArray(4096)
        var entry = zis.nextEntry
        while (entry != null) {
            val outFile = File(moduleDir, entry.name)

            if (entry.isDirectory) {
                val mkdirEntry = Shizuku.newProcess(arrayOf("mkdir", "-p", outFile.absolutePath), null, null)
                mkdirEntry.waitFor()
                if (mkdirEntry.exitValue() != 0) {
                    throw IOException("Failed to create directory: ${outFile.absolutePath}")
                }
                mkdirEntry.destroy()
            } else {
                val parent = outFile.parentFile
                if (parent != null && !parent.exists()) {
                    val mkdirParent = Shizuku.newProcess(arrayOf("mkdir", "-p", parent.absolutePath), null, null)
                    mkdirParent.waitFor()
                    if (mkdirParent.exitValue() != 0) {
                        throw IOException("Failed to create parent directory: ${parent.absolutePath}")
                    }
                    mkdirParent.destroy()
                }

                val copy = Shizuku.newProcess(arrayOf("sh", "-c", "cat > ${outFile.absolutePath}"), null, null)
                val os = copy.outputStream
                var len: Int
                while (zis.read(buffer).also { len = it } > 0) {
                    os.write(buffer, 0, len)
                }
                os.close()
                copy.waitFor()
                if (copy.exitValue() != 0) {
                    throw IOException("Failed to write file: ${outFile.absolutePath}")
                }
                copy.destroy()
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
        zis.close()
        `is`.close()

        val runScript = File(moduleDir, "run.sh")
        val revertScript = File(moduleDir, "revert.sh")
        if (!runScript.exists() || !revertScript.exists()) {
            deleteRecursive(moduleDir)
            val errorMessage = "Module extraction failed: Missing script"
            outputView?.post { outputView.append("[!] $errorMessage\n") }
            throw IOException(errorMessage)
        }

        val prefs = context.getSharedPreferences("modules_state", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(fileName, true).apply()

        val modName = fileName
        outputView?.post { outputView.append("[✓] Installed & enabled module: $modName\n") }

        return moduleDir.absolutePath
    }

    fun prepareScriptForExecution(moduleDir: File, scriptName: String): File {
        val scriptFile = File(moduleDir, scriptName)
        if (!scriptFile.exists()) {
            throw IOException("Script not found: $scriptName")
        }

        val mrpDir = File("/data/local/tmp/MRP")

        try {
            Shizuku.newProcess(arrayOf("sh", "-c", "rm -rf ${mrpDir.absolutePath}"), null, null).waitFor()
            Shizuku.newProcess(arrayOf("mkdir", "-p", mrpDir.absolutePath), null, null).waitFor()

            val outFile = File(mrpDir, scriptName)

            val process = Shizuku.newProcess(
                arrayOf("sh", "-c", "cat > ${outFile.absolutePath}"), null, null
            )

            val `in` = FileInputStream(scriptFile)
            val os = process.outputStream

            val buffer = ByteArray(4096)
            var len: Int
            while (`in`.read(buffer).also { len = it } > 0) {
                os.write(buffer, 0, len)
            }

            `in`.close()
            os.close()
            process.waitFor()
            process.destroy()

            return outFile
        } catch (e: Exception) {
            throw IOException("Failed to prepare script: ${e.message}", e)
        }
    }

    private fun deleteRecursive(file: File) {
        try {
            val rm = Shizuku.newProcess(arrayOf("rm", "-rf", file.absolutePath), null, null)
            rm.waitFor()
            if (rm.exitValue() != 0) {
                throw IOException("Failed to delete ${file.absolutePath}")
            }
            rm.destroy()
        } catch (e: IOException) {
            throw e
        }
    }
}

private class IOException(message: String, cause: Throwable? = null) : java.io.IOException(message, cause)
