package com.kev.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    // Bumped whenever apps are installed, removed or changed so the grid reloads.
    private var refreshKey by mutableIntStateOf(0)

    // Bumped each time HOME is pressed while we're already showing, so the UI can jump back to the home page.
    private var homePresses by mutableIntStateOf(0)

    // Bumped each time we come back to the foreground, so Continue watching reflects what was just watched.
    private var recentsKey by mutableIntStateOf(0)

    private val askForListings = registerForActivityResult(ActivityResultContracts.RequestPermission()) { recentsKey++ }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshKey++
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Profile.load(this)

        // A home screen has nowhere to go back to.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_EXPORTED)

        setContent {
            LauncherTheme {
                HomeScreen(refreshKey, homePresses, recentsKey)
            }
        }

        // Continue watching reads other apps' rows from the TV provider.
        if (ContextCompat.checkSelfPermission(this, READ_TV_LISTINGS) != PackageManager.PERMISSION_GRANTED) {
            askForListings.launch(READ_TV_LISTINGS)
        }
    }

    override fun onStart() {
        super.onStart()
        recentsKey++
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homePresses++
    }

    override fun onDestroy() {
        unregisterReceiver(packageReceiver)
        super.onDestroy()
    }

    private companion object {
        const val READ_TV_LISTINGS = "android.permission.READ_TV_LISTINGS"
    }
}
