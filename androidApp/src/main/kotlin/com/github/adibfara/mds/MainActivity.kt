package com.github.adibfara.mds

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.github.adibfara.mds.app.AppRoot
import com.github.adibfara.mds.app.di.initializeDependencies
import com.github.adibfara.mds.core.platform.AndroidPlatform

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidPlatform.install(this)
        initializeDependencies()

        setContent {
            AppRoot()
        }
    }
}
