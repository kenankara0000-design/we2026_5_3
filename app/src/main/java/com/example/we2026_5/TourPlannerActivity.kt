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
    private lateinit var weekAdapter: WeekViewAdapter
    private var viewDate = Calendar.getInstance()
    private var isWeekView = false
    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var networkMonitor: NetworkMonitor

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
        
        // Wochenansicht-Adapter initialisieren
        weekAdapter = WeekViewAdapter(
            weekData = emptyMap(),
            context = this,
            onCustomerClick = { customer ->
                val intent = Intent(this, CustomerDetailActivity::class.java).apply {
                    putExtra("CUSTOMER_ID", customer.id)
                }
                startActivity(intent)
            },
            customerAdapterFactory = { items ->
                val dayAdapter = CustomerAdapter(
                    items = items.toMutableList(),
                    context = this,
                    onClick = { customer ->
                        val intent = Intent(this, CustomerDetailActivity::class.java).apply {
                            putExtra("CUSTOMER_ID", customer.id)
                        }
                        startActivity(intent)
                    }
                )
                setupAdapterCallbacksForAdapter(dayAdapter)
                dayAdapter
            }
        )
        binding.rvWeekView.layoutManager = LinearLayoutManager(this)
        binding.rvWeekView.adapter = weekAdapter
        
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
            reloadCurrentView() // Daten neu laden wenn Section getoggelt wird
        }
        
        // Initial: Tagesansicht
        updateViewMode()
        
        // ViewModel Observer einrichten
        observeViewModel()

        binding.btnBackFromTour.setOnClickListener { finish() }

        binding.btnPrevDay.setOnClickListener {
            if (isWeekView) {
                viewDate.add(Calendar.WEEK_OF_YEAR, -1)
            } else {
                viewDate.add(Calendar.DAY_OF_YEAR, -1)
            }
            updateDisplay()
        }

        binding.btnNextDay.setOnClickListener {
            if (isWeekView) {
                viewDate.add(Calendar.WEEK_OF_YEAR, 1)
            } else {
                viewDate.add(Calendar.DAY_OF_YEAR, 1)
            }
            updateDisplay()
        }

        binding.btnToday.setOnClickListener {
            viewDate = Calendar.getInstance()
            updateDisplay()
        }

        binding.btnMapView.setOnClickListener {
            val intent = Intent(this, MapViewActivity::class.java)
            startActivity(intent)
        }
        
        // Toggle zwischen Tag- und Wochenansicht
        binding.btnToggleView.setOnClickListener {
            isWeekView = !isWeekView
            updateViewMode()
        }
        
        // Pull-to-Refresh
        binding.swipeRefresh.setOnRefreshListener {
            reloadCurrentView()
            binding.swipeRefresh.isRefreshing = false
        }
        
        // Offline-Status-Monitoring
        networkMonitor = NetworkMonitor(this)
        networkMonitor.startMonitoring()
        networkMonitor.isOnline.observe(this) { isOnline ->
            binding.tvOfflineStatus.visibility = if (isOnline) View.GONE else View.VISIBLE
        }
        
        // Swipe-Gesten für Datum-Wechsel einrichten
        setupSwipeGestures()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.stopMonitoring()
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
        // Tour-Items beobachten (Tagesansicht)
        viewModel.tourItems.observe(this) { items ->
            if (!isWeekView) {
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
        }
        
        // Wochen-Items beobachten
        viewModel.weekItems.observe(this) { weekData ->
            if (isWeekView) {
                val weekStart = Calendar.getInstance().apply {
                    timeInMillis = viewDate.timeInMillis
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                weekAdapter.updateWeekData(weekData, weekStart)
                
                // Empty State prüfen
                val hasAnyData = weekData.values.any { it.isNotEmpty() }
                if (hasAnyData) {
                    binding.emptyStateLayout.visibility = View.GONE
                    binding.rvWeekView.visibility = View.VISIBLE
                } else {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                    binding.rvWeekView.visibility = View.GONE
                }
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
        if (isWeekView) {
            // Wochenansicht: Zeige Woche (z.B. "KW 4, 2026")
            val cal = Calendar.getInstance().apply {
                timeInMillis = viewDate.timeInMillis
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            val weekNumber = cal.get(Calendar.WEEK_OF_YEAR)
            val year = cal.get(Calendar.YEAR)
            binding.tvCurrentDate.text = "KW $weekNumber, $year"
            loadWeekData(viewDate.timeInMillis)
        } else {
            // Tagesansicht
            val fmt = SimpleDateFormat("EEE, dd.MM.yyyy", Locale.GERMANY)
            binding.tvCurrentDate.text = fmt.format(viewDate.time)
            loadTourData(viewDate.timeInMillis)
        }
    }
    
    private fun updateViewMode() {
        if (isWeekView) {
            binding.rvTourList.visibility = View.GONE
            binding.rvWeekView.visibility = View.VISIBLE
            binding.btnPrevDay.contentDescription = "Vorherige Woche"
            binding.btnNextDay.contentDescription = "Nächste Woche"
            binding.btnToggleView.contentDescription = "Tagesansicht"
        } else {
            binding.rvTourList.visibility = View.VISIBLE
            binding.rvWeekView.visibility = View.GONE
            binding.btnPrevDay.contentDescription = "Vorheriger Tag"
            binding.btnNextDay.contentDescription = "Nächster Tag"
            binding.btnToggleView.contentDescription = "Wochenansicht"
        }
        updateDisplay()
    }

    private fun loadTourData(selectedTimestamp: Long) {
        viewModel.loadTourData(selectedTimestamp) { sectionType ->
            viewModel.isSectionExpanded(sectionType)
        }
    }
    
    private fun loadWeekData(selectedTimestamp: Long) {
        viewModel.loadWeekData(selectedTimestamp) { sectionType ->
            viewModel.isSectionExpanded(sectionType)
        }
    }
    
    private fun reloadCurrentView() {
        if (isWeekView) {
            loadWeekData(viewDate.timeInMillis)
        } else {
            loadTourData(viewDate.timeInMillis)
        }
    }
    
    private fun showErrorState(message: String) {
        binding.errorStateLayout.visibility = View.VISIBLE
        binding.rvTourList.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        binding.tvErrorMessage.text = message
        
        binding.btnRetry.setOnClickListener {
            reloadCurrentView()
        }
    }

    private fun setupAdapterCallbacks() {
        setupAdapterCallbacksForAdapter(adapter)
    }
    
    private fun setupAdapterCallbacksForAdapter(adapter: CustomerAdapter) {
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
                        reloadCurrentView() // Daten neu laden
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
                            reloadCurrentView() // Daten neu laden
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
                    reloadCurrentView() // Daten neu laden
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
                    reloadCurrentView() // Daten neu laden
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
                    reloadCurrentView() // Daten neu laden
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
                reloadCurrentView() // Daten neu laden
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
