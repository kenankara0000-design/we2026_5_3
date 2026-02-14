package com.example.we2026_5.ui.urlaub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.example.we2026_5.ui.common.AppTopBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.example.we2026_5.UrlaubEintrag
import com.example.we2026_5.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlaubScreen(
    customer: Customer?,
    urlaubEintraege: List<UrlaubEintrag>,
    isSaving: Boolean,
    onBack: () -> Unit,
    onNeuerUrlaub: () -> Unit,
    onUrlaubAendern: (Int) -> Unit,
    onUrlaubEintragLoeschen: (Int) -> Unit
) {
    val primaryBlue = colorResource(R.color.primary_blue)
    val surfaceWhite = colorResource(R.color.surface_white)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)
    val backgroundLight = colorResource(R.color.background_light)
    val buttonUrlaub = colorResource(R.color.button_urlaub)
    val statusOverdue = colorResource(R.color.status_overdue)

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.label_urlaub),
                onBack = onBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundLight)
                .padding(paddingValues)
                .padding(20.dp)
        ) {
            if (customer == null) {
                item {
                    Text(stringResource(R.string.stat_loading), color = textSecondary)
                }
            } else {
                if (urlaubEintraege.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.dialog_urlaub_new_hint),
                            fontSize = 14.sp,
                            color = textSecondary,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )
                    }
                }
                itemsIndexed(urlaubEintraege) { index, eintrag ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = surfaceWhite),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(
                                        R.string.dialog_urlaub_current,
                                        DateFormatter.formatDate(eintrag.von),
                                        DateFormatter.formatDate(eintrag.bis)
                                    ),
                                    fontSize = 16.sp,
                                    color = textPrimary
                                )
                            }
                            IconButton(onClick = { onUrlaubAendern(index) }) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = stringResource(R.string.content_desc_edit_urlaub),
                                    tint = primaryBlue
                                )
                            }
                            IconButton(onClick = { onUrlaubEintragLoeschen(index) }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.dialog_urlaub_delete_entry),
                                    tint = statusOverdue
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onNeuerUrlaub,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = buttonUrlaub),
                        enabled = !isSaving
                    ) {
                        Text(stringResource(R.string.dialog_urlaub_add_new))
                    }
                }
            }
        }
    }
}
