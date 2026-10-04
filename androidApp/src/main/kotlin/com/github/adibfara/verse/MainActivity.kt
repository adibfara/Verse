package com.github.adibfara.verse

import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.github.adibfara.verse.app.AppRoot
import com.github.adibfara.verse.app.di.initializeDependencies
import com.github.adibfara.verse.core.platform.AndroidPermissionBridge
import com.github.adibfara.verse.core.platform.AndroidPlatform
import com.github.adibfara.verse.core.platform.AndroidSpeechBridge

class MainActivity : ComponentActivity() {

    private val allFilesAccessLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        AndroidPermissionBridge.completeAllFilesAccess()
    }

    private val legacyPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        AndroidPermissionBridge.completeLegacyPermissions(result.values.all { it })
    }

    private val speechRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val text = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        AndroidSpeechBridge.complete(text)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidPlatform.install(this)
        AndroidPlatform.installActivity(this)
        AndroidPermissionBridge.launchAllFilesIntent = { allFilesAccessLauncher.launch(it) }
        AndroidPermissionBridge.launchLegacyRequest = { legacyPermissionsLauncher.launch(it) }
        AndroidSpeechBridge.launchSpeechIntent = { speechRecognitionLauncher.launch(it) }
        initializeDependencies()

        setContent {
            AppRoot()
        }
    }
}
