package com.aritma.musteritakip.ui.bireysel

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aritma.musteritakip.data.local.entity.NoteEntity
import com.aritma.musteritakip.databinding.ItemNoteHistoryBinding
import com.aritma.musteritakip.util.DateUtils

/**
 * Müşteriye ait geçmiş servis ve bakım notlarını listeleyen RecyclerView adaptörü.
 * Not metni, kayıt tarihi ve not silme etkileşimini yönetir.
 */
class NoteAdapter(
    private val onDeleteNoteClick: (NoteEntity) -> Unit
) : ListAdapter<NoteEntity, NoteAdapter.NoteViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(private val binding: ItemNoteHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: NoteEntity) {
            binding.tvNoteDate.text = "📅 ${DateUtils.formatDateTime(note.timestamp)}"
            binding.tvNoteText.text = note.noteText

            binding.btnDeleteNote.setOnClickListener {
                onDeleteNoteClick(note)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<NoteEntity>() {
        override fun areItemsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean {
            return oldItem == newItem
        }
    }
}
