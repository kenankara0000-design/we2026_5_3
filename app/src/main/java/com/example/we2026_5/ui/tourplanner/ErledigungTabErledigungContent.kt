package com.example.we2026_5.ui.tourplanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun ErledigungTabErledigungContent(
    customer: Customer,
    state: ErledigungSheetState,
    textSecondary: androidx.compose.ui.graphics.Color,
    buttonAbholung: androidx.compose.ui.graphics.Color,
    buttonAuslieferung: androidx.compose.ui.graphics.Color,
    buttonRueckgaengig: androidx.compose.ui.graphics.Color,
    buttonVerschieben: androidx.compose.ui.graphics.Color,
    toastAbholungNurHeute: String,
    toastUeberfaelligNurHeute: String,
    toastAuslieferungNachAbholung: String,
    toastAuslieferungNurHeute: String,
    toastKwNurAbholung: String,
    hintVerschieben: String,
    onAbholung: (Customer) -> Unit,
    onAuslieferung: (Customer) -> Unit,
    onRueckgaengig: (Customer) -> Unit,
    onKw: (Customer) -> Unit,
    onVerschieben: (Customer) -> Unit,
    onDismiss: () -> Unit,
    showToast: (String) -> Unit
) {
    Column(
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
        val buttonAusnahme = colorResource(R.color.status_ausnahme)
        if (state.showAusnahmeAbholung) {
            Button(
                onClick = {
                    if (state.enableAusnahmeAbholung) {
                        onAbholung(customer)
                        onDismiss()
                    } else {
                        showToast(toastAbholungNurHeute)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.enableAusnahmeAbholung) buttonAusnahme else buttonAusnahme.copy(alpha = 0.5f)
                ),
                enabled = true
            ) {
                Icon(painter = painterResource(R.drawable.ic_pickup), contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.White)
                Spacer(Modifier.size(10.dp))
                Text(stringResource(R.string.sheet_ausnahme_abholung_erledigen), color = Color.White)
            }
        }
        if (state.showAusnahmeAuslieferung) {
            Button(
                onClick = {
                    if (state.enableAusnahmeAuslieferung) {
                        onAuslieferung(customer)
                        onDismiss()
                    } else {
                        showToast(
                            when {
                                state.showAusnahmeAbholung && !customer.abholungErfolgt -> toastAuslieferungNachAbholung
                                else -> toastAuslieferungNurHeute
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.enableAusnahmeAuslieferung) buttonAusnahme else buttonAusnahme.copy(alpha = 0.5f)
                ),
                enabled = true
            ) {
                Icon(painter = painterResource(R.drawable.ic_delivery), contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.White)
                Spacer(Modifier.size(10.dp))
                Text(stringResource(R.string.sheet_ausnahme_auslieferung_erledigen), color = Color.White)
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
        Spacer(Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.sheet_aktionen),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textSecondary,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        if (state.showKw) {
            Button(
                onClick = {
                    if (state.enableKw) {
                        onKw(customer)
                        // Sheet wird nach Bestätigung im Aufrufer geschlossen
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
                    // Sheet wird nach Bestätigung im Aufrufer geschlossen
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
    }
}
