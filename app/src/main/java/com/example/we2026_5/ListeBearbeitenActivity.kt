package com.example.we2026_5

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.databinding.ActivityListeBearbeitenBinding
import com.example.we2026_5.databinding.ItemKundeListeBinding
import com.example.we2026_5.FirebaseRetryHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class ListeBearbeitenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListeBearbeitenBinding
    private val listeRepository: KundenListeRepository by inject()
    private val customerRepository: CustomerRepository by inject()
    private lateinit var liste: KundenListe
    private lateinit var kundenInListeAdapter: KundenListeAdapter
    private lateinit var verfuegbareKundenAdapter: KundenListeAdapter
    private var kundenInListe = mutableListOf<Customer>()
    private var verfuegbareKunden = mutableListOf<Customer>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListeBearbeitenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val listeId = intent.getStringExtra("LISTE_ID")
        if (listeId == null) {
            Toast.makeText(this, "Fehler: Keine Liste-ID übergeben", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnBack.setOnClickListener { finish() }

        // Adapter initialisieren
        kundenInListeAdapter = KundenListeAdapter(
            kunden = kundenInListe,
            showRemoveButton = true,
            onKundeClick = { customer ->
                // Kunde aus Liste entfernen
                entferneKundeAusListe(customer)
            }
        )

        verfuegbareKundenAdapter = KundenListeAdapter(
            kunden = verfuegbareKunden,
            showRemoveButton = false,
            onKundeClick = { customer ->
                // Kunde zur Liste hinzufügen
                fuegeKundeZurListeHinzu(customer)
            }
        )

        binding.rvKundenInListe.layoutManager = LinearLayoutManager(this)
        binding.rvKundenInListe.adapter = kundenInListeAdapter

        binding.rvVerfuegbareKunden.layoutManager = LinearLayoutManager(this)
        binding.rvVerfuegbareKunden.adapter = verfuegbareKundenAdapter

        binding.swipeRefresh.setOnRefreshListener {
            loadDaten()
            binding.swipeRefresh.isRefreshing = false
        }

        // Liste und Kunden laden
        loadDaten(listeId)
    }

    private fun loadDaten(listeId: String? = null) {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateLayout.visibility = View.GONE

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val targetListeId = listeId ?: liste.id
                
                // Liste laden
                val geladeneListe = listeRepository.getListeById(targetListeId)
                if (geladeneListe == null) {
                    Toast.makeText(this@ListeBearbeitenActivity, "Liste nicht gefunden", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }
                liste = geladeneListe
                binding.tvListeName.text = liste.name

                // Alle Kunden laden
                val alleKunden = customerRepository.getAllCustomers()

                // Kunden in Liste und verfügbare Kunden trennen
                kundenInListe.clear()
                verfuegbareKunden.clear()

                alleKunden.forEach { kunde ->
                    if (kunde.listeId == liste.id) {
                        kundenInListe.add(kunde)
                    } else if (kunde.listeId.isEmpty()) {
                        // Nur Kunden ohne Liste können hinzugefügt werden
                        verfuegbareKunden.add(kunde)
                    }
                }

                // Nach Name sortieren
                kundenInListe.sortBy { it.name }
                verfuegbareKunden.sortBy { it.name }

                kundenInListeAdapter.notifyDataSetChanged()
                verfuegbareKundenAdapter.notifyDataSetChanged()

                if (kundenInListe.isEmpty() && verfuegbareKunden.isEmpty()) {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                } else {
                    binding.emptyStateLayout.visibility = View.GONE
                }

            } catch (e: Exception) {
                Toast.makeText(this@ListeBearbeitenActivity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun entferneKundeAusListe(customer: Customer) {
        CoroutineScope(Dispatchers.Main).launch {
            val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                operation = {
                    customerRepository.updateCustomer(customer.id, mapOf("listeId" to ""))
                },
                context = this@ListeBearbeitenActivity,
                errorMessage = "Fehler beim Entfernen. Bitte erneut versuchen.",
                maxRetries = 3
            )

            if (success != null) {
                Toast.makeText(this@ListeBearbeitenActivity, "Kunde aus Liste entfernt", Toast.LENGTH_SHORT).show()
                loadDaten()
            }
        }
    }

    private fun fuegeKundeZurListeHinzu(customer: Customer) {
        CoroutineScope(Dispatchers.Main).launch {
            val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                operation = {
                    customerRepository.updateCustomer(customer.id, mapOf("listeId" to liste.id))
                },
                context = this@ListeBearbeitenActivity,
                errorMessage = "Fehler beim Hinzufügen. Bitte erneut versuchen.",
                maxRetries = 3
            )

            if (success != null) {
                Toast.makeText(this@ListeBearbeitenActivity, "Kunde zur Liste hinzugefügt", Toast.LENGTH_SHORT).show()
                loadDaten()
            }
        }
    }

    inner class KundenListeAdapter(
        private val kunden: MutableList<Customer>,
        private val showRemoveButton: Boolean,
        private val onKundeClick: (Customer) -> Unit
    ) : RecyclerView.Adapter<KundenListeAdapter.KundeViewHolder>() {

        inner class KundeViewHolder(val binding: ItemKundeListeBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KundeViewHolder {
            val binding = ItemKundeListeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return KundeViewHolder(binding)
        }

        override fun onBindViewHolder(holder: KundeViewHolder, position: Int) {
            val kunde = kunden[position]
            holder.binding.tvKundenName.text = kunde.name
            holder.binding.tvKundenAdresse.text = kunde.adresse

            if (showRemoveButton) {
                holder.binding.btnEntfernen.visibility = View.VISIBLE
                holder.binding.btnHinzufuegen.visibility = View.GONE
                holder.binding.btnEntfernen.setOnClickListener {
                    onKundeClick(kunde)
                }
            } else {
                holder.binding.btnEntfernen.visibility = View.GONE
                holder.binding.btnHinzufuegen.visibility = View.VISIBLE
                holder.binding.btnHinzufuegen.setOnClickListener {
                    onKundeClick(kunde)
                }
            }
        }

        override fun getItemCount(): Int = kunden.size
    }
}
