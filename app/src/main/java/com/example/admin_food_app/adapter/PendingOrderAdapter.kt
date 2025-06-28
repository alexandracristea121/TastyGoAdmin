package com.example.admin_food_app.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.admin_food_app.R
import com.example.admin_food_app.ui.PendingOrder
import com.example.admin_food_app.databinding.PendingOrderItemBinding

class PendingOrderAdapter(
    private val context: Context,
    private val pendingOrders: MutableList<PendingOrder>,
    private val itemClicked: OnItemClicked
) : RecyclerView.Adapter<PendingOrderAdapter.PendingOrderViewHolder>() {

    interface OnItemClicked {
        fun onItemClickListener(position: Int)
        fun onItemAcceptClickListener(position: Int)
        fun onItemDispatchClickListener(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingOrderViewHolder {
        val binding = PendingOrderItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PendingOrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PendingOrderViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = pendingOrders.size

    inner class PendingOrderViewHolder(private val binding: PendingOrderItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private var isAccepted = false

        @SuppressLint("SetTextI18n")
        fun bind(position: Int) {
            val order = pendingOrders[position]

            binding.apply {
                customerName.text = order.userName
                val price = order.totalPrice?.replace("$", "")
                if (price != null && !price.contains("RON")) {
                    pendingOrderQuantity.text = "$price RON"
                } else {
                    pendingOrderQuantity.text = price
                }

                val imageUrl = if (order.firstFoodImage.isNullOrEmpty()) {
                    R.drawable.placeholder_food_image
                } else {
                    Uri.parse(order.firstFoodImage)
                }

                Glide.with(context)
                    .load(imageUrl)
                    .into(orderFoodImage)

                orderedAcceptButton.apply {
                    text = if (!isAccepted) "Accept" else "Expediaza"

                    setOnClickListener {
                        val pos = adapterPosition
                        if (pos != RecyclerView.NO_POSITION) {
                            if (!isAccepted) {
                                text = "Expediere"
                                isAccepted = true
                                showToast("Comanda a fost acceptata")
                                itemClicked.onItemAcceptClickListener(pos)
                            } else {
                                pendingOrders.removeAt(pos)
                                notifyItemRemoved(pos)
                                showToast("Order is dispatched")
                                itemClicked.onItemDispatchClickListener(pos)
                            }
                        }
                    }
                }

                itemView.setOnClickListener {
                    val pos = adapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        itemClicked.onItemClickListener(pos)
                    }
                }
            }
        }

        private fun showToast(message: String) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}