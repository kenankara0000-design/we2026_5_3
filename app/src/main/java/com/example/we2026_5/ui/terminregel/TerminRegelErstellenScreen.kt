package com.example.we2026_5.ui.terminregel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.we2026_5.R
import com.example.we2026_5.TerminRegelTyp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminRegelErstellenScreen(
    state: TerminRegelState,
    onNameChange: (String) -> Unit,
    onBeschreibungChange: (String) -> Unit,
    onWiederholenChange: (Boolean) -> Unit,
    onIntervallTageChange: (String) -> Unit,
    onIntervallAnzahlChange: (String) -> Unit,
    onRegelTypChange: (TerminRegelTyp) -> Unit,
    onZyklusTageChange: (String) -> Unit,
    onStartDateClick: () -> Unit,
    onTaeglichChange: (Boolean) -> Unit,
    onAbholungWochentagToggle: (Int) -> Unit,
    onAuslieferungWochentagToggle: (Int) -> Unit,
    onAktivChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val primaryBlue = Color(ContextCompat.getColor(context, R.color.primary_blue))
    val backgroundLight = Color(ContextCompat.getColor(context, R.color.background_light))
    val textPrimary = Color(ContextCompat.getColor(context, R.color.text_primary))
    val statusOverdue = Color(ContextCompat.getColor(context, R.color.status_overdue))
    var overflowMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.success) {
        if (state.success) onFinish()
    }

    LaunchedEffect(state.errorMessageResId) {
        state.errorMessageResId?.let { resId ->
            android.widget.Toast.makeText(context, context.getString(resId), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.currentRegelId != null) stringResource(R.string.termin_regel_titel_bearbeiten)
                        else stringResource(R.string.termin_regel_titel_neu),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_desc_back), tint = Color.White)
                    }
                },
                actions = {
                    if (state.currentRegelId != null) {
                        Box {
                            IconButton(onClick = { overflowMenuExpanded = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.content_desc_more_options), tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = overflowMenuExpanded,
                                onDismissRequest = { overflowMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.termin_regel_btn_delete), color = statusOverdue) },
                                    onClick = {
                                        overflowMenuExpanded = false
                                        onDelete()
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryBlue)
            )
        },
        containerColor = backgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(stringResource(R.string.termin_regel_label_name), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.hint_rule_name)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.termin_regel_label_desc_optional), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = state.beschreibung,
                onValueChange = onBeschreibungChange,
                label = { Text(stringResource(R.string.hint_desc_optional)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.termin_regel_label_typ), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
            Spacer(Modifier.height(4.dp))
            RegelTypDropdown(current = state.regelTyp, onSelect = onRegelTypChange)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = state.aktiv, onCheckedChange = onAktivChange)
                Text(stringResource(R.string.termin_regel_label_active), color = textPrimary, fontSize = 14.sp)
            }
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.termin_regel_label_startdate), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
            Spacer(Modifier.height(4.dp))
            Button(onClick = onStartDateClick, colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)) {
                Text(if (state.startDatumText.isNotEmpty()) stringResource(R.string.termin_regel_startdatum, state.startDatumText) else stringResource(R.string.termin_regel_btn_choose_startdate))
            }
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Checkbox(
                    checked = state.taeglich,
                    onCheckedChange = onTaeglichChange
                )
                Column {
                    Text(stringResource(R.string.termin_regel_taeglich), color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(stringResource(R.string.termin_regel_taeglich_hint), color = textPrimary.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))

            if (!state.taeglich) {
                Text(stringResource(R.string.termin_regel_label_abholung), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                Spacer(Modifier.height(8.dp))
                WochentagListenMenue(
                    selectedDays = state.abholungWochentage,
                    onDayToggle = onAbholungWochentagToggle,
                    primaryBlue = primaryBlue,
                    textPrimary = textPrimary
                )
                Spacer(Modifier.height(16.dp))

                Text(stringResource(R.string.termin_regel_label_auslieferung), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                Spacer(Modifier.height(8.dp))
                WochentagListenMenue(
                    selectedDays = state.auslieferungWochentage,
                    onDayToggle = onAuslieferungWochentagToggle,
                    primaryBlue = primaryBlue,
                    textPrimary = textPrimary
                )
                Spacer(Modifier.height(16.dp))
            }

            Text(stringResource(R.string.termin_regel_label_repeat), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Checkbox(checked = state.wiederholen, onCheckedChange = onWiederholenChange)
                Text(stringResource(R.string.termin_regel_repeat_hint), color = textPrimary)
            }
            Spacer(Modifier.height(16.dp))

            if (state.wiederholen || state.regelTyp == TerminRegelTyp.WEEKLY || state.regelTyp == TerminRegelTyp.FLEXIBLE_CYCLE) {
                Text(stringResource(R.string.termin_regel_label_interval_days), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(value = state.intervallTage, onValueChange = onIntervallTageChange, label = { Text(stringResource(R.string.hint_interval_days)) }, modifier = Modifier.fillMaxWidth())
            }
            if (state.wiederholen) {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.termin_regel_label_repeat_count), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(value = state.intervallAnzahl, onValueChange = onIntervallAnzahlChange, label = { Text(stringResource(R.string.hint_repeat_unlimited)) }, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSave,
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(ContextCompat.getColor(context, R.color.termin_regel_button_save)),
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.termin_regel_btn_save))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RegelTypDropdown(
    current: TerminRegelTyp,
    onSelect: (TerminRegelTyp) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (current) {
        TerminRegelTyp.WEEKLY -> stringResource(R.string.termin_regel_typ_weekly)
        TerminRegelTyp.FLEXIBLE_CYCLE -> stringResource(R.string.termin_regel_typ_flexible)
        TerminRegelTyp.ADHOC -> stringResource(R.string.termin_regel_typ_adhoc)
    }
    Box {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            label = { Text(stringResource(R.string.termin_regel_label_typ)) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            readOnly = true,
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TerminRegelTyp.values().forEach { typ ->
                DropdownMenuItem(
                    text = {
                        Text(
                            when (typ) {
                                TerminRegelTyp.WEEKLY -> stringResource(R.string.termin_regel_typ_weekly)
                                TerminRegelTyp.FLEXIBLE_CYCLE -> stringResource(R.string.termin_regel_typ_flexible)
                                TerminRegelTyp.ADHOC -> stringResource(R.string.termin_regel_typ_adhoc)
                            }
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(typ)
                    }
                )
            }
        }
    }
}

private val WochentagChipPaddingVertical = 14.dp
private val WochentagChipMinHeight = 48.dp
private val WochentagChipCorner = 8.dp
private val WochentagChipSpacing = 8.dp
private val WochentagChipFontSize = 14.sp

@Composable
private fun WochentagListenMenue(
    selectedDays: List<Int>,
    onDayToggle: (Int) -> Unit,
    primaryBlue: Color,
    textPrimary: Color
) {
    val weekendColor = colorResource(R.color.status_overdue)
    val weekendColorLight = weekendColor.copy(alpha = 0.25f)
    val weekdayColorLight = Color(0xFFE0E0E0)

    val wochentageShortResIds = listOf(
        R.string.label_weekday_short_mo,
        R.string.label_weekday_short_tu,
        R.string.label_weekday_short_mi,
        R.string.label_weekday_short_do,
        R.string.label_weekday_short_fr,
        R.string.label_weekday_short_sa,
        R.string.label_weekday_short_su
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WochentagChipSpacing)
    ) {
        wochentageShortResIds.forEachIndexed { index, resId ->
            val isWeekend = index == 5 || index == 6 // Sa, So
            val isSelected = index in selectedDays
            val (bgColor, textColor) = when {
                isSelected && isWeekend -> weekendColor to Color.White
                isSelected && !isWeekend -> primaryBlue to Color.White
                !isSelected && isWeekend -> weekendColorLight to weekendColor
                else -> weekdayColorLight to textPrimary
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = WochentagChipMinHeight)
                    .background(bgColor, RoundedCornerShape(WochentagChipCorner))
                    .clickable { onDayToggle(index) }
                    .padding(vertical = WochentagChipPaddingVertical),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(resId),
                    color = textColor,
                    fontSize = WochentagChipFontSize,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
