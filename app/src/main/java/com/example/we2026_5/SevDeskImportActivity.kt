package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.FileProvider
import com.example.we2026_5.ui.sevdesk.SevDeskImportScreen
import com.example.we2026_5.ui.sevdesk.SevDeskImportViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                    onDiscoveryTest = { viewModel.runDiscoveryTest() },
                    onClearTestResult = { viewModel.clearTestResult() },
                    onExportTestResult = { exportAndShareTestResult(it) }
                )
            }
        }
    }

    /** Schreibt das Testergebnis (JSON/Text) in eine Datei und öffnet den Teilen-Dialog. */
    private fun exportAndShareTestResult(content: String) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
            val fileName = "sevdesk_api_test_${dateFormat.format(Date())}.txt"
            val file = File(cacheDir, fileName)
            file.writeText(content, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(this, "com.example.we2026_5.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.sevdesk_test_export)))
            Toast.makeText(this, getString(R.string.sevdesk_export_hint), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_export) + ": ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
