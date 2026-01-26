package com.example.we2026_5

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.databinding.ActivityMapViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MapViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapViewBinding
    private val repository: CustomerRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        
        loadCustomersForMap()
    }

    private fun loadCustomersForMap() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val allCustomers = repository.getAllCustomers()
                val customersWithAddress = allCustomers.filter { it.adresse.isNotBlank() }
                
                if (customersWithAddress.isEmpty()) {
                    binding.tvEmpty.visibility = android.view.View.VISIBLE
                    binding.progressBar.visibility = android.view.View.GONE
                    return@launch
                }
                
                // Erstelle Google Maps URL mit allen Adressen
                val addresses = customersWithAddress.joinToString("|") { 
                    Uri.encode(it.adresse)
                }
                
                // Öffne Google Maps mit allen Adressen
                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&waypoints=$addresses"))
                mapIntent.setPackage("com.google.android.apps.maps")
                
                if (mapIntent.resolveActivity(packageManager) != null) {
                    startActivity(mapIntent)
                    finish()
                } else {
                    // Fallback: Browser
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&waypoints=$addresses"))
                    startActivity(webIntent)
                    finish()
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@MapViewActivity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }
}
