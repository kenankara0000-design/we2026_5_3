package com.example.we2026_5

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.we2026_5.ui.sevdesk.SevDeskImportScreen
import com.example.we2026_5.ui.sevdesk.SevDeskImportViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class SevDeskImportActivity : AppCompatActivity() {

    private val viewModel: SevDeskImportViewModel by viewModel { parametersOf(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val state by viewModel.state.collectAsState()
                SevDeskImportScreen(
                    state = state,
                    onBack = { finish() },
                    onTokenChange = { viewModel.setToken(it) },
                    onSaveToken = { viewModel.saveToken() },
                    onImportContacts = { viewModel.importContacts() },
                    onImportArticles = { viewModel.importArticles() },
                    onClearMessage = { viewModel.clearMessage() },
                    onRunApiTest = { viewModel.runApiTest() },
                    onClearTestResult = { viewModel.clearTestResult() }
                )
            }
        }
    }
}
