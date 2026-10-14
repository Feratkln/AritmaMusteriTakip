package com.aritma.musteritakip.ui.bireysel

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.aritma.musteritakip.AritmaApplication
import com.aritma.musteritakip.R
import com.aritma.musteritakip.data.local.entity.CustomerEntity
import com.aritma.musteritakip.databinding.ActivityAddEditCustomerBinding
import com.aritma.musteritakip.ui.viewmodel.CustomerViewModel
import com.aritma.musteritakip.ui.viewmodel.CustomerViewModelFactory
import com.aritma.musteritakip.util.DateUtils
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Yeni müşteri ekleme ve var olan müşteri bilgilerini düzenleme ekranı.
 * Bireysel ve Lojistik modüllerine göre dinamik form alanlarını (şirket adı/telefonu,
 * modüle özel hızlı bakım periyotları: 3/6/9/12 ay veya 6 ay/1/2/3 yıl) yönetir.
 */
class AddEditCustomerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditCustomerBinding

    private val viewModel: CustomerViewModel by viewModels {
        CustomerViewModelFactory((application as AritmaApplication).repository)
    }

    private var customerId: Long = -1L
    private var module: String = CustomerEntity.MODULE_BIREYSEL
    private var editingCustomer: CustomerEntity? = null

    private var selectedInstallationDate: Long = System.currentTimeMillis()
    private var selectedMaintenanceDate: Long = DateUtils.addYears(System.currentTimeMillis(), 1)

    private val districtList = mutableListOf<String>()
    private var districtAdapter: ArrayAdapter<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditCustomerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        customerId = intent.getLongExtra("CUSTOMER_ID", -1L)
        module = intent.getStringExtra("MODULE") ?: CustomerEntity.MODULE_BIREYSEL

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        applyModuleUi()

        if (customerId != -1L) {
            binding.toolbar.title = "Müşteriyi Düzenle"
            loadCustomerForEditing()
        } else {
            binding.toolbar.title = if (module == CustomerEntity.MODULE_LOJISTIK) "Yeni Kurumsal Müşteri" else "Yeni Müşteri"
            updateDateViews()
        }

        observeDistricts()
        setupDatePickers()

        binding.btnAddDistrict.setOnClickListener { showAddDistrictDialog() }
        binding.btnSaveCustomer.setOnClickListener { saveCustomer() }
    }

    /** Corporate card is only for Lojistik; quick presets differ per module. */
    private fun applyModuleUi() {
        binding.cardCompany.visibility =
            if (module == CustomerEntity.MODULE_LOJISTIK) View.VISIBLE else View.GONE
        setupPresetButtons(module)
    }

    private fun loadCustomerForEditing() {
        lifecycleScope.launch {
            viewModel.getCustomerByIdFlow(customerId).collectLatest { customer ->
                if (customer == null) return@collectLatest
                editingCustomer = customer
                if (module != customer.module) {
                    module = customer.module
                    applyModuleUi()
                }

                binding.etCompanyName.setText(customer.companyName ?: "")
                binding.etCompanyPhone.setText(customer.companyPhone ?: "")
                binding.etName.setText(customer.name)
                binding.etSurname.setText(customer.surname)
                binding.etPhone.setText(customer.phone)
                binding.etAddress.setText(customer.address)

                selectedInstallationDate = customer.installationDate
                selectedMaintenanceDate = customer.maintenanceDate
                updateDateViews()

                // Set Spinner Selection
                val index = districtList.indexOf(customer.district)
                if (index >= 0) {
                    binding.spinnerDistrict.setSelection(index)
                }
            }
        }
    }

    private fun observeDistricts() {
        lifecycleScope.launch {
            viewModel.allDistricts.collectLatest { districts ->
                districtList.clear()
                districtList.addAll(districts.map { it.name })

                if (districtAdapter == null) {
                    districtAdapter = ArrayAdapter(
                        this@AddEditCustomerActivity,
                        android.R.layout.simple_spinner_item,
                        districtList
                    ).apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }
                    binding.spinnerDistrict.adapter = districtAdapter
                } else {
                    districtAdapter?.notifyDataSetChanged()
                }

                editingCustomer?.let { customer ->
                    val idx = districtList.indexOf(customer.district)
                    if (idx >= 0) binding.spinnerDistrict.setSelection(idx)
                }
            }
        }
    }

    private fun setupDatePickers() {
        val showInstallationPicker = View.OnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedInstallationDate }
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val selectedCal = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth, 0, 0, 0)
                    }
                    selectedInstallationDate = selectedCal.timeInMillis
                    if (customerId == -1L) {
                        selectedMaintenanceDate = DateUtils.addYears(selectedInstallationDate, 1)
                    }
                    updateDateViews()
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.etInstallationDate.setOnClickListener(showInstallationPicker)
        binding.tilInstallationDate.setOnClickListener(showInstallationPicker)

        val showMaintenancePicker = View.OnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedMaintenanceDate }
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val selectedCal = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth, 0, 0, 0)
                    }
                    selectedMaintenanceDate = selectedCal.timeInMillis
                    updateDateViews()
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.etMaintenanceDate.setOnClickListener(showMaintenancePicker)
        binding.tilMaintenanceDate.setOnClickListener(showMaintenancePicker)

        // Quick Period Presets based on module (Bireysel = 6M, 1Y, 2Y, 3Y; Lojistik = 3M, 6M, 9M, 12M)
        setupPresetButtons(module)
    }

    private fun setupPresetButtons(module: String) {
        val isLojistik = module == CustomerEntity.MODULE_LOJISTIK
        if (isLojistik) {
            binding.btnPreset6M.text = "+3 Ay"
            binding.btnPreset1Y.text = "+6 Ay"
            binding.btnPreset2Y.text = "+9 Ay"
            binding.btnPreset3Y.text = "+12 Ay"
        } else {
            binding.btnPreset6M.text = "+6 Ay"
            binding.btnPreset1Y.text = "+1 Yıl"
            binding.btnPreset2Y.text = "+2 Yıl"
            binding.btnPreset3Y.text = "+3 Yıl"
        }

        fun selectPresetButton(selectedBtn: MaterialButton, value: Int, isYears: Boolean) {
            val buttons = listOf(binding.btnPreset6M, binding.btnPreset1Y, binding.btnPreset2Y, binding.btnPreset3Y)
            val primaryColor = ContextCompat.getColor(this, R.color.bireysel_primary)
            val defaultTextColor = ContextCompat.getColor(this, R.color.text_primary)
            val whiteColor = ContextCompat.getColor(this, R.color.white)

            buttons.forEach { btn ->
                if (btn == selectedBtn) {
                    btn.backgroundTintList = ColorStateList.valueOf(primaryColor)
                    btn.setTextColor(whiteColor)
                    btn.strokeWidth = 0
                } else {
                    btn.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                    btn.setTextColor(defaultTextColor)
                    btn.strokeWidth = 2
                }
            }

            selectedMaintenanceDate = if (isYears) {
                DateUtils.addYears(selectedInstallationDate, value)
            } else {
                DateUtils.addMonths(selectedInstallationDate, value)
            }
            updateDateViews()
        }

        if (isLojistik) {
            binding.btnPreset6M.setOnClickListener { selectPresetButton(binding.btnPreset6M, 3, false) }
            binding.btnPreset1Y.setOnClickListener { selectPresetButton(binding.btnPreset1Y, 6, false) }
            binding.btnPreset2Y.setOnClickListener { selectPresetButton(binding.btnPreset2Y, 9, false) }
            binding.btnPreset3Y.setOnClickListener { selectPresetButton(binding.btnPreset3Y, 12, false) }
        } else {
            binding.btnPreset6M.setOnClickListener { selectPresetButton(binding.btnPreset6M, 6, false) }
            binding.btnPreset1Y.setOnClickListener { selectPresetButton(binding.btnPreset1Y, 1, true) }
            binding.btnPreset2Y.setOnClickListener { selectPresetButton(binding.btnPreset2Y, 2, true) }
            binding.btnPreset3Y.setOnClickListener { selectPresetButton(binding.btnPreset3Y, 3, true) }
        }
    }

    private fun updateDateViews() {
        binding.etInstallationDate.setText(DateUtils.formatDate(selectedInstallationDate))
        binding.etMaintenanceDate.setText(DateUtils.formatDate(selectedMaintenanceDate))
    }

    private fun showAddDistrictDialog() {
        val editText = EditText(this).apply {
            hint = "Semt Adı"
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
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

    private fun saveCustomer() {
        val companyName = binding.etCompanyName.text.toString().trim()
        val companyPhone = binding.etCompanyPhone.text.toString().trim()
        val name = binding.etName.text.toString().trim()
        val surname = binding.etSurname.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val selectedDistrict = binding.spinnerDistrict.selectedItem as? String ?: "Diğer"
        val initialNote = binding.etInitialNote.text.toString().trim()

        if (name.isEmpty() || surname.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Lütfen Ad, Soyad ve Telefon alanlarını doldurun", Toast.LENGTH_SHORT).show()
            return
        }

        val companyVal = companyName.ifBlank { null }
        val companyPhoneVal = companyPhone.ifBlank { null }

        if (customerId == -1L) {
            // Insert New Customer
            viewModel.addCustomer(
                module = module,
                name = name,
                surname = surname,
                phone = phone,
                address = address,
                district = selectedDistrict,
                installationDate = selectedInstallationDate,
                maintenanceDate = selectedMaintenanceDate,
                companyName = companyVal,
                companyPhone = companyPhoneVal,
                initialNote = initialNote.ifBlank { null }
            ) {
                Toast.makeText(this, "Yeni müşteri kaydedildi ✅", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            // Update Existing Customer
            val updated = editingCustomer?.copy(
                companyName = companyVal,
                companyPhone = companyPhoneVal,
                name = name,
                surname = surname,
                phone = phone,
                address = address,
                district = selectedDistrict,
                installationDate = selectedInstallationDate,
                maintenanceDate = selectedMaintenanceDate
            ) ?: return

            viewModel.updateCustomer(updated) {
                if (initialNote.isNotEmpty()) {
                    viewModel.addNote(customerId, initialNote)
                }
                Toast.makeText(this, "Müşteri bilgileri güncellendi ✅", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
