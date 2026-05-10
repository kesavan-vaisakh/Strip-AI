package com.stripai.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripai.app.ui.MainScreen
import com.stripai.app.ui.MainViewModel
import com.stripai.app.ui.theme.StripAITheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Whether granted or denied, proceed with scan — StorageScanner handles denial gracefully
        viewModel.startScan()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StripAITheme {
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                val uiState by viewModel.uiState.collectAsState()
                MainScreen(
                    uiState = uiState,
                    onScanClick = ::requestStorageAndScan,
                    onResetClick = viewModel::reset,
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                )
            }
        }
    }

    private fun requestStorageAndScan() {
        // API 33+ uses MediaStore without needing READ_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            viewModel.startScan()
            return
        }
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            viewModel.startScan()
        } else {
            storagePermissionLauncher.launch(permission)
        }
    }
}
