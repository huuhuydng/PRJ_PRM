package com.example.wavesoffoodadmin.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.wavesoffoodadmin.databinding.PendingOrdersBinding
import com.example.wavesoffoodadmin.model.OrderDetails

class PendingOrderAdapter(
    private val context: Context,
    private val customerNames: MutableList<String>,
    private val quantity: MutableList<String>,
    private val foodImage: MutableList<String>,
    private val orderDetailsList: MutableList<OrderDetails>,
    private val itemClicked: OnItemClicked
) : RecyclerView.Adapter<PendingOrderAdapter.PendingOrderViewHolder>() {
    interface OnItemClicked {
        fun onItemClickListener(position: Int)
        fun onItemAcceptClickListener(position: Int)
        fun onItemDispatchClickListener(position: Int)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingOrderViewHolder {
        val binding =
            PendingOrdersBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PendingOrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PendingOrderViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = customerNames.size

    inner class PendingOrderViewHolder(private val binding: PendingOrdersBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(position: Int) {
            binding.apply {
                customerName.text = customerNames[position]
                pendingOrderQuantity.text = quantity[position]
                var uriString = foodImage[position]
                var uri = Uri.parse(uriString)
                Glide.with(context).load(uri).into(orderedFoodImage)

                // Check if order is already accepted from OrderDetails data
                val isAccepted = orderDetailsList[position].orderAccepted
                
                orderedAcceptButton.apply {
                    // Set button text based on order status
                    text = if (isAccepted) "Dispatch" else "Accept"
                    
                    setOnClickListener {
                        if (!isAccepted) {
                            // First click: Accept order
                            showToast("Order is accepted")
                            itemClicked.onItemAcceptClickListener(position)
                            
                            // Update local data and UI
                            orderDetailsList[position].orderAccepted = true
                            notifyItemChanged(position)
                        } else {
                            // Second click: Dispatch order
                            showToast("Dispatching order...")
                            itemClicked.onItemDispatchClickListener(position)
                        }
                    }
                }
                itemView.setOnClickListener {
                    itemClicked.onItemClickListener(position)
                }
            }
        }
        private fun showToast(message: String) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}