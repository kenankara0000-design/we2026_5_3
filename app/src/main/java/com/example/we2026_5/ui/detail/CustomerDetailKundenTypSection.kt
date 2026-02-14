package com.example.we2026_5.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.we2026_5.KundenTyp
import com.example.we2026_5.ui.common.DetailUiConstants
import com.example.we2026_5.R
import com.example.we2026_5.ui.theme.AppColors

/**
 * Option C (UX-Kundendetail B1): Gemeinsame Überschrift „Termin-Info“, darunter 3 Bereiche,
 * jeder mit eigenem Wert – keine Labels pro Bereich. Kontext aus Reihenfolge.
 */
@Composable
fun CustomerDetailKundenTypSection(
    typeLabel: String,
    kundenTyp: KundenTyp,
    effectiveAbholungWochentage: List<Int>,
    effectiveAuslieferungWochentage: List<Int>,
    textPrimary: androidx.compose.ui.graphics.Color
) {
    val wochen = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    val typLabel = when (kundenTyp) {
        KundenTyp.REGELMAESSIG -> stringResource(R.string.label_kunden_typ_regelmaessig)
        KundenTyp.UNREGELMAESSIG -> stringResource(R.string.label_kunden_typ_unregelmaessig)
        KundenTyp.AUF_ABRUF -> stringResource(R.string.label_kunden_typ_auf_abruf)
    }
    val aLTagText = if (kundenTyp == KundenTyp.AUF_ABRUF) "–"
    else {
        val a = effectiveAbholungWochentage.map { wochen[it] }.joinToString(", ").takeIf { it.isNotEmpty() }?.let { "$it A" } ?: ""
        val l = effectiveAuslieferungWochentage.map { wochen[it] }.joinToString(", ").takeIf { it.isNotEmpty() }?.let { "$it L" } ?: ""
        listOf(a, l).filter { it.isNotEmpty() }.joinToString(" / ")
    }
    Text(
        stringResource(R.string.label_termin_info),
        fontSize = DetailUiConstants.FieldLabelSp,
        fontWeight = FontWeight.Bold,
        color = textPrimary
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(DetailUiConstants.FieldSpacing)
    ) {
        Text(
            text = typeLabel,
            modifier = Modifier.weight(1f).background(AppColors.LightGray).padding(12.dp),
            color = textPrimary,
            fontSize = DetailUiConstants.BodySp
        )
        Text(
            text = typLabel,
            modifier = Modifier.weight(1f).background(AppColors.LightGray).padding(12.dp),
            color = textPrimary,
            fontSize = DetailUiConstants.BodySp
        )
        Text(
            text = aLTagText,
            modifier = Modifier.weight(1f).background(AppColors.LightGray).padding(12.dp),
            color = textPrimary,
            fontSize = DetailUiConstants.BodySp
        )
    }
    Spacer(Modifier.height(DetailUiConstants.FieldSpacing))
}
