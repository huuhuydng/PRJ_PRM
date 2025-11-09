package com.example.wavesoffood.adaptar

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.wavesoffood.databinding.BuyAgainItemBinding
import com.example.wavesoffood.model.CartItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class BuyAgainAdapter(
    private val buyAgainFoodName: MutableList<String>,
    private val buyAgainFoodPrice: MutableList<String>,
    private val buyAgainFoodImage: MutableList<String>,
    private val buyAgainFoodQuantity: MutableList<Int>,
    private var requireContext: Context
) : RecyclerView.Adapter<BuyAgainAdapter.BuyAgainViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    override fun onBindViewHolder(holder: BuyAgainViewHolder, position: Int) {
        holder.bind(
            buyAgainFoodName[position],
            buyAgainFoodPrice[position],
            buyAgainFoodImage[position],
            buyAgainFoodQuantity[position]
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuyAgainViewHolder {
        val binding = BuyAgainItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BuyAgainViewHolder(binding)
    }

    override fun getItemCount(): Int = buyAgainFoodName.size

    inner class BuyAgainViewHolder(private val binding: BuyAgainItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(foodName: String, foodPrice: String, foodImage: String, foodQuantity: Int) {
            binding.buyAgainFoodName.text = foodName
            binding.buyAgainFoodPrice.text = foodPrice
            val uriString = foodImage
            val uri = Uri.parse(uriString)
            Glide.with(requireContext).load(uri).into(binding.buyAgainFoodImage)

            // Click listener for the "Buy Again" button
            binding.buyAgainFoodButton.setOnClickListener {
                addToCart(foodName, foodPrice, foodImage, foodQuantity)
            }
            
            // Optional: Click on whole item also adds to cart
            binding.root.setOnClickListener {
                addToCart(foodName, foodPrice, foodImage, foodQuantity)
            }
        }

        private fun addToCart(foodName: String, foodPrice: String, foodImage: String, foodQuantity: Int) {
            val userId = auth.currentUser?.uid ?: run {
                Toast.makeText(requireContext, "⚠️ Please login first", Toast.LENGTH_SHORT).show()
                return
            }

            android.util.Log.d("BUY_AGAIN", "🛒 Adding to cart: $foodName (qty: $foodQuantity)")

            // Create a cart item with quantity from the original order
            val cartItem = CartItems(
                foodName = foodName,
                foodPrice = foodPrice,
                foodDescription = "", // We don't have description in history
                foodImage = foodImage,
                foodQuantity = foodQuantity,
                foodIngredients = "" // We don't have ingredients in history
            )

            // Add to Firebase cart
            database.reference
                .child("user")
                .child(userId)
                .child("CartItems")
                .push()
                .setValue(cartItem)
                .addOnSuccessListener {
                    android.util.Log.d("BUY_AGAIN", "✅ Successfully added to cart")
                    val quantityText = if (foodQuantity > 1) " (x$foodQuantity)" else ""
                    Toast.makeText(
                        requireContext,
                        "✅ $foodName$quantityText added to cart!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("BUY_AGAIN", "❌ Failed to add to cart: ${e.message}")
                    Toast.makeText(
                        requireContext,
                        "❌ Failed to add to cart",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }
}