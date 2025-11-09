package com.example.wavesoffood.adaptar

import android.content.ContentValues.TAG
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.wavesoffood.databinding.CartItemBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.graphics.drawable.Drawable
import android.util.Log
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

class CartAdapter(
    private val context: Context,
    private val cartItems: MutableList<String>,
    private val cartItemPrices: MutableList<String>,
    private var cartDescriptions: MutableList<String>,
    private var cartImages: MutableList<String>,
    private var cartQuantity: MutableList<Int>,
    private var cartIngredient: MutableList<String>,
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    // Firebase
    private val auth = FirebaseAuth.getInstance()

    init {
        val database = FirebaseDatabase.getInstance()
        val userId = auth.currentUser?.uid ?: ""
        val cartItemsNumber = cartItems.size

        // Initialize itemQuantities with actual cart quantities
        itemQuantities = if (cartItemsNumber > 0) {
            IntArray(cartItemsNumber) { index ->
                if (index < cartQuantity.size) cartQuantity[index] else 1
            }
        } else {
            intArrayOf() // Empty array for empty cart
        }
        
        cartItemsReference = database.reference.child("user").child(userId).child("CartItems")
        
        Log.d("CART_ADAPTER", "Initialized with ${cartItems.size} items, itemQuantities size: ${itemQuantities.size}")
    }

    companion object {
        private var itemQuantities: IntArray = intArrayOf()
        private lateinit var cartItemsReference: DatabaseReference
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = CartItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        // Safety check before binding
        if (position >= 0 && position < cartItems.size) {
            holder.bind(position)
        } else {
            Log.e("CART_ADAPTER", "onBindViewHolder called with invalid position: $position, size: ${cartItems.size}")
        }
    }

    override fun getItemCount(): Int = cartItems.size

    //get update quantity


    fun getUpdatedItemsQuantities(): MutableList<Int> {
        val itemQuantity = mutableListOf<Int>()
        itemQuantity.addAll(cartQuantity)
        return itemQuantity

    }

    inner class CartViewHolder(private val binding: CartItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            // Safety check - validate all lists have data at this position
            if (position < 0 || position >= cartItems.size) {
                Log.e("CART_ADAPTER", "Invalid bind position: $position, size: ${cartItems.size}")
                return
            }
            
            // Additional checks for all lists
            if (position >= cartItemPrices.size || position >= cartImages.size) {
                Log.e("CART_ADAPTER", "Position $position out of bounds for prices/images. Items: ${cartItems.size}, Prices: ${cartItemPrices.size}, Images: ${cartImages.size}")
                return
            }
            
            binding.apply {
                val quantity = if (position < itemQuantities.size && position < cartQuantity.size) {
                    itemQuantities[position]
                } else {
                    Log.w("CART_ADAPTER", "Position $position out of bounds for quantities, using default 1")
                    1
                }
                
                cartFoodName.text = cartItems.getOrNull(position) ?: "Unknown"
                cartItemPrice.text = cartItemPrices.getOrNull(position) ?: "$0"

                // Load image using Glide
                val uriString = cartImages.getOrNull(position) ?: ""
                if (uriString.isNotEmpty()) {
                    val uri = Uri.parse(uriString)
                    Glide.with(context).load(uri).into(cartImage)
                }

                cartItemQuantity.text = quantity.toString()
                
                minusButton.setOnClickListener {
                    val currentPosition = adapterPosition
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        decreaseQuantity(currentPosition)
                    }
                }
                
                plusButton.setOnClickListener {
                    val currentPosition = adapterPosition
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        increaseQuantity(currentPosition)
                    }
                }

                deleteButton.setOnClickListener {
                    val itemPosition = adapterPosition
                    if (itemPosition != RecyclerView.NO_POSITION) {
                        deleteItem(itemPosition)
                    }
                }
            }
        }

        private fun increaseQuantity(position: Int) {
            if (position < 0 || position >= itemQuantities.size || position >= cartQuantity.size) {
                Log.e("CART_ADAPTER", "Invalid position for increase: $position")
                return
            }
            
            if (itemQuantities[position] < 10) {
                itemQuantities[position]++
                cartQuantity[position] = itemQuantities[position]
                binding.cartItemQuantity.text = itemQuantities[position].toString()
            }
        }

        private fun decreaseQuantity(position: Int) {
            if (position < 0 || position >= itemQuantities.size || position >= cartQuantity.size) {
                Log.e("CART_ADAPTER", "Invalid position for decrease: $position")
                return
            }
            
            if (itemQuantities[position] > 1) {
                itemQuantities[position]--
                cartQuantity[position] = itemQuantities[position]
                binding.cartItemQuantity.text = itemQuantities[position].toString()
            }
        }

        private fun deleteItem(position: Int) {
            // Validate position first
            if (position < 0 || position >= cartItems.size) {
                Log.e("CART_DELETE", "Invalid position: $position, size: ${cartItems.size}")
                Toast.makeText(context, "Cannot delete item", Toast.LENGTH_SHORT).show()
                return
            }
            
            Log.d("CART_DELETE", "Deleting item at position $position")
            getUniqueKeyAtPosition(position) { uniqueKey ->
                if (uniqueKey != null) {
                    removeItem(position, uniqueKey)
                } else {
                    Log.e("CART_DELETE", "Failed to get unique key for position $position")
                    Toast.makeText(context, "Failed to delete item", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun removeItem(position: Int, uniqueKey: String) {
            // Validate position and all list sizes
            if (position < 0 || position >= cartItems.size) {
                Log.e("CART_DELETE", "Position out of bounds during removal: $position, size: ${cartItems.size}")
                return
            }
            
            // Check all lists have the same size
            val minSize = minOf(
                cartItems.size,
                cartImages.size,
                cartDescriptions.size,
                cartQuantity.size,
                cartItemPrices.size,
                cartIngredient.size
            )
            
            if (position >= minSize) {
                Log.e("CART_DELETE", "Position $position >= minSize $minSize. Sizes - Items:${cartItems.size}, Images:${cartImages.size}, Desc:${cartDescriptions.size}, Qty:${cartQuantity.size}, Prices:${cartItemPrices.size}, Ing:${cartIngredient.size}")
                return
            }
            
            Log.d("CART_DELETE", "Removing item with key: $uniqueKey at position: $position")
            
            cartItemsReference.child(uniqueKey).removeValue().addOnSuccessListener {
                // Verify position is still valid after async operation
                if (position >= 0 && position < cartItems.size) {
                    try {
                        // Remove from all lists safely
                        if (position < cartItems.size) cartItems.removeAt(position)
                        if (position < cartImages.size) cartImages.removeAt(position)
                        if (position < cartDescriptions.size) cartDescriptions.removeAt(position)
                        if (position < cartQuantity.size) cartQuantity.removeAt(position)
                        if (position < cartItemPrices.size) cartItemPrices.removeAt(position)
                        if (position < cartIngredient.size) cartIngredient.removeAt(position)

                        // Update itemQuantities array safely
                        if (position < itemQuantities.size) {
                            itemQuantities = itemQuantities.filterIndexed { index, _ -> index != position }.toIntArray()
                        }

                        // Notify adapter
                        notifyItemRemoved(position)
                        
                        // Only notify range change if there are still items after this position
                        if (position < cartItems.size) {
                            notifyItemRangeChanged(position, cartItems.size - position)
                        }
                        
                        Log.d("CART_DELETE", "✅ Item removed successfully. Remaining items: ${cartItems.size}")
                        Toast.makeText(context, "Item removed from cart", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("CART_DELETE", "Error removing item: ${e.message}", e)
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("CART_DELETE", "Position invalid after Firebase delete: $position, current size: ${cartItems.size}")
                }
            }.addOnFailureListener { e ->
                Log.e("CART_DELETE", "Firebase delete failed: ${e.message}", e)
                Toast.makeText(context, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun getUniqueKeyAtPosition(positionRetrieve: Int, onComplete: (String?) -> Unit) {
        Log.d("CART_DELETE", "Getting unique key for position: $positionRetrieve")
        
        cartItemsReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var uniqueKey: String? = null
                var currentIndex = 0
                
                for (dataSnapshot in snapshot.children) {
                    if (currentIndex == positionRetrieve) {
                        uniqueKey = dataSnapshot.key
                        Log.d("CART_DELETE", "Found key: $uniqueKey at position: $positionRetrieve")
                        break
                    }
                    currentIndex++
                }
                
                if (uniqueKey == null) {
                    Log.e("CART_DELETE", "No key found at position: $positionRetrieve, total children: ${snapshot.childrenCount}")
                }
                
                onComplete(uniqueKey)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CART_DELETE", "Firebase error getting unique key: ${error.message}")
                onComplete(null)
            }
        })
    }
}
