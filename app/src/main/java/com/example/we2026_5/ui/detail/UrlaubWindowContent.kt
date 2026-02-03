package com.example.we2026_5.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.util.TerminFilterUtils

@Composable
fun UrlaubWindowContent(
    customer: Customer,
    onUrlaubEintragenAendern: () -> Unit,
    onUrlaubLoeschen: () -> Unit,
    onSchliessen: () -> Unit
) {
    val surfaceWhite = colorResource(R.color.surface_white)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)
    val primaryBlue = colorResource(R.color.primary_blue)
    val buttonUrlaub = colorResource(R.color.button_urlaub)
    val statusOverdue = colorResource(R.color.status_overdue)
    val eintraege = TerminFilterUtils.getEffectiveUrlaubEintraege(customer)
    val hasUrlaub = eintraege.isNotEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.dialog_urlaub_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = primaryBlue,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
            if (hasUrlaub) {
                eintraege.forEach { e ->
                    Text(
                        text = stringResource(
                            R.string.dialog_urlaub_current,
                            DateFormatter.formatDate(e.von),
                            DateFormatter.formatDate(e.bis)
                        ),
                        fontSize = 16.sp,
                        color = textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }
            } else {
                Text(
                    text = stringResource(R.string.dialog_urlaub_no_urlaub),
                    fontSize = 16.sp,
                    color = textSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onUrlaubEintragenAendern,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = buttonUrlaub)
            ) {
                Text(
                    if (hasUrlaub) stringResource(R.string.dialog_urlaub_aendern)
                    else stringResource(R.string.dialog_urlaub_new)
                )
            }
            if (hasUrlaub) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onUrlaubLoeschen,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = statusOverdue)
                ) {
                    Text(stringResource(R.string.dialog_urlaub_delete))
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSchliessen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.dialog_close))
            }
        }
    }
}
