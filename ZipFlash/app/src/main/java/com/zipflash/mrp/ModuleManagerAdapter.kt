package com.zipflash.mrp

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class ModuleManagerAdapter(
    private val context: Context,
    private val modules: MutableList<File>,
    private val emptyView: TextView?
) : RecyclerView.Adapter<ModuleManagerAdapter.ViewHolder>() {

    private val prefs: SharedPreferences = context.getSharedPreferences("modules_state", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        toggleEmptyView()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.item_module, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val moduleDir = modules[position]
        val moduleName = moduleDir.name

        holder.txtName.text = moduleName

        if (!prefs.contains(moduleName)) {
            saveModuleState(moduleName, true)
        }
        val enabled = prefs.getBoolean(moduleName, true)

        holder.switchEnable.setOnCheckedChangeListener(null)
        holder.switchEnable.isChecked = enabled

        holder.switchEnable.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.isEnabled = false

            if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                (context as Activity).runOnUiThread { buttonView.isEnabled = true }
                return@setOnCheckedChangeListener
            }

            if (isChecked) {
                if (!prefs.getBoolean(moduleName, true)) {
                    runScriptWithShizuku(moduleDir, "run.sh", "Enabling $moduleName...",
                        object : OnScriptFinishedListener {
                            override fun onFinished() {
                                (context as Activity).runOnUiThread {
                                    saveModuleState(moduleName, true)
                                    val pos = getSafeAdapterPosition(holder)
                                    if (pos >= 0) safeNotifyItemChanged(pos)
                                    buttonView.isEnabled = true
                                }
                            }

                            override fun onError(error: String) {
                                (context as Activity).runOnUiThread {
                                    val pos = getSafeAdapterPosition(holder)
                                    if (pos >= 0) safeNotifyItemChanged(pos)
                                    Toast.makeText(context, "Error enabling $moduleName: $error", Toast.LENGTH_LONG).show()
                                    buttonView.isEnabled = true
                                }
                            }
                        })
                } else {
                    buttonView.isEnabled = true
                }
            } else {
                runScriptWithShizuku(moduleDir, "revert.sh", "Disabling $moduleName...",
                    object : OnScriptFinishedListener {
                        override fun onFinished() {
                            (context as Activity).runOnUiThread {
                                saveModuleState(moduleName, false)
                                val pos = getSafeAdapterPosition(holder)
                                if (pos >= 0) safeNotifyItemChanged(pos)
                                buttonView.isEnabled = true
                            }
                        }

                        override fun onError(error: String) {
                            (context as Activity).runOnUiThread {
                                val pos = getSafeAdapterPosition(holder)
                                if (pos >= 0) safeNotifyItemChanged(pos)
                                Toast.makeText(context, "Error disabling $moduleName: $error", Toast.LENGTH_LONG).show()
                                buttonView.isEnabled = true
                            }
                        }
                    })
            }
        }

        holder.btnDelete.setOnClickListener {
            val inflater = LayoutInflater.from(context)
            val dialogView = inflater.inflate(R.layout.dialog_module_remove, null)

            val btnYes = dialogView.findViewById<Button>(R.id.btnYes)
            val btnNo = dialogView.findViewById<Button>(R.id.btnNo)
            val messageText = dialogView.findViewById<TextView>(R.id.message)

            messageText?.text = "Are you sure you want to remove the module $moduleName?"

            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            btnYes.setOnClickListener {
                holder.btnDelete.isEnabled = false
                dialog.dismiss()

                if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    (context as Activity).runOnUiThread {
                        Toast.makeText(context, "Shizuku is not available. Please grant permissions.", Toast.LENGTH_LONG).show()
                        holder.btnDelete.isEnabled = true
                    }
                    return@setOnClickListener
                }

                runScriptWithShizuku(moduleDir, "revert.sh", "Removing $moduleName...",
                    object : OnScriptFinishedListener {
                        override fun onFinished() {
                            removeModuleCompletely(holder, moduleDir, moduleName)
                        }

                        override fun onError(error: String) {
                            removeModuleCompletely(holder, moduleDir, moduleName)
                        }
                    })
            }

            btnNo.setOnClickListener { dialog.dismiss() }
            dialog.show()
        }
    }

    fun updateData(newModules: List<File>) {
        modules.clear()
        modules.addAll(newModules)
        notifyDataSetChanged()
        toggleEmptyView()
    }

    override fun getItemCount(): Int = modules.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName: TextView = itemView.findViewById(R.id.moduleName)
        val switchEnable: Switch = itemView.findViewById(R.id.moduleSwitch)
        val btnDelete: ImageButton = itemView.findViewById(R.id.moduleDelete)
    }

    private fun saveModuleState(moduleName: String, enabled: Boolean) {
        context.getSharedPreferences("modules_state", Context.MODE_PRIVATE)
            .edit().putBoolean(moduleName, enabled).apply()
    }

    private fun getSafeAdapterPosition(holder: ViewHolder): Int {
        val pos = holder.adapterPosition
        return if (pos == RecyclerView.NO_POSITION) -1 else pos
    }

    private fun deleteRecursive(fileOrDir: File, listener: OnScriptFinishedListener) {
        scope.launch {
            try {
                if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    throw Exception("Shizuku is not available or permission not granted")
                }
                val rm = Shizuku.newProcess(arrayOf("rm", "-rf", fileOrDir.absolutePath), null, null)
                val err = StringBuilder()
                val er = BufferedReader(InputStreamReader(rm.errorStream))
                var line: String?
                while (er.readLine().also { line = it } != null) err.append(line).append("\n")
                er.close()
                rm.waitFor()
                if (rm.exitValue() != 0) throw Exception(err.toString())
                rm.destroy()
                listener.onFinished()
            } catch (e: Exception) {
                listener.onError("Error deleting ${fileOrDir.name}: ${e.message}")
            }
        }
    }

    private fun removeModuleCompletely(holder: ViewHolder, moduleDir: File, moduleName: String) {
        (context as Activity).runOnUiThread {
            val pos = getSafeAdapterPosition(holder)
            deleteRecursive(moduleDir, object : OnScriptFinishedListener {
                override fun onFinished() {
                    saveModuleState(moduleName, false)
                    if (pos >= 0 && pos < modules.size) {
                        modules.removeAt(pos)
                        notifyItemRemoved(pos)
                        toggleEmptyView()
                    }
                    holder.btnDelete.isEnabled = true
                }

                override fun onError(error: String) {
                    holder.btnDelete.isEnabled = true
                }
            })
        }
    }

    private fun runScriptWithShizuku(
        moduleDir: File,
        scriptName: String,
        dialogMessage: String,
        listener: OnScriptFinishedListener
    ) {
        val loadingView = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null)
        val tvLoadingMessage = loadingView.findViewById<TextView>(R.id.tvLoadingMessage)
        tvLoadingMessage.text = dialogMessage

        val dialog = AlertDialog.Builder(context)
            .setView(loadingView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        (context as Activity).runOnUiThread { dialog.show() }

        scope.launch {
            try {
                if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
                    throw Exception("Shizuku not available or no permission")

                if (!moduleDir.exists() || !moduleDir.isDirectory)
                    throw Exception("Module directory not found: ${moduleDir.absolutePath}")

                val scriptSrc = File(moduleDir, scriptName)

                if (scriptName == "revert.sh" && !scriptSrc.exists()) {
                    (context as Activity).runOnUiThread {
                        dialog.dismiss()
                        listener.onFinished()
                    }
                    return@launch
                }

                if (!scriptSrc.exists())
                    throw Exception("Script not found: ${scriptSrc.absolutePath}")

                val chmod = Shizuku.newProcess(arrayOf("chmod", "755", scriptSrc.absolutePath), null, null)
                chmod.waitFor()
                chmod.destroy()

                val cmd = "cd ${moduleDir.absolutePath} && ./$scriptName"
                val exec = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)

                val out = BufferedReader(InputStreamReader(exec.inputStream))
                val output = StringBuilder()
                var line: String?
                while (out.readLine().also { line = it } != null) output.append(line).append("\n")
                out.close()

                val err = BufferedReader(InputStreamReader(exec.errorStream))
                val errorOutput = StringBuilder()
                while (err.readLine().also { line = it } != null) errorOutput.append(line).append("\n")
                err.close()

                val exitCode = exec.waitFor()
                exec.destroy()

                (context as Activity).runOnUiThread {
                    dialog.dismiss()
                    if (exitCode == 0) {
                        listener.onFinished()
                    } else {
                        listener.onError("Script exited with code $exitCode: $errorOutput")
                    }
                }
            } catch (e: Exception) {
                (context as Activity).runOnUiThread {
                    dialog.dismiss()
                    listener.onError("Error: ${e.message}")
                }
            }
        }
    }

    private fun toggleEmptyView() {
        if (emptyView != null) {
            emptyView.visibility = if (modules.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun safeNotifyItemChanged(position: Int) {
        if (position != RecyclerView.NO_POSITION && position < itemCount)
            notifyItemChanged(position)
    }

    interface OnScriptFinishedListener {
        fun onFinished()
        fun onError(error: String)
    }
}
