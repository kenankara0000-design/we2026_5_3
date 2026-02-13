package com.example.we2026_5.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R
import com.example.we2026_5.ui.common.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErfassungMenuScreen(
    onErfassungStarten: () -> Unit,
    onBelege: () -> Unit,
    onBack: () -> Unit
) {
    val primaryBlue = colorResource(R.color.primary_blue)
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.erfassung_menu_title),
                onBack = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onErfassungStarten,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.erfassung_menu_start), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onBelege,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.erfassung_menu_belege), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
