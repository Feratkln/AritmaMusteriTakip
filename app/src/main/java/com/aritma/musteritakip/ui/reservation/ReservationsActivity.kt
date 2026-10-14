package com.aritma.musteritakip.ui.reservation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aritma.musteritakip.AritmaApplication
import com.aritma.musteritakip.databinding.ActivityReservationsBinding
import com.aritma.musteritakip.ui.bireysel.CustomerDetailActivity
import com.aritma.musteritakip.ui.viewmodel.CustomerViewModel
import com.aritma.musteritakip.ui.viewmodel.CustomerViewModelFactory
import com.aritma.musteritakip.R
import com.aritma.musteritakip.util.CustomDialogHelper
import com.aritma.musteritakip.util.WhatsAppHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ReservationTimeFilter {
    ALL,
    TODAY,
    TOMORROW,
    THIS_WEEK
}

/**
 * İleri tarihli randevusu olan müşterileri kronolojik listeleyen Activity.
 * Tümü, Bugün, Yarın ve Bu Hafta zaman filtreleri ile randevu tamamlama
 * veya randevu iptal işlemlerini yönetir.
 */
class ReservationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReservationsBinding

    private val viewModel: CustomerViewModel by viewModels {
        CustomerViewModelFactory((application as AritmaApplication).repository)
    }

    private lateinit var adapter: ReservationAdapter
    private val timeFilterFlow = MutableStateFlow(ReservationTimeFilter.ALL)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReservationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        setupFilters()
        observeReservations()
    }

    private fun setupFilters() {
        binding.chipGroupReservationFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.chipResToday -> ReservationTimeFilter.TODAY
                R.id.chipResTomorrow -> ReservationTimeFilter.TOMORROW
                R.id.chipResThisWeek -> ReservationTimeFilter.THIS_WEEK
                else -> ReservationTimeFilter.ALL
            }
            timeFilterFlow.value = filter
        }
    }

    private fun setupRecyclerView() {
        adapter = ReservationAdapter(
            onItemClick = { customer ->
                val intent = Intent(this, CustomerDetailActivity::class.java).apply {
                    putExtra("CUSTOMER_ID", customer.id)
                }
                startActivity(intent)
            },
            onCallClick = { customer ->
                WhatsAppHelper.openDialer(this, customer.phone)
            },
            onWhatsAppClick = { customer ->
                val msg = "Merhaba ${customer.name} Bey, WATERNEON Su Arıtma Sistemleri randevunuz ile ilgili iletişime geçiyorum."
                WhatsAppHelper.openWhatsAppChat(this, customer.phone, msg)
            },
            onCompleteMaintenanceClick = { customer ->
                CustomDialogHelper.showMaintenanceConfirmDialog(
                    context = this,
                    customerName = customer.fullName,
                    district = customer.district
                ) {
                    viewModel.refreshMaintenanceDate(customer.id, 12)
                    CustomDialogHelper.showSuccessToast(this, "🎉", "Bakım yapıldı ve randevu tamamlandı!")
                }
            },
            onCancelReservationClick = { customer ->
                CustomDialogHelper.showCancelReservationDialog(
                    context = this,
                    customerName = customer.fullName
                ) {
                    viewModel.clearReservation(customer.id)
                    CustomDialogHelper.showSuccessToast(this, "❌", "Randevu iptal edildi.")
                }
            }
        )

        binding.rvReservations.layoutManager = LinearLayoutManager(this)
        binding.rvReservations.adapter = adapter
    }

    private fun observeReservations() {
        lifecycleScope.launch {
            viewModel.reservationsList.combine(timeFilterFlow) { list, filter ->
                when (filter) {
                    ReservationTimeFilter.ALL -> list
                    ReservationTimeFilter.TODAY -> {
                        val todayStart = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis

                        val todayEnd = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.timeInMillis

                        list.filter { customer ->
                            val date = customer.reservationDate ?: 0L
                            date in todayStart..todayEnd
                        }
                    }
                    ReservationTimeFilter.TOMORROW -> {
                        val tomorrowStart = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis

                        val tomorrowEnd = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 1)
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.timeInMillis

                        list.filter { customer ->
                            val date = customer.reservationDate ?: 0L
                            date in tomorrowStart..tomorrowEnd
                        }
                    }
                    ReservationTimeFilter.THIS_WEEK -> {
                        val todayStart = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis

                        val next7Days = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 7)
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                        }.timeInMillis

                        list.filter { customer ->
                            val date = customer.reservationDate ?: 0L
                            date in todayStart..next7Days
                        }
                    }
                }
            }.collectLatest { filtered ->
                adapter.submitList(filtered)
                binding.layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
}
