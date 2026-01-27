package com.example.we2026_5

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.data.repository.TerminRegelRepository
import com.example.we2026_5.databinding.ActivityListeBearbeitenBinding
import com.example.we2026_5.util.IntervallManager
import com.example.we2026_5.liste.ListeBearbeitenUIManager
import com.example.we2026_5.liste.ListeBearbeitenCallbacks
import com.example.we2026_5.databinding.ItemKundeListeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class ListeBearbeitenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListeBearbeitenBinding
    private val listeRepository: KundenListeRepository by inject()
    private val customerRepository: CustomerRepository by inject()
    private val regelRepository: TerminRegelRepository by inject()
    private lateinit var liste: KundenListe
    private lateinit var kundenInListeAdapter: KundenListeAdapter
    private lateinit var verfuegbareKundenAdapter: KundenListeAdapter
    private var kundenInListe = mutableListOf<Customer>()
    private var verfuegbareKunden = mutableListOf<Customer>()
    
    // Intervalle-Verwaltung
    private val intervalle = mutableListOf<ListeIntervall>()
    private lateinit var intervallAdapter: ListeIntervallAdapter
    private lateinit var intervallViewAdapter: ListeIntervallViewAdapter // Read-Only für View-Mode
    private var aktuellesIntervallPosition: Int = -1
    private var aktuellesDatumTyp: Boolean = true // true = Abholung, false = Auslieferung
    private var isInEditMode = false
    
    // Helper-Klassen
    private lateinit var uiManager: ListeBearbeitenUIManager
    private lateinit var callbacks: ListeBearbeitenCallbacks

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
        
        // Intervall-Adapter initialisieren
        intervallAdapter = ListeIntervallAdapter(
            intervalle = intervalle.toMutableList(),
            onIntervallChanged = { neueIntervalle ->
                intervalle.clear()
                intervalle.addAll(neueIntervalle)
            },
            onDatumSelected = { position, isAbholung ->
                aktuellesIntervallPosition = position
                aktuellesDatumTyp = isAbholung
                IntervallManager.showDatumPickerForListe(
                    context = this@ListeBearbeitenActivity,
                    intervalle = intervalle,
                    position = position,
                    isAbholung = isAbholung,
                    onDatumSelected = { updatedIntervall ->
                        intervallAdapter.updateIntervalle(intervalle.toList())
                    }
                )
            }
        )
        binding.rvListeIntervalle.layoutManager = LinearLayoutManager(this)
        binding.rvListeIntervalle.adapter = intervallAdapter

        // Intervall-View-Adapter für Read-Only-Anzeige im View-Mode
        intervallViewAdapter = ListeIntervallViewAdapter(emptyList())
        binding.rvListeIntervalleView.layoutManager = LinearLayoutManager(this)
        binding.rvListeIntervalleView.adapter = intervallViewAdapter

        // Intervall hinzufügen Button
        // Termin Anlegen Button
        binding.btnTerminAnlegen.setOnClickListener {
            callbacks.showRegelAuswahlDialog { regel ->
                callbacks.wendeRegelAn(regel, liste, intervalle, intervallAdapter) { updatedListe ->
                    liste = updatedListe
                }
            }
        }
        
        // Bearbeitungs-Button
        binding.btnEditListe.setOnClickListener { 
            isInEditMode = true
            uiManager.toggleEditMode(true, liste, intervalle)
        }
        binding.btnSaveListe.setOnClickListener { 
            val name = binding.etListeNameEdit.text.toString().trim()
            if (name.isEmpty()) {
                binding.etListeNameEdit.error = "Name fehlt"
                return@setOnClickListener
            }
            
            val listeArt = when (binding.rgListeArtEdit.checkedRadioButtonId) {
                binding.rbGewerbeEdit.id -> "Gewerbe"
                binding.rbPrivatEdit.id -> "Privat"
                binding.rbListeEdit.id -> "Liste"
                else -> "Gewerbe"
            }
            
            callbacks.handleSave(liste, name, listeArt, intervalle.toList()) { updatedListe ->
                liste = updatedListe
                isInEditMode = false
                uiManager.toggleEditMode(false, liste, intervalle)
            }
        }
        binding.btnDeleteListe.setOnClickListener { 
            callbacks.showDeleteConfirmation {
                callbacks.deleteListe(liste.id) {
                    finish()
                }
            }
        }

        // Adapter initialisieren
        kundenInListeAdapter = KundenListeAdapter(
            kunden = kundenInListe,
            showRemoveButton = true,
            onKundeClick = { customer ->
                callbacks.entferneKundeAusListe(customer)
            }
        )

        verfuegbareKundenAdapter = KundenListeAdapter(
            kunden = verfuegbareKunden,
            showRemoveButton = false,
            onKundeClick = { customer ->
                callbacks.fuegeKundeZurListeHinzu(customer, liste.id)
            }
        )

        // Helper-Klassen initialisieren
        uiManager = ListeBearbeitenUIManager(
            activity = this,
            binding = binding,
            intervallAdapter = intervallAdapter,
            intervallViewAdapter = intervallViewAdapter
        )
        uiManager.setupRecyclerViews(kundenInListeAdapter, verfuegbareKundenAdapter)
        
        callbacks = ListeBearbeitenCallbacks(
            activity = this,
            customerRepository = customerRepository,
            listeRepository = listeRepository,
            regelRepository = regelRepository,
            onDataReload = { loadDaten() }
        )

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
                binding.tvListeNameView.text = liste.name
                binding.tvListeArtView.text = liste.listeArt
                
                // Intervalle laden
                intervalle.clear()
                intervalle.addAll(liste.intervalle)
                intervallAdapter.updateIntervalle(intervalle.toList())
                
                // UI aktualisieren
                if (!isInEditMode) {
                    uiManager.updateUi(liste)
                }

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

                uiManager.updateEmptyState(kundenInListe.isEmpty() && verfuegbareKunden.isEmpty())

            } catch (e: Exception) {
                Toast.makeText(this@ListeBearbeitenActivity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
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

            // Kunden-Typ Button (G/P/L) anzeigen
            val context = holder.itemView.context
            com.example.we2026_5.ui.CustomerTypeButtonHelper.setupButton(holder.binding.btnKundenTyp, kunde, context)

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
