package com.example.we2026_5

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.example.we2026_5.ui.mapview.MapViewScreen
import com.example.we2026_5.ui.mapview.MapViewState
import com.example.we2026_5.ui.mapview.MapViewViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MapViewActivity : AppCompatActivity() {

    companion object {
        /** Optionale Wegpunkte in Reihenfolge (z. B. vom Tourenplaner). Wenn gesetzt, wird keine eigene Ladung ausgeführt. */
        const val EXTRA_ADDRESSES = "com.example.we2026_5.EXTRA_ADDRESSES"
    }

    private val viewModel: MapViewViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intentAddresses = intent.getStringArrayListExtra(EXTRA_ADDRESSES)
        if (!intentAddresses.isNullOrEmpty()) {
            openMapsWithAddresses(intentAddresses, filteredToToday = false)
            finish()
            return
        }
        setContent {
            MaterialTheme {
                val state by viewModel.state.observeAsState(initial = MapViewState.Loading)
                MapViewScreen(
                    state = state,
                    onBack = { finish() }
                )
                LaunchedEffect(Unit) {
                    viewModel.loadCustomersForMap()
                }
                LaunchedEffect(state) {
                    when (val s = state) {
                        is MapViewState.Success -> {
                            openMapsWithAddresses(s.addresses, s.filteredToToday)
                            if (s.filteredToToday) {
                                Toast.makeText(
                                    this@MapViewActivity,
                                    getString(R.string.map_filtered_today_toast),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            finish()
                        }
                        else -> { }
                    }
                }
            }
        }
    }

    private fun openMapsWithAddresses(addresses: List<String>, filteredToToday: Boolean) {
        if (addresses.isEmpty()) return
        val waypoints = addresses.joinToString("|") { Uri.encode(it) }
        val url = "https://www.google.com/maps/dir/?api=1&waypoints=$waypoints"
        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
}
