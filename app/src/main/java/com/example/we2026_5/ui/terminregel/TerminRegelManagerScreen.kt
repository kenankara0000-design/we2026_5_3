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
import androidx.core.content.ContextCompat
import java.util.Calendar

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

/** Wochentag-Kürzel Mo–So (Index 0=Mo … 6=So, wie in TerminRegel). */
private val WOCHENTAG_KURZ_REGEL = listOf(
    R.string.label_weekday_short_mo,
    R.string.label_weekday_short_tu,
    R.string.label_weekday_short_mi,
    R.string.label_weekday_short_do,
    R.string.label_weekday_short_fr,
    R.string.label_weekday_short_sa,
    R.string.label_weekday_short_su
)
/** Wochentag-Kürzel für Calendar.DAY_OF_WEEK (1=So … 7=Sa). */
private val WOCHENTAG_KURZ_CAL = listOf(
    R.string.label_weekday_short_su,
    R.string.label_weekday_short_mo,
    R.string.label_weekday_short_tu,
    R.string.label_weekday_short_mi,
    R.string.label_weekday_short_do,
    R.string.label_weekday_short_fr,
    R.string.label_weekday_short_sa
)

@Composable
private fun TerminRegelItem(
    regel: TerminRegel,
    cardBg: Color,
    buttonBlue: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    val abholungWochentag = regelAbholungWochentagText(regel)
    val auslieferungWochentag = regelAuslieferungWochentagText(regel)
    val intervallText = if (regel.wiederholen) stringResource(R.string.termin_regel_intervall_tage, regel.intervallTage.coerceAtLeast(1)) else "–"
    val wiederholenText = if (regel.wiederholen) stringResource(R.string.termin_regel_ja) else stringResource(R.string.termin_regel_nein)
    val usedCountText = stringResource(R.string.label_used_count_format, regel.verwendungsanzahl)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = regel.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(stringResource(R.string.label_abholung), abholungWochentag, textPrimary, textSecondary)
            InfoRow(stringResource(R.string.label_auslieferung), auslieferungWochentag, textPrimary, textSecondary)
            InfoRow(stringResource(R.string.label_intervall), intervallText, textPrimary, textSecondary)
            InfoRow(stringResource(R.string.label_wiederholen), wiederholenText, textPrimary, textSecondary)
            InfoRow(stringResource(R.string.label_verwendungen), usedCountText, textPrimary, textSecondary)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, textPrimary: Color, textSecondary: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = textSecondary)
        Text(text = value, fontSize = 12.sp, color = textPrimary)
    }
}

@Composable
private fun regelAbholungWochentagText(regel: TerminRegel): String {
    val wochentagRegel = WOCHENTAG_KURZ_REGEL.map { stringResource(it) }
    val wochentagCal = WOCHENTAG_KURZ_CAL.map { stringResource(it) }
    val heute = stringResource(R.string.hint_heute)
    return if (regel.wochentagBasiert) {
        val list = regel.abholungWochentage
        if (!list.isNullOrEmpty()) {
            list.sorted().joinToString(", ") { wochentagRegel[it.coerceIn(0, 6)] }
        } else if (regel.abholungWochentag in 0..6) {
            wochentagRegel[regel.abholungWochentag]
        } else heute
    } else {
        if (regel.abholungDatum > 0) {
            val cal = Calendar.getInstance().apply { timeInMillis = regel.abholungDatum }
            val idx = (cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)
            wochentagCal[idx]
        } else heute
    }
}

@Composable
private fun regelAuslieferungWochentagText(regel: TerminRegel): String {
    val wochentagRegel = WOCHENTAG_KURZ_REGEL.map { stringResource(it) }
    val wochentagCal = WOCHENTAG_KURZ_CAL.map { stringResource(it) }
    val heute = stringResource(R.string.hint_heute)
    return if (regel.wochentagBasiert) {
        val list = regel.auslieferungWochentage
        if (!list.isNullOrEmpty()) {
            list.sorted().joinToString(", ") { wochentagRegel[it.coerceIn(0, 6)] }
        } else if (regel.auslieferungWochentag in 0..6) {
            wochentagRegel[regel.auslieferungWochentag]
        } else heute
    } else {
        if (regel.auslieferungDatum > 0) {
            val cal = Calendar.getInstance().apply { timeInMillis = regel.auslieferungDatum }
            val idx = (cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)
            wochentagCal[idx]
        } else heute
    }
}
