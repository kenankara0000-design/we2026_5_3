package com.example.we2026_5.ui.customermanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import com.example.we2026_5.util.ShowErrorSnackbar
import com.example.we2026_5.util.rememberSnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.example.we2026_5.ui.customermanager.CustomerManagerCard

private const val SEARCH_DEBOUNCE_MS = 300L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerManagerScreen(
    customers: List<Customer>,
    selectedTab: Int,
    kundenTypFilter: Int,
    ohneTourFilter: Int,
    pausierteFilter: Int,
    searchQuery: String,
    isBulkMode: Boolean,
    selectedIds: Set<String>,
    isOffline: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onTabSelected: (Int) -> Unit,
    onKundenTypFilterChange: (Int) -> Unit,
    onOhneTourFilterChange: (Int) -> Unit,
    onPausierteFilterChange: (Int) -> Unit,
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
    var filterExpanded by remember { mutableStateOf(false) }
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
            CustomerManagerTopBar(
                isOffline = isOffline,
                isBulkMode = isBulkMode,
                pressedHeaderButton = pressedHeaderButton,
                onBack = onBack,
                onBulkSelectClick = onBulkSelectClick,
                onExportClick = onExportClick,
                onNewCustomerClick = onNewCustomerClick,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundLight)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(
                        bottom = if (isBulkMode && selectedIds.isNotEmpty()) 56.dp else 0.dp
                    )
            ) {
                CustomerManagerSearchAndFilter(
                    searchValue = localSearch,
                    onSearchChange = { localSearch = it },
                    filterExpanded = filterExpanded,
                    onFilterToggle = { filterExpanded = !filterExpanded },
                    kundenTypFilter = kundenTypFilter,
                    onKundenTypFilterChange = onKundenTypFilterChange,
                    ohneTourFilter = ohneTourFilter,
                    onOhneTourFilterChange = onOhneTourFilterChange,
                    pausierteFilter = pausierteFilter,
                    onPausierteFilterChange = onPausierteFilterChange,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    primaryBlue = primaryBlue
                )
                when {
                    isLoading -> CustomerManagerLoadingView(textSecondary = textSecondary)
                    customers.isEmpty() -> CustomerManagerEmptyView(textSecondary = textSecondary)
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(customers, key = { it.id }) { customer ->
                                CustomerManagerCard(
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

            if (isBulkMode && selectedIds.isNotEmpty()) {
                Box(Modifier.align(Alignment.BottomCenter)) {
                    CustomerManagerBulkBar(
                        selectedCount = selectedIds.size,
                        selectedCustomers = customers.filter { it.id in selectedIds },
                        primaryBlue = primaryBlue,
                        onBulkDone = onBulkDone,
                        onBulkCancel = onBulkCancel
                    )
                }
            }
        }
    }
}

