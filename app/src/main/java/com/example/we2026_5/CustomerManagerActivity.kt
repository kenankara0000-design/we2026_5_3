package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.we2026_5.databinding.ActivityCustomerManagerBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class CustomerManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerManagerBinding
    private val db = FirebaseFirestore.getInstance()
    private var allCustomers = listOf<Customer>()
    private lateinit var adapter: CustomerAdapter
    private var customerListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = CustomerAdapter(listOf()) { customer ->
            val intent = Intent(this, CustomerDetailActivity::class.java).apply {
                putExtra("CUSTOMER_ID", customer.id)
            }
            startActivity(intent)
        }

        binding.rvCustomerList.layoutManager = LinearLayoutManager(this)
        binding.rvCustomerList.adapter = adapter

        binding.btnBackFromManager.setOnClickListener { finish() }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onStart() {
        super.onStart()
        loadCustomers()
    }

    override fun onStop() {
        super.onStop()
        customerListener?.remove()
    }

    private fun loadCustomers() {
        customerListener = db.collection("customers").orderBy("name").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                return@addSnapshotListener
            }

            allCustomers = snapshot.toObjects(Customer::class.java)
            filterList(binding.etSearch.text.toString())
        }
    }

    private fun filterList(query: String) {
        val filtered = if (query.isEmpty()) {
            allCustomers
        } else {
            allCustomers.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.adresse.contains(query, ignoreCase = true)
            }
        }
        // Ruft die korrekte updateData-Methode ohne Datum auf
        adapter.updateData(filtered)
    }
}
