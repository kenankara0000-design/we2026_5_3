package com.example.we2026_5.ui.wasch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R
import com.example.we2026_5.wasch.Article
import com.example.we2026_5.wasch.TourPreis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourPreislisteScreen(
    state: TourPreislisteUiState,
    articles: List<Article>,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onCloseAddDialog: () -> Unit,
    onSelectArticle: (Article?) -> Unit,
    onPriceNetChange: (String) -> Unit,
    onPriceGrossChange: (String) -> Unit,
    onSaveTourPreis: () -> Unit,
    onRemoveTourPreis: (String) -> Unit
) {
    val primaryBlue = colorResource(R.color.primary_blue)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)
    val backgroundLight = colorResource(R.color.background_light)
    val articlesMap = articles.associateBy { it.id }
    val articleIdsWithPreis = state.tourPreise.map { it.articleId }.toSet()
    val articlesWithoutPreis = articles.filter { it.id !in articleIdsWithPreis }

    Scaffold(
        topBar = {
            WaschenErfassungTopBar(
                primaryBlue = primaryBlue,
                onBack = onBack,
                titleResId = R.string.erfassung_menu_tourpreise
            )
        },
        containerColor = backgroundLight,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = primaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.tour_preis_add))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                stringResource(R.string.tour_preis_hinweis),
                fontSize = 14.sp,
                color = textSecondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            if (state.tourPreise.isEmpty()) {
                Text(
                    stringResource(R.string.tour_preis_leer),
                    fontSize = 14.sp,
                    color = textSecondary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.tourPreise) { preis ->
                        val name = articlesMap[preis.articleId]?.name ?: preis.articleId
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, fontSize = 16.sp, color = textPrimary)
                                    Text(
                                        "Netto: %.2f €  ·  Brutto: %.2f €".format(preis.priceNet, preis.priceGross),
                                        fontSize = 14.sp,
                                        color = textSecondary
                                    )
                                }
                                IconButton(onClick = { onRemoveTourPreis(preis.articleId) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = textSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.addDialogOpen) {
        AlertDialog(
            onDismissRequest = onCloseAddDialog,
            title = { Text(stringResource(R.string.tour_preis_add)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        stringResource(R.string.tour_preis_article),
                        fontSize = 12.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    state.selectedArticleForAdd?.let { a ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = primaryBlue.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(a.name, color = textPrimary)
                                TextButton(onClick = { onSelectArticle(null) }) {
                                    Text("Ändern")
                                }
                            }
                        }
                    } ?: run {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            articlesWithoutPreis.forEach { article ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = { onSelectArticle(article) }),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Text(
                                        article.name,
                                        modifier = Modifier.padding(12.dp),
                                        color = textPrimary
                                    )
                                }
                            }
                            if (articlesWithoutPreis.isEmpty()) {
                                Text(
                                    stringResource(R.string.tour_preis_keine_artikel),
                                    fontSize = 12.sp,
                                    color = textSecondary,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = state.addPriceNet,
                        onValueChange = onPriceNetChange,
                        label = { Text("Netto (€)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.addPriceGross,
                        onValueChange = onPriceGrossChange,
                        label = { Text("Brutto (€)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    state.message?.let { msg ->
                        Spacer(Modifier.height(8.dp))
                        Text(msg, fontSize = 12.sp, color = Color.Red)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onSaveTourPreis,
                    enabled = state.selectedArticleForAdd != null && !state.isSaving
                ) {
                    Text(if (state.isSaving) "…" else stringResource(R.string.wasch_speichern))
                }
            },
            dismissButton = {
                TextButton(onClick = onCloseAddDialog) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}
