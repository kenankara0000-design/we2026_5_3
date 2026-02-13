package com.example.we2026_5.ui.tourplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.example.we2026_5.ui.theme.AppColors
import com.example.we2026_5.ui.CustomerTypeButtonHelper
import com.example.we2026_5.ui.common.AlWochentagText

private val CustomerCardPadding = 16.dp
private val CustomerNameSp = 19.sp
private val CustomerBadgeSp = 16.sp
private val CustomerBadgePaddingH = 8.dp
private val CustomerBadgePaddingV = 4.dp
private val CustomerBadgeFixedWidth = 100.dp
private val CustomerButtonTextSp = 17.sp
private val CustomerButtonMinHeight = 44.dp

private val ErledigtBadgeGreen = AppColors.StatusDone

@Composable
internal fun TourCustomerRow(
    customer: Customer,
    isOverdue: Boolean,
    isInUrlaub: Boolean,
    isVerschobenAmFaelligkeitstag: Boolean = false,
    verschobenInfo: String? = null,
    verschobenVonInfo: String? = null,
    statusBadgeText: String,
    /** Bei isOverdue: „A“, „L“ oder „AL“ für Badge neben Überfällig (z. B. „Ü A“). */
    overdueAlSuffix: String? = null,
    viewDateMillis: Long = 0L,
    showErledigtBadge: Boolean = false,
    onCustomerClick: () -> Unit,
    onAktionenClick: () -> Unit,
    dragHandleModifier: Modifier? = null,
    dragHandleContent: @Composable (() -> Unit)? = null,
    /** Wenn gesetzt: wird auf die Card angewendet (z. B. longPressDraggableHandle). */
    cardDragModifier: Modifier? = null,
    /** Muss mit cardDragModifier zusammen verwendet werden (gleiche Instance wie beim Handle). */
    cardInteractionSource: MutableInteractionSource? = null,
    /** true wenn die Karte gerade gezogen wird (Long-Press Drag) – visueller Hinweis. */
    isDragging: Boolean = false,
    /** Phase 4: Kartenanzeige-Optionen aus AppPreferences. */
    showAddress: Boolean = true,
    showPhone: Boolean = false,
    showNotes: Boolean = false
) {
    val isDeaktiviert = isVerschobenAmFaelligkeitstag
    val cardBg = when {
        isDragging -> colorResource(R.color.tour_planner_dragging_bg)
        isOverdue -> colorResource(R.color.section_overdue_bg)
        isInUrlaub -> colorResource(R.color.customer_urlaub_bg)
        isVerschobenAmFaelligkeitstag -> colorResource(R.color.surface_light)
        else -> colorResource(R.color.termin_regel_card_bg)
    }
    val nameColor = if (isOverdue) colorResource(R.color.section_overdue_text) else colorResource(R.color.text_primary)
    val gplColor = when {
        customer.kundenArt == "Gewerblich" -> colorResource(R.color.button_gewerblich_glossy)
        customer.listeId.isNotEmpty() -> colorResource(R.color.button_liste_glossy)
        else -> colorResource(R.color.button_privat_glossy)
    }
    val badgeText = when {
        isOverdue -> if (!overdueAlSuffix.isNullOrEmpty()) "Ü $overdueAlSuffix" else stringResource(R.string.status_badge_overdue)
        isInUrlaub -> stringResource(R.string.label_urlaub)
        isVerschobenAmFaelligkeitstag -> stringResource(R.string.badge_verschoben)
        else -> statusBadgeText
    }
    val showBadge = isOverdue || isInUrlaub || isVerschobenAmFaelligkeitstag || statusBadgeText.isNotEmpty()
    val isAusnahmeBadge = statusBadgeText == "A-A" || statusBadgeText == "A-L"
    val badgeColor = when {
        isOverdue -> colorResource(R.color.status_overdue)
        isInUrlaub -> colorResource(R.color.button_urlaub)
        isVerschobenAmFaelligkeitstag -> colorResource(R.color.status_info)
        isAusnahmeBadge -> colorResource(R.color.status_ausnahme)
        statusBadgeText == "L" -> colorResource(R.color.termin_regel_auslieferung)
        else -> colorResource(R.color.termin_regel_abholung)
    }
    val clickModifier = if (isDeaktiviert) Modifier else Modifier.clickable(
        onClick = onCustomerClick,
        interactionSource = cardInteractionSource ?: remember { MutableInteractionSource() },
        indication = null
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickModifier)
            .then(cardDragModifier ?: Modifier),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CustomerCardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = CustomerTypeButtonHelper.getKundenArtLabel(customer),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(gplColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = customer.displayName,
                        fontSize = CustomerNameSp,
                        color = nameColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                // Phase 4: Adresse, Telefon, Notizen (konfigurierbar)
                val secondaryColor = colorResource(R.color.text_secondary)
                if (showAddress) {
                    val fullAddr = buildString {
                        if (customer.adresse.isNotBlank()) append(customer.adresse.trim())
                        val plzStadt = listOf(customer.plz.trim(), customer.stadt.trim()).filter { it.isNotEmpty() }.joinToString(" ")
                        if (plzStadt.isNotEmpty()) { if (isNotEmpty()) append(", "); append(plzStadt) }
                    }.trim()
                    if (fullAddr.isNotEmpty()) {
                        Text(text = fullAddr, fontSize = 13.sp, color = secondaryColor, maxLines = 1)
                    }
                }
                if (showPhone && customer.telefon.isNotBlank()) {
                    Text(text = customer.telefon.trim(), fontSize = 13.sp, color = secondaryColor, maxLines = 1)
                }
                if (showNotes && customer.notizen.isNotBlank()) {
                    Text(text = customer.notizen.trim(), fontSize = 12.sp, color = secondaryColor, maxLines = 1)
                }
                AlWochentagText(customer = customer, color = colorResource(R.color.text_secondary))
                val infoToShow = verschobenInfo ?: verschobenVonInfo
                if (infoToShow != null && infoToShow.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = infoToShow,
                        fontSize = 13.sp,
                        color = colorResource(R.color.text_secondary)
                    )
                }
                if (showBadge) {
                    Spacer(Modifier.height(8.dp))
                    val isAlBadge = statusBadgeText == "AL" && !isOverdue && !isInUrlaub && !isVerschobenAmFaelligkeitstag
                    val showACheck = showErledigtBadge && (statusBadgeText == "A" || statusBadgeText == "AL") && !isOverdue && !isInUrlaub && !isVerschobenAmFaelligkeitstag
                    if (isAlBadge) {
                        Row(
                            modifier = Modifier.width(CustomerBadgeFixedWidth),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(colorResource(R.color.termin_regel_abholung), RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                                    .padding(horizontal = 4.dp, vertical = CustomerBadgePaddingV),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.badge_abholung_short), fontSize = CustomerBadgeSp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    if (showACheck) {
                                        Spacer(Modifier.size(2.dp))
                                        Text("✓", fontSize = CustomerBadgeSp, color = ErledigtBadgeGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(colorResource(R.color.termin_regel_auslieferung), RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                                    .padding(horizontal = 4.dp, vertical = CustomerBadgePaddingV),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(stringResource(R.string.badge_auslieferung_short), fontSize = CustomerBadgeSp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        val baseBadge = Modifier
                            .width(CustomerBadgeFixedWidth)
                            .background(color = badgeColor, shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = CustomerBadgePaddingH, vertical = CustomerBadgePaddingV)
                        Box(modifier = baseBadge, contentAlignment = Alignment.Center) {
                            if (showACheck && statusBadgeText == "A") {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.badge_abholung_short), fontSize = CustomerBadgeSp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.size(2.dp))
                                    Text("✓", fontSize = CustomerBadgeSp, color = ErledigtBadgeGreen, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(
                                    text = badgeText,
                                    fontSize = CustomerBadgeSp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
            if (dragHandleModifier != null && dragHandleContent != null) {
                Box(modifier = dragHandleModifier) { dragHandleContent() }
                Spacer(Modifier.size(4.dp))
            }
            Spacer(Modifier.size(12.dp))
            Button(
                onClick = if (isDeaktiviert) {{}} else onAktionenClick,
                modifier = Modifier.heightIn(min = CustomerButtonMinHeight),
                enabled = !isDeaktiviert,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.primary_blue_dark),
                    contentColor = Color.White,
                    disabledContainerColor = colorResource(R.color.button_inactive),
                    disabledContentColor = Color.White
                )
            ) {
                Text(
                    stringResource(R.string.sheet_aktionen),
                    fontSize = CustomerButtonTextSp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
