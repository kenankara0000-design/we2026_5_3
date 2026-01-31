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

    private val viewModel: MapViewViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                            val addresses = s.addresses.joinToString("|") { Uri.encode(it) }
                            val url = "https://www.google.com/maps/dir/?api=1&waypoints=$addresses"
                            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            if (mapIntent.resolveActivity(packageManager) != null) {
                                startActivity(mapIntent)
                            } else {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
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
}
