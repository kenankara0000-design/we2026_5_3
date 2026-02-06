package com.example.we2026_5.ui.wasch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.wasch.Article
import com.example.we2026_5.ui.wasch.ErfassungPositionenSection
import com.example.we2026_5.ui.wasch.WaschenErfassungUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaschenErfassungScreen(
    state: WaschenErfassungUiState,
    articles: List<Article>,
    erfassungen: List<com.example.we2026_5.wasch.WaschErfassung>,
    onBack: () -> Unit,
    onCustomerSearchQueryChange: (String) -> Unit,
    onKundeWaehlen: (Customer) -> Unit,
    onBackToKundeSuchen: () -> Unit,
    onErfassungClick: (com.example.we2026_5.wasch.WaschErfassung) -> Unit,
    onNeueErfassungFromListe: () -> Unit,
    onBackFromDetail: () -> Unit,
    onMengeChangeByIndex: (index: Int, menge: Int) -> Unit,
    onNotizChange: (String) -> Unit,
    onSpeichern: () -> Unit,
    onBackFromErfassen: () -> Unit,
    onArtikelSearchQueryChange: (String) -> Unit,
    onAddPosition: (Article) -> Unit,
    onRemovePosition: (Int) -> Unit,
    onDeleteErfassung: (com.example.we2026_5.wasch.WaschErfassung) -> Unit = {}
) {
    val context = LocalContext.current
    val primaryBlue = Color(ContextCompat.getColor(context, R.color.primary_blue))
    val backgroundLight = colorResource(R.color.background_light)
    val textSecondary = colorResource(R.color.text_secondary)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.wasch_title),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryBlue)
            )
        },
        containerColor = backgroundLight
    ) { paddingValues ->
        when (state) {
            is WaschenErfassungUiState.KundeSuchen -> {
                val filtered = state.customers.filter {
                    state.customerSearchQuery.isBlank() || it.displayName.contains(state.customerSearchQuery, ignoreCase = true)
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = state.customerSearchQuery,
                        onValueChange = onCustomerSearchQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.wasch_kunde_suchen), color = textSecondary) },
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.wasch_keine_kunden), color = textSecondary)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filtered.size) { i ->
                                val customer = filtered[i]
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onKundeWaehlen(customer) },
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        customer.displayName,
                                        modifier = Modifier.padding(16.dp),
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            is WaschenErfassungUiState.ErfassungenListe -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            state.customer.displayName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.text_primary)
                        )
                        IconButton(onClick = onBackToKundeSuchen) {
                            Text("↩", fontSize = 18.sp, color = primaryBlue)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.wasch_erfasste_sachen),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.text_primary),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Button(
                        onClick = onNeueErfassungFromListe,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.wasch_neue_erfassung))
                    }
                    Spacer(Modifier.height(12.dp))
                    if (erfassungen.isEmpty()) {
                        Text(
                            stringResource(R.string.wasch_keine_erfassungen),
                            fontSize = 14.sp,
                            color = textSecondary,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(erfassungen.size) { i ->
                                val e = erfassungen[i]
                                val datumStr = com.example.we2026_5.util.DateFormatter.formatDate(e.datum)
                                val zeitStr = if (e.zeit.isNotBlank()) " ${e.zeit}" else ""
                                val count = e.positionen.size
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onErfassungClick(e) }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("$datumStr$zeitStr", fontSize = 16.sp, color = colorResource(R.color.text_primary))
                                            Spacer(Modifier.widthIn(8.dp))
                                            Text(stringResource(R.string.wasch_x_artikel, count), fontSize = 14.sp, color = textSecondary)
                                        }
                                        IconButton(
                                            onClick = { onDeleteErfassung(e) },
                                            modifier = Modifier.padding(start = 4.dp)
                                        ) {
                                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.wasch_erfassung_loeschen), tint = Color.Red)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is WaschenErfassungUiState.ErfassungDetail -> {
                val e = state.erfassung
                val datumStr = com.example.we2026_5.util.DateFormatter.formatDate(e.datum)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "$datumStr ${e.zeit.ifBlank { "-" }}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.text_primary)
                        )
                        Button(
                            onClick = { onDeleteErfassung(e) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.widthIn(4.dp))
                            Text(stringResource(R.string.wasch_erfassung_loeschen), fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    state.positionenAnzeige.forEachIndexed { idx, pos ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(pos.artikelName, modifier = Modifier.weight(1f), fontSize = 14.sp, color = colorResource(R.color.text_primary))
                            Text("${pos.menge} ${pos.einheit}", fontSize = 14.sp, color = textSecondary)
                        }
                    }
                }
            }
            is WaschenErfassungUiState.Erfassen -> {
                val textPrimary = colorResource(R.color.text_primary)
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        stringResource(R.string.wasch_erfassung_kunde),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            state.customer.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        IconButton(onClick = onBackFromErfassen) {
                            Text("↩", fontSize = 18.sp, color = primaryBlue)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.notiz,
                        onValueChange = onNotizChange,
                        label = { Text("Notiz (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(16.dp))
                    val searchResults = articles.filter { it.name.contains(state.artikelSearchQuery, ignoreCase = true) }
                    ErfassungPositionenSection(
                        searchQuery = state.artikelSearchQuery,
                        onSearchQueryChange = onArtikelSearchQueryChange,
                        searchResults = searchResults,
                        onArticleSelected = onAddPosition,
                        zeilen = state.zeilen,
                        onMengeChange = onMengeChangeByIndex,
                        onRemovePosition = onRemovePosition,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                    state.errorMessage?.let { msg ->
                        Spacer(Modifier.height(8.dp))
                        Text(msg, color = Color.Red, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onSpeichern,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSaving
                    ) {
                        Text(if (state.isSaving) "…" else stringResource(R.string.wasch_speichern))
                    }
                }
            }
        }
    }
}
