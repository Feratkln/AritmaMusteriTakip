package com.aritma.musteritakip.ui.bireysel

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aritma.musteritakip.AritmaApplication
import com.aritma.musteritakip.R
import com.aritma.musteritakip.data.local.entity.CustomerEntity
import com.aritma.musteritakip.databinding.FragmentCustomerListBinding
import com.aritma.musteritakip.ui.viewmodel.CustomerViewModel
import com.aritma.musteritakip.ui.viewmodel.CustomerViewModelFactory
import com.aritma.musteritakip.ui.viewmodel.SortOption
import com.aritma.musteritakip.ui.viewmodel.StatusFilter
import com.aritma.musteritakip.util.WhatsAppHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Müşteri listesi ana ekranı Fragment bileşeni.
 * Arama, ilçe filtreleme, bakım durumu çipleri (Tümü, Gecikenler, Yaklaşanlar, Güvenliler, Susturulanlar, Randevulular),
 * sıralama seçenekleri ve yeni müşteri ekleme eylemlerini sunar.
 */
class CustomerListFragment : Fragment() {

    private var _binding: FragmentCustomerListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CustomerViewModel by activityViewModels {
        CustomerViewModelFactory((requireActivity().application as AritmaApplication).repository)
    }

    private lateinit var adapter: CustomerAdapter
    private var districtAdapter: ArrayAdapter<String>? = null
    private val districtList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = "Bireysel Müşteriler"
            setDisplayHomeAsUpEnabled(true)
        }

        setupRecyclerView()
        setupSearchAndFilters()

        // Observe Customers
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.customers.collectLatest { list ->
                adapter.submitList(list)
                binding.tvCustomerCount.text = "${list.size} Müşteri Listelendi"
                binding.layoutEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        // Observe Districts for Spinner
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allDistricts.collectLatest { districts ->
                districtList.clear()
                districtList.add("Tüm Semtler")
                districtList.addAll(districts.map { it.name })

                if (districtAdapter == null) {
                    districtAdapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        districtList
                    ).apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }
                    binding.spinnerDistrict.adapter = districtAdapter
                } else {
                    districtAdapter?.notifyDataSetChanged()
                }
            }
        }

        // FAB to Add Customer
        binding.fabAddCustomer.setOnClickListener {
            val intent = Intent(requireContext(), AddEditCustomerActivity::class.java).apply {
                putExtra("MODULE", CustomerEntity.MODULE_BIREYSEL)
            }
            startActivity(intent)
        }

        // Sort Dialog Button
        binding.btnSort.setOnClickListener { showSortDialog() }

        // Add New District Button
        binding.btnAddDistrict.setOnClickListener { showAddDistrictDialog() }
    }

    private fun setupRecyclerView() {
        adapter = CustomerAdapter(
            onItemClick = { customer ->
                val intent = Intent(requireContext(), CustomerDetailActivity::class.java).apply {
                    putExtra("CUSTOMER_ID", customer.id)
                }
                startActivity(intent)
            },
            onCallClick = { customer ->
                WhatsAppHelper.openDialer(requireContext(), customer.phone)
            },
            onWhatsAppClick = { customer ->
                val welcomeMsg = "Merhaba ${customer.name} Bey, WATERNEON Su Arıtma Sistemleri'nden iletişime geçiyorum. Cihazınızın bakım zamanı hakkında bilgilendirmedir."
                WhatsAppHelper.openWhatsAppChat(requireContext(), customer.phone, welcomeMsg)
            }
        )

        binding.rvCustomers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCustomers.adapter = adapter
    }

    private fun setupSearchAndFilters() {
        // Search text watcher
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // District spinner selection listener
        binding.spinnerDistrict.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = districtList.getOrNull(position)
                viewModel.setDistrictFilter(if (selected == "Tüm Semtler") null else selected)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Status Filter Chips
        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.chipOverdue -> StatusFilter.OVERDUE_ONLY
                R.id.chipDueSoon -> StatusFilter.DUE_SOON_ONLY
                R.id.chipReserved -> StatusFilter.RESERVED_ONLY
                R.id.chipMuted -> StatusFilter.MUTED_ONLY
                else -> StatusFilter.ALL
            }
            viewModel.setStatusFilter(filter)
        }
    }

    private fun showSortDialog() {
        val options = arrayOf("Bakım Tarihine Göre (En Yakın Üstte)", "İsme Göre (A-Z)", "Semte Göre")
        AlertDialog.Builder(requireContext())
            .setTitle("Sıralama Seçeneği")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        viewModel.setSortOption(SortOption.MAINTENANCE_DATE_ASC)
                        binding.btnSort.text = "🔃 Bakım Tarihi"
                    }
                    1 -> {
                        viewModel.setSortOption(SortOption.NAME_ASC)
                        binding.btnSort.text = "🔃 İsme Göre"
                    }
                    2 -> {
                        viewModel.setSortOption(SortOption.DISTRICT_ASC)
                        binding.btnSort.text = "🔃 Semte Göre"
                    }
                }
            }
            .show()
    }

    private fun showAddDistrictDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Semt Adı (Örn: Arnavutköy)"
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Yeni Semt Ekle")
            .setView(editText)
            .setPositiveButton("Ekle") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.addNewDistrict(name)
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
