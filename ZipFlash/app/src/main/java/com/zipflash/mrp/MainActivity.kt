package com.zipflash.mrp

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.zipflash.mrp.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        drawerLayout = binding.drawerLayout

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment?
        navHostFragment?.let {
            navController = it.navController
        }

        val navView = binding.navView
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_flash -> navController.navigate(R.id.flashFragment)
                R.id.nav_shell -> navController.navigate(R.id.shellFragment)
                R.id.nav_modules_manager -> navController.navigate(R.id.moduleManagerFragment)
                R.id.nav_modules -> navController.navigate(R.id.modulesFragment)
                R.id.nav_settings -> navController.navigate(R.id.settingsFragment)
                R.id.nav_about -> navController.navigate(R.id.aboutFragment)
                else -> return@setNavigationItemSelectedListener false
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.welcomeFragment, R.id.launchPermFragment, R.id.checkPermFragment ->
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                else ->
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            }
        }

        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        handleSharedIntent(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleSharedIntent(intent)
    }

    private fun handleSharedIntent(intent: android.content.Intent?) {
        if (intent?.action == android.content.Intent.ACTION_SEND) {
            val uri = intent.getParcelableExtra<android.net.Uri>(android.content.Intent.EXTRA_STREAM)
            if (uri != null) {
                val bundle = Bundle().apply {
                    putParcelable("shared_uri", uri)
                }
                navController.navigate(R.id.flashFragment, bundle)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController?.navigateUp() ?: false
    }

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }
}
