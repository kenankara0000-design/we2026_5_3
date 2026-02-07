package com.example.we2026_5.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.R
import com.example.we2026_5.TerminRegelTyp
import com.example.we2026_5.ui.common.MonthlyWeekdayRuleContent
import java.util.UUID

/**
 * Bottom-Sheet zum Hinzufügen eines monatlichen Wochentags-Intervalls (z. B. 1. Montag, 2. Donnerstag).
 * Eigenständige Datei, damit CustomerDetailScreen nicht wächst (Plan: kein Refactoring nötig).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMonthlyIntervallSheet(
    visible: Boolean,
    tourSlotId: String,
    onDismiss: () -> Unit,
    onAdd: (CustomerIntervall) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    var monthWeekOfMonth by remember { mutableStateOf(1) }
    var monthWeekday by remember { mutableStateOf(0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            MonthlyWeekdayRuleContent(
                monthWeekOfMonth = monthWeekOfMonth,
                monthWeekday = monthWeekday,
                onMonthWeekChange = { monthWeekOfMonth = it },
                onWeekdayChange = { monthWeekday = it }
            )
            Button(
                onClick = {
                    val intervall = CustomerIntervall(
                        id = UUID.randomUUID().toString(),
                        abholungDatum = 0L,
                        auslieferungDatum = 0L,
                        wiederholen = true,
                        intervallTage = 0,
                        intervallAnzahl = 0,
                        erstelltAm = System.currentTimeMillis(),
                        terminRegelId = "",
                        regelTyp = TerminRegelTyp.MONTHLY_WEEKDAY,
                        tourSlotId = tourSlotId,
                        zyklusTage = 0,
                        monthWeekOfMonth = monthWeekOfMonth,
                        monthWeekday = monthWeekday
                    )
                    onAdd(intervall)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(stringResource(R.string.btn_save))
            }
        }
    }
}
