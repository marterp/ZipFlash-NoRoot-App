package com.zipflash.mrp

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zipflash.mrp.databinding.FragmentFlashBinding
import com.zipflash.mrp.helper.FileHelper
import com.zipflash.mrp.helper.UpdateChecker
import com.zipflash.mrp.models.AppInfo
import com.zipflash.mrp.ui.AppsAdapter
import com.zipflash.mrp.viewmodel.FlashViewModel
import kotlinx.coroutines.launch

class FlashFragment : BaseFragment() {

    private var binding: FragmentFlashBinding? = null
    private val viewModel: FlashViewModel by viewModels()
    private lateinit var btnSelectFile: Button
    private lateinit var btnRunScript: Button
    private lateinit var cbRevert: CompoundButton
    private var appsAdapter: AppsAdapter? = null
    private var progressDialog: androidx.appcompat.app.AlertDialog? = null

    private var lastOutputLength = 0

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            handleFilePicked(result.data!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentFlashBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var currentVersion = "1.0.0"
        try {
            currentVersion = requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) {}

        UpdateChecker(requireActivity(),
            "https://raw.githubusercontent.com/marterp/ZipFlash-NoRoot/refs/heads/main/update.json"
        ).checkForUpdate(currentVersion)

        binding?.tvOutput?.movementMethod = ScrollingMovementMethod()

        btnSelectFile = binding!!.btnSelect
        btnRunScript = binding!!.btnRun
        btnRunScript.isEnabled = false
        cbRevert = binding!!.cbRevert

        setupToolbar(view, "ZipFlash - No Root")
        binding?.toolbar?.let { setupDrawerToggle(it) }

        setupButtons()

        viewModel.createMRPFolder(requireContext())

        observeViewModel()

        viewModel.preloadApps(requireContext())
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val tv = binding?.tvOutput
                    if (tv != null) {
                        if (state.outputText.length < lastOutputLength) {
                            tv.text = state.outputText
                        } else {
                            tv.append(state.outputText.substring(lastOutputLength))
                        }
                        lastOutputLength = state.outputText.length
                        if (tv.text.isNotEmpty()) {
                            tv.post { tv.scrollTo(0, tv.height) }
                        }
                    }

                    btnRunScript.isEnabled = !state.isRunning && state.isFileSelected
                    btnSelectFile.isEnabled = !state.isRunning

                    if (state.showSuccess) {
                        showSuccessDialog()
                        viewModel.dismissSuccess()
                    }

                    if (state.isRunning && progressDialog == null && !state.showSuccess) {
                        showProgressDialog()
                    } else if (!state.isRunning) {
                        dismissProgressDialog()
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
        for (i in 0 until menu.size()) {
            menu.getItem(i).icon?.setTint(requireContext().getColor(R.color.text_primary))
        }
        val toolbar = binding?.toolbar
        toolbar?.navigationIcon?.setTint(requireContext().getColor(R.color.text_primary))
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_apps) {
            showAppPickerPopup()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupButtons() {
        btnSelectFile.setOnClickListener {
            val zArchiverIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            zArchiverIntent.setPackage("ru.zdevs.zarchiver")
            zArchiverIntent.type = "*/*"
            zArchiverIntent.putExtra(Intent.EXTRA_MIME_TYPES,
                arrayOf("application/zip", "application/x-sh", "text/plain", "text/x-shellscript"))
            zArchiverIntent.addCategory(Intent.CATEGORY_OPENABLE)
            zArchiverIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            if (zArchiverIntent.resolveActivity(requireContext().packageManager) != null) {
                try {
                    filePickerLauncher.launch(zArchiverIntent)
                    return@setOnClickListener
                } catch (_: ActivityNotFoundException) {}
            }

            openGenericFilePicker()
        }

        btnRunScript.setOnClickListener {
            viewModel.runScript(requireContext(), cbRevert.isChecked)
        }
    }

    private fun openGenericFilePicker() {
        val getContentIntent = Intent(Intent.ACTION_GET_CONTENT)
        getContentIntent.type = "*/*"
        getContentIntent.putExtra(Intent.EXTRA_MIME_TYPES,
            arrayOf("application/zip", "application/x-sh", "text/plain", "text/x-shellscript"))
        getContentIntent.addCategory(Intent.CATEGORY_OPENABLE)
        getContentIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            filePickerLauncher.launch(Intent.createChooser(getContentIntent, "Select ZIP, SH, or Text File"))
        } catch (e: ActivityNotFoundException) {
            val openDocIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            openDocIntent.type = "*/*"
            openDocIntent.putExtra(Intent.EXTRA_MIME_TYPES,
                arrayOf("application/zip", "application/x-sh", "text/plain", "text/x-shellscript"))
            openDocIntent.addCategory(Intent.CATEGORY_OPENABLE)
            openDocIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            try {
                filePickerLauncher.launch(Intent.createChooser(openDocIntent, "Select ZIP, SH, or Text File"))
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(requireContext(), "No file picker found to select file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleFilePicked(data: Intent) {
        val uri = data.data ?: return
        val fileName = FileHelper.getFileName(requireContext(), uri)
        val settings = SettingsHelper(requireContext())

        viewModel.selectFile(uri, fileName, settings.isAnyFileMode)
    }

    private fun showProgressDialog() {
        if (progressDialog != null) return
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle("Running")
            .setMessage("Please wait...")
            .setCancelable(false)
        progressDialog = builder.show()
    }

    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun showSuccessDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_success, null)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .create()

        val btnOk = view.findViewById<Button>(R.id.btnOk)
        btnOk.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showAppPickerPopup() {
        val view = layoutInflater.inflate(R.layout.dialog_app_picker, null)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .create()
        dialog.window?.let {
            val lp = it.attributes
            lp.dimAmount = 0.6f
            it.attributes = lp
            it.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        }

        val searchApp = view.findViewById<EditText>(R.id.searchApp)
        val recyclerApps = view.findViewById<RecyclerView>(R.id.recyclerApps)
        val btnClose = view.findViewById<Button>(R.id.btnClose)

        val apps = viewModel.uiState.value.allApps
        if (apps.isEmpty()) {
            Toast.makeText(requireContext(), "No apps available", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            return
        }

        appsAdapter = AppsAdapter(requireContext(), ArrayList(apps))
        recyclerApps.layoutManager = LinearLayoutManager(requireContext())
        recyclerApps.adapter = appsAdapter

        searchApp.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                appsAdapter?.filter(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable) {}
        })

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
