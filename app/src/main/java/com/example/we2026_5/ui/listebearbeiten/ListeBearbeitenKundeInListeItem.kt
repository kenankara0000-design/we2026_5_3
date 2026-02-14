package com.example.we2026_5.ui.listebearbeiten

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.example.we2026_5.ui.common.AlWochentagText

@Composable
internal fun ListeBearbeitenKundeInListeItem(
    kunde: Customer,
    showRemove: Boolean,
    onRemove: () -> Unit,
    onAdd: () -> Unit,
    textPrimary: Color,
    textSecondary: Color,
    statusOverdue: Color,
    statusDone: Color = Color.Unspecified
) {
    val surfaceWhite = Color(ContextCompat.getColor(LocalContext.current, R.color.surface_white))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceWhite),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (kunde.fotoUrls.isNotEmpty()) {
                Card(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    AsyncImage(
                        model = kunde.fotoThumbUrls.firstOrNull() ?: kunde.fotoUrls.first(),
                        contentDescription = stringResource(R.string.content_desc_customer_photo),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(Modifier.size(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when (kunde.kundenArt) {
                            "Privat" -> stringResource(R.string.label_type_p_letter)
                            "Listenkunden" -> stringResource(R.string.label_type_l_letter)
                            else -> stringResource(R.string.label_type_g)
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(kunde.displayName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                }
                Text(kunde.adresse, fontSize = 14.sp, color = textSecondary)
                AlWochentagText(customer = kunde, color = textSecondary)
            }
            if (showRemove) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.content_desc_remove_from_list), tint = statusOverdue)
                }
            } else {
                IconButton(onClick = onAdd) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.content_desc_add_to_list), tint = statusDone)
                }
            }
        }
    }
}
