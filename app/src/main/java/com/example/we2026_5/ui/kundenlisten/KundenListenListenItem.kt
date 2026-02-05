package com.example.we2026_5.ui.kundenlisten

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.KundenListe
import com.example.we2026_5.R
import com.example.we2026_5.util.DateFormatter

@Composable
internal fun KundenListenListenItem(
    liste: KundenListe,
    kundenCount: Int,
    surfaceWhite: Color,
    textPrimary: Color,
    textSecondary: Color,
    statusOverdue: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val erstelltAm = DateFormatter.formatDateWithLeadingZeros(liste.erstelltAm)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = liste.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.content_desc_delete_list),
                        tint = statusOverdue
                    )
                }
            }
            Text(
                text = stringResource(R.string.list_art_format, liste.listeArt),
                fontSize = 13.sp,
                color = textSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.label_customers_count, kundenCount),
                fontSize = 14.sp,
                color = textSecondary
            )
            if (kundenCount == 0 && liste.wochentag in 0..6) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.label_list_empty_weekday),
                    fontSize = 12.sp,
                    color = textSecondary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.label_created, erstelltAm),
                fontSize = 12.sp,
                color = textSecondary
            )
        }
    }
}
