package com.example.we2026_5.ui.wasch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
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
    onBack: () -> Unit,
    onNeueErfassung: () -> Unit,
    onSevDeskImport: () -> Unit,
    onKundeWaehlen: (Customer) -> Unit,
    onMengeChange: (articleId: String, menge: Int) -> Unit,
    onMengeChangeByIndex: (index: Int, menge: Int) -> Unit,
    onZeitChange: (String) -> Unit,
    onNotizChange: (String) -> Unit,
    onSpeichern: () -> Unit,
    onBackToKundeWaehlen: () -> Unit,
    onDatumClick: (currentDatum: Long) -> Unit,
    onArtikelSearchQueryChange: (String) -> Unit,
    onAddPosition: (Article) -> Unit,
    onRemovePosition: (Int) -> Unit
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
            is WaschenErfassungUiState.Auswahl -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onNeueErfassung,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.wasch_btn_neue_erfassung), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onSevDeskImport,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.wasch_btn_sevdesk), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            is WaschenErfassungUiState.KundeWaehlen -> {
                if (state.customers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.wasch_keine_kunden), color = textSecondary)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(
                            stringResource(R.string.wasch_kunde_waehlen),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        state.customers.forEach { customer ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBackToKundeWaehlen() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            state.customer.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text("↩", fontSize = 18.sp, color = primaryBlue)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.wasch_erfassung_datum),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onDatumClick(state.datum) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            DateFormatter.formatDate(state.datum),
                            fontSize = 16.sp,
                            color = textPrimary
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.zeit,
                        onValueChange = onZeitChange,
                        label = { Text(stringResource(R.string.wasch_erfassung_zeit)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("z.B. 14:30") }
                    )
                    Spacer(Modifier.height(4.dp))
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
