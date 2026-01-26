package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.databinding.ActivityTourPlannerBinding
import com.example.we2026_5.ui.tourplanner.TourPlannerViewModel
import com.example.we2026_5.FirebaseRetryHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class TourPlannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTourPlannerBinding
    private val viewModel: TourPlannerViewModel by viewModel()
    private val repository: CustomerRepository by inject()
    private lateinit var adapter: CustomerAdapter
    private var viewDate = Calendar.getInstance()
    private lateinit var gestureDetector: GestureDetectorCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTourPlannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = CustomerAdapter(
            items = mutableListOf(),
            context = this,
            onClick = { customer ->
                val intent = Intent(this, CustomerDetailActivity::class.java).apply {
                    putExtra("CUSTOMER_ID", customer.id)
                }
                startActivity(intent)
            }
        )
        
        // Callbacks für Firebase-Operationen setzen
        setupAdapterCallbacks()

        binding.rvTourList.layoutManager = LinearLayoutManager(this)
        binding.rvTourList.adapter = adapter
        
        // Drag & Drop für Kunden-Reihenfolge
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                return adapter.onItemMove(fromPosition, toPosition)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Nicht verwendet
            }
            
            override fun isLongPressDragEnabled(): Boolean {
                return true // Lang drücken zum Verschieben
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.rvTourList)
        
        // Callback für Section-Toggle setzen
        adapter.onSectionToggle = { sectionType ->
            viewModel.toggleSection(sectionType)
            loadTourData(viewDate.timeInMillis) // Daten neu laden wenn Section getoggelt wird
        }
        
        // ViewModel Observer einrichten
        observeViewModel()

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
        
        // Swipe-Gesten für Datum-Wechsel einrichten
        setupSwipeGestures()
    }
    
    private fun setupSwipeGestures() {
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                
                val deltaX = e2.x - e1.x
                val deltaY = e2.y - e1.y
                
                // Nur horizontale Swipes erkennen (nicht vertikale)
                if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 100) {
                    if (deltaX > 0) {
                        // Swipe nach rechts = vorheriger Tag
                        viewDate.add(Calendar.DAY_OF_YEAR, -1)
                        updateDisplay()
                        return true
                    } else {
                        // Swipe nach links = nächster Tag
                        viewDate.add(Calendar.DAY_OF_YEAR, 1)
                        updateDisplay()
                        return true
                    }
                }
                return false
            }
        })
        
        // Swipe-Gesten auf dem RecyclerView aktivieren
        binding.rvTourList.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event) || false
        }
        
        // Auch auf dem gesamten Layout aktivieren (falls RecyclerView leer ist)
        binding.root.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event) || false
        }
    }

    override fun onStart() {
        super.onStart()
        updateDisplay()
    }

    private fun observeViewModel() {
        // Tour-Items beobachten
        viewModel.tourItems.observe(this) { items ->
            adapter.updateData(items, getStartOfDay(viewDate.timeInMillis))
            
            // Empty State anzeigen wenn keine Kunden vorhanden
            if (items.isEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.rvTourList.visibility = View.GONE
            } else {
                binding.emptyStateLayout.visibility = View.GONE
                binding.rvTourList.visibility = View.VISIBLE
            }
        }
        
        // Loading-State beobachten
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                binding.emptyStateLayout.visibility = View.GONE
                binding.errorStateLayout.visibility = View.GONE
            }
        }
        
        // Error-State beobachten
        viewModel.error.observe(this) { errorMessage ->
            if (errorMessage != null) {
                showErrorState(errorMessage)
            } else {
                binding.errorStateLayout.visibility = View.GONE
            }
        }
    }

    private fun updateDisplay() {
        val fmt = SimpleDateFormat("EEE, dd.MM.yyyy", Locale.GERMANY)
        binding.tvCurrentDate.text = fmt.format(viewDate.time)
        loadTourData(viewDate.timeInMillis)
    }

    private fun loadTourData(selectedTimestamp: Long) {
        viewModel.loadTourData(selectedTimestamp) { sectionType ->
            viewModel.isSectionExpanded(sectionType)
        }
    }
    
    private fun showErrorState(message: String) {
        binding.errorStateLayout.visibility = View.VISIBLE
        binding.rvTourList.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        binding.tvErrorMessage.text = message
        
        binding.btnRetry.setOnClickListener {
            loadTourData(viewDate.timeInMillis)
        }
    }

    private fun setupAdapterCallbacks() {
        // Abholung
        adapter.onAbholung = { customer ->
            if (!customer.abholungErfolgt) {
                CoroutineScope(Dispatchers.Main).launch {
                    val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                        operation = { 
                            repository.updateCustomer(customer.id, mapOf("abholungErfolgt" to true))
                        },
                        context = this@TourPlannerActivity,
                        errorMessage = "Fehler beim Registrieren der Abholung. Bitte erneut versuchen.",
                        maxRetries = 3
                    )
                    if (success == true) {
                        android.widget.Toast.makeText(this@TourPlannerActivity, "Abholung registriert", android.widget.Toast.LENGTH_SHORT).show()
                        loadTourData(viewDate.timeInMillis) // Daten neu laden
                    }
                }
            }
        }
        
        // Auslieferung
        adapter.onAuslieferung = { customer ->
            if (!customer.auslieferungErfolgt) {
                val wasAbholungErfolgt = customer.abholungErfolgt
                CoroutineScope(Dispatchers.Main).launch {
                    val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                        operation = { 
                            repository.updateCustomer(customer.id, mapOf("auslieferungErfolgt" to true))
                        },
                        context = this@TourPlannerActivity,
                        errorMessage = "Fehler beim Registrieren der Auslieferung. Bitte erneut versuchen.",
                        maxRetries = 3
                    )
                    if (success == true) {
                        android.widget.Toast.makeText(this@TourPlannerActivity, "Auslieferung registriert", android.widget.Toast.LENGTH_SHORT).show()
                        if (wasAbholungErfolgt) {
                            resetTourCycle(customer.id)
                        } else {
                            loadTourData(viewDate.timeInMillis) // Daten neu laden
                        }
                    }
                }
            }
        }
        
        // Tour-Zyklus zurücksetzen
        adapter.onResetTourCycle = { customerId ->
            resetTourCycle(customerId)
        }
        
        // Verschieben
        adapter.onVerschieben = { customer, newDate, alleVerschieben ->
            CoroutineScope(Dispatchers.Main).launch {
                val success = if (alleVerschieben) {
                    // Alle zukünftigen Termine verschieben
                    val aktuellerFaelligAm = if (customer.verschobenAufDatum > 0) customer.verschobenAufDatum
                                             else customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())
                    val diff = newDate - aktuellerFaelligAm
                    val neuerLetzterTermin = customer.letzterTermin + diff
                    FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                        operation = { 
                            repository.updateCustomer(customer.id, mapOf(
                                "letzterTermin" to neuerLetzterTermin,
                                "verschobenAufDatum" to 0
                            ))
                        },
                        context = this@TourPlannerActivity,
                        errorMessage = "Fehler beim Verschieben. Bitte erneut versuchen.",
                        maxRetries = 3
                    )
                } else {
                    // Nur diesen Termin verschieben
                    FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                        operation = { 
                            repository.updateCustomer(customer.id, mapOf("verschobenAufDatum" to newDate))
                        },
                        context = this@TourPlannerActivity,
                        errorMessage = "Fehler beim Verschieben. Bitte erneut versuchen.",
                        maxRetries = 3
                    )
                }
                if (success == true) {
                    android.widget.Toast.makeText(this@TourPlannerActivity, 
                        if (alleVerschieben) "Alle zukünftigen Termine verschoben" else "Termin verschoben", 
                        android.widget.Toast.LENGTH_SHORT).show()
                    loadTourData(viewDate.timeInMillis) // Daten neu laden
                }
            }
        }
        
        // Urlaub
        adapter.onUrlaub = { customer, von, bis ->
            CoroutineScope(Dispatchers.Main).launch {
                val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                    operation = { 
                        repository.updateCustomer(customer.id, mapOf(
                            "urlaubVon" to von, 
                            "urlaubBis" to bis
                        ))
                    },
                    context = this@TourPlannerActivity,
                    errorMessage = "Fehler beim Eintragen des Urlaubs. Bitte erneut versuchen.",
                    maxRetries = 3
                )
                if (success == true) {
                    android.widget.Toast.makeText(this@TourPlannerActivity, "Urlaub eingetragen", android.widget.Toast.LENGTH_SHORT).show()
                    loadTourData(viewDate.timeInMillis) // Daten neu laden
                }
            }
        }
        
        // Rückgängig
        adapter.onRueckgaengig = { customer ->
            val updates = mutableMapOf<String, Any>()
            if (customer.abholungErfolgt) updates["abholungErfolgt"] = false
            if (customer.auslieferungErfolgt) updates["auslieferungErfolgt"] = false
            
            CoroutineScope(Dispatchers.Main).launch {
                val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                    operation = { 
                        repository.updateCustomer(customer.id, updates)
                    },
                    context = this@TourPlannerActivity,
                    errorMessage = "Fehler beim Rückgängigmachen. Bitte erneut versuchen.",
                    maxRetries = 3
                )
                if (success == true) {
                    android.widget.Toast.makeText(this@TourPlannerActivity, "Rückgängig gemacht", android.widget.Toast.LENGTH_SHORT).show()
                    loadTourData(viewDate.timeInMillis) // Daten neu laden
                }
            }
        }
    }
    
    private fun resetTourCycle(customerId: String) {
        val resetData = mapOf(
            "letzterTermin" to System.currentTimeMillis(),
            "abholungErfolgt" to false,
            "auslieferungErfolgt" to false,
            "verschobenAufDatum" to 0
        )
        CoroutineScope(Dispatchers.Main).launch {
            val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                operation = { 
                    repository.updateCustomer(customerId, resetData)
                },
                context = this@TourPlannerActivity,
                errorMessage = "Fehler beim Zurücksetzen. Bitte erneut versuchen.",
                maxRetries = 3
            )
            if (success == true) {
                android.widget.Toast.makeText(this@TourPlannerActivity, "Tour abgeschlossen!", android.widget.Toast.LENGTH_SHORT).show()
                loadTourData(viewDate.timeInMillis) // Daten neu laden
            }
        }
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
