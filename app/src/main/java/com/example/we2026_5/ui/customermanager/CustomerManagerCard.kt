package com.example.we2026_5.ui.customermanager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerStatus
import com.example.we2026_5.R
import com.example.we2026_5.ui.common.formatALWochentag

@Composable
internal fun CustomerManagerCard(
    customer: Customer,
    isBulkMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleSelection: () -> Unit,
    textPrimary: Color,
    textSecondary: Color
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val alText = formatALWochentag(customer) { context.getString(it) }
    val surfaceWhite = colorResource(R.color.surface_white)
    val gplColor = when (customer.kundenArt) {
        "Privat" -> colorResource(R.color.button_privat_glossy)
        "Listenkunden" -> colorResource(R.color.button_liste_glossy)
        else -> colorResource(R.color.button_gewerblich_glossy)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceWhite),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isBulkMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() }
                )
                Spacer(Modifier.size(8.dp))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (customer.fotoUrls.isNotEmpty()) {
                        Card(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            AsyncImage(
                                model = customer.fotoThumbUrls.firstOrNull() ?: customer.fotoUrls.first(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(Modifier.size(8.dp))
                    }
                    Text(
                        text = when (customer.kundenArt) {
                            "Privat" -> "P"
                            "Listenkunden" -> stringResource(R.string.label_type_l_letter)
                            else -> "G"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(gplColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        customer.displayName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    // Phase 4: Status-Badge (Pausiert / Ad-hoc)
                    if (customer.status == CustomerStatus.PAUSIERT || customer.status == CustomerStatus.ADHOC) {
                        Spacer(Modifier.size(6.dp))
                        val badgeColor = if (customer.status == CustomerStatus.PAUSIERT)
                            colorResource(R.color.status_warning)
                        else
                            colorResource(R.color.status_info)
                        val badgeLabel = if (customer.status == CustomerStatus.PAUSIERT)
                            stringResource(R.string.customer_status_badge_pausiert)
                        else
                            stringResource(R.string.customer_status_badge_adhoc)
                        Text(
                            text = badgeLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier
                                .background(badgeColor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    customer.adresse,
                    fontSize = 14.sp,
                    color = textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (alText.isNotEmpty()) {
                    Text(
                        text = alText,
                        fontSize = 12.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
