package com.example.we2026_5.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.KundenTyp
import com.example.we2026_5.R

/** Arten von Terminen, die angelegt werden können. */
enum class NeuerTerminArt {
    REGELMAESSIG,
    MONATLICH,
    EINMALIG_KUNDEN_TERMIN,
    EINMALIG_AUSNAHME,
    URLAUB
}

/** Farben pro Art (gesamter Zeilenbereich): Hintergrund + Text. */
private val colorRegelBg = Color(0x401976D2)      // Blau
private val colorRegelText = Color(0xFF0D47A1)
private val colorMonatlichBg = Color(0x351976D2)  // Blau etwas kräftiger
private val colorMonatlichText = Color(0xFF0D47A1)
private val colorKundenBg = Color(0x40258E3C)     // Grün
private val colorKundenText = Color(0xFF1B5E20)
private val colorAusnahmeBg = Color(0x59F9A825)   // Gelb/Amber
private val colorAusnahmeText = Color(0xFFE65100)
private val colorUrlaubBg = Color(0x4DEF6C00)     // Orange
private val colorUrlaubText = Color(0xFFBF360C)

/** Erlaubte Termin-Arten pro KundenTyp: Regelmäßig nur Regel/Monatlich/Ausnahme/Urlaub; Unregelmäßig nur Ausnahme/Urlaub; Abruf Ausnahme/Einmalig/Urlaub. */
private fun allowedArtsFor(kundenTyp: KundenTyp): Set<NeuerTerminArt> = when (kundenTyp) {
    KundenTyp.REGELMAESSIG -> setOf(NeuerTerminArt.REGELMAESSIG, NeuerTerminArt.MONATLICH, NeuerTerminArt.EINMALIG_AUSNAHME, NeuerTerminArt.URLAUB)
    KundenTyp.UNREGELMAESSIG -> setOf(NeuerTerminArt.EINMALIG_AUSNAHME, NeuerTerminArt.URLAUB)
    KundenTyp.AUF_ABRUF -> setOf(NeuerTerminArt.EINMALIG_AUSNAHME, NeuerTerminArt.EINMALIG_KUNDEN_TERMIN, NeuerTerminArt.URLAUB)
}

/**
 * Bottom-Sheet zur Auswahl der Art beim Anlegen eines neuen Termins.
 * Zeigt nur die zum KundenTyp erlaubten Optionen (Regelmäßig: Regel, Monatlich, Ausnahme, Urlaub; Unregelmäßig: Ausnahme, Urlaub; Abruf: Ausnahme, Einmalig, Urlaub).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuerTerminArtSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onArtSelected: (NeuerTerminArt) -> Unit,
    kundenTyp: KundenTyp = KundenTyp.REGELMAESSIG,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    val allowed = allowedArtsFor(kundenTyp)
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
            if (NeuerTerminArt.REGELMAESSIG in allowed) {
                SheetOptionColored(
                    title = stringResource(R.string.label_termin_art_regelmaessig),
                    subtitle = stringResource(R.string.label_termin_art_regelmaessig_sub),
                    backgroundColor = colorRegelBg,
                    textColor = colorRegelText,
                    onClick = { onArtSelected(NeuerTerminArt.REGELMAESSIG); onDismiss() }
                )
            }
            if (NeuerTerminArt.MONATLICH in allowed) {
                SheetOptionColored(
                    title = stringResource(R.string.label_termin_art_monatlich),
                    subtitle = stringResource(R.string.label_termin_art_monatlich_sub),
                    backgroundColor = colorMonatlichBg,
                    textColor = colorMonatlichText,
                    onClick = { onArtSelected(NeuerTerminArt.MONATLICH); onDismiss() }
                )
            }
            if (NeuerTerminArt.EINMALIG_KUNDEN_TERMIN in allowed) {
                SheetOptionColored(
                    title = stringResource(R.string.label_termin_art_einmalig_kunde),
                    subtitle = stringResource(R.string.label_termin_art_einmalig_kunde_sub),
                    backgroundColor = colorKundenBg,
                    textColor = colorKundenText,
                    onClick = { onArtSelected(NeuerTerminArt.EINMALIG_KUNDEN_TERMIN); onDismiss() }
                )
            }
            if (NeuerTerminArt.EINMALIG_AUSNAHME in allowed) {
                SheetOptionColored(
                    title = stringResource(R.string.label_termin_art_einmalig_ausnahme),
                    subtitle = stringResource(R.string.label_termin_art_einmalig_ausnahme_sub),
                    backgroundColor = colorAusnahmeBg,
                    textColor = colorAusnahmeText,
                    onClick = { onArtSelected(NeuerTerminArt.EINMALIG_AUSNAHME); onDismiss() }
                )
            }
            if (NeuerTerminArt.URLAUB in allowed) {
                SheetOptionColored(
                    title = stringResource(R.string.label_termin_art_urlaub),
                    subtitle = stringResource(R.string.label_termin_art_urlaub_sub),
                    backgroundColor = colorUrlaubBg,
                    textColor = colorUrlaubText,
                    onClick = { onArtSelected(NeuerTerminArt.URLAUB); onDismiss() }
                )
            }
        }
    }
}

@Composable
private fun SheetOptionColored(
    title: String,
    subtitle: String,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 16.dp)
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = textColor
        )
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = textColor.copy(alpha = 0.9f)
        )
    }
}
