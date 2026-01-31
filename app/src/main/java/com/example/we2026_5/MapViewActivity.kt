package com.example.we2026_5

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.databinding.ActivityMapViewBinding
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.TerminTyp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

/** Maximale Wegpunkte für Google-Maps-URL; darüber Filter „nur heute fällig“. */
private const val MAX_WAYPOINTS = 25

class MapViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapViewBinding
    private val repository: CustomerRepository by inject()
    private val listeRepository: KundenListeRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        loadCustomersForMap()
    }

    private fun loadCustomersForMap() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        lifecycleScope.launch {
            try {
                val allCustomers = withContext(Dispatchers.IO) { repository.getAllCustomers() }
                var customersWithAddress = allCustomers.filter { it.adresse.isNotBlank() }
                var filteredToToday = false
                if (customersWithAddress.size > MAX_WAYPOINTS) {
                    val heuteStart = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
                    customersWithAddress = withContext(Dispatchers.IO) {
                        customersWithAddress.filter { customer ->
                            val liste = if (customer.listeId.isNotBlank())
                                listeRepository.getListeById(customer.listeId) else null
                            TerminBerechnungUtils.hatTerminAmDatum(customer, liste, heuteStart, TerminTyp.ABHOLUNG) ||
                                TerminBerechnungUtils.hatTerminAmDatum(customer, liste, heuteStart, TerminTyp.AUSLIEFERUNG)
                        }
                    }
                    filteredToToday = true
                }
                if (customersWithAddress.isEmpty()) {
                    binding.tvEmpty.visibility = android.view.View.VISIBLE
                    binding.progressBar.visibility = android.view.View.GONE
                    return@launch
                }
                val addresses = customersWithAddress.joinToString("|") { Uri.encode(it.adresse) }
                val url = "https://www.google.com/maps/dir/?api=1&waypoints=$addresses"
                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    setPackage("com.google.android.apps.maps")
                }
                if (mapIntent.resolveActivity(packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                if (filteredToToday) {
                    Toast.makeText(this@MapViewActivity, getString(R.string.map_filtered_today_toast), Toast.LENGTH_LONG).show()
                }
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@MapViewActivity, getString(R.string.error_message_generic, e.message ?: ""), Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }
}
