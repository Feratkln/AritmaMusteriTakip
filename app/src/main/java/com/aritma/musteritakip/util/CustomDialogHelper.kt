package com.aritma.musteritakip.util

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.aritma.musteritakip.R
import com.aritma.musteritakip.databinding.DialogConfirmActionBinding
import com.aritma.musteritakip.databinding.LayoutCustomToastBinding

/**
 * Modern onay iletişim pencereleri (Dialog) ve özel tasarım Toast bildirimlerini
 * yöneten yardımcı nesne sınıfı.
 */
object CustomDialogHelper {

    fun showMaintenanceConfirmDialog(
        context: Context,
        customerName: String,
        district: String,
        onConfirm: () -> Unit
    ) {
        val binding = DialogConfirmActionBinding.inflate(LayoutInflater.from(context))
        
        binding.tvHeaderIcon.text = "🛠️"
        binding.tvDialogTitle.text = "Bakımı Tamamla"
        binding.tvDialogMessage.text = "Müşteri için bakım yapıldı olarak işlenecek ve sıradaki bakım tarihi bugünden itibaren 1 yıl uzatılacaktır."
        binding.tvCustomerName.text = customerName
        binding.tvCustomerSubText.text = "📍 $district • Gelecek Bakım: +1 Yıl"
        binding.btnConfirm.text = "✅ Evet, Yapıldı"
        binding.btnConfirm.backgroundTintList = ContextCompat.getColorStateList(context, R.color.bireysel_primary)

        val dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        binding.btnConfirm.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        dialog.show()
    }

    fun showCancelReservationDialog(
        context: Context,
        customerName: String,
        onConfirm: () -> Unit
    ) {
        val binding = DialogConfirmActionBinding.inflate(LayoutInflater.from(context))

        binding.tvHeaderIcon.text = "🗓️"
        binding.tvDialogTitle.text = "Randevuyu İptal Et"
        binding.tvDialogMessage.text = "Bu müşteri için oluşturulmuş takvim randevusu silinecektir. Müşteri ana listede kalmaya devam eder."
        binding.tvCustomerName.text = customerName
        binding.tvCustomerSubText.text = "❌ Randevu Silinecek"
        binding.btnConfirm.text = "🗑️ İptal Et"
        binding.btnConfirm.backgroundTintList = ContextCompat.getColorStateList(context, R.color.status_red)

        val dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        binding.btnConfirm.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        dialog.show()
    }

    fun showArchiveCustomerDialog(
        context: Context,
        customerName: String,
        onConfirm: () -> Unit
    ) {
        val binding = DialogConfirmActionBinding.inflate(LayoutInflater.from(context))

        binding.tvHeaderIcon.text = "📦"
        binding.tvDialogTitle.text = "Müşteriyi Arşivle"
        binding.tvDialogMessage.text = "Müşteri pasif duruma getirilecek ve ana listeden gizlenecektir."
        binding.tvCustomerName.text = customerName
        binding.tvCustomerSubText.text = "⚠️ Müşteri Arşivlenecek"
        binding.btnConfirm.text = "📦 Arşivle"
        binding.btnConfirm.backgroundTintList = ContextCompat.getColorStateList(context, R.color.status_red)

        val dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        binding.btnConfirm.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        dialog.show()
    }

    fun showSuccessToast(context: Context, icon: String = "✅", message: String) {
        val binding = LayoutCustomToastBinding.inflate(LayoutInflater.from(context))
        binding.tvToastIcon.text = icon
        binding.tvToastMessage.text = message

        Toast(context).apply {
            duration = Toast.LENGTH_SHORT
            view = binding.root
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
            show()
        }
    }
}
