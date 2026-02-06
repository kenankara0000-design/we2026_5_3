package com.example.we2026_5.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R
import com.example.we2026_5.TerminSlotVorschlag
import com.example.we2026_5.util.DateFormatter

@Composable
fun MainScreen(
    isOffline: Boolean,
    isSyncing: Boolean,
    tourCount: Int,
    slotVorschlaege: List<TerminSlotVorschlag>,
    onNeuKunde: () -> Unit,
    onKunden: () -> Unit,
    onTouren: () -> Unit,
    onKundenListen: () -> Unit,
    onStatistiken: () -> Unit,
    onArtikelErfassen: () -> Unit,
    onSlotSelected: (TerminSlotVorschlag) -> Unit
) {
    val primaryBlueDark = colorResource(R.color.primary_blue_dark)
    val primaryBlue = colorResource(R.color.primary_blue)
    val primaryBlueLight = colorResource(R.color.primary_blue_light)
    val statusWarning = colorResource(R.color.status_warning)
    val textSecondary = colorResource(R.color.text_secondary)
    val backgroundLight = colorResource(R.color.background_light)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .fillMaxWidth()
            .background(backgroundLight)
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Offline- und Sync-Status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isOffline) {
                Row(
                    modifier = Modifier
                        .background(
                            color = Color(0xFFFFEB3B).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_offline),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp).size(14.dp),
                        tint = Color(0xFFFFEB3B)
                    )
                    Text(
                        stringResource(R.string.main_offline),
                        color = Color(0xFFFFEB3B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.padding(end = 8.dp))
            }
            if (isSyncing) {
                Row(
                    modifier = Modifier
                        .background(
                            color = primaryBlue.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_offline),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp).size(14.dp),
                        tint = primaryBlue
                    )
                    Text(
                        stringResource(R.string.main_sync_status),
                        color = primaryBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (isOffline) {
            Text(
                stringResource(R.string.offline_sync_hinweis),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                fontSize = 12.sp,
                color = textSecondary
            )
        }

        Text(
            stringResource(R.string.main_title),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = primaryBlueDark,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Haupt-Buttons
        Button(
            onClick = onNeuKunde,
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryBlueDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                stringResource(R.string.main_btn_neu_kunde),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onKunden,
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
        ) {
            Text(
                stringResource(R.string.main_btn_kunden),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onTouren,
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
        ) {
            Text(
                stringResource(R.string.main_tour_btn_with_count, tourCount),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(28.dp))

        Text(
            stringResource(R.string.main_weitere),
            fontSize = 14.sp,
            color = textSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        Button(
            onClick = onArtikelErfassen,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryBlueLight)
        ) {
            Text(
                stringResource(R.string.main_btn_artikel_erfassen),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onKundenListen,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryBlueLight)
        ) {
            Text(
                stringResource(R.string.main_btn_listen),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onStatistiken,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryBlueLight)
        ) {
            Text(
                stringResource(R.string.main_btn_statistiken),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.main_slot_section_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = primaryBlueDark,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )
        if (slotVorschlaege.isEmpty()) {
            Text(
                text = stringResource(R.string.main_slot_section_empty),
                color = textSecondary,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                slotVorschlaege.take(5).forEach { slot ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(slot.customerName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                DateFormatter.formatDateWithWeekday(slot.datum),
                                fontSize = 14.sp,
                                color = textSecondary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(slot.beschreibung.ifBlank { slot.typ.name }, fontSize = 14.sp)
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { onSlotSelected(slot) },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                            ) {
                                Text(stringResource(R.string.main_slot_button_label))
                            }
                        }
                    }
                }
            }
        }
    }
}
