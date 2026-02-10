package com.example.we2026_5.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R

/** Arten von Terminen, die angelegt werden können. */
enum class NeuerTerminArt {
    REGELMAESSIG,
    MONATLICH,
    EINMALIG_KUNDEN_TERMIN,
    EINMALIG_AUSNAHME
}

/**
 * Bottom-Sheet zur Auswahl der Art beim Anlegen eines neuen Termins.
 * Einziger Einstieg: FAB oder „Neuer Termin anlegen“.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuerTerminArtSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onArtSelected: (NeuerTerminArt) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text(
                text = stringResource(R.string.label_termin_art_sheet_title),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            SheetOption(
                title = stringResource(R.string.label_termin_art_regelmaessig),
                subtitle = stringResource(R.string.label_termin_art_regelmaessig_sub),
                onClick = { onArtSelected(NeuerTerminArt.REGELMAESSIG); onDismiss() }
            )
            SheetOption(
                title = stringResource(R.string.label_termin_art_monatlich),
                subtitle = stringResource(R.string.label_termin_art_monatlich_sub),
                onClick = { onArtSelected(NeuerTerminArt.MONATLICH); onDismiss() }
            )
            SheetOption(
                title = stringResource(R.string.label_termin_art_einmalig_kunde),
                subtitle = stringResource(R.string.label_termin_art_einmalig_kunde_sub),
                onClick = { onArtSelected(NeuerTerminArt.EINMALIG_KUNDEN_TERMIN); onDismiss() }
            )
            SheetOption(
                title = stringResource(R.string.label_termin_art_einmalig_ausnahme),
                subtitle = stringResource(R.string.label_termin_art_einmalig_ausnahme_sub),
                onClick = { onArtSelected(NeuerTerminArt.EINMALIG_AUSNAHME); onDismiss() }
            )
        }
    }
}

@Composable
private fun SheetOption(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp)
    ) {
        Text(text = title, fontWeight = FontWeight.SemiBold)
        Text(text = subtitle, fontSize = 13.sp, color = Color(0xFF757575))
    }
}
