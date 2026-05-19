package com.example.contactapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import com.example.contactapp.Adapter.ViewPagerAdapter
import com.example.contactapp.databinding.ItemBottomTabBinding
import com.example.contactapp.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : EdgeToEdgeActivity() {
    private lateinit var binding: ActivityMainBinding



    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                Toast.makeText(this, "All Permissions Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Some permissions were denied. Dialer may not fully function.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        checkPermission()
    }



    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.customView = createBottomTab(
                title = when (position) {
                    0 -> "Phone"
                    1 -> "Recent"
                    2 -> "Contact"
                    else -> "Tab $position"
                },
                iconRes = when (position) {
                    0 -> R.drawable.ic_keypad
                    1 -> R.drawable.ic_call
                    2 -> R.drawable.ic_groups
                    else -> R.drawable.ic_information
                }
            )
        }.attach()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = updateTabState(tab, true)
            override fun onTabUnselected(tab: TabLayout.Tab) = updateTabState(tab, false)
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })

        binding.tabLayout.post {
            binding.tabLayout.getTabAt(binding.tabLayout.selectedTabPosition)?.let {
                updateTabState(it, true)
            }
        }
    }

    private fun createBottomTab(title: String, iconRes: Int): LinearLayout {
        val tabBinding = ItemBottomTabBinding.inflate(LayoutInflater.from(this))
        tabBinding.tabLabel.text = title
        tabBinding.tabIcon.setImageResource(iconRes)
        updateTabView(tabBinding.tabItemRoot, false)
        return tabBinding.tabItemRoot
    }

    private fun updateTabState(tab: TabLayout.Tab, selected: Boolean) {
        val customView = tab.customView as? LinearLayout ?: return
        updateTabView(customView, selected)
    }

    private fun updateTabView(view: LinearLayout, selected: Boolean) {
        val icon = view.findViewById<ImageView>(R.id.tabIcon)
        val label = view.findViewById<TextView>(R.id.tabLabel)
        val textColor = ContextCompat.getColor(
            this,
            if (selected) R.color.brand_primary else R.color.brand_text_inverse
        )
        val iconColor = ContextCompat.getColor(
            this,
            if (selected) R.color.brand_primary else R.color.brand_surface_soft
        )

        view.background = ContextCompat.getDrawable(
            this,
            if (selected) R.drawable.bg_bottom_tab_item_active else android.R.color.transparent
        )
        label.setTextColor(textColor)
        icon.setColorFilter(iconColor)
        view.alpha = if (selected) 1f else 0.92f
    }

    private fun checkPermission() {
        val permissionsToRequest = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.WRITE_CONTACTS)
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CALL_PHONE)
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CALL_LOG)
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

}
