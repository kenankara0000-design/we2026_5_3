package com.example.we2026_5.ui.customermanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
fun CustomerManagerTopBar(
    isAdmin: Boolean,
    isOffline: Boolean,
    isBulkMode: Boolean,
    pressedHeaderButton: String?,
    onBack: () -> Unit,
    onBulkSelectClick: () -> Unit,
    onExportClick: () -> Unit,
    onNewCustomerClick: () -> Unit,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val primaryBlue = colorResource(R.color.primary_blue)
    val buttonBlue = colorResource(R.color.button_blue)
    val statusWarning = colorResource(R.color.status_warning)

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
            navigationIcon = { },
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
        if (isAdmin) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(primaryBlue)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val auswaehlenOrange = isBulkMode || pressedHeaderButton == "Auswählen"
                Button(
                    onClick = onBulkSelectClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (auswaehlenOrange) statusWarning else buttonBlue),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(painter = painterResource(R.drawable.ic_checklist), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.cm_btn_select), fontSize = 14.sp)
                }
                val exportOrange = pressedHeaderButton == "Exportieren"
                Button(
                    onClick = onExportClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (exportOrange) statusWarning else buttonBlue),
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
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_desc_new_customer))
                }
            }
        }
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = primaryBlue,
            contentColor = Color.White
        ) {
            listOf("Gewerblich", "Privat", stringResource(R.string.label_type_tour)).forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    text = { Text(title) }
                )
            }
        }
    }
}
