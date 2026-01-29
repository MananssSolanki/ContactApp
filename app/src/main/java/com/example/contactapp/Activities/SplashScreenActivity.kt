package com.example.contactapp.Activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.contactapp.MainActivity
import com.example.contactapp.RoomDatabase.Contact
import com.example.contactapp.RoomDatabase.ContactDatabase
import com.example.contactapp.databinding.ActivitySplashScreenBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var binding : ActivitySplashScreenBinding
    private val notificationPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            proceedToNextScreen()
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            // background work
            withContext(Dispatchers.IO) {
                initializeContactsIfNeeded()
            }

            proceedToNextScreen()
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        proceedToNextScreen()

    }

    private fun proceedToNextScreen() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private suspend fun initializeContactsIfNeeded() {
        val db = ContactDatabase.getDatabase(applicationContext)
        val dao = db.contactDao()

        try {
            val count = dao.getCount()
            if (count == 0) {
                dao.insert(
                    Contact(
                        name = "Emergency",
                        number = "112"
                    )
                )
                Log.d("SplashScreen", "Default contact inserted")
            }
        } catch (e: Exception) {
            Log.e("SplashScreen", "Contact initialization failed", e)
        }
    }
}