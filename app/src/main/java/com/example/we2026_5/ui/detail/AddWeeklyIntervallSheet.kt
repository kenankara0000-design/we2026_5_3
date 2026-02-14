package com.example.we2026_5.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.R
import com.example.we2026_5.TerminRegelTyp
import com.example.we2026_5.ui.common.WochentagChipRowFromResources
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.WochentagBerechnung
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Bottom-Sheet zum Hinzufügen eines wöchentlichen Intervalls (z. B. Di A → So L, alle 7 Tage).
 * Der Nutzer wählt A-Wochentag, L-Wochentag und Intervall-Tage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWeeklyIntervallSheet(
    visible: Boolean,
    tourSlotId: String,
    onDismiss: () -> Unit,
    onAdd: (CustomerIntervall) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    var abholungTag by remember { mutableIntStateOf(1) }       // 0=Mo .. 6=So, Default: Di
    var auslieferungTag by remember { mutableIntStateOf(6) }   // Default: So
    var intervallTage by remember { mutableIntStateOf(7) }

    val primaryBlue = colorResource(R.color.primary_blue)
    val textPrimary = colorResource(R.color.text_primary)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.label_weekly_sheet_title),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // A-Wochentag
            Text(
                text = stringResource(R.string.label_abholung_wochentag_waehlen),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            WochentagChipRowFromResources(
                selected = abholungTag,
                onSelect = { abholungTag = it },
                primaryBlue = primaryBlue,
                textPrimary = textPrimary
            )

            Spacer(Modifier.height(16.dp))

            // L-Wochentag
            Text(
                text = stringResource(R.string.label_auslieferung_wochentag_waehlen),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            WochentagChipRowFromResources(
                selected = auslieferungTag,
                onSelect = { auslieferungTag = it },
                primaryBlue = primaryBlue,
                textPrimary = textPrimary
            )

            Spacer(Modifier.height(16.dp))

            // Intervall-Tage
            Text(
                text = stringResource(R.string.label_intervall_tage),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(7, 14, 21, 28).forEach { tage ->
                    FilterChip(
                        selected = intervallTage == tage,
                        onClick = { intervallTage = tage },
                        label = { Text("$tage") },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Hinzufügen
            Button(
                onClick = {
                    val now = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
                    val abholungDatum = WochentagBerechnung.naechsterWochentagAb(now, abholungTag)
                    val tageAzuL = tageAzuLBetween(abholungTag, auslieferungTag)
                    val auslieferungDatum = abholungDatum + TimeUnit.DAYS.toMillis(tageAzuL.toLong())
                    val intervall = CustomerIntervall(
                        id = UUID.randomUUID().toString(),
                        abholungDatum = abholungDatum,
                        auslieferungDatum = auslieferungDatum,
                        wiederholen = true,
                        intervallTage = intervallTage,
                        intervallAnzahl = 0,
                        erstelltAm = now,
                        terminRegelId = "manual-weekly",
                        regelTyp = TerminRegelTyp.WEEKLY,
                        tourSlotId = tourSlotId,
                        zyklusTage = intervallTage
                    )
                    onAdd(intervall)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(stringResource(R.string.btn_save))
            }
        }
    }
}

/** Tage von A-Wochentag bis L-Wochentag (0–7). Bei gleichem Tag: 0. */
private fun tageAzuLBetween(aTag: Int, lTag: Int): Int {
    if (aTag == lTag) return 0
    val d = (lTag - aTag + 7) % 7
    return if (d == 0) 7 else d
}
