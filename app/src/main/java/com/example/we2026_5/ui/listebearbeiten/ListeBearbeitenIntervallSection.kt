package com.example.we2026_5.ui.listebearbeiten

import com.example.we2026_5.ListeIntervall
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R

@Composable
fun ListeBearbeitenIntervallSection(
    wochentag: Int?,
    intervalle: List<ListeIntervall>,
    isInEditMode: Boolean,
    onTerminAnlegen: () -> Unit,
    onDatumSelected: (position: Int, isAbholung: Boolean) -> Unit,
    surfaceWhite: Color,
    textSecondary: Color,
    primaryBlue: Color
) {
    val isWochentagsliste = (wochentag ?: -1) in 0..6
    if (isWochentagsliste) {
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = surfaceWhite),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = stringResource(R.string.label_list_wochentag_grouping_hint),
                modifier = Modifier.padding(12.dp),
                fontSize = 14.sp,
                color = textSecondary
            )
        }
    } else if (intervalle.isNotEmpty() || isInEditMode) {
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = surfaceWhite),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(stringResource(R.string.label_intervals), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = primaryBlue)
                Spacer(modifier = Modifier.height(8.dp))
                intervalle.forEachIndexed { index, intervall ->
                    ListeBearbeitenIntervallRow(
                        intervall = intervall,
                        isEditMode = isInEditMode,
                        onAbholungClick = { onDatumSelected(index, true) },
                        onAuslieferungClick = { onDatumSelected(index, false) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.Button(onClick = onTerminAnlegen, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.label_termine_anlegen)) }
            }
        }
    } else {
        Spacer(modifier = Modifier.height(12.dp))
        androidx.compose.material3.Button(onClick = onTerminAnlegen, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.label_termine_anlegen)) }
    }
}
