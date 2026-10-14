package com.aritma.musteritakip.ui.bireysel

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aritma.musteritakip.R
import com.aritma.musteritakip.data.local.entity.CustomerEntity
import com.aritma.musteritakip.databinding.ItemCustomerCardBinding
import com.aritma.musteritakip.util.DateUtils
import com.aritma.musteritakip.util.MaintenanceStatus
import com.aritma.musteritakip.util.StatusType
import com.aritma.musteritakip.util.WhatsAppHelper

/**
 * Müşteri listesi öğelerini bağlayan RecyclerView adaptörü.
 * Kart üzerinde durum rozetleri, hızlı telefon araması ve WhatsApp mesaj butonlarını
 * dinamik durum renklerine göre render eder.
 */
class CustomerAdapter(
    private val onItemClick: (CustomerEntity) -> Unit,
    private val onCallClick: (CustomerEntity) -> Unit,
    private val onWhatsAppClick: (CustomerEntity) -> Unit
) : ListAdapter<CustomerEntity, CustomerAdapter.CustomerViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val binding = ItemCustomerCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CustomerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CustomerViewHolder(private val binding: ItemCustomerCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(customer: CustomerEntity) {
            val context = binding.root.context
            binding.tvFullName.text = customer.fullName
            if (!customer.companyName.isNullOrBlank()) {
                binding.tvCompanyName.visibility = android.view.View.VISIBLE
                binding.tvCompanyName.text = "🏢 ${customer.companyName}"
            } else {
                binding.tvCompanyName.visibility = android.view.View.GONE
            }
            binding.tvPhone.text = customer.phone
            binding.tvDistrict.text = customer.district
            binding.tvAddress.text = "📍 ${customer.address.ifBlank { customer.district }}"
            binding.tvMaintenanceDate.text = "🔧 Bakım: ${DateUtils.formatDate(customer.maintenanceDate)}"

            // Initials Avatar (e.g. "Ahmet Yılmaz" -> "AY")
            val firstInitial = customer.name.firstOrNull()?.uppercaseChar() ?: ""
            val lastInitial = customer.surname.firstOrNull()?.uppercaseChar() ?: ""
            binding.tvInitials.text = "$firstInitial$lastInitial"

            // Maintenance Status Calculation & Color Coding
            val status = MaintenanceStatus.calculate(customer.maintenanceDate, customer.isMuted)
            when (status.type) {
                StatusType.MUTED -> {
                    val mutedColor = ContextCompat.getColor(context, R.color.status_muted)
                    val mutedBg = ContextCompat.getColor(context, R.color.status_muted_bg)
                    binding.tvStatusBadge.text = "🔇 Susturuldu"
                    binding.tvStatusBadge.setTextColor(mutedColor)
                    binding.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(mutedBg)
                    binding.cardCustomer.strokeColor = mutedColor
                }
                StatusType.OVERDUE -> {
                    val redColor = ContextCompat.getColor(context, R.color.status_red)
                    val redBg = ContextCompat.getColor(context, R.color.status_red_bg)
                    binding.tvStatusBadge.text = "🔴 ${Math.abs(status.daysRemaining)} Gün Geçti"
                    binding.tvStatusBadge.setTextColor(redColor)
                    binding.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(redBg)
                    binding.cardCustomer.strokeColor = redColor
                }
                StatusType.DUE_SOON -> {
                    val orangeColor = ContextCompat.getColor(context, R.color.status_orange)
                    val orangeBg = ContextCompat.getColor(context, R.color.status_orange_bg)
                    val label = if (status.daysRemaining == 0L) "🟠 BUGÜN!" else "🟠 ${status.daysRemaining} Gün"
                    binding.tvStatusBadge.text = label
                    binding.tvStatusBadge.setTextColor(orangeColor)
                    binding.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(orangeBg)
                    binding.cardCustomer.strokeColor = orangeColor
                }
                StatusType.SAFE -> {
                    val safeColor = ContextCompat.getColor(context, R.color.status_safe)
                    val safeBg = ContextCompat.getColor(context, R.color.status_safe_bg)
                    binding.tvStatusBadge.text = "⚪ ${status.daysRemaining} Gün"
                    binding.tvStatusBadge.setTextColor(safeColor)
                    binding.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(safeBg)
                    binding.cardCustomer.strokeColor = Color.TRANSPARENT
                }
            }

            // Reservation Badge Strip
            if (customer.reservationDate != null) {
                binding.layoutReservationBadge.visibility = android.view.View.VISIBLE
                val formattedDate = DateUtils.formatDateTime(customer.reservationDate)
                val noteText = if (!customer.reservationNote.isNullOrBlank()) " (${customer.reservationNote})" else ""
                binding.tvReservationInfo.text = "🗓️ Randevu: $formattedDate$noteText"
            } else {
                binding.layoutReservationBadge.visibility = android.view.View.GONE
            }

            // Click Listeners
            binding.cardCustomer.setOnClickListener { onItemClick(customer) }
            binding.btnQuickCall.setOnClickListener { onCallClick(customer) }
            binding.btnQuickWhatsApp.setOnClickListener { onWhatsAppClick(customer) }
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
