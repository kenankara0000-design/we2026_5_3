package com.example.we2026_5.ui.tourplanner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.example.we2026_5.tourplanner.ErledigungSheetState
import com.example.we2026_5.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErledigungSheetContent(
    customer: Customer,
    viewDateMillis: Long,
    state: ErledigungSheetState,
    onDismiss: () -> Unit,
    onAbholung: (Customer) -> Unit,
    onAuslieferung: (Customer) -> Unit,
    onKw: (Customer) -> Unit,
    onRueckgaengig: (Customer) -> Unit,
    onVerschieben: (Customer) -> Unit,
    onUrlaub: (Customer) -> Unit,
    getNaechstesTourDatum: (Customer) -> Long?,
    showToast: (String) -> Unit,
    onTelefonClick: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val dateStr = if (viewDateMillis > 0) DateFormatter.formatDate(viewDateMillis) else ""
    val title = "${customer.name} – $dateStr"
    val primaryBlueDark = colorResource(R.color.primary_blue_dark)
    val divider = colorResource(R.color.divider)
    val textSecondary = colorResource(R.color.text_secondary)
    val textPrimary = colorResource(R.color.text_primary)
    val buttonAbholung = colorResource(R.color.button_abholung)
    val buttonAuslieferung = colorResource(R.color.button_auslieferung)
    val buttonRueckgaengig = colorResource(R.color.button_rueckgaengig)
    val buttonVerschieben = colorResource(R.color.button_verschieben)
    val buttonUrlaub = colorResource(R.color.button_urlaub)
    val toastAbholungNurHeute = stringResource(R.string.toast_abholung_nur_heute)
    val toastUeberfaelligNurHeute = stringResource(R.string.toast_ueberfaellig_nur_heute)
    val toastAuslieferungNachAbholung = stringResource(R.string.toast_auslieferung_nur_nach_abholung)
    val toastAuslieferungNurHeute = stringResource(R.string.toast_auslieferung_nur_heute)
    val toastKwNurAbholung = stringResource(R.string.toast_kw_nur_abholung_auslieferung)
    val legendText = stringResource(R.string.sheet_legend)
    val sheetFixedHeightDp = 520.dp
    val hintVerschieben = stringResource(R.string.sheet_termin_verschieben_hint)
    val hintUrlaub = stringResource(R.string.sheet_urlaub_eintragen_hint)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(sheetFixedHeightDp)
            .padding(20.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = primaryBlueDark
        )
        Text(
            text = legendText,
            fontSize = 11.sp,
            color = textSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(vertical = 12.dp)
                .background(divider)
        )
        Spacer(Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = primaryBlueDark,
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(stringResource(R.string.sheet_tab_erledigung)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(stringResource(R.string.sheet_tab_termin)) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text(stringResource(R.string.sheet_tab_details)) }
            )
        }

        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                0 -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.overdueInfoText.isNotEmpty()) {
                    Text(
                        text = state.overdueInfoText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorResource(R.color.status_overdue),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                if (state.completedInfoText.isNotEmpty()) {
                    Text(
                        text = state.completedInfoText,
                        fontSize = 12.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                if (state.showAbholung) {
                    Button(
                        onClick = {
                            if (state.enableAbholung) {
                                onAbholung(customer)
                                onDismiss()
                            } else {
                                showToast(if (state.isOverdueBadge) toastUeberfaelligNurHeute else toastAbholungNurHeute)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.enableAbholung) buttonAbholung else buttonAbholung.copy(alpha = 0.5f)
                        ),
                        enabled = true
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_pickup), contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.White)
                        Spacer(Modifier.size(10.dp))
                        Text(stringResource(R.string.sheet_abholung_erledigen), color = Color.White)
                    }
                }
                if (state.showAuslieferung) {
                    Button(
                        onClick = {
                            if (state.enableAuslieferung) {
                                onAuslieferung(customer)
                                onDismiss()
                            } else {
                                showToast(
                                    when {
                                        !customer.abholungErfolgt -> toastAuslieferungNachAbholung
                                        state.isOverdueBadge -> toastUeberfaelligNurHeute
                                        else -> toastAuslieferungNurHeute
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.enableAuslieferung) buttonAuslieferung else buttonAuslieferung.copy(alpha = 0.5f)
                        ),
                        enabled = true
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_delivery), contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.White)
                        Spacer(Modifier.size(10.dp))
                        Text(stringResource(R.string.sheet_auslieferung_erledigen), color = Color.White)
                    }
                }
                if (state.showRueckgaengig) {
                    OutlinedButton(
                        onClick = {
                            onRueckgaengig(customer)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = buttonRueckgaengig)
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_undo), contentDescription = null, modifier = Modifier.size(22.dp), tint = buttonRueckgaengig)
                        Spacer(Modifier.size(10.dp))
                        Text(stringResource(R.string.sheet_rueckgaengig), color = buttonRueckgaengig)
                    }
                }
            }
            1 -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.showKw) {
                    Button(
                        onClick = {
                            if (state.enableKw) {
                                onKw(customer)
                                onDismiss()
                            } else {
                                showToast(toastKwNurAbholung)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.status_warning)),
                        enabled = state.enableKw
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_checklist), contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.White)
                        Spacer(Modifier.size(10.dp))
                        Text(stringResource(R.string.sheet_keine_waesche), color = Color.White)
                    }
                }
                Button(
                    onClick = {
                        if (state.showVerschieben) {
                            onVerschieben(customer)
                            onDismiss()
                        } else {
                            showToast(hintVerschieben)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = buttonVerschieben),
                    enabled = state.showVerschieben
                ) {
                    Icon(painter = painterResource(R.drawable.ic_reschedule), contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.White)
                    Spacer(Modifier.size(10.dp))
                    Text(stringResource(R.string.sheet_termin_verschieben), color = Color.White)
                }
                Button(
                    onClick = {
                        if (state.showUrlaub) {
                            onUrlaub(customer)
                            onDismiss()
                        } else {
                            showToast(hintUrlaub)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = buttonUrlaub),
                    enabled = state.showUrlaub
                ) {
                    Icon(painter = painterResource(R.drawable.ic_vacation), contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.White)
                    Spacer(Modifier.size(10.dp))
                    Text(stringResource(R.string.sheet_urlaub_eintragen), color = Color.White)
                }
            }
            2 -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (customer.telefon.isNotBlank()) {
                    Text(stringResource(R.string.sheet_telefon), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                    Text(
                        text = customer.telefon,
                        fontSize = 15.sp,
                        color = textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable { onTelefonClick(customer.telefon.trim()) }
                    )
                }
                Text(stringResource(R.string.sheet_naechste_tour), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                val naechsteTour = getNaechstesTourDatum(customer)
                Text(
                    text = if (naechsteTour != null && naechsteTour > 0) DateFormatter.formatDate(naechsteTour) else stringResource(R.string.sheet_kein_termin),
                    fontSize = 15.sp,
                    color = textPrimary,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .then(Modifier.fillMaxWidth())
                )
                if (customer.notizen.isNotBlank()) {
                    Text(stringResource(R.string.sheet_notizen), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                    Text(text = customer.notizen, fontSize = 15.sp, color = textPrimary, modifier = Modifier.fillMaxWidth())
                }
            }
            }
        }
    }
}
