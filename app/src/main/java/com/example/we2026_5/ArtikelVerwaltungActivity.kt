package com.example.we2026_5

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.we2026_5.ui.wasch.ArtikelVerwaltungScreen
import com.example.we2026_5.ui.wasch.ArtikelVerwaltungViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class ArtikelVerwaltungActivity : AppCompatActivity() {

    private val viewModel: ArtikelVerwaltungViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val articles by viewModel.articles.collectAsState(initial = emptyList())
                ArtikelVerwaltungScreen(
                    articles = articles,
                    onBack = { finish() },
                    onDeleteArticle = { article ->
                        AlertDialog.Builder(this@ArtikelVerwaltungActivity)
                            .setTitle(R.string.dialog_artikel_loeschen_title)
                            .setMessage(R.string.dialog_artikel_loeschen_message)
                            .setPositiveButton(R.string.dialog_loeschen) { _, _ ->
                                viewModel.deleteArticle(article) { ok ->
                                    if (ok) {
                                        Toast.makeText(
                                            this@ArtikelVerwaltungActivity,
                                            getString(R.string.artikel_geloescht),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            this@ArtikelVerwaltungActivity,
                                            getString(R.string.error_save_generic),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                            .setNegativeButton(R.string.btn_cancel, null)
                            .show()
                    }
                )
            }
        }
    }
}
