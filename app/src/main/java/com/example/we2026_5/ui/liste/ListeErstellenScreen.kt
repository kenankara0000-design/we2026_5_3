package com.example.we2026_5.ui.liste

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import com.example.we2026_5.ui.common.AppTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
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
import com.example.we2026_5.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeErstellenScreen(
    state: ListeErstellenState,
    onListNameChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onWochentagListeChange: (Boolean) -> Unit,
    onWochentagChange: (Int) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val backgroundLight = Color(ContextCompat.getColor(context, R.color.background_light))
    val textPrimary = Color(ContextCompat.getColor(context, R.color.text_primary))
    val textSecondary = Color(ContextCompat.getColor(context, R.color.text_secondary))
    val surfaceWhite = Color(ContextCompat.getColor(context, R.color.surface_white))

    LaunchedEffect(state.success) {
        if (state.success) {
            delay(800)
            onFinish()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.list_create_title)
            )
        },
        containerColor = backgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.listName,
                onValueChange = onListNameChange,
                label = { Text(stringResource(R.string.hint_list_name_short)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = state.errorMessage != null
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.label_list_type),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = state.selectedType == "Gewerbe",
                        onClick = { onTypeChange("Gewerbe") }
                    )
                    Text(stringResource(R.string.label_type_gewerblich), color = textPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = state.selectedType == "Privat",
                        onClick = { onTypeChange("Privat") }
                    )
                    Text(stringResource(R.string.label_type_privat), color = textPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = state.selectedType == "Listenkunden",
                        onClick = { onTypeChange("Listenkunden") }
                    )
                    Text(stringResource(R.string.label_type_tour), color = textPrimary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = state.isWochentagListe,
                    onCheckedChange = onWochentagListeChange
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(stringResource(R.string.label_list_wochentag), color = textPrimary)
            }
            if (state.isWochentagListe) {
                Spacer(modifier = Modifier.height(8.dp))
                WeekdaySelector(
                    label = stringResource(R.string.label_wochentag),
                    selected = state.wochentag,
                    onSelect = onWochentagChange,
                    textPrimary = textPrimary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(ContextCompat.getColor(context, R.color.termin_regel_button_save)),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = when {
                        state.isSaving -> stringResource(R.string.save_in_progress)
                        state.success -> stringResource(R.string.toast_saved_success)
                        else -> stringResource(R.string.btn_save)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeekdaySelector(
    label: String,
    selected: Int,
    onSelect: (Int) -> Unit,
    textPrimary: Color
) {
    val context = LocalContext.current
    val primaryBlue = Color(ContextCompat.getColor(context, R.color.primary_blue))
    val weekdays = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    val chipBg = AppColors.LightGray
    Column {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            weekdays.forEachIndexed { index, title ->
                val isSelected = selected == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minWidth = 0.dp)
                        .heightIn(min = 36.dp)
                        .background(
                            if (isSelected) primaryBlue else chipBg,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onSelect(index) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) Color.White else textPrimary,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
