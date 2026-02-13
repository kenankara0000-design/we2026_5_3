package com.example.we2026_5

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.lifecycleScope
import com.example.we2026_5.ui.sevdesk.SevDeskImportScreen
import com.example.we2026_5.ui.sevdesk.SevDeskImportViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class SevDeskImportActivity : AppCompatActivity() {

    private val viewModel: SevDeskImportViewModel by viewModel { parametersOf(applicationContext) }
    private lateinit var networkMonitor: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        networkMonitor = NetworkMonitor(this, lifecycleScope)
        networkMonitor.startMonitoring()
        setContent {
            MaterialTheme {
                val state by viewModel.state.collectAsState()
                val isOnline by networkMonitor.isOnline.observeAsState(initial = true)
                SevDeskImportScreen(
                    state = state,
                    isOnline = isOnline,
                    onBack = { finish() },
                    onTokenChange = { viewModel.setToken(it) },
                    onSaveToken = { viewModel.saveToken() },
                    onImportContacts = { viewModel.importContacts() },
                    onImportArticles = { viewModel.importArticles() },
                    onImportPrices = { viewModel.importKundenpreise() },
                    onDeleteSevDeskContacts = { viewModel.deleteAllSevDeskContacts() },
                    onDeleteSevDeskArticles = { viewModel.deleteAllSevDeskArticles() },
                    onClearReimportList = { viewModel.clearReimportIgnoreList() },
                    onClearMessage = { viewModel.clearMessage() }
                )
            }
        }
    }

    override fun onDestroy() {
        networkMonitor.stopMonitoring()
        super.onDestroy()
    }
}
