package com.zipflash.mrp

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.Navigation
import com.zipflash.mrp.databinding.LaunchPermBinding

class LaunchPermFragment : BaseFragment() {

    private var binding: LaunchPermBinding? = null
    private var currentStep = 0

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { checkNextPermission() }

    private val installLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { showPermissionGranted("Install apps permission granted") }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = LaunchPermBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = binding?.toolbar
        (requireActivity() as androidx.appcompat.app.AppCompatActivity).setSupportActionBar(toolbar)
        toolbar?.setTitleTextColor(requireContext().getColor(R.color.text_primary))
        (requireActivity() as androidx.appcompat.app.AppCompatActivity).supportActionBar?.title = "Permissions"

        checkNextPermission()
    }

    private fun checkNextPermission() {
        when (currentStep) {
            0 -> {
                if (!Settings.System.canWrite(requireContext())) {
                    setPermissionStep(
                        "System settings permission is required.",
                        "Grant System Settings",
                        requireContext().getColor(R.color.accent_warning)
                    ) {
                        settingsLauncher.launch(
                            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                Uri.parse("package:${requireContext().packageName}")))
                    }
                } else {
                    showPermissionGranted("System settings permission granted!")
                }
            }

            1 -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    !requireContext().packageManager.canRequestPackageInstalls()
                ) {
                    setPermissionStep(
                        "Permission to install apps is required.",
                        "Allow Install Apps",
                        requireContext().getColor(R.color.accent_warning)
                    ) {
                        installLauncher.launch(
                            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:${requireContext().packageName}")))
                    }
                } else {
                    showPermissionGranted("Install apps permission granted!")
                }
            }

            2 -> {
                setPermissionStep(
                    "Grant access to list apps",
                    "Continue",
                    requireContext().getColor(R.color.text_primary)
                ) {
                    val pm = requireContext().packageManager
                    val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    showPermissionGranted("List apps permission granted!")
                }
            }

            else -> {
                markPermissionsDone()
                goToMain()
            }
        }
    }

    private fun showPermissionGranted(message: String) {
        binding?.tvMessage?.text = message
        binding?.tvMessage?.setTextColor(requireContext().getColor(R.color.accent_success))
        binding?.btnGrant?.text = "Continue"
        binding?.btnGrant?.setOnClickListener {
            currentStep++
            checkNextPermission()
        }
    }

    private fun setPermissionStep(message: String, buttonText: String, color: Int, listener: View.OnClickListener) {
        binding?.tvMessage?.text = message
        binding?.tvMessage?.setTextColor(color)
        binding?.btnGrant?.text = buttonText
        binding?.btnGrant?.setOnClickListener(listener)
    }

    private fun markPermissionsDone() {
        val prefs = requireContext().getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("launch_perm_done", true)
            .putBoolean("welcome_done", true)
            .apply()
    }

    private fun goToMain() {
        val prefs = requireContext().getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE)
        val launchPermDone = prefs.getBoolean("launch_perm_done", false)
        val welcomeDone = prefs.getBoolean("welcome_done", false)

        if (launchPermDone && welcomeDone) {
            Navigation.findNavController(requireView()).navigate(R.id.action_launch_perm_to_flash)
        }
    }

    override fun onResume() {
        super.onResume()
        checkNextPermission()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object
}
