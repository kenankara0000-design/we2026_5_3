package com.example.we2026_5.ui.customermanager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R

@Composable
fun CustomerManagerSearchAndFilter(
    searchValue: String,
    onSearchChange: (String) -> Unit,
    filterExpanded: Boolean,
    onFilterToggle: () -> Unit,
    kundenTypFilter: Int,
    onKundenTypFilterChange: (Int) -> Unit,
    ohneTourFilter: Int,
    onOhneTourFilterChange: (Int) -> Unit,
    pausierteFilter: Int,
    onPausierteFilterChange: (Int) -> Unit,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
    primaryBlue: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = searchValue,
            onValueChange = onSearchChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.hint_search_customer)) },
            singleLine = true
        )
        IconButton(onClick = onFilterToggle) {
            Icon(
                painter = painterResource(R.drawable.ic_filter),
                contentDescription = stringResource(R.string.content_desc_filter),
                tint = if (filterExpanded) primaryBlue else textSecondary
            )
        }
    }
    if (filterExpanded) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.label_filter_kunden_typ),
            color = textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = kundenTypFilter == 0,
                onClick = { onKundenTypFilterChange(0) },
                label = { Text(stringResource(R.string.label_filter_all)) }
            )
            FilterChip(
                selected = kundenTypFilter == 1,
                onClick = { onKundenTypFilterChange(1) },
                label = { Text(stringResource(R.string.label_kunden_typ_regelmaessig)) }
            )
            FilterChip(
                selected = kundenTypFilter == 2,
                onClick = { onKundenTypFilterChange(2) },
                label = { Text(stringResource(R.string.label_kunden_typ_unregelmaessig)) }
            )
            FilterChip(
                selected = kundenTypFilter == 3,
                onClick = { onKundenTypFilterChange(3) },
                label = { Text(stringResource(R.string.label_kunden_typ_auf_abruf)) }
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.label_filter_ohne_tour),
            color = textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = ohneTourFilter == 0,
                onClick = { onOhneTourFilterChange(0) },
                label = { Text(stringResource(R.string.label_filter_all)) }
            )
            FilterChip(
                selected = ohneTourFilter == 1,
                onClick = { onOhneTourFilterChange(1) },
                label = { Text(stringResource(R.string.label_ohne_tour_anzeigen)) }
            )
            FilterChip(
                selected = ohneTourFilter == 2,
                onClick = { onOhneTourFilterChange(2) },
                label = { Text(stringResource(R.string.label_ohne_tour_ausblenden)) }
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.label_filter_pausierte),
            color = textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = pausierteFilter == 0,
                onClick = { onPausierteFilterChange(0) },
                label = { Text(stringResource(R.string.label_pausierte_ausblenden)) }
            )
            FilterChip(
                selected = pausierteFilter == 1,
                onClick = { onPausierteFilterChange(1) },
                label = { Text(stringResource(R.string.label_pausierte_anzeigen)) }
            )
        }
        Spacer(Modifier.height(8.dp))
    }
    Spacer(Modifier.height(8.dp))
}
