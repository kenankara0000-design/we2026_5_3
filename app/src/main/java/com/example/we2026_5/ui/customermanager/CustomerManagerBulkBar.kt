package com.example.we2026_5.ui.customermanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
import com.example.we2026_5.R

@Composable
fun CustomerManagerBulkBar(
    selectedCount: Int,
    selectedCustomers: List<Customer>,
    primaryBlue: androidx.compose.ui.graphics.Color,
    onBulkDone: (List<Customer>) -> Unit,
    onBulkCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(primaryBlue)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.cm_selected_count, selectedCount),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onBulkDone(selectedCustomers) },
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.status_done))
            ) {
                Text(stringResource(R.string.cm_mark_done))
            }
            OutlinedButton(onClick = onBulkCancel) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    }
}
