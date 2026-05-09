package com.stripai.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stripai.app.scanner.ApkScanner
import com.stripai.app.scanner.ScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ScanUiState {
    data object Idle : ScanUiState()
    data class Scanning(
        val currentApp: String = "",
        val progress: Int = 0,
        val total: Int = 0,
    ) : ScanUiState()
    data class Complete(val result: ScanResult, val completedAtMs: Long = System.currentTimeMillis()) : ScanUiState()
    data class Error(val message: String) : ScanUiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun startScan() {
        if (_uiState.value is ScanUiState.Scanning) return

        viewModelScope.launch {
            _uiState.value = ScanUiState.Scanning()
            try {
                val scanner = ApkScanner(getApplication())
                val result = scanner.scan { progress ->
                    _uiState.value = ScanUiState.Scanning(
                        currentApp = progress.currentAppName,
                        progress = progress.current,
                        total = progress.total,
                    )
                }
                _uiState.value = ScanUiState.Complete(result)
            } catch (e: Exception) {
                _uiState.value = ScanUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun reset() {
        _uiState.value = ScanUiState.Idle
    }
}
