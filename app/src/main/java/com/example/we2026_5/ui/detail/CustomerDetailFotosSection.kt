package com.example.we2026_5.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.we2026_5.ui.common.DetailUiConstants
import com.example.we2026_5.R

@Composable
fun CustomerDetailFotosSection(
    fotoUrls: List<String>,
    isInEditMode: Boolean,
    textPrimary: androidx.compose.ui.graphics.Color,
    onPhotoClick: (String) -> Unit,
    onTakePhoto: () -> Unit
) {
    if (!isInEditMode && fotoUrls.isNotEmpty()) {
        Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
        Text(
            stringResource(R.string.label_fotos),
            fontSize = DetailUiConstants.SectionTitleSp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(fotoUrls, key = { it }) { url ->
                Card(
                    modifier = Modifier
                        .size(100.dp)
                        .clickable(onClick = { onPhotoClick(url) }),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.ic_camera),
                        error = painterResource(R.drawable.ic_camera)
                    )
                }
            }
        }
    }
    if (isInEditMode) {
        Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
        OutlinedButton(onClick = onTakePhoto, modifier = Modifier.fillMaxWidth()) {
            Icon(painter = painterResource(R.drawable.ic_camera), contentDescription = null, Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.label_add_photo))
        }
    }
}
