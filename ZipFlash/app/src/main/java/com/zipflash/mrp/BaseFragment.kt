package com.zipflash.mrp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment() {

    protected fun setupToolbar(view: View, title: String) {
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar) ?: return
        val activity = requireActivity() as? AppCompatActivity ?: return

        activity.setSupportActionBar(toolbar)
        toolbar.setTitleTextColor(requireContext().getColor(R.color.text_primary))

        val actionBar = activity.supportActionBar
        actionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
            it.title = title
        }
    }

    protected fun setupDrawerToggle(toolbar: Toolbar) {
        val activity = requireActivity() as? AppCompatActivity ?: return
        val drawerLayout = activity.findViewById<DrawerLayout>(R.id.drawer_layout) ?: return

        val toggle = androidx.appcompat.app.ActionBarDrawerToggle(
            activity, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    protected fun navigateTo(destinationId: Int) {
        val navController = androidx.navigation.Navigation.findNavController(requireView())
        navController.navigate(destinationId)
    }
}
