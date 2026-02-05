package com.example.we2026_5.ui.kundenlisten

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.KundenListe
import com.example.we2026_5.R
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.ui.kundenlisten.KundenListenListenItem
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KundenListenScreen(
    state: KundenListenState,
    onBack: () -> Unit,
    onNewListe: () -> Unit,
    onRefresh: () -> Unit,
    onListeClick: (KundenListe) -> Unit,
    onListeLoeschen: (KundenListe) -> Unit
) {
    val context = LocalContext.current
    val primaryBlue = Color(ContextCompat.getColor(context, R.color.primary_blue))
    val buttonBlue = Color(ContextCompat.getColor(context, R.color.button_blue))
    val surfaceWhite = Color(ContextCompat.getColor(context, R.color.surface_white))
    val textPrimary = Color(ContextCompat.getColor(context, R.color.text_primary))
    val textSecondary = Color(ContextCompat.getColor(context, R.color.text_secondary))
    val statusOverdue = Color(ContextCompat.getColor(context, R.color.status_overdue))
    val fabColor = Color(ContextCompat.getColor(context, R.color.status_warning))
    var searchQuery by remember { mutableStateOf("") }
    var sortByCount by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.list_screen_title),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryBlue)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewListe,
                containerColor = fabColor,
                contentColor = Color.White,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.content_desc_new_list))
            }
        },
        content = { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(primaryBlue)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Button(
                    onClick = onNewListe,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = buttonBlue),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.list_btn_new),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                androidx.compose.material3.Button(
                    onClick = onRefresh,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = buttonBlue),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.label_refresh),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text(stringResource(R.string.label_list_search)) },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !sortByCount,
                    onClick = { sortByCount = false },
                    label = { Text(stringResource(R.string.label_list_sort_name)) }
                )
                FilterChip(
                    selected = sortByCount,
                    onClick = { sortByCount = true },
                    label = { Text(stringResource(R.string.label_list_sort_count)) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (state) {
                    is KundenListenState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.stat_loading),
                                color = textSecondary,
                                fontSize = 16.sp
                            )
                        }
                    }
                    is KundenListenState.Empty -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = "📋", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.list_empty_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.list_empty_subtitle),
                                fontSize = 14.sp,
                                color = textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    is KundenListenState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = "⚠️", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.tour_error_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusOverdue
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (state.messageArg != null)
                                    stringResource(state.messageResId, state.messageArg)
                                else
                                    stringResource(state.messageResId),
                                fontSize = 14.sp,
                                color = textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.Button(
                                onClick = onRefresh,
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = primaryBlue)
                            ) {
                                Text(stringResource(R.string.tour_retry), color = Color.White)
                            }
                        }
                    }
                    is KundenListenState.Success -> {
                        val filteredListen = state.listen.filter { it.name.contains(searchQuery, ignoreCase = true) }
                        val sortedListen = if (sortByCount) {
                            filteredListen.sortedByDescending { liste -> state.kundenProListe[liste.id] ?: 0 }
                        } else {
                            filteredListen
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(sortedListen, key = { it.id }) { liste ->
                                val kundenCount = state.kundenProListe[liste.id] ?: 0
                                KundenListenListenItem(
                                    liste = liste,
                                    kundenCount = kundenCount,
                                    surfaceWhite = surfaceWhite,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    statusOverdue = statusOverdue,
                                    onClick = { onListeClick(liste) },
                                    onDelete = { onListeLoeschen(liste) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    )
}

