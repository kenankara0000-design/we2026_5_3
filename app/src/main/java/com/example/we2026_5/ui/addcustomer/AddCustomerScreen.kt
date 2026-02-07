package com.example.we2026_5.ui.addcustomer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.we2026_5.KundenTyp
import com.example.we2026_5.R
import com.example.we2026_5.util.DialogBaseHelper
import com.example.we2026_5.util.TerminBerechnungUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerScreen(
    state: AddCustomerState,
    onBack: () -> Unit,
    onUpdate: (AddCustomerState) -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val primaryBlue = Color(ContextCompat.getColor(context, R.color.primary_blue))
    val backgroundLight = Color(ContextCompat.getColor(context, R.color.background_light))

    Scaffold(
        containerColor = backgroundLight,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.add_customer_title),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = { },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryBlue)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .background(backgroundLight)
                .padding(paddingValues)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            CustomerStammdatenForm(
                state = state,
                onUpdate = onUpdate,
                onStartDatumClick = {
                    DialogBaseHelper.showDatePickerDialog(
                        context = context,
                        initialDate = state.erstelltAm.takeIf { it > 0 } ?: System.currentTimeMillis(),
                        title = context.getString(R.string.label_startdatum_a),
                        onDateSelected = { selected ->
                            onUpdate(state.copy(erstelltAm = TerminBerechnungUtils.getStartOfDay(selected)))
                        }
                    )
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(ContextCompat.getColor(context, R.color.termin_regel_button_save)),
                    contentColor = Color.White
                ),
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !state.isSaving
            ) {
                Text(
                    if (state.isSaving) stringResource(R.string.save_in_progress)
                    else stringResource(R.string.btn_save)
                )
            }
        }
    }
}
