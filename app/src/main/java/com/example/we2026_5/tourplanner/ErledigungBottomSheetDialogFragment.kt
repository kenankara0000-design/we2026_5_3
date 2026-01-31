package com.example.we2026_5.tourplanner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.example.we2026_5.databinding.BottomSheetErledigungBinding
import com.example.we2026_5.util.DateFormatter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Bottom Sheet mit Tabs (Erledigung, Termin, Details) für alle Kundenarten inkl. Listen.
 * Zeigt Aktionen und Kunden-Details; ruft Callbacks für Erledigung auf.
 * Customer wird vom Callback-Host bereitgestellt (nicht serialisiert).
 */
class ErledigungBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetErledigungBinding? = null
    private val binding get() = _binding!!

    private var customer: Customer? = null
    private var viewDateMillis: Long = 0L
    private var state: ErledigungSheetState? = null
    private var callbacks: ErledigungSheetCallbacks? = null

    /** Wird vom Host gesetzt, nachdem das Fragment angezeigt wird (z. B. mit customer aus Adapter). */
    fun setCustomer(c: Customer) {
        customer = c
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetErledigungBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewDateMillis = arguments?.getLong(ARG_VIEW_DATE_MILLIS, 0L) ?: 0L
        state = arguments?.getSerializable(ARG_STATE) as? ErledigungSheetState
        callbacks = parentFragment as? ErledigungSheetCallbacks
            ?: activity as? ErledigungSheetCallbacks

        if (customer == null || state == null) {
            dismiss()
            return
        }
        val c = customer!!
        val s = state!!

        val dateStr = if (viewDateMillis > 0) DateFormatter.formatDate(viewDateMillis) else ""
        binding.sheetTitle.text = getString(com.example.we2026_5.R.string.sheet_title_format, c.name, dateStr)

        setupTabs()
        applyState(s)
        setupDetailsTab(c)
        setupClickListeners(c, s)
    }

    private fun setupTabs() {
        binding.sheetTabs.addTab(binding.sheetTabs.newTab().setText(getString(R.string.sheet_tab_erledigung)))
        binding.sheetTabs.addTab(binding.sheetTabs.newTab().setText(getString(R.string.sheet_tab_termin)))
        binding.sheetTabs.addTab(binding.sheetTabs.newTab().setText(getString(R.string.sheet_tab_details)))

        binding.contentErledigung.visibility = View.VISIBLE
        binding.contentTermin.visibility = View.GONE
        binding.contentDetails.visibility = View.GONE

        binding.sheetTabs.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        binding.contentErledigung.visibility = View.VISIBLE
                        binding.contentTermin.visibility = View.GONE
                        binding.contentDetails.visibility = View.GONE
                    }
                    1 -> {
                        binding.contentErledigung.visibility = View.GONE
                        binding.contentTermin.visibility = View.VISIBLE
                        binding.contentDetails.visibility = View.GONE
                    }
                    2 -> {
                        binding.contentErledigung.visibility = View.GONE
                        binding.contentTermin.visibility = View.GONE
                        binding.contentDetails.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun applyState(s: ErledigungSheetState) {
        binding.btnSheetAbholung.visibility = if (s.showAbholung) View.VISIBLE else View.GONE
        binding.btnSheetAbholung.isEnabled = s.enableAbholung
        binding.btnSheetAuslieferung.visibility = if (s.showAuslieferung) View.VISIBLE else View.GONE
        binding.btnSheetAuslieferung.isEnabled = s.enableAuslieferung
        binding.btnSheetKw.visibility = if (s.showKw) View.VISIBLE else View.GONE
        binding.btnSheetKw.isEnabled = s.enableKw
        binding.btnSheetRueckgaengig.visibility = if (s.showRueckgaengig) View.VISIBLE else View.GONE
        binding.btnSheetVerschieben.visibility = if (s.showVerschieben) View.VISIBLE else View.GONE
        binding.btnSheetUrlaub.visibility = if (s.showUrlaub) View.VISIBLE else View.GONE
    }

    private fun setupDetailsTab(c: Customer) {
        if (c.telefon.isNotBlank()) {
            binding.labelTelefon.visibility = View.VISIBLE
            binding.sheetTelefon.visibility = View.VISIBLE
            binding.sheetTelefon.text = c.telefon
        } else {
            binding.labelTelefon.visibility = View.GONE
            binding.sheetTelefon.visibility = View.GONE
        }
        val naechsteTour = callbacks?.getNaechstesTourDatum(c) ?: 0L
        binding.sheetNaechsteTour.text = if (naechsteTour > 0) DateFormatter.formatDate(naechsteTour) else getString(R.string.sheet_kein_termin)
        if (c.notizen.isNotBlank()) {
            binding.labelNotizen.visibility = View.VISIBLE
            binding.sheetNotizen.visibility = View.VISIBLE
            binding.sheetNotizen.text = c.notizen
        } else {
            binding.labelNotizen.visibility = View.GONE
            binding.sheetNotizen.visibility = View.GONE
        }
    }

    private fun setupClickListeners(c: Customer, s: ErledigungSheetState) {
        binding.btnSheetAbholung.setOnClickListener {
            if (s.enableAbholung) {
                callbacks?.onAbholung(c)
                dismiss()
            } else {
                Toast.makeText(requireContext(), getString(R.string.toast_abholung_nur_heute), Toast.LENGTH_LONG).show()
            }
        }
        binding.btnSheetAuslieferung.setOnClickListener {
            if (s.enableAuslieferung) {
                callbacks?.onAuslieferung(c)
                dismiss()
            } else {
                if (!c.abholungErfolgt) Toast.makeText(requireContext(), getString(R.string.toast_auslieferung_nur_nach_abholung), Toast.LENGTH_LONG).show()
                else Toast.makeText(requireContext(), getString(R.string.toast_auslieferung_nur_heute), Toast.LENGTH_LONG).show()
            }
        }
        binding.btnSheetKw.setOnClickListener {
            if (s.enableKw) {
                callbacks?.onKw(c)
                dismiss()
            } else {
                Toast.makeText(requireContext(), getString(R.string.toast_kw_nur_abholung_auslieferung), Toast.LENGTH_LONG).show()
            }
        }
        binding.btnSheetRueckgaengig.setOnClickListener {
            callbacks?.onRueckgaengig(c)
            dismiss()
        }
        binding.btnSheetVerschieben.setOnClickListener {
            callbacks?.onVerschieben(c)
            dismiss()
        }
        binding.btnSheetUrlaub.setOnClickListener {
            callbacks?.onUrlaub(c)
            dismiss()
        }
        binding.sheetTelefon.setOnClickListener {
            val tel = customer?.telefon?.trim() ?: return@setOnClickListener
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel"))
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface ErledigungSheetCallbacks {
        fun onAbholung(customer: Customer)
        fun onAuslieferung(customer: Customer)
        fun onKw(customer: Customer)
        fun onRueckgaengig(customer: Customer)
        fun onVerschieben(customer: Customer)
        fun onUrlaub(customer: Customer)
        fun getNaechstesTourDatum(customer: Customer): Long?
    }

    companion object {
        private const val ARG_VIEW_DATE_MILLIS = "view_date_millis"
        private const val ARG_STATE = "state"

        fun newInstance(customer: Customer, viewDateMillis: Long, state: ErledigungSheetState): ErledigungBottomSheetDialogFragment {
            return ErledigungBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_VIEW_DATE_MILLIS, viewDateMillis)
                    putSerializable(ARG_STATE, state)
                }
                setCustomer(customer)
            }
        }
    }
}
