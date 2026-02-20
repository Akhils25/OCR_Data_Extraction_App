package com.game.ocrapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.game.ocrapp.databinding.ItemIdFieldBinding

class AdapterIdDetails(private val fields: List<IDField>) :
    RecyclerView.Adapter<AdapterIdDetails.ViewHolder>() {

    class ViewHolder(val binding: ItemIdFieldBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIdFieldBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = fields[position]
        holder.binding.tvFieldName.text = "${item.fieldName}:"
        holder.binding.etFieldValue.text = item.fieldValue
    }

    override fun getItemCount() = fields.size
}