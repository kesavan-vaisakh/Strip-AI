package com.stripai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StripAITheme {
                val viewModel: MainViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()
                MainScreen(
                    uiState = uiState,
                    onScanClick = viewModel::startScan,
                    onResetClick = viewModel::reset,
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                )
            }
        }
    }
}
