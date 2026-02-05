package com.example.we2026_5.ui.tourplanner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.SectionType

@Composable
internal fun SectionHeaderRow(
    title: String,
    countText: String,
    isExpanded: Boolean,
    sectionType: SectionType,
    sectionOverdueBg: Color,
    sectionOverdueText: Color,
    sectionDoneBg: Color,
    sectionDoneText: Color,
    onToggle: () -> Unit
) {
    val (bg, textColor) = when (sectionType) {
        SectionType.OVERDUE -> sectionOverdueBg to sectionOverdueText
        SectionType.DONE -> sectionDoneBg to sectionDoneText
        SectionType.LISTE -> sectionDoneBg to sectionDoneText
    }
    val titleFontSize = if (sectionType == SectionType.DONE) 18.sp else 14.sp
    val rowPadding = if (sectionType == SectionType.DONE) 14.dp else 12.dp
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(rowPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = titleFontSize,
                modifier = Modifier.weight(1f)
            )
            Text(text = countText, color = textColor, fontSize = 14.sp)
            Spacer(Modifier.size(8.dp))
            Text(text = if (isExpanded) "-" else "+", color = textColor, fontWeight = FontWeight.Bold)
        }
    }
}
