package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.databinding.ActivityKundenListenBinding
import com.example.we2026_5.databinding.ItemListeBinding
import com.example.we2026_5.FirebaseRetryHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.*

class KundenListenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKundenListenBinding
    private val listeRepository: KundenListeRepository by inject()
    private val customerRepository: CustomerRepository by inject()
    private lateinit var adapter: ListenAdapter
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
    private var kundenProListe = mapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKundenListenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ListenAdapter(
            listen = mutableListOf(),
            onListeClick = { liste ->
                // Liste bearbeiten - öffne ListeBearbeitenActivity
                val intent = Intent(this, ListeBearbeitenActivity::class.java).apply {
                    putExtra("LISTE_ID", liste.id)
                }
                startActivity(intent)
            },
            onListeLoeschen = { liste ->
                loescheListe(liste)
            }
        )

        binding.rvListen.layoutManager = LinearLayoutManager(this)
        binding.rvListen.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }

        binding.btnNeueListe.setOnClickListener {
            val intent = Intent(this, ListeErstellenActivity::class.java)
            startActivity(intent)
        }

        binding.fabNeueListe.setOnClickListener {
            val intent = Intent(this, ListeErstellenActivity::class.java)
            startActivity(intent)
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadListen()
            binding.swipeRefresh.isRefreshing = false
        }

        binding.btnRetry.setOnClickListener {
            loadListen()
        }

        loadListen()
    }

    override fun onResume() {
        super.onResume()
        // Listen neu laden wenn Activity wieder sichtbar wird (z.B. nach Erstellen)
        loadListen()
    }

    private fun loadListen() {
        binding.progressBar.visibility = View.VISIBLE
        binding.errorStateLayout.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val listen = listeRepository.getAllListen()
                val allCustomers = customerRepository.getAllCustomers()
                
                // Anzahl Kunden pro Liste berechnen
                kundenProListe = allCustomers
                    .filter { it.listeId.isNotEmpty() }
                    .groupBy { it.listeId }
                    .mapValues { it.value.size }
                
                adapter.updateListen(listen, kundenProListe)

                if (listen.isEmpty()) {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                    binding.rvListen.visibility = View.GONE
                } else {
                    binding.emptyStateLayout.visibility = View.GONE
                    binding.rvListen.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                binding.errorStateLayout.visibility = View.VISIBLE
                binding.rvListen.visibility = View.GONE
                binding.emptyStateLayout.visibility = View.GONE
                binding.tvErrorMessage.text = e.message ?: "Fehler beim Laden der Listen"
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun loescheListe(liste: KundenListe) {
        // Prüfen ob Kunden in dieser Liste sind
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val allCustomers = customerRepository.getAllCustomers()
                val kundenInListe = allCustomers.filter { it.listeId == liste.id }

                if (kundenInListe.isNotEmpty()) {
                    AlertDialog.Builder(this@KundenListenActivity)
                        .setTitle("Liste kann nicht gelöscht werden")
                        .setMessage("Es sind noch ${kundenInListe.size} Kunde(n) in dieser Liste. Bitte entfernen Sie zuerst alle Kunden aus der Liste.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@launch
                }

                // Bestätigungsdialog
                AlertDialog.Builder(this@KundenListenActivity)
                    .setTitle("Liste löschen?")
                    .setMessage("Möchten Sie die Liste '${liste.name}' wirklich löschen?")
                    .setPositiveButton("Löschen") { _, _ ->
                        CoroutineScope(Dispatchers.Main).launch {
                            val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                                operation = {
                                    listeRepository.deleteListe(liste.id)
                                },
                                context = this@KundenListenActivity,
                                errorMessage = "Fehler beim Löschen. Bitte erneut versuchen.",
                                maxRetries = 3
                            )

                            if (success != null) {
                                // Liste sofort aus dem Adapter entfernen
                                val position = adapter.listen.indexOfFirst { it.id == liste.id }
                                if (position != -1) {
                                    adapter.listen.removeAt(position)
                                    adapter.notifyItemRemoved(position)
                                    adapter.notifyItemRangeChanged(position, adapter.listen.size)
                                }
                                
                                // Dann die Liste neu laden um sicherzustellen, dass alles synchron ist
                                loadListen()
                                
                                Toast.makeText(this@KundenListenActivity, "Liste gelöscht", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Abbrechen", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@KundenListenActivity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class ListenAdapter(
        var listen: MutableList<KundenListe>,
        private var kundenProListe: Map<String, Int> = emptyMap(),
        private val onListeClick: (KundenListe) -> Unit,
        private val onListeLoeschen: (KundenListe) -> Unit
    ) : RecyclerView.Adapter<ListenAdapter.ListeViewHolder>() {

        inner class ListeViewHolder(val binding: ItemListeBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListeViewHolder {
            val binding = ItemListeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ListeViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ListeViewHolder, position: Int) {
            val liste = listen[position]
            holder.binding.tvListeName.text = liste.name
            
            val anzahlKunden = kundenProListe[liste.id] ?: 0
            holder.binding.tvIntervallInfo.text = "${liste.intervalle.size} Intervall(e) • $anzahlKunden Kunde(n)"
            
            val erstelltAm = dateFormat.format(Date(liste.erstelltAm))
            holder.binding.tvErstelltAm.text = "Erstellt: $erstelltAm"

            holder.itemView.setOnClickListener {
                onListeClick(liste)
            }

            holder.binding.btnLoeschen.setOnClickListener {
                onListeLoeschen(liste)
            }
        }

        override fun getItemCount(): Int = listen.size

        fun updateListen(newListen: List<KundenListe>, newKundenProListe: Map<String, Int> = emptyMap()) {
            listen.clear()
            listen.addAll(newListen)
            kundenProListe = newKundenProListe
            notifyDataSetChanged()
        }
    }
}
