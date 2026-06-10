package com.zipflash.mrp

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.zipflash.mrp.databinding.WelcomePageBinding

class WelcomeFragment : BaseFragment() {

    private var binding: WelcomePageBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = WelcomePageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE)

        val welcomeDone = prefs.getBoolean("welcome_done", false)
        if (welcomeDone) {
            Navigation.findNavController(view).navigate(R.id.action_welcome_to_launch_perm)
            return
        }

        binding?.tvMessage?.visibility = View.INVISIBLE
        binding?.btnContinue?.visibility = View.GONE

        val titleText = "Welcome to ZipFlash!"
        val messageText = "Thanks for installing ZipFlash!\n\nReady to unleash your phone\u2019s full potential?"

        typeText(binding?.tvTitle!!, titleText, 100) {
            binding?.tvMessage?.visibility = View.VISIBLE
            typeText(binding?.tvMessage!!, messageText, 50) { fadeInButton(binding?.btnContinue!!) }
        }

        binding?.btnContinue?.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_welcome_to_launch_perm)
        }
    }

    private fun typeText(textView: TextView, text: String, delay: Long, onComplete: Runnable?) {
        textView.text = ""
        val handler = Handler(Looper.getMainLooper())
        val index = intArrayOf(0)

        val runnable = object : Runnable {
            override fun run() {
                if (index[0] < text.length) {
                    textView.append(text[index[0]].toString())
                    index[0]++
                    handler.postDelayed(this, delay)
                } else {
                    onComplete?.run()
                }
            }
        }

        handler.post(runnable)
    }

    private fun fadeInButton(button: Button) {
        button.visibility = View.VISIBLE
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 500
        fadeIn.fillAfter = true
        button.startAnimation(fadeIn)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
