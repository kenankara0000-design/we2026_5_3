package com.example.we2026_5.ui.wasch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.wasch.Article
import com.example.we2026_5.wasch.WaschErfassung
import java.util.Locale

/** Aggregierte Position für Gesamtblock: Artikel + summierte Menge + Einheit. */
private data class GesamtZeile(val articleId: String, val artikelName: String, val menge: Double, val einheit: String)

@Composable
fun WaschenErfassungBelegDetailContent(
    customerName: String,
    monthLabel: String,
    erfassungen: List<WaschErfassung>,
    articlesMap: Map<String, Article>,
    /** Brutto-Preis pro articleId für Gesamtpreis-Berechnung (Tour- oder Kundenpreise). */
    preiseGross: Map<String, Double>,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
    onBack: () -> Unit,
    onDeleteBeleg: () -> Unit,
    /** Nur bei offenen Belegen (nicht erledigt). */
    onErledigt: (() -> Unit)? = null
) {
    var kebabExpanded by remember { mutableStateOf(false) }

    val gesamtZeilen: List<GesamtZeile> = remember(erfassungen, articlesMap) {
        erfassungen
            .flatMap { it.positionen }
            .groupBy { it.articleId }
            .map { (articleId, positions) ->
                val sum = positions.sumOf { it.menge }
                val einheit = positions.firstOrNull()?.einheit?.ifBlank { null } ?: "Stk"
                GesamtZeile(
                    articleId = articleId,
                    artikelName = articlesMap[articleId]?.name ?: articleId,
                    menge = sum,
                    einheit = einheit.ifBlank { "Stk" }
                )
            }
            .sortedBy { it.artikelName }
    }

    val gesamtPreisBrutto: Double = remember(erfassungen, preiseGross) {
        erfassungen.flatMap { it.positionen }.sumOf { pos ->
            (pos.menge * (preiseGross[pos.articleId] ?: 0.0))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    customerName,
                    fontSize = 16.sp,
                    color = textSecondary
                )
                Text(
                    monthLabel,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            }
            IconButton(onClick = { kebabExpanded = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.content_desc_more_options),
                    tint = textPrimary
                )
            }
            DropdownMenu(
                expanded = kebabExpanded,
                onDismissRequest = { kebabExpanded = false }
            ) {
                if (onErledigt != null && erfassungen.isNotEmpty() && erfassungen.all { !it.erledigt }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.beleg_erledigt), color = textPrimary) },
                        onClick = {
                            kebabExpanded = false
                            onErledigt()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.beleg_loeschen), color = Color.Red) },
                    onClick = {
                        kebabExpanded = false
                        onDeleteBeleg()
                    }
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        erfassungen.forEach { erfassung ->
            val datumStr = DateFormatter.formatDate(erfassung.datum)
            val zeitStr = erfassung.zeit.ifBlank { "-" }
            val positionenAnzeige = erfassung.positionen.map { pos ->
                val name = articlesMap[pos.articleId]?.name ?: pos.articleId
                ErfassungPositionAnzeige(name, pos.menge, pos.einheit.ifBlank { "Stk" })
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "$datumStr $zeitStr",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    positionenAnzeige.forEach { pos ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(pos.artikelName, fontSize = 14.sp, color = textPrimary, modifier = Modifier.weight(1f))
                            Text("${formatMenge(pos.menge)} ${pos.einheit}", fontSize = 14.sp, color = textSecondary)
                        }
                    }
                }
            }
        }

        if (gesamtZeilen.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.wasch_gesamt),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    gesamtZeilen.forEach { z ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(z.artikelName, fontSize = 14.sp, color = textPrimary, modifier = Modifier.weight(1f))
                            Text("${formatMenge(z.menge)} ${z.einheit}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                        }
                    }
                    if (gesamtPreisBrutto > 0.0) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.wasch_gesamtpreis_brutto), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Text("%.2f €".format(Locale.GERMAN, gesamtPreisBrutto), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                    }
                }
            }
        }
    }
}

private fun formatMenge(value: Double): String =
    if (value == value.toLong().toDouble()) "%.0f".format(Locale.GERMAN, value)
    else "%.2f".format(Locale.GERMAN, value).trimEnd('0').trimEnd(',')
