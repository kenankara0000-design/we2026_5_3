package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.we2026_5.databinding.ActivityTourPlannerBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class TourPlannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTourPlannerBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: CustomerAdapter
    private var viewDate = Calendar.getInstance()
    private var tourDataListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTourPlannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = CustomerAdapter(listOf()) { customer ->
            val intent = Intent(this, CustomerDetailActivity::class.java).apply {
                putExtra("CUSTOMER_ID", customer.id)
            }
            startActivity(intent)
        }

        binding.rvTourList.layoutManager = LinearLayoutManager(this)
        binding.rvTourList.adapter = adapter

        binding.btnPrevDay.setOnClickListener {
            viewDate.add(Calendar.DAY_OF_YEAR, -1)
            updateDisplay()
        }

        binding.btnNextDay.setOnClickListener {
            viewDate.add(Calendar.DAY_OF_YEAR, 1)
            updateDisplay()
        }

        binding.btnBackFromTour.setOnClickListener { finish() }
    }

    override fun onStart() {
        super.onStart()
        updateDisplay()
    }

    override fun onStop() {
        super.onStop()
        tourDataListener?.remove()
    }

    private fun updateDisplay() {
        val fmt = SimpleDateFormat("EEE, dd.MM.yyyy", Locale.GERMANY)
        binding.tvCurrentDate.text = fmt.format(viewDate.time)
        loadTourData(viewDate.timeInMillis)
    }

    private fun loadTourData(selectedTimestamp: Long) {
        tourDataListener?.remove()
        tourDataListener = db.collection("customers").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            val allCustomers = snapshot.toObjects(Customer::class.java)
            val viewDateStart = getStartOfDay(selectedTimestamp)

            val filteredList = allCustomers.filter { customer ->
                val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
                val faelligAm = customerFaelligAm(customer)
                val istAnDiesemTagFaellig = faelligAm >= viewDateStart && faelligAm < viewDateStart + TimeUnit.DAYS.toMillis(1)

                if (isDone && !istAnDiesemTagFaellig) return@filter false

                val imUrlaub = customer.urlaubVon > 0 && customer.urlaubBis > 0 && viewDateStart in customer.urlaubVon..customer.urlaubBis
                if (imUrlaub) return@filter false

                faelligAm < viewDateStart + TimeUnit.DAYS.toMillis(1)
            }.sortedWith(compareBy(
                { it.abholungErfolgt && it.auslieferungErfolgt }, 
                { customerFaelligAm(it) < viewDateStart },
                { customerFaelligAm(it) }
            ))

            adapter.updateData(filteredList, viewDateStart)
        }
    }

    private fun customerFaelligAm(c: Customer): Long {
        return if (c.verschobenAufDatum > 0) c.verschobenAufDatum
        else c.letzterTermin + TimeUnit.DAYS.toMillis(c.intervallTage.toLong())
    }

    private fun getStartOfDay(ts: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
