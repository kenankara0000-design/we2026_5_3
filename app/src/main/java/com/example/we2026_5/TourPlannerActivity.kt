package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.we2026_5.databinding.ActivityTourPlannerBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class TourPlannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTourPlannerBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: CustomerAdapter
    private var viewDate = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTourPlannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Adapter Initialisierung
        adapter = CustomerAdapter(listOf()) { customer ->
            val intent = Intent(this, CustomerDetailActivity::class.java)
            intent.putExtra("CUSTOMER_ID", customer.id)
            startActivity(intent)
        }

        binding.rvTourList.layoutManager = LinearLayoutManager(this)
        binding.rvTourList.adapter = adapter

        // Datum wechseln (Punkt 5)
        binding.btnPrevDay.setOnClickListener {
            viewDate.add(Calendar.DAY_OF_YEAR, -1)
            updateDisplay()
        }

        binding.btnNextDay.setOnClickListener {
            viewDate.add(Calendar.DAY_OF_YEAR, 1)
            updateDisplay()
        }

        binding.btnBackFromTour.setOnClickListener { finish() }

        updateDisplay()
    }

    private fun updateDisplay() {
        val fmt = SimpleDateFormat("EEE, dd.MM.yyyy", Locale.GERMANY)
        binding.tvCurrentDate.text = fmt.format(viewDate.time)
        loadTourData(viewDate.timeInMillis)
    }

    private fun loadTourData(selectedTimestamp: Long) {
        db.collection("customers").addSnapshotListener { snapshot, _ ->
            val allCustomers = snapshot?.toObjects(Customer::class.java) ?: listOf()
            val gewaehltesDatumStart = getStartOfDay(selectedTimestamp)
            val gewaehltesDatumEnde = gewaehltesDatumStart + TimeUnit.DAYS.toMillis(1)

            // Filter & Sortierung (Punkte 2, 5, 8, 9)
            val filteredList = allCustomers.filter { customer ->
                // 1. Urlaubs-Check (Punkt 8): Ist der gewählte Tag im Urlaubszeitraum?
                val imUrlaub = if (customer.urlaubVon > 0 && customer.urlaubBis > 0) {
                    gewaehltesDatumStart in customer.urlaubVon..customer.urlaubBis
                } else {
                    false
                }

                // 2. Fälligkeits-Check
                val faelligAm = if (customer.verschobenAufDatum > 0) {
                    customer.verschobenAufDatum
                } else {
                    customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())
                }

                // Nur anzeigen wenn: Nicht im Urlaub UND (heute fällig ODER überfällig)
                !imUrlaub && faelligAm < gewaehltesDatumEnde
            }.sortedWith(compareBy(
                // Erledigte nach unten (Punkt 2)
                { it.auslieferungErfolgt && it.abholungErfolgt },
                // Überfällige nach oben (Punkt 5)
                { if (customerFaelligAm(it) < gewaehltesDatumStart) 0 else 1 },
                // Dann nach tatsächlichem Datum
                { customerFaelligAm(it) }
            ))

            adapter.updateData(filteredList)
        }
    }

    private fun customerFaelligAm(c: Customer): Long {
        return if (c.verschobenAufDatum > 0) c.verschobenAufDatum
        else c.letzterTermin + TimeUnit.DAYS.toMillis(c.intervallTage.toLong())
    }

    private fun getStartOfDay(ts: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ts
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}