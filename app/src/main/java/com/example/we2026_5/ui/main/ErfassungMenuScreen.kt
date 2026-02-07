package com.example.we2026_5.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErfassungMenuScreen(
    onErfassungStarten: () -> Unit,
    onArtikelVerwalten: () -> Unit,
    onBack: () -> Unit
) {
    val primaryBlue = colorResource(R.color.primary_blue)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.erfassung_menu_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_desc_back), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryBlue, titleContentColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onErfassungStarten,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.erfassung_menu_start), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onArtikelVerwalten,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.erfassung_menu_artikel), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
