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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
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

/**
 * Hauptbildschirm Variante 2 (Tour als Hero):
 * - Tour-Planner-Hero (Karte mit Titel, Badge „X fällig“, Button „Öffnen“)
 * - Zeile: Kunden | + Neu Kunde
 * - Weitere: 2x2 dezent (Outlined)
 * - Slot-Vorschläge (Ad-hoc-Termine)
 */
@Composable
fun MainScreen(
    isAdmin: Boolean,
    isOffline: Boolean,
    isSyncing: Boolean,
    tourCount: Int,
    slotVorschlaege: List<TerminSlotVorschlag>,
    onNeuKunde: () -> Unit,
    onKunden: () -> Unit,
    onTouren: () -> Unit,
    onKundenListen: () -> Unit,
    onStatistiken: () -> Unit,
    onErfassung: () -> Unit,
    onSettings: () -> Unit,
    onSlotSelected: (TerminSlotVorschlag) -> Unit
) {
    val primaryBlueDark = colorResource(R.color.primary_blue_dark)
    val primaryBlue = colorResource(R.color.primary_blue)
    val primaryBlueLight = colorResource(R.color.primary_blue_light)
    val textSecondary = colorResource(R.color.text_secondary)
    val backgroundLight = colorResource(R.color.background_light)
    val offlineYellow = colorResource(R.color.status_offline_yellow)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .fillMaxWidth()
            .background(backgroundLight)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Offline / Sync
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isOffline) {
                Row(
                    modifier = Modifier
                        .background(offlineYellow.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_offline),
                        contentDescription = stringResource(R.string.content_desc_offline),
                        modifier = Modifier.padding(end = 4.dp).size(14.dp),
                        tint = offlineYellow
                    )
                    Text(stringResource(R.string.main_offline), color = offlineYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
            }
            if (isSyncing) {
                Row(
                    modifier = Modifier
                        .background(primaryBlue.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_offline),
                        contentDescription = stringResource(R.string.content_desc_sync),
                        modifier = Modifier.padding(end = 4.dp).size(14.dp),
                        tint = primaryBlue
                    )
                    Text(stringResource(R.string.main_sync_status), color = primaryBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (isOffline) {
            Text(
                stringResource(R.string.offline_sync_hinweis),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                fontSize = 12.sp,
                color = textSecondary
            )
        }

        // Titel
        Text(
            stringResource(R.string.main_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = primaryBlueDark,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Tour-Planner-Hero (Variante 2: oben, als Karte)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = primaryBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.main_btn_touren),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        stringResource(R.string.main_tour_hero_faellig, tourCount),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onTouren,
                    modifier = Modifier.fillMaxWidth().height(com.example.we2026_5.ui.theme.AppSpacing.ButtonHeight),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        stringResource(R.string.main_tour_hero_open),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryBlue
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        // Zeile: Kunden | + Neu Kunde (Neu Kunde nur für Admin)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onKunden,
                modifier = Modifier.weight(1f).height(com.example.we2026_5.ui.theme.AppSpacing.ButtonHeight),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.main_btn_kunden), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            if (isAdmin) {
                Button(
                    onClick = onNeuKunde,
                    modifier = Modifier.weight(1f).height(com.example.we2026_5.ui.theme.AppSpacing.ButtonHeight),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlueDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.main_btn_neu_kunde), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        // Weitere – 2x2 (nur für Admin)
        if (isAdmin) {
            Text(
                stringResource(R.string.main_weitere),
                fontSize = 14.sp,
                color = textSecondary,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onKundenListen,
                        modifier = Modifier.fillMaxWidth().height(com.example.we2026_5.ui.theme.AppSpacing.ButtonHeight),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.main_btn_listen), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onStatistiken,
                        modifier = Modifier.fillMaxWidth().height(com.example.we2026_5.ui.theme.AppSpacing.ButtonHeight),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.main_btn_statistiken), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onErfassung,
                        modifier = Modifier.fillMaxWidth().height(com.example.we2026_5.ui.theme.AppSpacing.ButtonHeight),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.main_btn_erfassung), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onSettings,
                        modifier = Modifier.fillMaxWidth().height(com.example.we2026_5.ui.theme.AppSpacing.ButtonHeight),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.settings_title), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(28.dp))

            // Slot-Vorschläge (Ad-hoc, nur für Admin)
            Text(
                stringResource(R.string.main_slot_section_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = primaryBlueDark,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            )
            Text(
                stringResource(R.string.main_slot_section_subtitle),
                fontSize = 12.sp,
                color = textSecondary,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
            if (slotVorschlaege.isEmpty()) {
                Text(
                    stringResource(R.string.main_slot_section_empty),
                    color = textSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                )
            } else {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    slotVorschlaege.take(5).forEach { slot ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(slot.customerName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(DateFormatter.formatDateWithWeekday(slot.datum), fontSize = 14.sp, color = textSecondary)
                                Spacer(Modifier.height(2.dp))
                                Text(slot.beschreibung.ifBlank { slot.typ.name }, fontSize = 14.sp)
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = { onSlotSelected(slot) }, colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)) {
                                    Text(stringResource(R.string.main_slot_button_label))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
