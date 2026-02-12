package com.example.we2026_5

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.ui.wasch.WaschenErfassungScreen
import com.example.we2026_5.ui.wasch.WaschenErfassungUiState
import com.example.we2026_5.ui.wasch.WaschenErfassungViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

class WaschenErfassungActivity : AppCompatActivity() {

    private val viewModel: WaschenErfassungViewModel by viewModel()
    private val customerRepository: CustomerRepository by inject()

    private var formularCameraTmpUri: Uri? = null

    private val formularTakePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            formularCameraTmpUri?.let { viewModel.onFormularImageSelected(it.toString()) }
        }
        formularCameraTmpUri = null
    }
    private val formularPickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.onFormularImageSelected(it.toString()) }
    }
    private val formularCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startFormularCamera()
        else Toast.makeText(this, getString(R.string.toast_kamera_verweigert), Toast.LENGTH_SHORT).show()
    }

    private fun showFormularKameraFotoDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_foto_add_title))
            .setItems(arrayOf(getString(R.string.foto_source_camera), getString(R.string.foto_source_gallery))) { _, which ->
                when (which) {
                    0 -> {
                        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            startFormularCamera()
                        } else {
                            formularCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    }
                    1 -> formularPickImageLauncher.launch("image/*")
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun startFormularCamera() {
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: run {
            Toast.makeText(this, getString(R.string.toast_speicherort_nicht_verfuegbar), Toast.LENGTH_SHORT).show()
            return
        }
        val tmpFile = File.createTempFile("waescheliste_${System.currentTimeMillis()}_", ".jpg", dir)
        formularCameraTmpUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", tmpFile)
        formularCameraTmpUri?.let { formularTakePictureLauncher.launch(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val customerId = intent.getStringExtra("CUSTOMER_ID")
        if (customerId != null) {
            CoroutineScope(Dispatchers.Main).launch {
                val customer = customerRepository.getCustomerById(customerId)
                customer?.let { viewModel.startErfassenFuerKunde(it) }
            }
        }
        setContent {
            MaterialTheme {
                val state by viewModel.uiState.collectAsState()
                val articles by viewModel.articles.collectAsState(initial = emptyList())
                val erfassungen by viewModel.erfassungenList.collectAsState(initial = emptyList())
                val belegMonate by viewModel.belegMonate.collectAsState(initial = emptyList())
                val erfassungArticles by viewModel.erfassungArticles.collectAsState(initial = emptyList())
                val showAllgemeinePreiseHint by viewModel.showAllgemeinePreiseHint.collectAsState(initial = true)
                val belegPreiseGross by viewModel.belegPreiseGross.collectAsState(initial = emptyMap())
                val belegMonateErledigt by viewModel.belegMonateErledigt.collectAsState(initial = emptyList())
                var belegMonthKeyHandled by remember { mutableStateOf(false) }
                LaunchedEffect(state, belegMonate) {
                    if (state !is WaschenErfassungUiState.ErfassungenListe || belegMonate.isEmpty() || belegMonthKeyHandled) return@LaunchedEffect
                    val key = intent.getStringExtra("BELEG_MONTH_KEY") ?: return@LaunchedEffect
                    belegMonate.find { it.monthKey == key }?.let {
                        viewModel.openBelegDetail(it)
                        belegMonthKeyHandled = true
                    }
                }
                WaschenErfassungScreen(
                    state = state,
                    articles = articles,
                    erfassungen = erfassungen,
                    belegMonate = belegMonate,
                    erfassungArticles = erfassungArticles,
                    showAllgemeinePreiseHint = showAllgemeinePreiseHint,
                    onBack = {
                        when (state) {
                            is WaschenErfassungUiState.KundeSuchen -> finish()
                            is WaschenErfassungUiState.ErfassungenListe -> viewModel.backToKundeSuchen()
                            is WaschenErfassungUiState.BelegDetail -> viewModel.backFromBelegDetail()
                            is WaschenErfassungUiState.ErfassungDetail -> viewModel.backFromDetail()
                            is WaschenErfassungUiState.Erfassen -> viewModel.backFromErfassenToListe()
                            is WaschenErfassungUiState.Formular -> viewModel.backFromFormularToListe()
                        }
                    },
                    onCustomerSearchQueryChange = { viewModel.setCustomerSearchQuery(it) },
                    onKundeWaehlen = { viewModel.kundeGewaehlt(it) },
                    onBackToKundeSuchen = { viewModel.backToKundeSuchen() },
                    onErfassungClick = { viewModel.openErfassungDetail(it) },
                    onNeueErfassungFromListe = {
                        (state as? WaschenErfassungUiState.ErfassungenListe)?.let { viewModel.neueErfassungClick(it.customer) }
                    },
                    onWaeschelisteFormularFromListe = {
                        (state as? WaschenErfassungUiState.ErfassungenListe)?.let { viewModel.openFormular(it.customer) }
                    },
                    onBelegClick = { viewModel.openBelegDetail(it) },
                    onBelegListeShowErledigtTabChange = { viewModel.setBelegListeShowErledigtTab(it) },
                    onBackFromBelegDetail = { viewModel.backFromBelegDetail() },
                    onBackFromDetail = { viewModel.backFromDetail() },
                    onMengeChangeByIndex = { index, menge -> viewModel.setMengeByIndex(index, menge) },
                    onNotizChange = { viewModel.setNotiz(it) },
                    onSpeichern = {
                        viewModel.speichern {
                            Toast.makeText(this@WaschenErfassungActivity, getString(R.string.wasch_erfassung_gespeichert), Toast.LENGTH_SHORT).show()
                        }
                    },
                    onBackFromErfassen = { viewModel.backFromErfassenToListe() },
                    onArtikelSearchQueryChange = { viewModel.setArtikelSearchQuery(it) },
                    onAddPosition = { viewModel.addPositionFromDisplay(it) },
                    onRemovePosition = { viewModel.removePosition(it) },
                    belegPreiseGross = belegPreiseGross,
                    belegMonateErledigt = belegMonateErledigt,
                    onDeleteErfassung = { erfassung ->
                        AlertDialog.Builder(this@WaschenErfassungActivity)
                            .setTitle(R.string.dialog_erfassung_loeschen_title)
                            .setMessage(R.string.dialog_erfassung_loeschen_message)
                            .setPositiveButton(R.string.dialog_loeschen) { _, _ ->
                                viewModel.deleteErfassung(erfassung) {
                                    Toast.makeText(this@WaschenErfassungActivity, getString(R.string.wasch_erfassung_geloescht), Toast.LENGTH_SHORT).show()
                                }
                            }
                            .setNegativeButton(R.string.btn_cancel, null)
                            .show()
                    },
                    onDeleteBeleg = {
                        (state as? WaschenErfassungUiState.BelegDetail)?.let { detail ->
                            AlertDialog.Builder(this@WaschenErfassungActivity)
                                .setTitle(R.string.dialog_beleg_loeschen_title)
                                .setMessage(getString(R.string.dialog_beleg_loeschen_message, detail.monthLabel, detail.erfassungen.size))
                                .setPositiveButton(R.string.dialog_loeschen) { _, _ ->
                                    viewModel.deleteBeleg(detail.erfassungen) {
                                        Toast.makeText(this@WaschenErfassungActivity, getString(R.string.beleg_geloescht), Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .setNegativeButton(R.string.btn_cancel, null)
                                .show()
                        }
                    },
                    onErledigtBeleg = {
                        (state as? WaschenErfassungUiState.BelegDetail)?.let { detail ->
                            if (detail.erfassungen.any { it.erledigt }) return@let
                            AlertDialog.Builder(this@WaschenErfassungActivity)
                                .setTitle(R.string.dialog_beleg_erledigt_title)
                                .setMessage(R.string.dialog_beleg_erledigt_message)
                                .setPositiveButton(R.string.dialog_ok) { _, _ ->
                                    viewModel.markBelegErledigt(detail.erfassungen) {
                                        Toast.makeText(this@WaschenErfassungActivity, getString(R.string.beleg_erledigt_toast), Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .setNegativeButton(R.string.btn_cancel, null)
                                .show()
                        }
                    },
                    onFormularNameChange = { viewModel.setFormularName(it) },
                    onFormularAdresseChange = { viewModel.setFormularAdresse(it) },
                    onFormularTelefonChange = { viewModel.setFormularTelefon(it) },
                    onFormularMengeChange = { key, value -> viewModel.setFormularMenge(key, value) },
                    onFormularSonstigesChange = { viewModel.setFormularSonstiges(it) },
                    onFormularKameraFoto = { showFormularKameraFotoDialog() },
                    onFormularAbbrechen = { viewModel.backFromFormularToListe() },
                    onFormularSpeichern = {
                        viewModel.saveFormular(
                            onSaved = {
                                Toast.makeText(this@WaschenErfassungActivity, getString(R.string.wasch_erfassung_gespeichert), Toast.LENGTH_SHORT).show()
                            },
                            onSuggestStammdatenUpdate = { customer, adresse, telefon ->
                                AlertDialog.Builder(this@WaschenErfassungActivity)
                                    .setTitle(R.string.dialog_stammdaten_ergaenzen_title)
                                    .setMessage(getString(R.string.dialog_stammdaten_ergaenzen_message))
                                    .setPositiveButton(android.R.string.ok) { _, _ ->
                                        lifecycleScope.launch {
                                            withContext(Dispatchers.IO) {
                                                customerRepository.updateCustomer(
                                                    customer.id,
                                                    mapOf("adresse" to adresse, "telefon" to telefon)
                                                )
                                            }
                                            viewModel.finishFormularAfterStammdatenConfirm()
                                            Toast.makeText(this@WaschenErfassungActivity, getString(R.string.wasch_erfassung_gespeichert), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .setNegativeButton(R.string.btn_cancel) { _, _ ->
                                        viewModel.finishFormularWithoutStammdatenUpdate()
                                        Toast.makeText(this@WaschenErfassungActivity, getString(R.string.wasch_erfassung_gespeichert), Toast.LENGTH_SHORT).show()
                                    }
                                    .show()
                            }
                        )
                    }
                )
            }
        }
    }
}
