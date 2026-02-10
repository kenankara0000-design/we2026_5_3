package com.example.we2026_5.ui.wasch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R
import com.example.we2026_5.ui.wasch.ErfassungZeile
import java.util.Locale

/** Erlaubt Ziffern und höchstens ein Dezimaltrennzeichen (Komma oder Punkt). Deutscher Standard: 5,5 kg. */
private fun filterDecimalInput(s: String): String {
    val allowed = s.filter { it.isDigit() || it == ',' || it == '.' }
    val comma = allowed.count { it == ',' }
    val period = allowed.count { it == '.' }
    if (comma <= 1 && period <= 1 && comma + period <= 1) return allowed
    return allowed.dropLast(1)
}

/** Parst Menge aus Eingabe (5,5 oder 5.5 → 5.5). */
private fun parseMengeFromInput(s: String): Double = s.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

/** Formatiert Menge für Anzeige (5.5 → "5,5"; 5.0 → "5"). */
private fun formatMengeForDisplay(value: Double): String {
    if (value <= 0.0) return ""
    return if (value == value.toLong().toDouble()) "%.0f".format(Locale.GERMAN, value)
    else "%.2f".format(Locale.GERMAN, value).trimEnd('0').trimEnd(',')
}

@Composable
fun ErfassungPositionenSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<ArticleDisplay>,
    onArticleSelected: (ArticleDisplay) -> Unit,
    zeilen: List<ErfassungZeile>,
    onMengeChange: (Int, Double) -> Unit,
    onRemovePosition: (Int) -> Unit,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.wasch_positionen),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.wasch_artikel_suchen), color = textSecondary) },
            singleLine = true
        )
        if (searchQuery.isNotBlank() && searchResults.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            searchResults.take(8).forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clickable { onArticleSelected(item) },
                    colors = CardDefaults.cardColors(containerColor = colorResource(R.color.surface_white))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "${item.name}${if (item.einheit.isNotBlank()) " (${item.einheit})" else ""}${item.priceLabel?.let { " · $it" } ?: ""}",
                            fontSize = 14.sp,
                            color = textPrimary
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        zeilen.forEachIndexed { index, zeile ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${index + 1}.",
                    fontSize = 12.sp,
                    color = textSecondary,
                    modifier = Modifier.width(20.dp)
                )
                Text(
                    text = zeile.artikelName,
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    color = textPrimary
                )
                Text(
                    text = zeile.einheit.ifBlank { "Stk" },
                    fontSize = 12.sp,
                    color = textSecondary,
                    modifier = Modifier.width(32.dp)
                )
                OutlinedTextField(
                    value = formatMengeForDisplay(zeile.menge),
                    onValueChange = { s ->
                        val filtered = filterDecimalInput(s)
                        onMengeChange(index, parseMengeFromInput(filtered))
                    },
                    modifier = Modifier.width(88.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                IconButton(onClick = { onRemovePosition(index) }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.content_desc_remove_from_list), tint = textSecondary)
                }
            }
        }
    }
}
