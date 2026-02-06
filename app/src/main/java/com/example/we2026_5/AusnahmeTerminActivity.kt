package com.example.we2026_5

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.util.TerminBerechnungUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import androidx.lifecycle.lifecycleScope

@OptIn(ExperimentalMaterial3Api::class)
class AusnahmeTerminActivity : AppCompatActivity() {

    private val repository: com.example.we2026_5.data.repository.CustomerRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val customerId = intent.getStringExtra("CUSTOMER_ID") ?: run {
            Toast.makeText(this, getString(R.string.error_customer_id_missing), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        setContent {
            MaterialTheme {
                val context = LocalContext.current
                val primaryBlue = Color(ContextCompat.getColor(context, R.color.primary_blue))
                val startOfToday = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
                val twoWeeks = (0 until 14).map { day ->
                    startOfToday + java.util.concurrent.TimeUnit.DAYS.toMillis(day.toLong())
                }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    stringResource(R.string.termin_anlegen_option_ausnahme),
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryBlue)
                        )
                    }
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp)
                    ) {
                        Text(
                            stringResource(R.string.termin_anlegen_ausnahme_datum_waehlen),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(twoWeeks) { datum ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            AlertDialog.Builder(this@AusnahmeTerminActivity)
                                                .setTitle(getString(R.string.termin_anlegen_ausnahme_typ_waehlen))
                                                .setPositiveButton(getString(R.string.termin_anlegen_ausnahme_abholung)) { _, _ ->
                                                    saveAusnahme(customerId, datum, "A")
                                                }
                                                .setNegativeButton(getString(R.string.termin_anlegen_ausnahme_auslieferung)) { _, _ ->
                                                    saveAusnahme(customerId, datum, "L")
                                                }
                                                .setNeutralButton(getString(R.string.btn_cancel), null)
                                                .show()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        DateFormatter.formatDateWithWeekday(datum),
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun saveAusnahme(customerId: String, datum: Long, typ: String) {
        val dayStart = TerminBerechnungUtils.getStartOfDay(datum)
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                repository.addAusnahmeTermin(customerId, AusnahmeTermin(datum = dayStart, typ = typ))
            }
            if (ok) {
                Toast.makeText(this@AusnahmeTerminActivity, getString(R.string.toast_gespeichert), Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@AusnahmeTerminActivity, getString(R.string.error_save_generic), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
