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

        adapter = CustomerAdapter(
            items = listOf(),
            context = this,
            onClick = { customer ->
                val intent = Intent(this, CustomerDetailActivity::class.java).apply {
                    putExtra("CUSTOMER_ID", customer.id)
                }
                startActivity(intent)
            }
        )

        binding.rvTourList.layoutManager = LinearLayoutManager(this)
        binding.rvTourList.adapter = adapter
        
        // Callback für Section-Toggle setzen
        adapter.onSectionToggle = { sectionType ->
            loadTourData(viewDate.timeInMillis) // Daten neu laden wenn Section getoggelt wird
        }

        binding.btnBackFromTour.setOnClickListener { finish() }

        binding.btnPrevDay.setOnClickListener {
            viewDate.add(Calendar.DAY_OF_YEAR, -1)
            updateDisplay()
        }

        binding.btnNextDay.setOnClickListener {
            viewDate.add(Calendar.DAY_OF_YEAR, 1)
            updateDisplay()
        }

        binding.btnToday.setOnClickListener {
            viewDate = Calendar.getInstance()
            updateDisplay()
        }
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

            val heuteStart = getStartOfDay(System.currentTimeMillis())
            
            val filteredCustomers = allCustomers.filter { customer ->
                val faelligAm = customerFaelligAm(customer)

                // Urlaub-Logik: Nur Termine im Urlaubszeitraum als Urlaub behandeln
                val faelligAmImUrlaub = customer.urlaubVon > 0 && customer.urlaubBis > 0 && 
                                       faelligAm in customer.urlaubVon..customer.urlaubBis
                if (faelligAmImUrlaub) return@filter false

                // Alle Kunden anzeigen, die fällig sind oder waren (auch zukünftige Termine für erledigte Kunden)
                // Erledigte Kunden zeigen auch zukünftige Termine
                faelligAm <= viewDateStart + TimeUnit.DAYS.toMillis(1) || 
                (customer.abholungErfolgt && customer.auslieferungErfolgt && faelligAm > viewDateStart)
            }
            
            // Kunden in Gruppen einteilen
            val overdueCustomers = mutableListOf<Customer>()
            val normalCustomers = mutableListOf<Customer>()
            val doneCustomers = mutableListOf<Customer>()
            
            filteredCustomers.forEach { customer ->
                val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
                val faelligAm = customerFaelligAm(customer)
                // Überfällig: Termin liegt in der Vergangenheit UND Kunde ist nicht erledigt
                // Keine Beschränkung auf viewDateStart, damit auch in Vergangenheit sichtbar
                val isOverdue = !isDone && faelligAm < heuteStart
                
                when {
                    isDone -> doneCustomers.add(customer)
                    isOverdue -> overdueCustomers.add(customer)
                    else -> normalCustomers.add(customer)
                }
            }
            
            // Sortierung innerhalb der Gruppen
            overdueCustomers.sortBy { customerFaelligAm(it) }
            normalCustomers.sortBy { customerFaelligAm(it) }
            doneCustomers.sortBy { customerFaelligAm(it) }
            
            // Liste mit Section Headers erstellen - Sections immer anzeigen, auch wenn leer
            val items = mutableListOf<ListItem>()
            
            // Überfällig-Section immer anzeigen
            items.add(ListItem.SectionHeader("ÜBERFÄLLIG", overdueCustomers.size, SectionType.OVERDUE))
            if (adapter.isSectionExpanded(SectionType.OVERDUE)) {
                overdueCustomers.forEach { items.add(ListItem.CustomerItem(it)) }
            }
            
            normalCustomers.forEach { items.add(ListItem.CustomerItem(it)) }
            
            // Erledigt-Section immer anzeigen
            items.add(ListItem.SectionHeader("ERLEDIGT", doneCustomers.size, SectionType.DONE))
            if (adapter.isSectionExpanded(SectionType.DONE)) {
                doneCustomers.forEach { items.add(ListItem.CustomerItem(it)) }
            }

            adapter.updateData(items, viewDateStart)
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
