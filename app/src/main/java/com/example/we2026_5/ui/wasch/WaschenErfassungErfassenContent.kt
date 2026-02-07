package com.example.we2026_5.ui.wasch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.example.we2026_5.ui.wasch.ErfassungPositionenSection
import com.example.we2026_5.ui.wasch.ErfassungZeile
import com.example.we2026_5.wasch.Article

@Composable
fun WaschenErfassungErfassenContent(
    customer: Customer,
    notiz: String,
    onNotizChange: (String) -> Unit,
    artikelSearchQuery: String,
    onArtikelSearchQueryChange: (String) -> Unit,
    searchResults: List<Article>,
    zeilen: List<ErfassungZeile>,
    onMengeChangeByIndex: (Int, Int) -> Unit,
    onAddPosition: (Article) -> Unit,
    onRemovePosition: (Int) -> Unit,
    errorMessage: String?,
    isSaving: Boolean,
    onSpeichern: () -> Unit,
    onBackFromErfassen: () -> Unit,
    primaryBlue: androidx.compose.ui.graphics.Color,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
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
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                customer.displayName,
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
            value = notiz,
            onValueChange = onNotizChange,
            label = { Text("Notiz (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        ErfassungPositionenSection(
            searchQuery = artikelSearchQuery,
            onSearchQueryChange = onArtikelSearchQueryChange,
            searchResults = searchResults,
            onArticleSelected = onAddPosition,
            zeilen = zeilen,
            onMengeChange = onMengeChangeByIndex,
            onRemovePosition = onRemovePosition,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )
        errorMessage?.let { msg ->
            Spacer(Modifier.height(8.dp))
            Text(msg, color = Color.Red, fontSize = 12.sp)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onSpeichern,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        ) {
            Text(if (isSaving) "…" else stringResource(R.string.wasch_speichern))
        }
    }
}
