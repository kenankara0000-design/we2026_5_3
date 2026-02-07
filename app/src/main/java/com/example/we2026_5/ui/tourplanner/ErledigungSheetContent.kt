package com.example.we2026_5.ui.tourplanner

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    getNaechstesTourDatum: (Customer) -> Long?,
    showToast: (String) -> Unit,
    onTelefonClick: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val dateStr = if (viewDateMillis > 0) DateFormatter.formatDate(viewDateMillis) else ""
    val title = "${customer.displayName} – $dateStr"
    val primaryBlueDark = colorResource(R.color.primary_blue_dark)
    val divider = colorResource(R.color.divider)
    val textSecondary = colorResource(R.color.text_secondary)
    val textPrimary = colorResource(R.color.text_primary)
    val buttonAbholung = colorResource(R.color.button_abholung)
    val buttonAuslieferung = colorResource(R.color.button_auslieferung)
    val buttonRueckgaengig = colorResource(R.color.button_rueckgaengig)
    val buttonVerschieben = colorResource(R.color.button_verschieben)
    val toastAbholungNurHeute = stringResource(R.string.toast_abholung_nur_heute)
    val toastUeberfaelligNurHeute = stringResource(R.string.toast_ueberfaellig_nur_heute)
    val toastAuslieferungNachAbholung = stringResource(R.string.toast_auslieferung_nur_nach_abholung)
    val toastAuslieferungNurHeute = stringResource(R.string.toast_auslieferung_nur_heute)
    val toastKwNurAbholung = stringResource(R.string.toast_kw_nur_abholung_auslieferung)
    val hintVerschieben = stringResource(R.string.sheet_termin_verschieben_hint)
    val sheetFixedHeightDp = 520.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(sheetFixedHeightDp)
            .padding(20.dp)
            .padding(bottom = 32.dp)
    ) {
        ErledigungSheetKopf(
            title = title,
            primaryBlueDark = primaryBlueDark,
            textSecondary = textSecondary,
            divider = divider
        )
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
                0 -> ErledigungTabErledigungContent(
                    customer = customer,
                    state = state,
                    textSecondary = textSecondary,
                    buttonAbholung = buttonAbholung,
                    buttonAuslieferung = buttonAuslieferung,
                    buttonRueckgaengig = buttonRueckgaengig,
                    toastAbholungNurHeute = toastAbholungNurHeute,
                    toastUeberfaelligNurHeute = toastUeberfaelligNurHeute,
                    toastAuslieferungNachAbholung = toastAuslieferungNachAbholung,
                    toastAuslieferungNurHeute = toastAuslieferungNurHeute,
                    onAbholung = onAbholung,
                    onAuslieferung = onAuslieferung,
                    onRueckgaengig = onRueckgaengig,
                    onDismiss = onDismiss,
                    showToast = showToast
                )
                1 -> ErledigungTabTerminContent(
                    customer = customer,
                    state = state,
                    buttonVerschieben = buttonVerschieben,
                    toastKwNurAbholung = toastKwNurAbholung,
                    hintVerschieben = hintVerschieben,
                    onKw = onKw,
                    onVerschieben = onVerschieben,
                    onDismiss = onDismiss,
                    showToast = showToast
                )
                2 -> ErledigungTabDetailsContent(
                    customer = customer,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    getNaechstesTourDatum = getNaechstesTourDatum,
                    onTelefonClick = onTelefonClick
                )
            }
        }
    }
}
