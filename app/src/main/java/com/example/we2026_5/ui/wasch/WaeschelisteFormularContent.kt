package com.example.we2026_5.ui.wasch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.example.we2026_5.wasch.WaeschelisteArtikel
import com.example.we2026_5.wasch.WaeschelisteFormularState
import com.example.we2026_5.wasch.WaeschelisteArtikelItem

private val buttonShape = RoundedCornerShape(12.dp)

@Composable
fun WaeschelisteFormularContent(
    customer: Customer,
    formularState: WaeschelisteFormularState,
    onNameChange: (String) -> Unit,
    onAdresseChange: (String) -> Unit,
    onTelefonChange: (String) -> Unit,
    onMengeChange: (String, Int) -> Unit,
    onSonstigesChange: (String) -> Unit,
    onKameraFoto: () -> Unit,
    onAbbrechen: () -> Unit,
    onSpeichern: () -> Unit,
    isSaving: Boolean,
    isScanning: Boolean = false,
    errorMessage: String?,
    textPrimary: Color,
    textSecondary: Color
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
        Text(
            stringResource(R.string.waescheliste_formular_title),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        OutlinedTextField(
            value = formularState.name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.waescheliste_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = formularState.adresse,
            onValueChange = onAdresseChange,
            label = { Text(stringResource(R.string.waescheliste_adresse)) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            singleLine = true
        )
        OutlinedTextField(
            value = formularState.telefon,
            onValueChange = onTelefonChange,
            label = { Text(stringResource(R.string.waescheliste_telefon)) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        Text(
            stringResource(R.string.waescheliste_artikl),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                WaeschelisteArtiklSpalte(
                    items = WaeschelisteArtikel.spalteLinks(),
                    formularState = formularState,
                    onMengeChange = onMengeChange,
                    textPrimary = textPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                WaeschelisteArtiklSpalte(
                    items = WaeschelisteArtikel.spalteRechts(),
                    formularState = formularState,
                    onMengeChange = onMengeChange,
                    textPrimary = textPrimary
                )
            }
        }
        OutlinedTextField(
            value = formularState.sonstiges,
            onValueChange = onSonstigesChange,
            label = { Text(stringResource(R.string.waescheliste_sonstiges)) },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            minLines = 2
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.wasch_gesamt), color = textSecondary, fontSize = 12.sp)
            Text("${formularState.gesamtStueck()}", color = textPrimary, fontWeight = FontWeight.Bold)
        }
        errorMessage?.let { Text(it, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onKameraFoto, modifier = Modifier.weight(1f), shape = buttonShape) {
                Text(stringResource(R.string.btn_kamera_foto))
            }
            Button(onClick = onAbbrechen, modifier = Modifier.weight(1f), shape = buttonShape) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
        Button(
            onClick = onSpeichern,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            enabled = !isSaving && !isScanning,
            shape = buttonShape
        ) {
            Text(if (isSaving) "…" else stringResource(R.string.wasch_speichern))
        }
        }
        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        stringResource(R.string.waescheliste_ocr_laden),
                        modifier = Modifier.padding(top = 8.dp),
                        color = textSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun WaeschelisteArtiklSpalte(
    items: List<WaeschelisteArtikelItem>,
    formularState: WaeschelisteFormularState,
    onMengeChange: (String, Int) -> Unit,
    textPrimary: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = formularState.mengeForKey(item.key).toString().takeIf { it != "0" }.orEmpty(),
                    onValueChange = { str ->
                        val v = str.toIntOrNull() ?: 0
                        onMengeChange(item.key, v.coerceAtLeast(0))
                    },
                    modifier = Modifier.widthIn(min = 36.dp, max = 44.dp).padding(end = 6.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                )
                Text(item.label, fontSize = 12.sp, color = textPrimary, modifier = Modifier.weight(1f))
            }
        }
    }
}
