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

@Composable
fun CustomerDetailKundenTypSection(
    typeLabel: String,
    kundenTyp: KundenTyp,
    effectiveAbholungWochentage: List<Int>,
    effectiveAuslieferungWochentage: List<Int>,
    textPrimary: androidx.compose.ui.graphics.Color
) {
    val wochen = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DetailUiConstants.FieldSpacing)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.label_customer_type),
                fontSize = DetailUiConstants.FieldLabelSp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Text(
                text = typeLabel,
                modifier = Modifier.fillMaxWidth().background(AppColors.LightGray).padding(12.dp),
                color = textPrimary,
                fontSize = DetailUiConstants.BodySp
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.label_kunden_typ),
                fontSize = DetailUiConstants.FieldLabelSp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Text(
                text = when (kundenTyp) {
                    KundenTyp.REGELMAESSIG -> stringResource(R.string.label_kunden_typ_regelmaessig)
                    KundenTyp.UNREGELMAESSIG -> stringResource(R.string.label_kunden_typ_unregelmaessig)
                    KundenTyp.AUF_ABRUF -> stringResource(R.string.label_kunden_typ_auf_abruf)
                },
                modifier = Modifier.fillMaxWidth().background(AppColors.LightGray).padding(12.dp),
                color = textPrimary,
                fontSize = DetailUiConstants.BodySp
            )
        }
        if (kundenTyp != KundenTyp.AUF_ABRUF && (effectiveAbholungWochentage.isNotEmpty() || effectiveAuslieferungWochentage.isNotEmpty())) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.label_abholung_auslieferung_tag),
                    fontSize = DetailUiConstants.FieldLabelSp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                val a = effectiveAbholungWochentage.map { wochen[it] }.joinToString(", ").takeIf { it.isNotEmpty() }?.let { "$it A" } ?: ""
                val l = effectiveAuslieferungWochentage.map { wochen[it] }.joinToString(", ").takeIf { it.isNotEmpty() }?.let { "$it L" } ?: ""
                Text(
                    text = listOf(a, l).filter { it.isNotEmpty() }.joinToString(" / "),
                    modifier = Modifier.fillMaxWidth().background(AppColors.LightGray).padding(12.dp),
                    color = textPrimary,
                    fontSize = DetailUiConstants.BodySp
                )
            }
        }
        if (kundenTyp == KundenTyp.AUF_ABRUF) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.label_abholung_auslieferung_tag),
                    fontSize = DetailUiConstants.FieldLabelSp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    text = "–",
                    modifier = Modifier.fillMaxWidth().background(AppColors.LightGray).padding(12.dp),
                    color = textPrimary,
                    fontSize = DetailUiConstants.BodySp
                )
            }
        }
    }
    Spacer(Modifier.height(DetailUiConstants.FieldSpacing))
}
