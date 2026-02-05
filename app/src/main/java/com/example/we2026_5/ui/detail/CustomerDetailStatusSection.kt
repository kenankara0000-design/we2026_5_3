package com.example.we2026_5.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    onPauseCustomer: () -> Unit,
    onResumeCustomer: () -> Unit,
    textPrimary: Color,
    surfaceWhite: Color
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceWhite)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.customer_status_title),
                fontSize = DetailUiConstants.SectionTitleSp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = statusText, color = statusColor, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.customer_status_current, statusText),
                    color = textPrimary,
                    fontSize = DetailUiConstants.BodySp
                )
            }
            if (customer.pauseEnde > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.customer_status_paused_until,
                        DateFormatter.formatDateWithWeekday(customer.pauseEnde)
                    ),
                    color = textPrimary,
                    fontSize = DetailUiConstants.BodySp
                )
            }
            if (customer.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.customer_tags_label, customer.tags.joinToString(", ")),
                    color = textPrimary,
                    fontSize = DetailUiConstants.BodySp
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = if (customer.status == CustomerStatus.PAUSIERT) onResumeCustomer else onPauseCustomer,
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.primary_blue))
            ) {
                Text(
                    text = if (customer.status == CustomerStatus.PAUSIERT) {
                        stringResource(R.string.customer_btn_resume)
                    } else {
                        stringResource(R.string.customer_btn_pause)
                    }
                )
            }
        }
    }
}
