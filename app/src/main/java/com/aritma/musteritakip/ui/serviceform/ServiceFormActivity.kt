package com.aritma.musteritakip.ui.serviceform

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.aritma.musteritakip.AritmaApplication
import com.aritma.musteritakip.R
import com.aritma.musteritakip.databinding.ActivityServiceFormBinding
import com.aritma.musteritakip.ui.viewmodel.CustomerViewModel
import com.aritma.musteritakip.ui.viewmodel.CustomerViewModelFactory
import com.aritma.musteritakip.util.DateUtils
import com.aritma.musteritakip.util.ServiceFormData
import com.aritma.musteritakip.util.ServiceFormPdfGenerator
import java.io.File
import java.util.Calendar

/**
 * Dijital Servis Bilgi Formu oluşturma ve yönetme ekranı.
 * Müşteri profilinden (bilgiler otomatik çekilerek) veya ana panelden (tamamı elle girilerek)
 * açılabilir; serbest malzeme ve adet satırları, servis raporu ve A4 PDF çıktısı sunar.
 */
class ServiceFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServiceFormBinding

    private val viewModel: CustomerViewModel by viewModels {
        CustomerViewModelFactory((application as AritmaApplication).repository)
    }

    private var customerId: Long = -1L
    private var selectedDateMillis: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServiceFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        readIntentData()
        setupDatePickers()
        setupMaterialRows()
        setupActionButtons()
    }

    private fun readIntentData() {
        customerId = intent.getLongExtra("CUSTOMER_ID", -1L)
        val customerName = intent.getStringExtra("CUSTOMER_NAME") ?: ""
        val customerPhone = intent.getStringExtra("CUSTOMER_PHONE") ?: ""
        val customerAddress = intent.getStringExtra("CUSTOMER_ADDRESS") ?: ""
        val formNo = intent.getStringExtra("FORM_NO") ?: "000341"

        binding.etFormNo.setText(formNo)
        binding.etCustomerName.setText(customerName)
        binding.etCustomerPhone.setText(customerPhone)
        binding.etCustomerAddress.setText(customerAddress)

        // Checkbox only visible if customer is registered in database
        binding.cbSaveToCustomerNotes.visibility = if (customerId != -1L) View.VISIBLE else View.GONE
    }

    private fun setupDatePickers() {
        binding.etServiceDate.setText(DateUtils.formatDate(selectedDateMillis))

        val datePickerListener = View.OnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val selectedCal = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth, 0, 0, 0)
                    }
                    selectedDateMillis = selectedCal.timeInMillis
                    binding.etServiceDate.setText(DateUtils.formatDate(selectedDateMillis))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.etServiceDate.setOnClickListener(datePickerListener)
    }

    private fun setupMaterialRows() {
        // Start with 2 empty manual rows
        addMaterialRow()
        addMaterialRow()

        binding.btnAddMaterialRow.setOnClickListener {
            addMaterialRow()
        }
    }

    private fun addMaterialRow(initialName: String = "", initialQuantity: String = "1 Adet") {
        val rowView = LayoutInflater.from(this).inflate(R.layout.item_material_row, binding.layoutMaterialsContainer, false)

        val etName = rowView.findViewById<EditText>(R.id.etMaterialName)
        val etQty = rowView.findViewById<EditText>(R.id.etMaterialQuantity)
        val btnDelete = rowView.findViewById<ImageButton>(R.id.btnDeleteRow)

        etName.setText(initialName)
        etQty.setText(initialQuantity)

        btnDelete.setOnClickListener {
            binding.layoutMaterialsContainer.removeView(rowView)
        }

        binding.layoutMaterialsContainer.addView(rowView)
    }

    private fun collectFormData(): ServiceFormData {
        val formNo = binding.etFormNo.text.toString().trim()
        val serviceDate = binding.etServiceDate.text.toString().trim()
        val customerName = binding.etCustomerName.text.toString().trim()
        val customerPhone = binding.etCustomerPhone.text.toString().trim()
        val customerAddress = binding.etCustomerAddress.text.toString().trim()
        val serviceReport = binding.etServiceReport.text.toString().trim()
        val technicianName = binding.etTechnicianName.text.toString().trim()

        val materialsList = mutableListOf<Pair<String, String>>()
        val count = binding.layoutMaterialsContainer.childCount
        for (i in 0 until count) {
            val rowView = binding.layoutMaterialsContainer.getChildAt(i)
            val etName = rowView.findViewById<EditText>(R.id.etMaterialName)
            val etQty = rowView.findViewById<EditText>(R.id.etMaterialQuantity)

            val name = etName.text.toString().trim()
            val qty = etQty.text.toString().trim().ifBlank { "1 Adet" }
            if (name.isNotEmpty()) {
                materialsList.add(Pair(name, qty))
            }
        }

        return ServiceFormData(
            formNo = formNo.ifBlank { "000341" },
            serviceDate = serviceDate.ifBlank { DateUtils.formatDate(System.currentTimeMillis()) },
            customerName = customerName.ifBlank { "İsimsiz Müşteri" },
            customerPhone = customerPhone,
            customerAddress = customerAddress,
            materials = materialsList,
            serviceReport = serviceReport,
            technicianName = technicianName.ifBlank { "Muhammed Talha ÇETİNTAŞ" }
        )
    }

    private fun saveReportToCustomerNotesIfRequested(formData: ServiceFormData) {
        if (customerId != -1L && binding.cbSaveToCustomerNotes.isChecked) {
            val sb = StringBuilder("📄 Servis Formu No: ${formData.formNo} (${formData.serviceDate})\n")
            if (formData.materials.isNotEmpty()) {
                sb.append("Kullanılan Malzemeler: ")
                sb.append(formData.materials.joinToString(", ") { "${it.first} (${it.second})" })
                sb.append("\n")
            }
            if (formData.serviceReport.isNotBlank()) {
                sb.append("Rapor: ${formData.serviceReport}")
            }
            viewModel.addNote(customerId, sb.toString())
        }
    }

    private fun setupActionButtons() {
        // 1. PDF Oluştur & Paylaş
        binding.btnGeneratePdf.setOnClickListener {
            val data = collectFormData()
            val pdfFile = ServiceFormPdfGenerator.generatePdf(this, data)
            if (pdfFile != null) {
                saveReportToCustomerNotesIfRequested(data)
                Toast.makeText(this, "Servis Bilgi Formu PDF oluşturuldu ✅", Toast.LENGTH_SHORT).show()
                ServiceFormPdfGenerator.sharePdfFile(this, pdfFile, "Servis Bilgi Formu - ${data.customerName}")
            } else {
                Toast.makeText(this, "PDF oluşturulurken hata meydana geldi!", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. Doğrudan WhatsApp ile Gönder
        binding.btnSendWhatsApp.setOnClickListener {
            val data = collectFormData()
            val pdfFile = ServiceFormPdfGenerator.generatePdf(this, data)
            if (pdfFile != null) {
                saveReportToCustomerNotesIfRequested(data)
                ServiceFormPdfGenerator.sendPdfViaWhatsApp(this, pdfFile, data.customerPhone, data.customerName)
            } else {
                Toast.makeText(this, "PDF oluşturulurken hata meydana geldi!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
