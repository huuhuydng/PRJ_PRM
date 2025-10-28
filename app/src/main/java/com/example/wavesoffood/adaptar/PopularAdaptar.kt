package com.example.wavesoffood.adaptar

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.wavesoffood.databinding.PopularItemBinding

class PopularAdaptar(private val items: List<String>, private val image:List<Int>, private val price: List<String>) : RecyclerView.Adapter<PopularAdaptar.PopularViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopularViewHolder{
       return PopularViewHolder(
            PopularItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ))
    }

    override fun onBindViewHolder(holder: PopularViewHolder, position: Int){
        val item = items[position]
        val images = image[position]
        val price = price[position]
        holder.bind(item,price,images)

    }

    override fun getItemCount(): Int {
        return items.size
    }

    class PopularViewHolder(val binding: PopularItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
            private val imagesView = binding.imageView5
            fun bind(item: String,price:String, images: Int) {
                binding.foodNamePopular.text = item
                binding.pricePopular.text = price
                imagesView.setImageResource(images)
            }

    }
}