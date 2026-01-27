package com.example.we2026_5

import android.app.DatePickerDialog
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
import com.example.we2026_5.databinding.ActivityListeBearbeitenBinding
import com.example.we2026_5.util.IntervallManager
import com.example.we2026_5.databinding.ItemKundeListeBinding
import com.example.we2026_5.FirebaseRetryHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.Calendar

class ListeBearbeitenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListeBearbeitenBinding
    private val listeRepository: KundenListeRepository by inject()
    private val customerRepository: CustomerRepository by inject()
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
        binding.btnListeIntervallHinzufuegen.setOnClickListener {
            val neuesIntervall = ListeIntervall()
            intervallAdapter.addIntervall(neuesIntervall)
        }
        
        // Bearbeitungs-Button
        binding.btnEditListe.setOnClickListener { toggleEditMode(true) }
        binding.btnSaveListe.setOnClickListener { handleSave() }
        binding.btnDeleteListe.setOnClickListener { showDeleteConfirmation() }

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
                binding.tvListeNameView.text = liste.name
                binding.tvListeArtView.text = liste.listeArt
                
                // Intervalle laden
                intervalle.clear()
                intervalle.addAll(liste.intervalle)
                intervallAdapter.updateIntervalle(intervalle.toList())
                
                // UI aktualisieren
                if (!isInEditMode) {
                    updateUi()
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
    
    // showDatumPicker Funktion entfernt - jetzt in IntervallManager
    
    private fun toggleEditMode(isEditing: Boolean) {
        isInEditMode = isEditing
        val viewModeVisibility = if (isEditing) View.GONE else View.VISIBLE
        val editModeVisibility = if (isEditing) View.VISIBLE else View.GONE
        
        binding.groupListView.visibility = viewModeVisibility
        binding.groupListEdit.visibility = editModeVisibility
        
        binding.btnEditListe.visibility = viewModeVisibility
        binding.btnSaveListe.visibility = editModeVisibility
        binding.btnDeleteListe.visibility = editModeVisibility
        
        if (isEditing) {
            // Edit-Felder mit aktuellen Werten füllen
            binding.etListeNameEdit.setText(liste.name)
            
            // Liste-Art RadioButton setzen
            when (liste.listeArt) {
                "Gewerbe" -> binding.rgListeArtEdit.check(binding.rbGewerbeEdit.id)
                "Privat" -> binding.rgListeArtEdit.check(binding.rbPrivatEdit.id)
                "Liste" -> binding.rgListeArtEdit.check(binding.rbListeEdit.id)
                else -> binding.rgListeArtEdit.check(binding.rbGewerbeEdit.id)
            }
            
            // Intervalle laden und anzeigen
            intervalle.clear()
            intervalle.addAll(liste.intervalle)
            intervallAdapter.updateIntervalle(intervalle.toList())
        } else {
            // View-Mode: UI aktualisieren
            updateUi()
        }
    }
    
    private fun handleSave() {
        val name = binding.etListeNameEdit.text.toString().trim()
        if (name.isEmpty()) {
            binding.etListeNameEdit.error = "Name fehlt"
            return
        }
        
        // Liste-Art bestimmen
        val listeArt = when (binding.rgListeArtEdit.checkedRadioButtonId) {
            binding.rbGewerbeEdit.id -> "Gewerbe"
            binding.rbPrivatEdit.id -> "Privat"
            binding.rbListeEdit.id -> "Liste"
            else -> "Gewerbe"
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Intervalle für Firebase vorbereiten
                val intervalleMap = intervalle.map { 
                    mapOf(
                        "abholungDatum" to it.abholungDatum,
                        "auslieferungDatum" to it.auslieferungDatum,
                        "wiederholen" to it.wiederholen,
                        "intervallTage" to it.intervallTage,
                        "intervallAnzahl" to it.intervallAnzahl
                    )
                }
                
                val updatedData = mapOf(
                    "name" to name,
                    "listeArt" to listeArt,
                    "intervalle" to intervalleMap
                )
                
                val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                    operation = {
                        listeRepository.updateListe(liste.id, updatedData)
                    },
                    context = this@ListeBearbeitenActivity,
                    errorMessage = "Fehler beim Speichern. Bitte erneut versuchen.",
                    maxRetries = 3
                )
                
                if (success != null) {
                    // Lokale Liste aktualisieren
                    liste = liste.copy(
                        name = name,
                        listeArt = listeArt,
                        intervalle = intervalle.toList()
                    )
                    
                    Toast.makeText(this@ListeBearbeitenActivity, "Liste gespeichert", Toast.LENGTH_SHORT).show()
                    toggleEditMode(false)
                }
            } catch (e: Exception) {
                Toast.makeText(this@ListeBearbeitenActivity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Liste löschen")
            .setMessage("Bist du sicher, dass du diese Liste endgültig löschen möchtest?")
            .setPositiveButton("Löschen") { _, _ -> deleteListe() }
            .setNegativeButton("Abbrechen", null)
            .show()
    }
    
    private fun deleteListe() {
        CoroutineScope(Dispatchers.Main).launch {
            val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                operation = {
                    listeRepository.deleteListe(liste.id)
                },
                context = this@ListeBearbeitenActivity,
                errorMessage = "Fehler beim Löschen. Bitte erneut versuchen.",
                maxRetries = 3
            )
            
            if (success != null) {
                Toast.makeText(this@ListeBearbeitenActivity, "Liste gelöscht", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun updateUi() {
        binding.tvListeNameView.text = liste.name
        binding.tvListeArtView.text = liste.listeArt
        
        // Intervalle im View-Mode anzeigen
        if (liste.intervalle.isNotEmpty()) {
            binding.cardListeIntervallView.visibility = View.VISIBLE
            intervallViewAdapter.updateIntervalle(liste.intervalle)
        } else {
            binding.cardListeIntervallView.visibility = View.GONE
        }
    }
}
