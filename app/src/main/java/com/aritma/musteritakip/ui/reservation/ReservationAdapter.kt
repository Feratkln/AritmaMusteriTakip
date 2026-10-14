package com.aritma.musteritakip.ui.reservation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aritma.musteritakip.data.local.entity.CustomerEntity
import com.aritma.musteritakip.databinding.ItemReservationCardBinding
import com.aritma.musteritakip.util.DateUtils

/**
 * Randevu ve rezervasyon kartlarını bağlayan RecyclerView adaptörü.
 * Randevu tarihi, notu, doğrudan arama, WhatsApp mesajı,
 * bakım tamamlama ve randevu iptal butonlarını yönetir.
 */
class ReservationAdapter(
    private val onItemClick: (CustomerEntity) -> Unit,
    private val onCallClick: (CustomerEntity) -> Unit,
    private val onWhatsAppClick: (CustomerEntity) -> Unit,
    private val onCompleteMaintenanceClick: (CustomerEntity) -> Unit,
    private val onCancelReservationClick: (CustomerEntity) -> Unit
) : ListAdapter<CustomerEntity, ReservationAdapter.ReservationViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservationViewHolder {
        val binding = ItemReservationCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReservationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReservationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReservationViewHolder(private val binding: ItemReservationCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(customer: CustomerEntity) {
            binding.tvCustomerName.text = customer.fullName
            binding.tvAddress.text = "📍 ${customer.district} / ${customer.address.ifBlank { "Adres belirtilmedi" }}"

            val resDate = customer.reservationDate
            binding.tvReservationDate.text = if (resDate != null) {
                "🗓️ ${DateUtils.formatDateTime(resDate)}"
            } else {
                "🗓️ Randevu Tarihi Yok"
            }

            if (!customer.reservationNote.isNullOrBlank()) {
                binding.tvReservationNote.visibility = View.VISIBLE
                binding.tvReservationNote.text = "📝 Not: ${customer.reservationNote}"
            } else {
                binding.tvReservationNote.visibility = View.GONE
            }

            binding.cardReservation.setOnClickListener { onItemClick(customer) }
            binding.btnCall.setOnClickListener { onCallClick(customer) }
            binding.btnWhatsApp.setOnClickListener { onWhatsAppClick(customer) }
            binding.btnCompleteMaintenance.setOnClickListener { onCompleteMaintenanceClick(customer) }
            binding.btnCancelReservation.setOnClickListener { onCancelReservationClick(customer) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<CustomerEntity>() {
        override fun areItemsTheSame(oldItem: CustomerEntity, newItem: CustomerEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CustomerEntity, newItem: CustomerEntity): Boolean {
            return oldItem == newItem
        }
    }
}
