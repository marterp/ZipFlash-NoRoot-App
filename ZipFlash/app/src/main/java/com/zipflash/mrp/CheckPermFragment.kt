package com.zipflash.mrp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.zipflash.mrp.databinding.CheckpermBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class CheckPermFragment : BaseFragment() {

    private var binding: CheckpermBinding? = null
    private var handler: Handler? = null
    private var checkRunnable: Runnable? = null
    private var movedToMain = false
    private var skipShizuku = false
    private var prefs: SharedPreferences? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE_SHIZUKU) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                showPermissionGranted()
                delayGoToMain()
            } else {
                showMessage("Permission denied. Please try again.", android.R.color.holo_red_dark)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = CheckpermBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = binding?.toolbar
        (requireActivity() as androidx.appcompat.app.AppCompatActivity).setSupportActionBar(toolbar)
        toolbar?.setTitleTextColor(requireContext().getColor(R.color.text_primary))
        (requireActivity() as androidx.appcompat.app.AppCompatActivity).supportActionBar?.title = "Permission"

        prefs = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        skipShizuku = prefs!!.getBoolean("skip_shizuku", false)

        handler = Handler(Looper.getMainLooper())
        Shizuku.addRequestPermissionResultListener(permissionResultListener)

        binding?.btnGrant?.setOnClickListener { updateStatus() }

        delayGoToMain()
    }

    private fun updateStatus() {
        if (movedToMain) return

        val shizukuInstalled: Boolean = try {
            requireContext().packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

        if (!shizukuInstalled) {
            if (com.zipflash.mrp.helper.CheckPermHelper.hasWriteSecureSettings(requireContext())) {
                showSkipDialog()
                return
            }
            showMessage("Shizuku not installed.\nPlease install from Play Store or continue in limited mode.", android.R.color.holo_red_dark)
            binding?.btnShizukuAction?.visibility = View.GONE
            if (!BuildConfig.DEBUG) delayGoToMain()
            return
        }

        if (!Shizuku.pingBinder()) {
            showMessage("Shizuku is not running. Please start Shizuku.", android.R.color.darker_gray)
            binding?.btnShizukuAction?.visibility = View.VISIBLE
            binding?.btnShizukuAction?.text = "Open Shizuku"
            binding?.btnShizukuAction?.setOnClickListener {
                val launchIntent = requireContext().packageManager
                    .getLaunchIntentForPackage("moe.shizuku.privileged.api")
                if (launchIntent != null) {
                    startActivity(launchIntent)
                } else {
                    showMessage("Unable to open Shizuku app.", android.R.color.holo_red_dark)
                }
            }
            return
        }

        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            showMessage("Requesting Shizuku permission...", android.R.color.holo_orange_dark)
            binding?.btnShizukuAction?.visibility = View.VISIBLE
            binding?.btnShizukuAction?.text = "Grant Permission"
            binding?.btnShizukuAction?.setOnClickListener {
                Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
            }
            return
        }

        showPermissionGranted()
        delayGoToMain()
    }

    private fun delayGoToMain() {
        if (movedToMain) return
        movedToMain = true

        handler!!.postDelayed({
            Navigation.findNavController(requireView()).navigate(R.id.action_check_perm_to_flash)
        }, 300)
    }

    private fun showPermissionGranted() {
        binding?.tvMessage?.text = "Permission granted on ZipFlash!"
        binding?.tvMessage?.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
        binding?.btnShizukuAction?.visibility = View.GONE
        showNotification("ZipFlash", "Shizuku permission granted!")
    }

    private fun showMessage(msg: String, colorRes: Int) {
        binding?.tvMessage?.text = msg
        binding?.tvMessage?.setTextColor(requireContext().getColor(colorRes))
    }

    private fun showNotification(title: String, message: String) {
        val nm = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "ZipFlash Permission", NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "Notifications for ZipFlash permission"
            nm.createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
        nm.notify(1, builder.build())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.checkperm_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_skip) {
            showSkipDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler?.removeCallbacks(checkRunnable!!)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        binding = null
    }

    companion object {
        private const val REQUEST_CODE_SHIZUKU = 1000
        private const val CHANNEL_ID = "zipflash_channel"
    }

    private fun showSkipDialog() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_skip, null)

        val btnContinue = dialogView.findViewById<Button>(R.id.btnContinue)
        val btnGrant = dialogView.findViewById<Button>(R.id.btnGrant)

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.popup_bg)
        dialog.setCanceledOnTouchOutside(true)

        btnContinue.setOnClickListener {
            com.zipflash.mrp.helper.CheckPermHelper.setSkipShizukuTransient(true)
            dialog.dismiss()
            delayGoToMain()
        }

        btnGrant.setOnClickListener {
            try {
                Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            dialog.dismiss()
        }

        dialog.show()
    }
}
