package com.example.itemmanagement.ui.add

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.itemmanagement.databinding.ItemFieldBinding

class FieldsAdapter(
    private var fields: List<Field>,
    private val onFieldSelected: (Field, Boolean) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<FieldsAdapter.ViewHolder>() {

    fun updateFields(newFields: List<Field>) {
        fields = newFields.sortedBy { it.order }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFieldBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(fields[position])
    }

    override fun getItemCount() = fields.size

    inner class ViewHolder(
        private val binding: ItemFieldBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private var isInternalUpdate = false

        fun bind(field: Field) {
            // 防止触发 CheckedChangeListener
            isInternalUpdate = true
            binding.fieldName.text = field.name
            binding.fieldCheckbox.isChecked = field.isSelected
            isInternalUpdate = false

            binding.root.setOnClickListener {
                binding.fieldCheckbox.isChecked = !binding.fieldCheckbox.isChecked
            }

            binding.fieldCheckbox.setOnCheckedChangeListener { _, isChecked ->
                if (!isInternalUpdate) {
                    field.isSelected = isChecked
                    onFieldSelected(field, isChecked)
                }
            }
        }
    }
}