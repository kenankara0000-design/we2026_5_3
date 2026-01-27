package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.we2026_5.data.repository.TerminRegelRepository
import com.example.we2026_5.databinding.ActivityTerminRegelManagerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class TerminRegelManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTerminRegelManagerBinding
    private val regelRepository: TerminRegelRepository by inject()
    private lateinit var adapter: TerminRegelAdapter
    private val regeln = mutableListOf<TerminRegel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTerminRegelManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        loadRegeln()
    }

    private fun setupRecyclerView() {
        adapter = TerminRegelAdapter(
            regeln = regeln,
            onRegelClick = { regel ->
                val intent = Intent(this, TerminRegelErstellenActivity::class.java).apply {
                    putExtra("REGEL_ID", regel.id)
                }
                startActivity(intent)
            },
            onRegelDelete = { regel ->
                showDeleteDialog(regel)
            }
        )
        binding.rvRegeln.layoutManager = LinearLayoutManager(this)
        binding.rvRegeln.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnNewRegel.setOnClickListener {
            val intent = Intent(this, TerminRegelErstellenActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadRegeln() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val allRegeln = regelRepository.getAllRegeln()
                regeln.clear()
                regeln.addAll(allRegeln)
                adapter.updateRegeln(regeln)
                
                if (regeln.isEmpty()) {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                    binding.rvRegeln.visibility = View.GONE
                } else {
                    binding.emptyStateLayout.visibility = View.GONE
                    binding.rvRegeln.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Toast.makeText(this@TerminRegelManagerActivity, "Fehler beim Laden: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteDialog(regel: TerminRegel) {
        AlertDialog.Builder(this)
            .setTitle("Regel löschen")
            .setMessage("Möchten Sie die Regel '${regel.name}' wirklich löschen?")
            .setPositiveButton("Löschen") { _, _ ->
                deleteRegel(regel)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun deleteRegel(regel: TerminRegel) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val success = regelRepository.deleteRegel(regel.id)
                if (success) {
                    regeln.remove(regel)
                    adapter.updateRegeln(regeln)
                    Toast.makeText(this@TerminRegelManagerActivity, "Regel gelöscht", Toast.LENGTH_SHORT).show()
                    
                    if (regeln.isEmpty()) {
                        binding.emptyStateLayout.visibility = View.VISIBLE
                        binding.rvRegeln.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(this@TerminRegelManagerActivity, "Fehler beim Löschen", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TerminRegelManagerActivity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
