package com.example.we2026_5.ui.tourplanner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R

@Composable
fun TourPlannerOverviewDialog(
    payload: CustomerOverviewPayload,
    overviewRegelNamen: String?,
    onDismiss: () -> Unit,
    onOpenDetails: (customerId: String) -> Unit,
    onNavigate: (com.example.we2026_5.Customer) -> Unit = {}
) {
    val customer = payload.customer
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(customer.displayName, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    stringResource(R.string.label_termine_regeln),
                    fontSize = 13.sp,
                    color = colorResource(R.color.text_secondary)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = overviewRegelNamen ?: stringResource(R.string.no_termin_regeln),
                    fontSize = 16.sp,
                    color = colorResource(R.color.text_primary)
                )
                if (payload.urlaubInfo != null && payload.urlaubInfo.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.label_urlaub),
                        fontSize = 13.sp,
                        color = colorResource(R.color.text_secondary)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = payload.urlaubInfo,
                        fontSize = 16.sp,
                        color = colorResource(R.color.text_primary)
                    )
                }
                if (payload.verschobenInfo != null && payload.verschobenInfo.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.badge_verschoben),
                        fontSize = 13.sp,
                        color = colorResource(R.color.text_secondary)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = payload.verschobenInfo,
                        fontSize = 16.sp,
                        color = colorResource(R.color.text_primary)
                    )
                }
                if (payload.verschobenVonInfo != null && payload.verschobenVonInfo.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.badge_verschoben),
                        fontSize = 13.sp,
                        color = colorResource(R.color.text_secondary)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = payload.verschobenVonInfo,
                        fontSize = 16.sp,
                        color = colorResource(R.color.text_primary)
                    )
                }
                if (payload.ueberfaelligInfo != null && payload.ueberfaelligInfo.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.status_badge_overdue),
                        fontSize = 13.sp,
                        color = colorResource(R.color.text_secondary)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = payload.ueberfaelligInfo,
                        fontSize = 16.sp,
                        color = colorResource(R.color.text_primary)
                    )
                }
            }
        },
        confirmButton = {
            Column {
                if (customer.adresse.isNotBlank() || customer.plz.isNotBlank() || customer.stadt.isNotBlank() || (customer.latitude != null && customer.longitude != null)) {
                    Button(onClick = {
                        onNavigate(customer)
                        onDismiss()
                    }) {
                        Text(stringResource(R.string.label_navigation))
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Button(onClick = {
                    val id = customer.id
                    if (id.isNotBlank()) {
                        onOpenDetails(id)
                    }
                    onDismiss()
                }) {
                    Text(stringResource(R.string.label_details_open))
                }
            }
        }
    )
}
