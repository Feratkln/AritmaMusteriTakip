package com.aritma.musteritakip.ui.bireysel

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aritma.musteritakip.AritmaApplication
import com.aritma.musteritakip.R
import com.aritma.musteritakip.data.local.entity.CustomerEntity
import com.aritma.musteritakip.databinding.ActivityCustomerDetailBinding
import com.aritma.musteritakip.ui.viewmodel.CustomerViewModel
import com.aritma.musteritakip.ui.viewmodel.CustomerViewModelFactory
import com.aritma.musteritakip.util.CustomDialogHelper
import com.aritma.musteritakip.util.DateUtils
import com.aritma.musteritakip.util.MaintenanceStatus
import com.aritma.musteritakip.util.StatusType
import com.aritma.musteritakip.util.WhatsAppHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Müşteri detay ve profil yönetim ekranı.
 * İletişim, adres, cihaz bakım zamanı, randevu oluşturma/düzenleme,
 * müşteri susturma, bakım yenileme, servis notu ekleme ve arşivleme işlemlerini yönetir.
 */
class CustomerDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerDetailBinding

    private val viewModel: CustomerViewModel by viewModels {
        CustomerViewModelFactory((application as AritmaApplication).repository)
    }

    private var currentCustomerId: Long = -1L
    private var currentCustomer: CustomerEntity? = null
    private lateinit var noteAdapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentCustomerId = intent.getLongExtra("CUSTOMER_ID", -1L)
        if (currentCustomerId == -1L) {
            Toast.makeText(this, "Müşteri bulunamadı", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupNotesRecyclerView()
        observeCustomerData()

        // Call Action
        binding.btnCall.setOnClickListener {
            currentCustomer?.let { customer ->
                WhatsAppHelper.openDialer(this, customer.phone)
            }
        }

        // WhatsApp Action
        binding.btnWhatsApp.setOnClickListener {
            currentCustomer?.let { customer ->
                val msg = when (customer.module) {
                    CustomerEntity.MODULE_UV_LAMBA ->
                        "Merhaba ${customer.name} Bey, WATERNEON Su Arıtma Sistemleri'nden iletişime geçiyorum. UV lambanızın değişim ve bakım zamanı ile ilgili bilgilendirmedir."
                    CustomerEntity.MODULE_MINERAL ->
                        "Merhaba ${customer.name} Bey, WATERNEON Su Arıtma Sistemleri'nden iletişime geçiyorum. Mineral filtre kartuşunuzun değişim zamanı ile ilgili bilgilendirmedir."
                    CustomerEntity.MODULE_LOJISTIK ->
                        "Merhaba ${customer.name} Bey, WATERNEON Su Arıtma Sistemleri'nden kurumsal arıtma sisteminizin periyodik bakım zamanı ile ilgili bilgilendirmedir."
                    else ->
                        "Merhaba ${customer.name} Bey, WATERNEON Su Arıtma Sistemleri'nden iletişime geçiyorum. Su arıtma cihazınızın bakım zamanı ile ilgili bilgilendirme mesajıdır."
                }
                WhatsAppHelper.openWhatsAppChat(this, customer.phone, msg)
            }
        }

        // Toggle Mute Action
        binding.btnToggleMute.setOnClickListener {
            currentCustomer?.let { customer ->
                viewModel.toggleMuteStatus(customer.id, customer.isMuted)
                val msg = if (customer.isMuted) "Susturma kaldırıldı 🔔" else "Kişi susturuldu (Bildirim dışı yapıldı) 🔇"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }

        // Set Reservation Action
        binding.btnSetReservation.setOnClickListener {
            currentCustomer?.let { customer ->
                showReservationDialog(customer)
            }
        }

        // Create Service Form Action
        binding.btnCreateServiceForm.setOnClickListener {
            currentCustomer?.let { customer ->
                val intent = Intent(this, com.aritma.musteritakip.ui.serviceform.ServiceFormActivity::class.java).apply {
                    putExtra("CUSTOMER_ID", customer.id)
                    putExtra("CUSTOMER_NAME", customer.fullName)
                    putExtra("CUSTOMER_PHONE", customer.phone)
                    putExtra("CUSTOMER_ADDRESS", "${customer.district} / ${customer.address}")
                    putExtra("FORM_NO", String.format("00%04d", customer.id % 10000))
                }
                startActivity(intent)
            }
        }

        // Add Note Action
        binding.btnAddNote.setOnClickListener { showAddNoteDialog() }

        // Edit Customer Action
        binding.btnEditCustomer.setOnClickListener {
            currentCustomer?.let { customer ->
                val intent = Intent(this, AddEditCustomerActivity::class.java).apply {
                    putExtra("CUSTOMER_ID", customer.id)
                }
                startActivity(intent)
            }
        }

        // Archive Customer Action
        binding.btnArchiveCustomer.setOnClickListener {
            currentCustomer?.let { customer ->
                CustomDialogHelper.showArchiveCustomerDialog(
                    context = this,
                    customerName = customer.fullName
                ) {
                    viewModel.archiveCustomer(currentCustomerId)
                    CustomDialogHelper.showSuccessToast(this, "📦", "Müşteri arşivlendi.")
                    finish()
                }
            }
        }
    }

    private fun setupNotesRecyclerView() {
        noteAdapter = NoteAdapter(
            onDeleteNoteClick = { note ->
                viewModel.deleteNote(note)
                Toast.makeText(this, "Not silindi", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvNotes.layoutManager = LinearLayoutManager(this)
        binding.rvNotes.adapter = noteAdapter
    }

    private fun observeCustomerData() {
        // Observe Customer Details
        lifecycleScope.launch {
            viewModel.getCustomerByIdFlow(currentCustomerId).collectLatest { customer ->
                if (customer == null) return@collectLatest
                currentCustomer = customer

                binding.tvFullName.text = customer.fullName
                if (!customer.companyName.isNullOrBlank()) {
                    binding.tvCompanyName.visibility = View.VISIBLE
                    binding.tvCompanyName.text = "🏢 ${customer.companyName}"
                } else {
                    binding.tvCompanyName.visibility = View.GONE
                }
                binding.tvPhone.text = "📱 ${customer.phone}"
                if (!customer.companyPhone.isNullOrBlank()) {
                    binding.tvCompanyPhone.visibility = View.VISIBLE
                    binding.tvCompanyPhone.text = "☎️ Sabit/Şirket Tel: ${customer.companyPhone}"
                } else {
                    binding.tvCompanyPhone.visibility = View.GONE
                }
                binding.tvDistrictAddress.text = "${customer.district} / ${customer.address.ifBlank { "Açık adres belirtilmedi" }}"
                binding.tvInstallationDate.text = DateUtils.formatDate(customer.installationDate)
                binding.tvMaintenanceDate.text = DateUtils.formatDate(customer.maintenanceDate)

                // Initials
                val firstInitial = customer.name.firstOrNull()?.uppercaseChar() ?: ""
                val lastInitial = customer.surname.firstOrNull()?.uppercaseChar() ?: ""
                binding.tvInitials.text = "$firstInitial$lastInitial"

                // Mute Button Appearance
                if (customer.isMuted) {
                    binding.btnToggleMute.text = "🔔 Sesini Aç"
                    binding.btnToggleMute.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@CustomerDetailActivity, R.color.status_safe_bg))
                    binding.btnToggleMute.setTextColor(ContextCompat.getColor(this@CustomerDetailActivity, R.color.status_safe))
                } else {
                    binding.btnToggleMute.text = "🔇 Sustur"
                    binding.btnToggleMute.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@CustomerDetailActivity, R.color.status_muted_bg))
                    binding.btnToggleMute.setTextColor(ContextCompat.getColor(this@CustomerDetailActivity, R.color.status_muted))
                }

                // Reservation Button Appearance
                if (customer.reservationDate != null) {
                    binding.btnSetReservation.text = "🗓️ Randevu Değiştir"
                } else {
                    binding.btnSetReservation.text = "📅 Randevu Al"
                }

                // Status Badge
                val status = MaintenanceStatus.calculate(customer.maintenanceDate, customer.isMuted)
                when (status.type) {
                    StatusType.MUTED -> {
                        val color = ContextCompat.getColor(this@CustomerDetailActivity, R.color.status_muted)
                        val bg = ContextCompat.getColor(this@CustomerDetailActivity, R.color.status_muted_bg)
                        binding.tvStatusBadge.text = "🔇 Susturuldu"
                        binding.tvStatusBadge.setTextColor(color)
                        binding.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(bg)
                    }
                    StatusType.OVERDUE -> {
                        val color = ContextCompat.getColor(this@CustomerDetailActivity, R.color.status_red)
                        val bg = ContextCompat.getColor(this@CustomerDetailActivity, R.color.status_red_bg)
                        binding.tvStatusBadge.text = "🔴 ${Math.abs(status.daysRemaining)} Gün Geçti"
                        binding.tvStatusBadge.setTextColor(color)
                        binding.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(bg)
                    }
                    StatusType.DUE_SOON -> {
                        val color = ContextCompat.getColor(this@CustomerDetailActivity, R.color.status_orange)
                        val bg = ContextCompat.getColor(this@CustomerDetailActivity, R.color.status_orange_bg)
                        val label = if (status.daysRemaining == 0L) "🟠 BUGÜN!" else "🟠 ${status.daysRemaining} Gün"
                        binding.tvStatusBadge.text = label
                        binding.tvStatusBadge.setTextColor(color)
                        binding.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(bg)
                    }
                    StatusType.SAFE -> {
                        val color = ContextCompat.getColor(this@CustomerDetailActivity, R.color.status_safe)
                        val bg = ContextCompat.getColor(this@CustomerDetailActivity, R.color.status_safe_bg)
                        binding.tvStatusBadge.text = "⚪ ${status.daysRemaining} Gün"
                        binding.tvStatusBadge.setTextColor(color)
                        binding.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(bg)
                    }
                }

                // Setup Maintenance Refresh Options based on Module
                when (customer.module) {
                    CustomerEntity.MODULE_LOJISTIK -> {
                        binding.btnRefreshMaintenanceBireysel.visibility = View.GONE
                        binding.layoutLojistikRefresh.visibility = View.VISIBLE
                        binding.tvRefreshPeriodTitle.text = "✅ BAKIM YAPILDI — SIRADAKİ PERİYOT"

                        binding.btnRefresh3Months.text = "+3 Ay"
                        binding.btnRefresh6Months.text = "+6 Ay"
                        binding.btnRefresh9Months.text = "+9 Ay"
                        binding.btnRefresh12Months.text = "+12 Ay"

                        binding.btnRefresh3Months.setOnClickListener {
                            viewModel.refreshMaintenanceDate(customer.id, 3)
                            CustomDialogHelper.showSuccessToast(this@CustomerDetailActivity, "✅", "Bakım tarihi +3 ay uzatıldı")
                        }
                        binding.btnRefresh6Months.setOnClickListener {
                            viewModel.refreshMaintenanceDate(customer.id, 6)
                            CustomDialogHelper.showSuccessToast(this@CustomerDetailActivity, "✅", "Bakım tarihi +6 ay uzatıldı")
                        }
                        binding.btnRefresh9Months.setOnClickListener {
                            viewModel.refreshMaintenanceDate(customer.id, 9)
                            CustomDialogHelper.showSuccessToast(this@CustomerDetailActivity, "✅", "Bakım tarihi +9 ay uzatıldı")
                        }
                        binding.btnRefresh12Months.setOnClickListener {
                            viewModel.refreshMaintenanceDate(customer.id, 12)
                            CustomDialogHelper.showSuccessToast(this@CustomerDetailActivity, "✅", "Bakım tarihi +12 ay uzatıldı")
                        }
                    }
                    CustomerEntity.MODULE_UV_LAMBA -> {
                        binding.btnRefreshMaintenanceBireysel.visibility = View.VISIBLE
                        binding.layoutLojistikRefresh.visibility = View.GONE
                        binding.btnRefreshMaintenanceBireysel.text = "☀️ UV Lamba Değiştirildi (+1 Yıl)"
                        binding.btnRefreshMaintenanceBireysel.backgroundTintList =
                            ColorStateList.valueOf(ContextCompat.getColor(this@CustomerDetailActivity, R.color.uv_lamba_primary))

                        binding.btnRefreshMaintenanceBireysel.setOnClickListener {
                            CustomDialogHelper.showMaintenanceConfirmDialog(
                                context = this@CustomerDetailActivity,
                                customerName = customer.fullName,
                                district = customer.district
                            ) {
                                viewModel.refreshMaintenanceDate(customer.id, 12)
                                CustomDialogHelper.showSuccessToast(this@CustomerDetailActivity, "☀️", "UV Lamba değişimi kaydedildi! (+1 Yıl)")
                            }
                        }
                    }
                    CustomerEntity.MODULE_MINERAL -> {
                        binding.btnRefreshMaintenanceBireysel.visibility = View.GONE
                        binding.layoutLojistikRefresh.visibility = View.VISIBLE
                        binding.tvRefreshPeriodTitle.text = "💎 MİNERAL DEĞİŞTİRİLDİ — SIRADAKİ PERİYOT"

                        binding.btnRefresh3Months.text = "+1 Yıl"
                        binding.btnRefresh6Months.text = "+2 Yıl"
                        binding.btnRefresh9Months.text = "+3 Yıl"
                        binding.btnRefresh12Months.text = "+5 Yıl"

                        binding.btnRefresh3Months.setOnClickListener {
                            viewModel.refreshMaintenanceDate(customer.id, 12)
                            CustomDialogHelper.showSuccessToast(this@CustomerDetailActivity, "💎", "Mineral filtre değişimi kaydedildi (+1 Yıl)")
                        }
                        binding.btnRefresh6Months.setOnClickListener {
                            viewModel.refreshMaintenanceDate(customer.id, 24)
                            CustomDialogHelper.showSuccessToast(this@CustomerDetailActivity, "💎", "Mineral filtre değişimi kaydedildi (+2 Yıl)")
                        }
                        binding.btnRefresh9Months.setOnClickListener {
                            viewModel.refreshMaintenanceDate(customer.id, 36)
                            CustomDialogHelper.showSuccessToast(this@CustomerDetailActivity, "💎", "Mineral filtre değişimi kaydedildi (+3 Yıl)")
                        }
                        binding.btnRefresh12Months.setOnClickListener {
                            viewModel.refreshMaintenanceDate(customer.id, 60)
                            CustomDialogHelper.showSuccessToast(this@CustomerDetailActivity, "💎", "Mineral filtre değişimi kaydedildi (+5 Yıl)")
                        }
                    }
                    else -> {
                        binding.btnRefreshMaintenanceBireysel.visibility = View.VISIBLE
                        binding.layoutLojistikRefresh.visibility = View.GONE
                        binding.btnRefreshMaintenanceBireysel.text = "✅ Bakım Yapıldı (+1 Yıl)"
                        binding.btnRefreshMaintenanceBireysel.backgroundTintList =
                            ColorStateList.valueOf(ContextCompat.getColor(this@CustomerDetailActivity, R.color.bireysel_primary))

                        binding.btnRefreshMaintenanceBireysel.setOnClickListener {
                            CustomDialogHelper.showMaintenanceConfirmDialog(
                                context = this@CustomerDetailActivity,
                                customerName = customer.fullName,
                                district = customer.district
                            ) {
                                viewModel.refreshMaintenanceDate(customer.id, 12)
                                CustomDialogHelper.showSuccessToast(this@CustomerDetailActivity, "🎉", "Bakım yapıldı! (+1 Yıl)")
                            }
                        }
                    }
                }
            }
        }

        // Observe Notes
        lifecycleScope.launch {
            viewModel.getNotesForCustomer(currentCustomerId).collectLatest { notes ->
                noteAdapter.submitList(notes)
                binding.tvEmptyNotes.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showReservationDialog(customer: CustomerEntity) {
        val calendar = Calendar.getInstance()
        if (customer.reservationDate != null) {
            calendar.timeInMillis = customer.reservationDate
        }

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        calendar.set(Calendar.SECOND, 0)

                        // Input reservation note
                        val noteEditText = EditText(this).apply {
                            hint = "Randevu Notu (Örn: Salı 14:00 filtre değişimi)"
                            setText(customer.reservationNote ?: "")
                            minLines = 2
                            setPadding(48, 32, 48, 32)
                        }

                        AlertDialog.Builder(this)
                            .setTitle("Randevu Notu Ekle")
                            .setView(noteEditText)
                            .setPositiveButton("Randevuyu Kaydet") { _, _ ->
                                val resNote = noteEditText.text.toString().trim()
                                viewModel.setReservation(customer.id, calendar.timeInMillis, resNote)
                                Toast.makeText(this, "Randevu oluşturuldu 🗓️", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("İptal", null)
                            .show()
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showAddNoteDialog() {
        val editText = EditText(this).apply {
            hint = "Servis Notu (Örn: 3'lü filtre seti değişti, 1200 TL)"
            minLines = 3
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Servis Notu Ekle")
            .setView(editText)
            .setPositiveButton("Kaydet") { _, _ ->
                val noteText = editText.text.toString().trim()
                if (noteText.isNotEmpty()) {
                    viewModel.addNote(currentCustomerId, noteText)
                    Toast.makeText(this, "Not kaydedildi", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}
