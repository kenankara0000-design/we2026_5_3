package com.example.we2026_5.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.we2026_5.KundenTyp
import com.example.we2026_5.ui.common.DetailUiConstants
import com.example.we2026_5.R

@Composable
fun CustomerDetailTerminRegelCard(
    intervalleToShow: List<com.example.we2026_5.CustomerIntervall>,
    isInEditMode: Boolean,
    kundenTyp: KundenTyp?,
    regelNameByRegelId: Map<String, String>,
    primaryBlue: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
    surfaceWhite: androidx.compose.ui.graphics.Color,
    onRegelClick: (String) -> Unit,
    onRemoveRegel: ((String) -> Unit)?,
    onResetToAutomatic: () -> Unit,
    onTerminAnlegen: () -> Unit,
    onAddMonthlyClick: (() -> Unit)? = null
) {
    Text(
        stringResource(R.string.label_termin_regel),
        fontSize = DetailUiConstants.SectionTitleSp,
        fontWeight = FontWeight.Bold,
        color = primaryBlue
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceWhite),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                start = DetailUiConstants.IntervalCardPaddingH,
                top = DetailUiConstants.IntervalCardPaddingTop,
                end = DetailUiConstants.IntervalCardPaddingH,
                bottom = DetailUiConstants.IntervalCardPaddingBottom
            )
        ) {
            val distinctRegelIds = intervalleToShow.map { it.terminRegelId }.distinct()
            distinctRegelIds.forEach { regelId ->
                CustomerDetailRegelNameRow(
                    regelName = if (regelId.isBlank()) stringResource(R.string.label_not_set)
                    else regelNameByRegelId[regelId] ?: stringResource(R.string.label_not_set),
                    isClickable = regelId.isNotBlank(),
                    primaryBlue = primaryBlue,
                    textSecondary = textSecondary,
                    onClick = { if (regelId.isNotBlank()) onRegelClick(regelId) },
                    showDeleteButton = isInEditMode && regelId.isNotBlank(),
                    onDeleteClick = if (isInEditMode && regelId.isNotBlank()) { { onRemoveRegel?.invoke(regelId) } } else null
                )
                Spacer(Modifier.height(DetailUiConstants.IntervalRowSpacing))
            }
            Spacer(Modifier.height(DetailUiConstants.FieldSpacing))
            if (isInEditMode && kundenTyp == KundenTyp.REGELMAESSIG) {
                OutlinedButton(onClick = onResetToAutomatic, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.btn_reset_to_automatic))
                }
                Spacer(Modifier.height(DetailUiConstants.FieldSpacing))
                onAddMonthlyClick?.let { onAdd ->
                    OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.label_add_monthly_termin))
                    }
                    Spacer(Modifier.height(DetailUiConstants.FieldSpacing))
                }
            }
            if (!isInEditMode) {
                Button(onClick = onTerminAnlegen, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.label_termine_anlegen))
                }
            }
        }
    }
}
