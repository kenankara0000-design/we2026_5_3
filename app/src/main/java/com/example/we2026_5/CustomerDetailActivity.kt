package com.example.we2026_5

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.we2026_5.R
import java.io.File

class CustomerDetailActivity : AppCompatActivity() {

    private val photoList = mutableListOf<File>()
    private lateinit var photoAdapter: PhotoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_detail)

        val customerName = intent.getStringExtra("CUSTOMER_NAME") ?: "Unbekannter Kunde"

        // UI Initialisierung
        val tvName = findViewById<TextView>(R.id.tvDetailName)
        val btnBack = findViewById<ImageButton>(R.id.btnDetailBack)
        val btnAbholung = findViewById<Button>(R.id.btnAbholung)
        val btnAuslieferung = findViewById<Button>(R.id.btnAuslieferung)
        val btnVerschieben = findViewById<Button>(R.id.btnVerschieben)
        val btnUrlaub = findViewById<Button>(R.id.btnUrlaub)
        val btnTakePhoto = findViewById<Button>(R.id.btnTakePhoto)
        val rvPhotos = findViewById<RecyclerView>(R.id.rvPhotoThumbnails)

        tvName.text = customerName

        // Navigation
        btnBack.setOnClickListener { finish() }

        // Klick-Logik für neue Buttons
        btnAbholung.setOnClickListener { Toast.makeText(this, "Abholung registriert", Toast.LENGTH_SHORT).show() }
        btnAuslieferung.setOnClickListener { Toast.makeText(this, "Auslieferung registriert", Toast.LENGTH_SHORT).show() }
        btnVerschieben.setOnClickListener { Toast.makeText(this, "Verschieben gewählt", Toast.LENGTH_SHORT).show() }
        btnUrlaub.setOnClickListener { Toast.makeText(this, "Urlaub eingetragen", Toast.LENGTH_SHORT).show() }
        btnTakePhoto.setOnClickListener { Toast.makeText(this, "Kamera startet...", Toast.LENGTH_SHORT).show() }

        // RecyclerView Setup
        photoAdapter = PhotoAdapter(photoList)
        rvPhotos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvPhotos.adapter = photoAdapter
    }
}