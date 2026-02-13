package com.example.we2026_5.ui.wasch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight

@Composable
fun WaschenErfassungKundeSuchenContent(
    customerSearchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filteredCustomers: List<Customer>,
    textSecondary: androidx.compose.ui.graphics.Color,
    onKundeWaehlen: (Customer) -> Unit,
    /** Wenn true und Liste leer: Hinweis „Name eingeben“ statt „Keine Kunden“. */
    searchHintWhenEmpty: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = customerSearchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.wasch_kunde_suchen), color = textSecondary) },
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        if (filteredCustomers.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(if (searchHintWhenEmpty) R.string.wasch_kunde_suchen_hinweis else R.string.wasch_keine_kunden),
                    color = textSecondary
                )
            }
        } else {
            LazyColumn(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                itemsIndexed(filteredCustomers) { _, customer ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onKundeWaehlen(customer) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                customer.displayName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            // Phase 4: Alias (wenn abweichend) und Adresse anzeigen
                            val aliasHint = if (customer.alias.isNotBlank() && customer.alias.trim() != customer.name.trim())
                                customer.name.trim() else null
                            if (aliasHint != null) {
                                Text(
                                    text = aliasHint,
                                    fontSize = 13.sp,
                                    color = textSecondary
                                )
                            }
                            val fullAddr = buildString {
                                if (customer.adresse.isNotBlank()) append(customer.adresse.trim())
                                val plzStadt = listOf(customer.plz.trim(), customer.stadt.trim()).filter { it.isNotEmpty() }.joinToString(" ")
                                if (plzStadt.isNotEmpty()) { if (isNotEmpty()) append(", "); append(plzStadt) }
                            }.trim()
                            if (fullAddr.isNotEmpty()) {
                                Text(
                                    text = fullAddr,
                                    fontSize = 13.sp,
                                    color = textSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
