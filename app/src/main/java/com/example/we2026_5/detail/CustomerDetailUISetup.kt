package com.example.we2026_5.detail

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.IntervallAdapter
import com.example.we2026_5.IntervallViewAdapter
import com.example.we2026_5.PhotoAdapter
import com.example.we2026_5.databinding.ActivityCustomerDetailBinding

/**
 * Helper-Klasse für UI-Setup und -Aktualisierung in CustomerDetailActivity.
 * Extrahiert die UI-Logik aus CustomerDetailActivity.
 */
class CustomerDetailUISetup(
    private val activity: AppCompatActivity,
    private val binding: ActivityCustomerDetailBinding,
    private val photoManager: CustomerPhotoManager,
    private val intervalle: MutableList<CustomerIntervall>
) {
    lateinit var photoAdapter: PhotoAdapter
    lateinit var intervallAdapter: IntervallAdapter
    lateinit var intervallViewAdapter: IntervallViewAdapter

    /**
     * Initialisiert alle UI-Komponenten (Adapters, LayoutManager, Click-Listener).
     */
    fun setupUI(
        onTerminAnlegenClick: () -> Unit,
        onBackClick: () -> Unit,
        onAdresseClick: () -> Unit,
        onTelefonClick: () -> Unit,
        onTakePhotoClick: () -> Unit,
        onEditClick: () -> Unit,
        onSaveClick: () -> Unit,
        onDeleteClick: () -> Unit,
        onDatumSelected: (Int, Boolean) -> Unit
    ) {
        // Photo-Adapter initialisieren
        photoAdapter = PhotoAdapter(listOf()) { photoUrl ->
            photoManager.showImageInDialog(photoUrl)
        }
        binding.rvPhotoThumbnails.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        binding.rvPhotoThumbnails.adapter = photoAdapter

        // Intervall-Adapter initialisieren
        intervallAdapter = IntervallAdapter(
            intervalle = intervalle.toMutableList(),
            onIntervallChanged = { neueIntervalle ->
                intervalle.clear()
                intervalle.addAll(neueIntervalle)
            },
            onDatumSelected = { position, isAbholung ->
                onDatumSelected(position, isAbholung)
            }
        )
        binding.rvDetailIntervalle.layoutManager = LinearLayoutManager(activity)
        binding.rvDetailIntervalle.adapter = intervallAdapter

        // Intervall-View-Adapter für Read-Only-Anzeige im View-Mode
        intervallViewAdapter = IntervallViewAdapter(emptyList())
        binding.rvDetailIntervalleView.layoutManager = LinearLayoutManager(activity)
        binding.rvDetailIntervalleView.adapter = intervallViewAdapter

        // Click-Listener setzen
        binding.btnTerminAnlegen.setOnClickListener { onTerminAnlegenClick() }
        binding.btnDetailBack.setOnClickListener { onBackClick() }
        binding.tvDetailAdresse.setOnClickListener { onAdresseClick() }
        binding.tvDetailTelefon.setOnClickListener { onTelefonClick() }
        binding.btnTakePhoto.setOnClickListener { onTakePhotoClick() }
        binding.btnEditCustomer.setOnClickListener { onEditClick() }
        binding.btnSaveCustomer.setOnClickListener { onSaveClick() }
        binding.btnDeleteCustomer.setOnClickListener { onDeleteClick() }
    }

    /**
     * Aktualisiert die UI mit Kundendaten.
     */
    fun updateUi(customer: Customer) {
        binding.tvDetailName.text = customer.name
        binding.tvDetailKundenArt.text = customer.kundenArt
        binding.tvDetailAdresse.text = customer.adresse
        binding.tvDetailTelefon.text = customer.telefon
        binding.tvDetailNotizen.text = customer.notizen
        photoAdapter.updatePhotos(customer.fotoUrls)

        // Kunden-Typ Button (G/P/L) anzeigen
        com.example.we2026_5.ui.CustomerTypeButtonHelper.setupButton(binding.btnKundenTyp, customer, activity)
        
        // Intervalle im View-Mode anzeigen (nur für Gewerblich und Liste)
        val sollIntervallAnzeigen = customer.kundenArt == "Gewerblich" || customer.kundenArt == "Liste"
        if (sollIntervallAnzeigen && customer.intervalle.isNotEmpty()) {
            binding.cardDetailIntervallView.visibility = View.VISIBLE
            intervallViewAdapter.updateIntervalle(customer.intervalle)
        } else {
            binding.cardDetailIntervallView.visibility = View.GONE
        }
    }
}
