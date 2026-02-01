package com.example.we2026_5.ui.terminregel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.we2026_5.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminRegelErstellenScreen(
    state: TerminRegelState,
    onNameChange: (String) -> Unit,
    onBeschreibungChange: (String) -> Unit,
    onWiederholenChange: (Boolean) -> Unit,
    onIntervallTageChange: (String) -> Unit,
    onIntervallAnzahlChange: (String) -> Unit,
    onWochentagBasiertChange: (Boolean) -> Unit,
    onStartDateClick: () -> Unit,
    onAbholungDateClick: () -> Unit,
    onAuslieferungDateClick: () -> Unit,
    onAbholungWochentagClick: () -> Unit,
    onAuslieferungWochentagClick: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val primaryBlue = Color(ContextCompat.getColor(context, R.color.primary_blue))
    val backgroundLight = Color(ContextCompat.getColor(context, R.color.background_light))
    val textPrimary = Color(ContextCompat.getColor(context, R.color.text_primary))

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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = state.wochentagBasiert, onCheckedChange = onWochentagBasiertChange)
                Text(stringResource(R.string.termin_regel_label_weekday_hint), color = textPrimary)
            }
            Spacer(Modifier.height(16.dp))

            if (state.wochentagBasiert) {
                Text(stringResource(R.string.termin_regel_label_startdate), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                Spacer(Modifier.height(4.dp))
                Button(onClick = onStartDateClick, colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)) {
                    Text(if (state.startDatumText.isNotEmpty()) stringResource(R.string.termin_regel_startdatum, state.startDatumText) else stringResource(R.string.termin_regel_btn_choose_startdate))
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.termin_regel_label_abholung), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                Spacer(Modifier.height(4.dp))
                Button(onClick = onAbholungWochentagClick, colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)) {
                    Text(if (state.abholungWochentagText.isNotEmpty()) state.abholungWochentagText else stringResource(R.string.termin_regel_wochentag_waehlen))
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.termin_regel_label_auslieferung), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                Spacer(Modifier.height(4.dp))
                Button(onClick = onAuslieferungWochentagClick, colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)) {
                    Text(if (state.auslieferungWochentagText.isNotEmpty()) state.auslieferungWochentagText else stringResource(R.string.termin_regel_wochentag_waehlen))
                }
            } else {
                Text(stringResource(R.string.termin_regel_label_abholung), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                Spacer(Modifier.height(4.dp))
                Button(onClick = onAbholungDateClick, colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)) {
                    Text(if (state.abholungDatumText.isNotEmpty()) stringResource(R.string.termin_regel_abholung_datum, state.abholungDatumText) else stringResource(R.string.termin_regel_abholung))
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.termin_regel_label_auslieferung), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                Spacer(Modifier.height(4.dp))
                Button(onClick = onAuslieferungDateClick, colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)) {
                    Text(if (state.auslieferungDatumText.isNotEmpty()) stringResource(R.string.termin_regel_auslieferung_datum, state.auslieferungDatumText) else stringResource(R.string.termin_regel_auslieferung))
                }
            }
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.termin_regel_label_repeat), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = state.wiederholen, onCheckedChange = onWiederholenChange)
                Text(stringResource(R.string.termin_regel_repeat_hint), color = textPrimary)
            }
            Spacer(Modifier.height(16.dp))

            if (state.wiederholen) {
                Text(stringResource(R.string.termin_regel_label_interval_days), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(value = state.intervallTage, onValueChange = onIntervallTageChange, label = { Text(stringResource(R.string.hint_interval_days)) }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.termin_regel_label_repeat_count), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(value = state.intervallAnzahl, onValueChange = onIntervallAnzahlChange, label = { Text(stringResource(R.string.hint_repeat_unlimited)) }, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSave,
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(ContextCompat.getColor(context, R.color.termin_regel_button_save)))
                ) {
                    Text(stringResource(R.string.termin_regel_btn_save))
                }
                if (state.currentRegelId != null) {
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(ContextCompat.getColor(context, R.color.status_overdue)))
                    ) {
                        Text(stringResource(R.string.termin_regel_btn_delete))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
