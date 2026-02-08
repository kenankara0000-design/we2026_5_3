package com.example.we2026_5.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.we2026_5.KundenTyp
import com.example.we2026_5.R
import com.example.we2026_5.ui.addcustomer.AddCustomerIntervallSchnellauswahl
import com.example.we2026_5.ui.addcustomer.AddCustomerRadioOption
import com.example.we2026_5.ui.addcustomer.AddCustomerState
import com.example.we2026_5.ui.addcustomer.AddCustomerTageAzuLField
import com.example.we2026_5.ui.addcustomer.AddCustomerWeekdaySelectorMulti
import com.example.we2026_5.ui.common.DetailUiConstants
import com.example.we2026_5.ui.common.ExpandableSection

/**
 * Nur der Termin- und Tour-Bereich (Kundenart, T.-Typ, Startdatum, Ohne Tour, Intervall, L-Termin, Abhol-/Auslieferungstage, Weitere Angaben).
 * Wird im Tab „Termine & Tour“ angezeigt (Kunden-Detail Bearbeitung).
 */
@Composable
fun CustomerDetailTermineTourForm(
    state: AddCustomerState,
    onUpdate: (AddCustomerState) -> Unit,
    onStartDatumClick: () -> Unit
) {
    val context = LocalContext.current
    val textPrimary = Color(ContextCompat.getColor(context, R.color.text_primary))
    val textSecondary = Color(ContextCompat.getColor(context, R.color.text_secondary))
    val spacing = DetailUiConstants.FieldSpacing

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.label_customer_type),
                fontSize = DetailUiConstants.FieldLabelSp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            AddCustomerRadioOption(
                text = stringResource(R.string.label_type_gewerblich),
                selected = state.kundenArt == "Gewerblich",
                onSelect = { onUpdate(state.copy(kundenArt = "Gewerblich")) },
                textPrimary = textPrimary
            )
            AddCustomerRadioOption(
                text = stringResource(R.string.label_type_privat),
                selected = state.kundenArt == "Privat",
                onSelect = { onUpdate(state.copy(kundenArt = "Privat")) },
                textPrimary = textPrimary
            )
            AddCustomerRadioOption(
                text = stringResource(R.string.label_type_tour),
                selected = state.kundenArt == "Tour",
                onSelect = { onUpdate(state.copy(kundenArt = "Tour")) },
                textPrimary = textPrimary
            )
        }

        Spacer(modifier = Modifier.height(spacing))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.label_kunden_typ),
                fontSize = DetailUiConstants.FieldLabelSp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            AddCustomerRadioOption(
                text = stringResource(R.string.label_kunden_typ_regelmaessig),
                selected = state.kundenTyp == KundenTyp.REGELMAESSIG,
                onSelect = { onUpdate(state.copy(kundenTyp = KundenTyp.REGELMAESSIG)) },
                textPrimary = textPrimary
            )
            AddCustomerRadioOption(
                text = stringResource(R.string.label_kunden_typ_unregelmaessig),
                selected = state.kundenTyp == KundenTyp.UNREGELMAESSIG,
                onSelect = { onUpdate(state.copy(kundenTyp = KundenTyp.UNREGELMAESSIG)) },
                textPrimary = textPrimary
            )
            AddCustomerRadioOption(
                text = stringResource(R.string.label_kunden_typ_auf_abruf),
                selected = state.kundenTyp == KundenTyp.AUF_ABRUF,
                onSelect = { onUpdate(state.copy(kundenTyp = KundenTyp.AUF_ABRUF)) },
                textPrimary = textPrimary
            )
        }

        Spacer(modifier = Modifier.height(spacing))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onStartDatumClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_startdatum_a),
                    fontSize = DetailUiConstants.FieldLabelSp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = if (state.erstelltAm > 0) com.example.we2026_5.util.DateFormatter.formatDate(state.erstelltAm) else stringResource(R.string.hint_heute),
                    fontSize = DetailUiConstants.FieldLabelSp,
                    color = textSecondary
                )
            }
            androidx.compose.material3.Checkbox(
                checked = state.ohneTour,
                onCheckedChange = { onUpdate(state.copy(ohneTour = it)) }
            )
            Text(
                text = stringResource(R.string.label_ohne_tour),
                color = textPrimary,
                fontSize = DetailUiConstants.FieldLabelSp,
                modifier = Modifier.clickable { onUpdate(state.copy(ohneTour = !state.ohneTour)) }
            )
        }

        if (state.kundenTyp == KundenTyp.UNREGELMAESSIG) {
            Spacer(modifier = Modifier.height(spacing))
            Text(
                text = stringResource(R.string.label_l_termin),
                fontSize = DetailUiConstants.FieldLabelSp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = state.tageAzuL?.takeIf { it in 0..365 }?.toString() ?: "",
                onValueChange = { s ->
                    val v = s.filter { it.isDigit() }.toIntOrNull()?.coerceIn(0, 365)
                    onUpdate(state.copy(tageAzuL = v))
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("z.B. 7") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { Text(stringResource(R.string.hint_a_plus_tage), color = textSecondary, fontSize = 12.sp) }
            )
        }

        if (state.kundenTyp == KundenTyp.REGELMAESSIG) {
            Spacer(modifier = Modifier.height(spacing))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    AddCustomerIntervallSchnellauswahl(
                        selected = state.intervallTage,
                        onSelect = { onUpdate(state.copy(intervallTage = it?.coerceIn(1, 365))) },
                        textPrimary = textPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    AddCustomerTageAzuLField(
                        value = state.tageAzuL,
                        onValueChange = { onUpdate(state.copy(tageAzuL = it?.coerceIn(0, 365))) },
                        textPrimary = textPrimary
                    )
                }
            }
        }

        if (state.kundenTyp != KundenTyp.AUF_ABRUF) {
            Spacer(modifier = Modifier.height(spacing))
            AddCustomerWeekdaySelectorMulti(
                label = stringResource(R.string.label_default_pickup_day),
                selectedDays = state.abholungWochentage,
                onDayToggle = { day ->
                    val list = state.abholungWochentage.toMutableList()
                    if (day in list) list.remove(day) else list.add(day)
                    list.sort()
                    onUpdate(state.copy(abholungWochentage = list))
                }
            )
            Spacer(modifier = Modifier.height(spacing))
            AddCustomerWeekdaySelectorMulti(
                label = stringResource(R.string.label_default_delivery_day),
                selectedDays = state.auslieferungWochentage,
                onDayToggle = { day ->
                    val list = state.auslieferungWochentage.toMutableList()
                    if (day in list) list.remove(day) else list.add(day)
                    list.sort()
                    onUpdate(state.copy(auslieferungWochentage = list))
                }
            )
        }

        Spacer(modifier = Modifier.height(DetailUiConstants.SectionSpacing))
        ExpandableSection(
            defaultExpanded = false,
            textPrimary = textPrimary
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.defaultUhrzeit,
                onValueChange = { onUpdate(state.copy(defaultUhrzeit = it)) },
                label = { Text(stringResource(R.string.label_default_time_optional)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("09:00") }
            )
            Spacer(modifier = Modifier.height(spacing))
            OutlinedTextField(
                value = state.kundennummer,
                onValueChange = { onUpdate(state.copy(kundennummer = it)) },
                label = { Text(stringResource(R.string.label_kundennummer)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(spacing))
            OutlinedTextField(
                value = state.tagsInput,
                onValueChange = { onUpdate(state.copy(tagsInput = it)) },
                label = { Text(stringResource(R.string.label_customer_tags)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                placeholder = { Text(stringResource(R.string.hint_tags_example)) }
            )
            Spacer(modifier = Modifier.height(spacing))
            Text(
                text = stringResource(R.string.label_tour_plan),
                fontSize = DetailUiConstants.FieldLabelSp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = state.tourStadt,
                onValueChange = { onUpdate(state.copy(tourStadt = it)) },
                label = { Text(stringResource(R.string.label_tour_city)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.tourZeitStart,
                    onValueChange = { onUpdate(state.copy(tourZeitStart = it)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.label_time_from)) },
                    placeholder = { Text("09:00") }
                )
                OutlinedTextField(
                    value = state.tourZeitEnde,
                    onValueChange = { onUpdate(state.copy(tourZeitEnde = it)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.label_time_to)) },
                    placeholder = { Text("13:00") }
                )
            }
        }
    }
}
