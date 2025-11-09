package com.example.wavesoffoodadmin.adapter

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.wavesoffoodadmin.databinding.ItemItemBinding
import com.example.wavesoffoodadmin.model.AllMenu
import com.google.firebase.database.DatabaseReference

class MenuItemAdapter(
//    private val MenuItemName: ArrayList<String>,
//    private val MenuItemPrice: ArrayList<String>,
//    private val MenuItemImage: ArrayList<Int>
//

    private val context: Context,
    private val menuList: ArrayList<AllMenu>,
    private val databaseReference: DatabaseReference  // ← Changed to private val
) : RecyclerView.Adapter<MenuItemAdapter.AddItemViewHolder>() {
    private val itemQuantities = IntArray(menuList.size)
    
    companion object {
        private const val TAG = "MenuItemAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddItemViewHolder {
        val binding = ItemItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AddItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddItemViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = menuList.size
    inner class AddItemViewHolder(private val binding: ItemItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.apply {
                val quantity = itemQuantities[position]
                val menuItem :AllMenu = menuList[position]
                val uriString :String? = menuItem.foodImage
                val uri = Uri.parse(uriString)


                foodNameTextView.text = menuItem.foodName
                priceTextView.text = menuItem.foodPrice
                Glide.with(context).load(uri).into(foodImageView)

                quantityTextView.text = quantity.toString()

                minusButton.setOnClickListener {
                    decreaseQuantity(position)
                }
                deleteButton.setOnClickListener {
                    deleteQuantity(position)
                }
                plusButton.setOnClickListener {
                    increaseQuantity(position)
                }
            }
        }

        private fun increaseQuantity(position: Int) {
            if (itemQuantities[position] < 10) {
                itemQuantities[position]++
                binding.quantityTextView.text = itemQuantities[position].toString()
            }
        }

        private fun decreaseQuantity(position: Int) {
            if (itemQuantities[position] > 1) {
                itemQuantities[position]--
                binding.quantityTextView.text = itemQuantities[position].toString()
            }
        }

        private fun deleteQuantity(position: Int) {
            // Get the menu item to delete
            val menuItemToDelete = menuList[position]
            val itemKey = menuItemToDelete.key
            
            Log.d(TAG, "========== DELETE ITEM START ==========")
            Log.d(TAG, "Attempting to delete item at position: $position")
            Log.d(TAG, "Item name: ${menuItemToDelete.foodName}")
            Log.d(TAG, "Item key: $itemKey")
            
            if (itemKey == null) {
                Log.e(TAG, "❌ ERROR: Item key is null - cannot delete from Firebase")
                Toast.makeText(context, "Error: Cannot delete item (missing key)", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Delete from Firebase first
            val menuRef = databaseReference.child("menu").child(itemKey)
            
            Log.d(TAG, "🗑️ Deleting from Firebase: menu/$itemKey")
            
            menuRef.removeValue()
                .addOnSuccessListener {
                    Log.d(TAG, "✅ SUCCESS: Item deleted from Firebase")
                    Log.d(TAG, "   → Removing from local list at position $position")
                    
                    // Remove from local list
                    menuList.removeAt(position)
                    
                    // Notify adapter
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, menuList.size)
                    
                    Log.d(TAG, "✅ SUCCESS: Item removed from RecyclerView")
                    Log.d(TAG, "   → Remaining items: ${menuList.size}")
                    Log.d(TAG, "========== DELETE ITEM COMPLETE ==========")
                    
                    // Show success message
                    Toast.makeText(
                        context, 
                        "✅ ${menuItemToDelete.foodName} deleted successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ FAILED: Could not delete from Firebase")
                    Log.e(TAG, "   Error: ${e.message}", e)
                    Log.e(TAG, "========== DELETE ITEM FAILED ==========")
                    
                    // Show error message
                    Toast.makeText(
                        context,
                        "❌ Failed to delete: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }
}