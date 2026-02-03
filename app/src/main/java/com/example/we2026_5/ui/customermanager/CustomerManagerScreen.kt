package com.example.we2026_5.ui.customermanager

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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.example.we2026_5.util.ShowErrorSnackbar
import com.example.we2026_5.util.rememberSnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.we2026_5.Customer
import com.example.we2026_5.R

private const val SEARCH_DEBOUNCE_MS = 300L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerManagerScreen(
    customers: List<Customer>,
    selectedTab: Int,
    kundenTypFilter: Int,
    searchQuery: String,
    isBulkMode: Boolean,
    selectedIds: Set<String>,
    isOffline: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onTabSelected: (Int) -> Unit,
    onKundenTypFilterChange: (Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    pressedHeaderButton: String?, // "Auswählen", "Exportieren", "NeuerKunde" für orangefarbenes Feedback
    onBulkSelectClick: () -> Unit,
    onExportClick: () -> Unit,
    onNewCustomerClick: () -> Unit,
    onCustomerClick: (Customer) -> Unit,
    onToggleSelection: (String) -> Unit,
    onBulkDone: (List<Customer>) -> Unit,
    onBulkCancel: () -> Unit,
    onRetry: () -> Unit
) {
    var localSearch by remember { mutableStateOf(searchQuery) }
    val snackbarHostState = rememberSnackbarHostState()
    
    LaunchedEffect(localSearch) {
        kotlinx.coroutines.delay(SEARCH_DEBOUNCE_MS.toLong())
        onSearchQueryChange(localSearch)
    }
    
    // Zeige Fehler als Snackbar statt Toast
    ShowErrorSnackbar(
        errorMessage = errorMessage,
        snackbarHostState = snackbarHostState
    )

    val primaryBlue = colorResource(R.color.primary_blue)
    val primaryBlueDark = colorResource(R.color.primary_blue_dark)
    val buttonBlue = colorResource(R.color.button_blue)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)
    val surfaceWhite = colorResource(R.color.surface_white)
    val statusWarning = colorResource(R.color.status_warning)
    val backgroundLight = colorResource(R.color.background_light)

    Scaffold(
        containerColor = backgroundLight,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.menu_kunden),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.content_desc_back),
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        if (isOffline) {
                            Row(
                                modifier = Modifier
                                    .background(Color(0xFFFFEB3B).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_offline),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFFFFEB3B)
                                )
                                Text(
                                    stringResource(R.string.main_offline),
                                    color = Color(0xFFFFEB3B),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryBlue)
                )
                // Button-Zeile
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(primaryBlue)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val auswaehlenOrange = isBulkMode || pressedHeaderButton == "Auswählen"
                    androidx.compose.material3.Button(
                        onClick = onBulkSelectClick,
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = if (auswaehlenOrange) statusWarning else buttonBlue),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_checklist), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.cm_btn_select), fontSize = 14.sp)
                    }
                    val exportOrange = pressedHeaderButton == "Exportieren"
                    androidx.compose.material3.Button(
                        onClick = onExportClick,
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = if (exportOrange) statusWarning else buttonBlue),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.content_desc_export), fontSize = 14.sp)
                    }
                    val neuKundeOrange = pressedHeaderButton == "NeuerKunde"
                    FloatingActionButton(
                        onClick = onNewCustomerClick,
                        modifier = Modifier.size(48.dp),
                        containerColor = if (neuKundeOrange) statusWarning else buttonBlue,
                        contentColor = Color.White
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.content_desc_new_customer)
                        )
                    }
                }
                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = primaryBlue,
                    contentColor = Color.White
                ) {
                    listOf("Gewerblich", "Privat", "Liste").forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { onTabSelected(index) },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundLight)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = localSearch,
                    onValueChange = { localSearch = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.hint_search_customer)) },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.label_filter_kunden_typ),
                    color = textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = kundenTypFilter == 0,
                        onClick = { onKundenTypFilterChange(0) },
                        label = { Text(stringResource(R.string.label_filter_all)) }
                    )
                    FilterChip(
                        selected = kundenTypFilter == 1,
                        onClick = { onKundenTypFilterChange(1) },
                        label = { Text(stringResource(R.string.label_kunden_typ_regelmaessig)) }
                    )
                    FilterChip(
                        selected = kundenTypFilter == 2,
                        onClick = { onKundenTypFilterChange(2) },
                        label = { Text(stringResource(R.string.label_kunden_typ_unregelmaessig)) }
                    )
                }
                Spacer(Modifier.height(8.dp))

                when {
                    isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.stat_loading), color = textSecondary)
                        }
                    }
                    // Fehler werden jetzt als Snackbar angezeigt (siehe ShowErrorSnackbar oben)
                    customers.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("👥", fontSize = 64.sp)
                                Spacer(Modifier.height(16.dp))
                                Text(stringResource(R.string.cm_empty_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                                Spacer(Modifier.height(8.dp))
                                Text(stringResource(R.string.cm_empty_subtitle), fontSize = 14.sp, color = textSecondary)
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(customers, key = { it.id }) { customer ->
                                CustomerRow(
                                    customer = customer,
                                    isBulkMode = isBulkMode,
                                    isSelected = customer.id in selectedIds,
                                    onClick = {
                                        if (isBulkMode) onToggleSelection(customer.id)
                                        else onCustomerClick(customer)
                                    },
                                    onToggleSelection = { onToggleSelection(customer.id) },
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Bulk-Action-Bar unten
            if (isBulkMode && selectedIds.isNotEmpty()) {
                val selectedCustomers = customers.filter { it.id in selectedIds }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(primaryBlue)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.cm_selected_count, selectedIds.size),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.Button(
                            onClick = { onBulkDone(selectedCustomers) },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = colorResource(R.color.status_done))
                        ) {
                            Text(stringResource(R.string.cm_mark_done))
                        }
                        androidx.compose.material3.OutlinedButton(onClick = onBulkCancel) {
                            Text(stringResource(R.string.btn_cancel))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerRow(
    customer: Customer,
    isBulkMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleSelection: () -> Unit,
    textPrimary: Color,
    textSecondary: Color
) {
    val surfaceWhite = colorResource(R.color.surface_white)
    val gplColor = when (customer.kundenArt) {
        "Privat" -> colorResource(R.color.button_privat_glossy)
        "Liste" -> colorResource(R.color.button_liste_glossy)
        else -> colorResource(R.color.button_gewerblich_glossy)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceWhite),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isBulkMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() }
                )
                Spacer(Modifier.size(8.dp))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (customer.fotoUrls.isNotEmpty()) {
                        Card(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            AsyncImage(
                                model = customer.fotoUrls.first(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                        Spacer(Modifier.size(8.dp))
                    }
                    Text(
                        text = when (customer.kundenArt) {
                            "Privat" -> "P"
                            "Liste" -> "L"
                            else -> "G"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(gplColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        customer.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
                Text(
                    customer.adresse,
                    fontSize = 14.sp,
                    color = textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
