package com.example.we2026_5.ui.listebearbeiten

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R

@Composable
fun ListeBearbeitenMetadatenBlock(
    isInEditMode: Boolean,
    displayName: String,
    displayListeArt: String,
    onEdit: () -> Unit,
    editName: String,
    editListeArt: String,
    nameError: String?,
    onNameChange: (String) -> Unit,
    onListeArtChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    textPrimary: Color,
    statusOverdue: Color,
    saveButtonContainerColor: Color
) {
    if (!isInEditMode) {
        androidx.compose.material3.Button(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.label_list_edit)) }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.label_list_name_field), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Text(text = displayName, modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(8.dp), color = textPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.label_list_type), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Text(text = displayListeArt, modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(8.dp), color = textPrimary)
    } else {
        Text(text = stringResource(R.string.label_list_name_field), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        OutlinedTextField(
            value = editName,
            onValueChange = { value -> onNameChange(value) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = nameError != null,
            supportingText = nameError?.let { err -> { Text(err, color = statusOverdue) } }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.label_list_type), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = editListeArt == "Gewerbe", onClick = { onListeArtChange("Gewerbe") })
                Text(stringResource(R.string.label_type_gewerbe), color = textPrimary)
            }
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = editListeArt == "Privat", onClick = { onListeArtChange("Privat") })
                Text(stringResource(R.string.label_type_privat), color = textPrimary)
            }
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = editListeArt == "Listenkunden", onClick = { onListeArtChange("Listenkunden") })
                Text(stringResource(R.string.label_type_tour), color = textPrimary)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.Button(
                onClick = onSaveClick,
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = saveButtonContainerColor,
                    contentColor = Color.White
                )
            ) { Text(stringResource(R.string.btn_save)) }
        }
    }
}
