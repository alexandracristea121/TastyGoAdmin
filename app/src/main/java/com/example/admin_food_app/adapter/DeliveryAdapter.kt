package com.example.admin_food_app.adapter

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.admin_food_app.databinding.DeliveryItemBinding

class DeliveryAdapter(private val customerNames: MutableList<String>, private val moneyStatus: MutableList<Boolean>): RecyclerView.Adapter<DeliveryAdapter.DeliveryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeliveryViewHolder {
        val binding = DeliveryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeliveryViewHolder(binding)
    }


    override fun onBindViewHolder(holder: DeliveryViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = customerNames.size

    inner class DeliveryViewHolder(private val binding: DeliveryItemBinding) :RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(position: Int) {
            binding.apply {
                customerName.text=customerNames[position]
                if(moneyStatus[position]){
                    statusMoney.text="Received"
                }else{
                    statusMoney.text="NotReceived"
                }
                val colorMap = mapOf(true to Color.GREEN, false to Color.RED)
                statusMoney.setTextColor(colorMap[moneyStatus[position]]?:Color.BLACK)
                StatusColor.backgroundTintList = ColorStateList.valueOf(colorMap[moneyStatus[position]]?:Color.BLACK)
            }
        }

    }
}