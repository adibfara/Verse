package com.github.adibfara.mds

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.github.adibfara.mds.app.AppRoot
import com.github.adibfara.mds.app.di.initializeDependencies
import com.github.adibfara.mds.core.platform.AndroidPermissionBridge
import com.github.adibfara.mds.core.platform.AndroidPlatform

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

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidPlatform.install(this)
        AndroidPlatform.installActivity(this)
        AndroidPermissionBridge.launchAllFilesIntent = { allFilesAccessLauncher.launch(it) }
        AndroidPermissionBridge.launchLegacyRequest = { legacyPermissionsLauncher.launch(it) }
        initializeDependencies()

        setContent {
            AppRoot()
        }
    }
}
