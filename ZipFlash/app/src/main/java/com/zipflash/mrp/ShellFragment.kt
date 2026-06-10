package com.zipflash.mrp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.text.style.ForegroundColorSpan
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zipflash.mrp.databinding.ShellMainBinding
import com.zipflash.mrp.viewmodel.ShellViewModel
import kotlinx.coroutines.launch

class ShellFragment : BaseFragment() {

    private var binding: ShellMainBinding? = null
    private val viewModel: ShellViewModel by viewModels()
    private var isUserScrolling = false
    private var lastScrollTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = ShellMainBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.tvOutput?.movementMethod = ScrollingMovementMethod()
        binding?.tvOutput?.setTextIsSelectable(true)
        binding?.tvOutput?.isFocusable = true
        binding?.tvOutput?.isFocusableInTouchMode = true
        binding?.tvOutput?.isVerticalScrollBarEnabled = true

        binding?.tvOutput?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> isUserScrolling = true
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isUserScrolling = false
            }
            false
        }

        setupToolbar(view, "Shell")
        binding?.toolbar?.let { setupDrawerToggle(it) }

        viewModel.loadHistory(requireContext())
        setupRunButton()
        setupCommandHistory()
        updateRecentCommands()

        observeViewModel()

        binding?.btnRun?.isEnabled = false
        binding?.etCommand?.requestFocus()
        binding?.etCommand?.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                binding?.btnRun?.isEnabled = s.toString().trim().isNotEmpty() && !viewModel.uiState.value.isRunning
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val tv = binding?.tvOutput
                    if (tv != null) {
                        tv.text = state.outputText
                        if (!isUserScrolling) scrollToBottom()
                    }
                    binding?.btnRun?.isEnabled = binding?.etCommand?.text.toString().trim().isNotEmpty() && !state.isRunning
                    updateRecentCommands()
                }
            }
        }
    }

    private fun setupRunButton() {
        binding?.btnRun?.setOnClickListener {
            val cmd = binding?.etCommand?.text.toString().trim()
            viewModel.recordCommand(cmd)
            viewModel.saveHistory(requireContext())
            viewModel.handleCommand(cmd)
            binding?.etCommand?.setText("")
        }
    }

    private fun setupCommandHistory() {
        binding?.etCommand?.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        val cmd = viewModel.navigateHistory(ShellViewModel.KEY_UP)
                        if (cmd != null) {
                            binding?.etCommand?.setText(cmd)
                            binding?.etCommand?.setSelection(cmd.length)
                            return@setOnKeyListener true
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        val cmd = viewModel.navigateHistory(ShellViewModel.KEY_DOWN)
                        if (cmd != null) {
                            binding?.etCommand?.setText(cmd)
                            binding?.etCommand?.setSelection(cmd.length)
                            return@setOnKeyListener true
                        }
                    }
                }
            }
            false
        }
    }

    private fun updateRecentCommands() {
        val container = binding?.recentCommandsContainer ?: return
        container.removeAllViews()
        val maxRecent = 8
        val history = viewModel.uiState.value.commandHistory
        val start = maxOf(0, history.size - maxRecent)
        for (i in history.indices.reversed()) {
            if (i < start) break
            val cmd = history[i]
            val btn = Button(requireContext())
            btn.text = cmd
            btn.isSingleLine = true
            btn.ellipsize = android.text.TextUtils.TruncateAt.END
            btn.maxWidth = dpToPx(150)
            btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            btn.setBackgroundResource(R.drawable.card_bg)
            btn.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0)
            btn.layoutParams = params
            btn.isAllCaps = false
            btn.textSize = 13f
            btn.setOnClickListener {
                binding?.etCommand?.setText(cmd)
                binding?.etCommand?.setSelection(cmd.length)
            }
            container.addView(btn)
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return Math.round(dp.toFloat() * density)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_shell, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_clear -> {
                viewModel.clearOutput()
                scrollToBottom()
                return true
            }
            R.id.action_copy -> {
                val text = binding?.tvOutput?.text.toString()
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("ShellOutput", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Output copied", Toast.LENGTH_SHORT).show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun scrollToBottom() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScrollTime < SCROLL_DEBOUNCE_MS) return
        lastScrollTime = currentTime

        binding?.tvOutput?.post {
            val tv = binding?.tvOutput
            if (tv != null && tv.layout != null) {
                val scrollAmount = tv.layout.getLineTop(tv.lineCount) - tv.height
                tv.scrollTo(0, if (scrollAmount > 0) scrollAmount else 0)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        private const val SCROLL_DEBOUNCE_MS = 100L
    }
}
