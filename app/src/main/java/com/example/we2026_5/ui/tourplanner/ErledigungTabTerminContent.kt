package com.example.we2026_5.ui.tourplanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.example.we2026_5.tourplanner.ErledigungSheetState

@Composable
fun ErledigungTabTerminContent(
    customer: Customer,
    state: ErledigungSheetState,
    buttonVerschieben: androidx.compose.ui.graphics.Color,
    toastKwNurAbholung: String,
    hintVerschieben: String,
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
    }
}
