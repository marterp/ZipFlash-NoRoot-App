package com.zipflash.mrp

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.zipflash.mrp.databinding.ActivityAboutBinding
import com.zipflash.mrp.util.CustomTabsHelper

class AboutFragment : BaseFragment() {

    private var binding: ActivityAboutBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = ActivityAboutBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar(view, "About")

        binding?.tvDescription?.text = Html.fromHtml(getString(R.string.app_description))
        binding?.tvDescription?.movementMethod = android.text.method.LinkMovementMethod.getInstance()

        binding?.btnZipflashWebsite?.setOnClickListener {
            CustomTabsHelper.openUrl(requireContext(), "https://zip-flash-modules.vercel.app/")
        }

        binding?.btnGithub?.setOnClickListener { CustomTabsHelper.openUrl(requireContext(), "https://github.com/marterp/ZipFlash-NoRoot") }
        binding?.btnYouTube?.setOnClickListener { CustomTabsHelper.openUrl(requireContext(), "https://youtube.com/@mister_p27official?si=cfCk3QK3_xDPL72N") }
        binding?.btnTikTok?.setOnClickListener { CustomTabsHelper.openUrl(requireContext(), "https://www.tiktok.com/@yourusername") }
        binding?.btnFacebook?.setOnClickListener { CustomTabsHelper.openUrl(requireContext(), "https://www.facebook.com/yourpage") }

        binding?.btnDonate?.setOnClickListener {
            CustomTabsHelper.openUrl(requireContext(), "https://ko-fi.com/mister_p0427")
        }

        binding?.btnTermsCond?.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_terms_privacy, null)

            val dialog = android.app.AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
                .setView(dialogView)
                .create()

            val btnClose = dialogView.findViewById<Button>(R.id.btnClose)
            btnClose.setOnClickListener { dialog.dismiss() }

            dialog.show()

            dialog.window?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
