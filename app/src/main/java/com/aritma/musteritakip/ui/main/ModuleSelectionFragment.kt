package com.aritma.musteritakip.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.aritma.musteritakip.AritmaApplication
import com.aritma.musteritakip.R
import com.aritma.musteritakip.data.local.entity.CustomerEntity
import com.aritma.musteritakip.databinding.FragmentModuleSelectionBinding
import com.aritma.musteritakip.ui.backup.BackupRestoreActivity
import com.aritma.musteritakip.ui.bireysel.CustomerListFragment
import com.aritma.musteritakip.ui.lojistik.LojistikCustomerListFragment
import com.aritma.musteritakip.ui.reservation.ReservationsActivity
import com.aritma.musteritakip.ui.serviceform.ServiceFormActivity
import com.aritma.musteritakip.ui.viewmodel.CustomerViewModel
import com.aritma.musteritakip.ui.viewmodel.CustomerViewModelFactory
import com.aritma.musteritakip.ui.viewmodel.StatusFilter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Uygulama karşılama ve ana modül seçim ekranı (Dashboard).
 * Canlı günün özeti (bugünkü randevular ve acil bakımlar),
 * Bireysel, Lojistik, UV Lamba ve Mineral modül kartları ile yedekleme ekranına hızlı erişim sağlar.
 */
class ModuleSelectionFragment : Fragment() {

    private var _binding: FragmentModuleSelectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CustomerViewModel by activityViewModels {
        CustomerViewModelFactory((requireActivity().application as AritmaApplication).repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModuleSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = getString(R.string.app_name)
            setDisplayHomeAsUpEnabled(false)
        }

        // Customer count badges
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.bireyselCount.collectLatest { count ->
                binding.tvBireyselCount.text = "$count Kayıtlı Müşteri"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.lojistikCount.collectLatest { count ->
                binding.tvLojistikCount.text = "$count Kurumsal Müşteri"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uvLambaCount.collectLatest { count ->
                binding.tvUvLambaCount.text = "$count Kayıtlı Değişim"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.mineralCount.collectLatest { count ->
                binding.tvMineralCount.text = "$count Kayıtlı Değişim"
            }
        }

        // Live Dashboard metrics (Today's reservations & Urgent maintenance)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.todayReservationsCount.collectLatest { count ->
                binding.tvTodayReservationsCount.text = "$count Randevu"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.urgentCustomerCount.collectLatest { count ->
                binding.tvUrgentMaintenanceCount.text = "$count Müşteri"
            }
        }

        binding.cardTodayReservations.setOnClickListener {
            startActivity(Intent(requireContext(), ReservationsActivity::class.java))
        }

        binding.cardUrgentMaintenance.setOnClickListener {
            viewModel.setModule(CustomerEntity.MODULE_BIREYSEL)
            viewModel.setStatusFilter(StatusFilter.OVERDUE_ONLY)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CustomerListFragment.newInstance(CustomerEntity.MODULE_BIREYSEL))
                .addToBackStack("urgent_customers")
                .commit()
        }

        // Module 1: Bireysel Müşteriler
        val openBireyselListener = View.OnClickListener {
            viewModel.setModule(CustomerEntity.MODULE_BIREYSEL)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CustomerListFragment.newInstance(CustomerEntity.MODULE_BIREYSEL))
                .addToBackStack("bireysel")
                .commit()
        }
        binding.btnOpenBireysel.setOnClickListener(openBireyselListener)
        binding.cardBireysel.setOnClickListener(openBireyselListener)

        // Module 2: Lojistik / Kurumsal Müşteriler
        val openLojistikListener = View.OnClickListener {
            viewModel.setModule(CustomerEntity.MODULE_LOJISTIK)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, LojistikCustomerListFragment())
                .addToBackStack("lojistik")
                .commit()
        }
        binding.btnOpenLojistik.setOnClickListener(openLojistikListener)
        binding.cardLojistik.setOnClickListener(openLojistikListener)

        // Module 3: UV Lamba Değişimi
        val openUvLambaListener = View.OnClickListener {
            viewModel.setModule(CustomerEntity.MODULE_UV_LAMBA)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CustomerListFragment.newInstance(CustomerEntity.MODULE_UV_LAMBA))
                .addToBackStack("uv_lamba")
                .commit()
        }
        binding.btnOpenUvLamba.setOnClickListener(openUvLambaListener)
        binding.cardUvLamba.setOnClickListener(openUvLambaListener)

        // Module 4: Mineral Değişimi
        val openMineralListener = View.OnClickListener {
            viewModel.setModule(CustomerEntity.MODULE_MINERAL)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CustomerListFragment.newInstance(CustomerEntity.MODULE_MINERAL))
                .addToBackStack("mineral")
                .commit()
        }
        binding.btnOpenMineral.setOnClickListener(openMineralListener)
        binding.cardMineral.setOnClickListener(openMineralListener)

        // Service Form Generator (Blank manual form from dashboard)
        binding.btnCreateServiceForm.setOnClickListener {
            startActivity(Intent(requireContext(), ServiceFormActivity::class.java))
        }

        // Backup & Restore Action
        binding.btnBackupRestore.setOnClickListener {
            startActivity(Intent(requireContext(), BackupRestoreActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
