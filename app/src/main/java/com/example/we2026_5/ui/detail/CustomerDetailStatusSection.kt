package com.example.we2026_5.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerStatus
import com.example.we2026_5.R
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.ui.common.DetailUiConstants

@Composable
fun CustomerDetailStatusSection(
    customer: Customer,
    canChangeStatus: Boolean = true,
    onPauseCustomer: (pauseEndeWochen: Int?) -> Unit,
    onResumeCustomer: () -> Unit,
    textPrimary: Color,
    surfaceWhite: Color,
    /** Optional: z. B. „+ Neue Termin“-Button in derselben Zeile rechts. */
    trailingContent: @Composable (() -> Unit)? = null
) {
    val statusText = when (customer.status) {
        CustomerStatus.AKTIV -> stringResource(R.string.customer_status_active)
        CustomerStatus.PAUSIERT -> stringResource(R.string.customer_status_paused)
        CustomerStatus.ADHOC -> stringResource(R.string.customer_status_adhoc)
    }
    val statusColor = when (customer.status) {
        CustomerStatus.AKTIV -> colorResource(R.color.status_done)
        CustomerStatus.PAUSIERT -> colorResource(R.color.status_warning)
        CustomerStatus.ADHOC -> colorResource(R.color.status_info)
    }
    val isActive = customer.status == CustomerStatus.AKTIV || customer.status == CustomerStatus.ADHOC
    val greenColor = colorResource(R.color.status_done)
    val redColor = colorResource(R.color.status_overdue)
    var showResumeConfirm by remember { mutableStateOf(false) }
    var showPauseDurationDialog by remember { mutableStateOf(false) }
    var selectedPauseWeeks by remember { mutableStateOf<Int?>(1) }

    if (showResumeConfirm) {
        AlertDialog(
            onDismissRequest = { showResumeConfirm = false },
            title = { Text(stringResource(R.string.customer_confirm_resume_title)) },
            text = { Text(stringResource(R.string.customer_confirm_resume_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onResumeCustomer()
                    showResumeConfirm = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showResumeConfirm = false }) { Text(stringResource(android.R.string.cancel)) }
            }
        )
    }

    if (showPauseDurationDialog) {
        AlertDialog(
            onDismissRequest = { showPauseDurationDialog = false },
            title = { Text(stringResource(R.string.customer_pause_duration_title)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    (1..8).forEach { w ->
                        Row(Modifier.fillMaxWidth().clickable { selectedPauseWeeks = w }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedPauseWeeks == w, onClick = { selectedPauseWeeks = w })
                            Spacer(Modifier.width(8.dp))
                            Text(if (w == 1) stringResource(R.string.customer_pause_1_week) else stringResource(R.string.customer_pause_weeks, w), color = textPrimary)
                        }
                    }
                    Row(Modifier.fillMaxWidth().clickable { selectedPauseWeeks = null }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedPauseWeeks == null, onClick = { selectedPauseWeeks = null })
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.customer_pause_unbestimmt), color = textPrimary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onPauseCustomer(selectedPauseWeeks)
                    showPauseDurationDialog = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPauseDurationDialog = false }) { Text(stringResource(android.R.string.cancel)) }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceWhite)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
                Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = statusText, color = statusColor, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    if (canChangeStatus) {
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = isActive,
                            onCheckedChange = { newValue ->
                                if (newValue) showResumeConfirm = true else { selectedPauseWeeks = 1; showPauseDurationDialog = true }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = greenColor,
                                checkedTrackColor = greenColor.copy(alpha = 0.5f),
                                uncheckedThumbColor = redColor,
                                uncheckedTrackColor = redColor.copy(alpha = 0.5f)
                            )
                        )
                    }
                    if (customer.pauseEnde > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                R.string.customer_status_paused_until,
                                DateFormatter.formatDateWithWeekday(customer.pauseEnde)
                            ),
                            color = textPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
                trailingContent?.invoke()
            }
            if (customer.tags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.customer_tags_label, customer.tags.joinToString(", ")),
                    color = textPrimary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
