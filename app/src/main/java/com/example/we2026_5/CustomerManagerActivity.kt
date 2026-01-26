package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.we2026_5.ListItem
import com.example.we2026_5.databinding.ActivityCustomerManagerBinding
import com.example.we2026_5.ui.customermanager.CustomerManagerViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class CustomerManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerManagerBinding
    private val viewModel: CustomerManagerViewModel by viewModel()
    private lateinit var adapter: CustomerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerManagerBinding.inflate(layoutInflater)
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

        binding.rvCustomerList.layoutManager = LinearLayoutManager(this)
        binding.rvCustomerList.adapter = adapter

        binding.btnBackFromManager.setOnClickListener { finish() }

        binding.btnNewCustomer.setOnClickListener {
            val intent = Intent(this, AddCustomerActivity::class.java)
            startActivity(intent)
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.filterCustomers(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // ViewModel Observer einrichten
        observeViewModel()
    }
    
    private fun observeViewModel() {
        // Kunden-Liste beobachten
        viewModel.filteredCustomers.observe(this) { customers ->
            adapter.updateData(customers.map { ListItem.CustomerItem(it) })
            
            // Empty State anzeigen wenn keine Kunden vorhanden
            if (customers.isEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.rvCustomerList.visibility = View.GONE
            } else {
                binding.emptyStateLayout.visibility = View.GONE
                binding.rvCustomerList.visibility = View.VISIBLE
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
    
    private fun showErrorState(message: String) {
        binding.errorStateLayout.visibility = View.VISIBLE
        binding.rvCustomerList.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        binding.tvErrorMessage.text = message
        
        binding.btnRetry.setOnClickListener {
            viewModel.loadCustomers()
        }
    }
}
