package com.example.we2026_5.ui.statistics

import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.core.content.ContextCompat
import com.example.we2026_5.R
import com.example.we2026_5.ui.statistics.StatisticsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    state: StatisticsState?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val primaryBlue = Color(ContextCompat.getColor(context, R.color.primary_blue))
    val surfaceWhite = Color(ContextCompat.getColor(context, R.color.surface_white))
    val backgroundLight = Color(ContextCompat.getColor(context, R.color.background_light))
    val textPrimary = Color(ContextCompat.getColor(context, R.color.text_primary))
    val sectionOverdueBg = Color(ContextCompat.getColor(context, R.color.section_overdue_bg))
    val sectionOverdueText = Color(ContextCompat.getColor(context, R.color.section_overdue_text))
    val statusOverdue = Color(ContextCompat.getColor(context, R.color.status_overdue))
    val sectionDoneBg = Color(ContextCompat.getColor(context, R.color.section_done_bg))
    val sectionDoneText = Color(ContextCompat.getColor(context, R.color.section_done_text))
    val statusDone = Color(ContextCompat.getColor(context, R.color.status_done))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.stat_title),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = { },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryBlue
                )
            )
        },
        containerColor = backgroundLight
    ) { paddingValues ->
        when {
            state == null || state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.stat_loading),
                        color = primaryBlue,
                        fontSize = 16.sp
                    )
                }
            }
            state.sleepMode -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.stat_sleep_mode),
                        color = textPrimary,
                        fontSize = 16.sp
                    )
                }
            }
            state.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = context.getString(R.string.error_message_generic, state.errorMessage),
                        color = statusOverdue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            state != null && !state.isLoading && state.errorMessage == null -> {
                val s = state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    StatCard(
                        label = stringResource(R.string.stat_heute_faellig),
                        value = s.heuteCount.toString(),
                        labelColor = textPrimary,
                        valueColor = primaryBlue,
                        backgroundColor = surfaceWhite
                    )
                    StatCard(
                        label = stringResource(R.string.stat_diese_woche),
                        value = s.wocheCount.toString(),
                        labelColor = textPrimary,
                        valueColor = primaryBlue,
                        backgroundColor = surfaceWhite
                    )
                    StatCard(
                        label = stringResource(R.string.stat_dieser_monat),
                        value = s.monatCount.toString(),
                        labelColor = textPrimary,
                        valueColor = primaryBlue,
                        backgroundColor = surfaceWhite
                    )
                    StatCard(
                        label = stringResource(R.string.stat_ueberfaellig),
                        value = s.overdueCount.toString(),
                        labelColor = sectionOverdueText,
                        valueColor = statusOverdue,
                        backgroundColor = sectionOverdueBg
                    )
                    StatCard(
                        label = stringResource(R.string.stat_erledigt_heute),
                        value = s.doneTodayCount.toString(),
                        labelColor = sectionDoneText,
                        valueColor = statusDone,
                        backgroundColor = sectionDoneBg
                    )
                    StatCard(
                        label = stringResource(R.string.stat_erledigungsquote_heute),
                        value = s.quoteHeute,
                        labelColor = sectionDoneText,
                        valueColor = statusDone,
                        backgroundColor = sectionDoneBg
                    )
                    StatCard(
                        label = stringResource(R.string.stat_gesamt_kunden),
                        value = s.totalCustomers.toString(),
                        labelColor = textPrimary,
                        valueColor = primaryBlue,
                        backgroundColor = surfaceWhite
                    )
                    StatCard(
                        label = stringResource(R.string.stat_regelmaessig_kunden),
                        value = s.regelmaessigCount.toString(),
                        labelColor = textPrimary,
                        valueColor = primaryBlue,
                        backgroundColor = surfaceWhite
                    )
                    StatCard(
                        label = stringResource(R.string.stat_unregelmaessig_kunden),
                        value = s.unregelmaessigCount.toString(),
                        labelColor = textPrimary,
                        valueColor = primaryBlue,
                        backgroundColor = surfaceWhite
                    )
                    StatCard(
                        label = stringResource(R.string.stat_auf_abruf_kunden),
                        value = s.aufAbrufCount.toString(),
                        labelColor = textPrimary,
                        valueColor = primaryBlue,
                        backgroundColor = surfaceWhite
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.stat_loading),
                        color = primaryBlue,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color,
    backgroundColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = label,
                color = labelColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
