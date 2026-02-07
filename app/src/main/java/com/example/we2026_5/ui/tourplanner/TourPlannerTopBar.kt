package com.example.we2026_5.ui.tourplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourPlannerTopBar(
    dateText: String,
    tourCounts: Pair<Int, Int>,
    isToday: Boolean,
    isOffline: Boolean,
    pressedHeaderButton: String?,
    erledigtCount: Int,
    onBack: () -> Unit,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onMap: () -> Unit,
    onRefresh: () -> Unit,
    onErledigtClick: () -> Unit
) {
    val primaryBlue = colorResource(R.color.primary_blue)
    val buttonBlue = colorResource(R.color.button_blue)
    val statusWarning = colorResource(R.color.status_warning)

    Column(modifier = Modifier.background(primaryBlue)) {
        TopAppBar(
            title = { },
            navigationIcon = { },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryBlue, navigationIconContentColor = Color.White),
            actions = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrevDay) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.content_desc_prev_day),
                            tint = Color.White
                        )
                    }
                    Text(
                        text = dateText,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = onNextDay) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_forward),
                            contentDescription = stringResource(R.string.content_desc_next_day),
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.label_refresh),
                            tint = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.label_tour_counts, tourCounts.first, tourCounts.second),
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
            }
        )
        if (isOffline) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_offline),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFFFFEB3B)
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    stringResource(R.string.main_offline),
                    color = Color(0xFFFFEB3B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val headerButtonHeight = 40.dp
            val headerButtonShape = RoundedCornerShape(8.dp)
            val headerContentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
            Button(
                onClick = onMap,
                modifier = Modifier.weight(1f).height(headerButtonHeight),
                shape = headerButtonShape,
                contentPadding = headerContentPadding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (pressedHeaderButton == "Karte") statusWarning else buttonBlue
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_map),
                    contentDescription = stringResource(R.string.content_desc_map),
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.tour_btn_map), color = Color.White, fontSize = 14.sp)
            }
            Button(
                onClick = onToday,
                modifier = Modifier.weight(1f).height(headerButtonHeight),
                shape = headerButtonShape,
                contentPadding = headerContentPadding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isToday) statusWarning else buttonBlue
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_calendar_today),
                    contentDescription = stringResource(R.string.content_desc_today),
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.tour_btn_today), color = Color.White, fontSize = 14.sp)
            }
            Button(
                onClick = onErledigtClick,
                modifier = Modifier.weight(1f).height(headerButtonHeight),
                shape = headerButtonShape,
                contentPadding = headerContentPadding,
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.section_done_bg))
            ) {
                Text(
                    stringResource(R.string.tour_btn_erledigte, erledigtCount),
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}
