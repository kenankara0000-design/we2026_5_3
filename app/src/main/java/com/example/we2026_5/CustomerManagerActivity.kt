package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.we2026_5.databinding.ActivityCustomerManagerBinding
import com.google.firebase.firestore.FirebaseFirestore

class CustomerManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerManagerBinding
    private val db = FirebaseFirestore.getInstance()
    private var allCustomers = listOf<Customer>()
    private lateinit var adapter: CustomerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Adapter Setup mit Klick-Navigation zur Detailseite
        adapter = CustomerAdapter(listOf()) { customer ->
            val intent = Intent(this, CustomerDetailActivity::class.java)
            intent.putExtra("CUSTOMER_ID", customer.id)
            intent.putExtra("CUSTOMER_NAME", customer.name)
            intent.putExtra("CUSTOMER_ADRESS", customer.adresse)
            intent.putExtra("CUSTOMER_PHONE", customer.telefon)
            startActivity(intent)
        }

        binding.rvCustomerList.layoutManager = LinearLayoutManager(this)
        binding.rvCustomerList.adapter = adapter

        // 2. Zurück Button
        binding.btnBackFromManager.setOnClickListener {
            finish()
        }

        // 3. Daten aus Firebase laden (Echtzeit-Synchronisierung)
        loadCustomers()

        // 4. Suchfunktion (Punkt 1 der Liste)
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadCustomers() {
        db.collection("customers").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener

            allCustomers = snapshot?.toObjects(Customer::class.java) ?: listOf()

            // Sortiere alphabetisch nach Name beim ersten Laden
            val sortedList = allCustomers.sortedBy { it.name.lowercase() }
            adapter.updateData(sortedList)
        }
    }

    private fun filterList(query: String) {
        val filtered = allCustomers.filter { customer ->
            customer.name.contains(query, ignoreCase = true) ||
                    customer.adresse.contains(query, ignoreCase = true)
        }.sortedBy { it.name.lowercase() }

        adapter.updateData(filtered)
    }
}