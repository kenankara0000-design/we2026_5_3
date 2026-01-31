package com.example.we2026_5.ui.terminregel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R
import com.example.we2026_5.TerminRegel
import com.example.we2026_5.util.DateFormatter
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminRegelManagerScreen(
    regeln: List<TerminRegel>,
    onBack: () -> Unit,
    onNewRegel: () -> Unit,
    onRegelClick: (TerminRegel) -> Unit
) {
    val context = LocalContext.current
    val headerBg = Color(ContextCompat.getColor(context, R.color.termin_regel_header_bg))
    val headerText = Color(ContextCompat.getColor(context, R.color.termin_regel_header_text))
    val buttonBlue = Color(ContextCompat.getColor(context, R.color.button_blue))
    val cardBg = Color(ContextCompat.getColor(context, R.color.termin_regel_card_bg))
    val textPrimary = Color(ContextCompat.getColor(context, R.color.text_primary))
    val textSecondary = Color(ContextCompat.getColor(context, R.color.text_secondary))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.termin_regel_screen_title),
                        color = headerText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back),
                            tint = headerText
                        )
                    }
                },
                actions = {
                    androidx.compose.material3.Button(
                        modifier = Modifier.padding(end = 8.dp),
                        onClick = onNewRegel,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = buttonBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.termin_regel_btn_new),
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = headerBg)
            )
        }
    ) { paddingValues ->
        if (regeln.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                Text(text = "📋", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.termin_regel_empty_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.termin_regel_empty_subtitle),
                    fontSize = 14.sp,
                    color = textSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(regeln, key = { it.id }) { regel ->
                    TerminRegelItem(
                        regel = regel,
                        cardBg = cardBg,
                        buttonBlue = buttonBlue,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onClick = { onRegelClick(regel) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminRegelItem(
    regel: TerminRegel,
    cardBg: Color,
    buttonBlue: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    val abholungText = if (regel.abholungDatum > 0) {
        DateFormatter.formatDateWithLeadingZeros(regel.abholungDatum)
    } else "Heute"
    val auslieferungText = if (regel.auslieferungDatum > 0) {
        DateFormatter.formatDateWithLeadingZeros(regel.auslieferungDatum)
    } else "Heute"
    val usedCountText = stringResource(R.string.label_used_count_format, regel.verwendungsanzahl)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .background(buttonBlue, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = regel.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            if (regel.beschreibung.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = regel.beschreibung,
                    fontSize = 14.sp,
                    color = textSecondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = stringResource(R.string.label_abholung), fontSize = 12.sp, color = textSecondary)
                Text(text = abholungText, fontSize = 12.sp, color = textPrimary)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = stringResource(R.string.label_auslieferung), fontSize = 12.sp, color = textSecondary)
                Text(text = auslieferungText, fontSize = 12.sp, color = textPrimary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = stringResource(R.string.label_verwendungen), fontSize = 12.sp, color = textSecondary)
                Text(text = usedCountText, fontSize = 12.sp, color = textPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
