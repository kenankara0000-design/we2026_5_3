package com.example.we2026_5.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.we2026_5.R
import com.example.we2026_5.ui.common.DetailUiConstants

@Composable
fun CustomerDetailFotosSection(
    fotoUrls: List<String>,
    fotoThumbUrls: List<String>,
    isInEditMode: Boolean,
    textPrimary: androidx.compose.ui.graphics.Color,
    onPhotoClick: (String) -> Unit,
    onTakePhoto: () -> Unit,
    onDeletePhoto: ((String) -> Unit)? = null,
    pendingUploadCount: Int = 0
) {
    Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
    Text(
        stringResource(R.string.label_fotos),
        fontSize = DetailUiConstants.SectionTitleSp,
        fontWeight = FontWeight.Bold,
        color = textPrimary
    )
    if (pendingUploadCount > 0) {
        Spacer(Modifier.height(4.dp))
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = com.example.we2026_5.ui.theme.AppColors.StatusWarning
            )
            Text(
                text = if (pendingUploadCount == 1) stringResource(R.string.foto_upload_pending_singular)
                else stringResource(R.string.foto_upload_pending, pendingUploadCount),
                fontSize = 12.sp,
                color = com.example.we2026_5.ui.theme.AppColors.StatusWarning
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    if (fotoUrls.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(fotoUrls.size, key = { fotoUrls[it] }) { index ->
                val fullUrl = fotoUrls[index]
                val displayUrl = fotoThumbUrls.getOrNull(index) ?: fullUrl
                Box(modifier = Modifier.size(100.dp)) {
                    Card(
                        modifier = Modifier
                            .size(100.dp)
                            .clickable(onClick = { onPhotoClick(fullUrl) }),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        AsyncImage(
                            model = displayUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(R.drawable.ic_camera),
                            error = painterResource(R.drawable.ic_camera)
                        )
                    }
                    if (isInEditMode && onDeletePhoto != null) {
                        IconButton(
                            onClick = { onDeletePhoto(fullUrl) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(36.dp)
                                .zIndex(2f)
                                .background(
                                    color = colorResource(R.color.surface_white).copy(alpha = 0.9f),
                                    shape = CircleShape
                                )
                                .clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.label_delete),
                                tint = colorResource(R.color.status_overdue),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    OutlinedButton(onClick = onTakePhoto, modifier = Modifier.fillMaxWidth()) {
        Icon(painter = painterResource(R.drawable.ic_camera), contentDescription = null, Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
        Text(stringResource(R.string.label_add_photo))
    }
}
