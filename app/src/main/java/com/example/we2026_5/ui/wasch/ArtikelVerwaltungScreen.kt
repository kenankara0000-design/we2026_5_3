package com.example.we2026_5.ui.wasch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R
import com.example.we2026_5.ui.common.AppEmptyView
import com.example.we2026_5.ui.common.AppErrorView
import com.example.we2026_5.ui.common.AppLoadingView
import com.example.we2026_5.ui.common.AppTopBar
import com.example.we2026_5.wasch.Article

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtikelVerwaltungScreen(
    articles: List<Article>,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onBack: () -> Unit,
    onDeleteArticle: (Article) -> Unit
) {
    val textSecondary = colorResource(R.color.text_secondary)

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.wasch_artikel_verwalten)
            )
        },
        containerColor = colorResource(R.color.background_light)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isLoading) {
                AppLoadingView(text = stringResource(R.string.stat_loading))
            } else if (errorMessage != null) {
                AppErrorView(message = errorMessage)
            } else if (articles.isEmpty()) {
                AppEmptyView(
                    title = stringResource(R.string.artikel_empty),
                    emoji = "📦"
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(articles) { article ->
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
                                    Text(
                                        article.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorResource(R.color.text_primary)
                                    )
                                    if (article.einheit.isNotBlank()) {
                                        Text(
                                            article.einheit,
                                            fontSize = 14.sp,
                                            color = textSecondary
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteArticle(article) },
                                    modifier = Modifier.padding(start = 8.dp),
                                    enabled = false
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.artikel_loeschen),
                                        tint = Color.Red,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
