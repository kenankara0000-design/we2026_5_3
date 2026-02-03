package com.example.we2026_5

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.TourPlanRepository
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.util.TerminRegelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.android.ext.android.inject
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
class TerminAnlegenUnregelmaessigActivity : AppCompatActivity() {

    private val repository: CustomerRepository by inject()
    private val tourPlanRepository: TourPlanRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val customerId = intent.getStringExtra("CUSTOMER_ID") ?: run {
            Toast.makeText(this, getString(R.string.error_customer_id_missing), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            val scope = rememberCoroutineScope()
            var customer by remember { mutableStateOf<Customer?>(null) }
            var vorschlaege by remember { mutableStateOf<List<TerminSlotVorschlag>>(emptyList()) }
            var selectedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
            var isLoading by remember { mutableStateOf(true) }
            var loadError by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(customerId) {
                isLoading = true
                loadError = null
                try {
                    val c = withContext(Dispatchers.IO) {
                        repository.getCustomerById(customerId)
                    }
                    customer = c
                    if (c != null) {
                        val tourSlots = withTimeoutOrNull(5000) {
                            withContext(Dispatchers.IO) {
                                tourPlanRepository.getTourSlotsFlow().first()
                            }
                        } ?: emptyList()
                        vorschlaege = TerminRegelManager.schlageSlotsVor(
                            kunde = c,
                            regel = null,
                            tourSlots = tourSlots.orEmpty(),
                            startDatum = System.currentTimeMillis(),
                            tageVoraus = 56
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("TerminAnlegenUnregel", "Fehler beim Laden", e)
                    loadError = e.message ?: "Unbekannter Fehler"
                } finally {
                    isLoading = false
                }
            }

            val context = LocalContext.current
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.dialog_termin_anlegen_title)) },
                        navigationIcon = {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            }
                        }
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (loadError != null) {
                        Text(loadError!!, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(getString(R.string.error_load_generic))
                    } else if (customer == null) {
                        Text("Kunde nicht gefunden")
                    } else if (vorschlaege.isEmpty()) {
                        Text("Keine Terminvorschläge verfügbar. Bitte A/L-Tage beim Kunden setzen.")
                    } else {
                        Text(
                            "Termine auswählen (bis 8 Wochen):",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(vorschlaege.size) { index ->
                                val v = vorschlaege[index]
                                val isSelected = index in selectedIds
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedIds = if (isSelected) {
                                                selectedIds - index
                                            } else {
                                                selectedIds + index
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                DateFormatter.formatDateWithWeekday(v.datum),
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                v.typ.name,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        if (isSelected) {
                                            Text("✓", style = MaterialTheme.typography.titleMedium)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (selectedIds.isEmpty()) {
                                    Toast.makeText(context, "Bitte mindestens einen Termin wählen", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                scope.launch {
                                    val c = customer ?: return@launch
                                    val list = vorschlaege
                                    val toAdd = selectedIds.mapNotNull { idx ->
                                        list.getOrNull(idx)
                                    }
                                    if (toAdd.isEmpty()) {
                                        Toast.makeText(context, "Bitte mindestens einen Termin wählen", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }
                                    val newIntervalle = toAdd.map { slot ->
                                        CustomerIntervall(
                                            id = UUID.randomUUID().toString(),
                                            abholungDatum = if (slot.typ == TerminTyp.ABHOLUNG) slot.datum else 0,
                                            auslieferungDatum = if (slot.typ == TerminTyp.AUSLIEFERUNG) slot.datum else 0,
                                            wiederholen = false,
                                            intervallTage = 0,
                                            intervallAnzahl = 0
                                        )
                                    }
                                    val existing = c.intervalle ?: emptyList()
                                    val updated = c.copy(intervalle = existing + newIntervalle)
                                    try {
                                        val success = withContext(Dispatchers.IO) {
                                            repository.saveCustomer(updated)
                                        }
                                        if (success) {
                                            Toast.makeText(context, getString(R.string.toast_gespeichert), Toast.LENGTH_SHORT).show()
                                            finish()
                                        } else {
                                            Toast.makeText(context, getString(R.string.error_save_generic), Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("TerminAnlegenUnregel", "Fehler beim Speichern", e)
                                        Toast.makeText(context, getString(R.string.error_save_generic), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Gewählte Termine anlegen")
                        }
                    }
                }
            }
        }
    }
}
