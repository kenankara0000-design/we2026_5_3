package com.example.we2026_5.ui.listebearbeiten

import com.example.we2026_5.Customer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R
import com.example.we2026_5.ui.common.ExpandableSection

@Composable
fun ListeBearbeitenKundenSectionCollapsible(
    isTourListe: Boolean,
    wochentag: Int?,
    kundenInListe: List<Customer>,
    verfuegbareKunden: List<Customer>,
    onRemoveKunde: (Customer) -> Unit,
    onAddKunde: (Customer) -> Unit,
    textPrimary: Color,
    textSecondary: Color,
    statusOverdue: Color,
    statusDone: Color
) {
    if (isTourListe) {
        ExpandableSection(
            titleResId = R.string.label_customers_in_list,
            defaultExpanded = false,
            textPrimary = textPrimary
        ) {
            ListeBearbeitenKundenSectionContent(
                showTitle = false,
                wochentag = wochentag,
                kundenInListe = kundenInListe,
                verfuegbareKunden = verfuegbareKunden,
                onRemoveKunde = onRemoveKunde,
                onAddKunde = onAddKunde,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                statusOverdue = statusOverdue,
                statusDone = statusDone
            )
        }
    } else {
        Spacer(modifier = Modifier.height(16.dp))
        ListeBearbeitenKundenSectionContent(
            showTitle = true,
            wochentag = wochentag,
            kundenInListe = kundenInListe,
            verfuegbareKunden = verfuegbareKunden,
            onRemoveKunde = onRemoveKunde,
            onAddKunde = onAddKunde,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            statusOverdue = statusOverdue,
            statusDone = statusDone
        )
    }
}

@Composable
private fun ListeBearbeitenKundenSectionContent(
    showTitle: Boolean,
    wochentag: Int?,
    kundenInListe: List<Customer>,
    verfuegbareKunden: List<Customer>,
    onRemoveKunde: (Customer) -> Unit,
    onAddKunde: (Customer) -> Unit,
    textPrimary: Color,
    textSecondary: Color,
    statusOverdue: Color,
    statusDone: Color
) {
    if (showTitle) {
        Text(stringResource(R.string.label_customers_in_list), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Spacer(modifier = Modifier.height(8.dp))
    }
    if (kundenInListe.isEmpty() && (wochentag ?: -1) in 0..6) {
        Text(stringResource(R.string.label_list_empty_weekday), color = textSecondary, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
    }
    kundenInListe.forEach { kunde ->
        ListeBearbeitenKundeInListeItem(kunde, showRemove = true, onRemove = { onRemoveKunde(kunde) }, onAdd = {}, textPrimary = textPrimary, textSecondary = textSecondary, statusOverdue = statusOverdue, statusDone = statusDone)
        Spacer(modifier = Modifier.height(8.dp))
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(stringResource(R.string.label_add_available_customers), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
    Spacer(modifier = Modifier.height(8.dp))
    verfuegbareKunden.forEach { kunde ->
        ListeBearbeitenKundeInListeItem(kunde, showRemove = false, onRemove = {}, onAdd = { onAddKunde(kunde) }, textPrimary = textPrimary, textSecondary = textSecondary, statusOverdue = statusOverdue, statusDone = statusDone)
        Spacer(modifier = Modifier.height(8.dp))
    }
}
