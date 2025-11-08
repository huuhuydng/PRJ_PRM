package com.example.wavesoffood.Fragment

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.wavesoffood.adaptar.BuyAgainAdapter
import com.example.wavesoffood.databinding.FragmentHistoryBinding
import com.example.wavesoffood.model.CartItems
import com.example.wavesoffood.model.OrderDetails
import com.example.wavesoffood.RecentOrderItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HistoryFragment : Fragment() {
    private lateinit var binding: FragmentHistoryBinding
    private lateinit var buyAgainAdapter: BuyAgainAdapter
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var userId: String
    private var listOfOrderItem: ArrayList<OrderDetails> = arrayListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHistoryBinding.inflate(layoutInflater, container, false)
        //Inflate the layout for this fragment
        auth = FirebaseAuth.getInstance()
        database= FirebaseDatabase.getInstance()
        // retrieve and display the user order history
        retrieveBuyHistory()

        // recent buy button click - view order details
        binding.recentbuyitem.setOnClickListener {
            seeItemsRecentBuy()
        }
        
        // Optional: Buy again button for recent order (if exists in layout)
        // Uncomment if you add a "Buy Again" button to fragment_history.xml
        /*
        binding.buyAgainButton.setOnClickListener {
            buyRecentOrderAgain()
        }
        */
        
        binding.receivedButton.setOnClickListener {
            updateOrderStatus()
        }
        return binding.root
    }

    private fun updateOrderStatus() {
        // Get the most recent order
        val recentOrder = listOfOrderItem.firstOrNull()
        if (recentOrder == null) {
            showToast("No order found")
            return
        }
        
        val itemPushKey = recentOrder.itemPushKey
        if (itemPushKey.isNullOrEmpty()) {
            showToast("Invalid order ID")
            return
        }
        
        // Disable button to prevent multiple clicks
        binding.receivedButton.isEnabled = false
        
        // Update in CompleteOrder (your Firebase node name)
        val completeOrderReference = database.reference
            .child("CompleteOrder")
            .child(itemPushKey)
        
        completeOrderReference.child("paymentReceived").setValue(true)
            .addOnSuccessListener {
                // Also update in user's BuyHistory
                updateUserBuyHistory(itemPushKey)
            }
            .addOnFailureListener { exception ->
                showToast("Failed to update order: ${exception.message}")
                binding.receivedButton.isEnabled = true
            }
    }
    
    private fun updateUserBuyHistory(itemPushKey: String) {
        val userOrderReference = database.reference
            .child("user")
            .child(userId)
            .child("BuyHistory")
            .child(itemPushKey)
        
        userOrderReference.child("paymentReceived").setValue(true)
            .addOnSuccessListener {
                // Update local data
                listOfOrderItem.firstOrNull()?.paymentReceived = true
                
                // Update UI
                updateReceivedUI()
                
                showToast("Order marked as received!")
            }
            .addOnFailureListener { exception ->
                showToast("Failed to update history: ${exception.message}")
                binding.receivedButton.isEnabled = true
            }
    }
    
    private fun updateReceivedUI() {
        with(binding) {
            // Change status color to indicate received
            orderStatus.background.setTint(Color.parseColor("#4CAF50")) // Green for received
            
            // Hide the received button
            receivedButton.visibility = View.GONE
            
            // Optionally show a checkmark or "Received" text
            // You could add a TextView in your layout to show "✓ Received"
        }
    }
    
    private fun showToast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }


    //funct to see item recent buy
    private fun seeItemsRecentBuy() {
        listOfOrderItem.firstOrNull()?.let { recentBuy ->
            val intent = Intent(requireContext(), RecentOrderItems::class.java)
            intent.putParcelableArrayListExtra("RecentBuyOrderItem", listOfOrderItem)
            startActivity(intent)
        }
    }
    
    // Function to add entire recent order back to cart
    private fun buyRecentOrderAgain() {
        val recentOrder = listOfOrderItem.firstOrNull()
        if (recentOrder == null) {
            showToast("No recent order found")
            return
        }
        
        val foodNames = recentOrder.foodNames
        val foodPrices = recentOrder.foodPrices
        val foodImages = recentOrder.foodImages
        val foodQuantities = recentOrder.foodQuantities
        
        if (foodNames.isNullOrEmpty()) {
            showToast("Order has no items")
            return
        }
        
        // Add all items from the order to cart
        var itemsAdded = 0
        val totalItems = foodNames.size
        
        for (i in foodNames.indices) {
            val cartItem = CartItems(
                foodName = foodNames[i],
                foodPrice = foodPrices?.getOrNull(i) ?: "",
                foodDescription = "",
                foodImage = foodImages?.getOrNull(i) ?: "",
                foodQuantity = foodQuantities?.getOrNull(i) ?: 1,
                foodIngredients = ""
            )
            
            database.reference
                .child("user")
                .child(userId)
                .child("CartItems")
                .push()
                .setValue(cartItem)
                .addOnSuccessListener {
                    itemsAdded++
                    if (itemsAdded == totalItems) {
                        showToast("✅ All $totalItems items added to cart!")
                    }
                }
                .addOnFailureListener {
                    showToast("❌ Failed to add some items")
                }
        }
    }

    private fun retrieveBuyHistory() {
        binding.recentbuyitem.visibility= View.INVISIBLE
        userId = auth.currentUser?.uid ?: ""

        val buyItemReference : DatabaseReference =
            database.reference.child("user").child(userId).child("BuyHistory")
        val shortingQuery =
            buyItemReference.orderByChild("currentTime")

        shortingQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (buySnapshot in snapshot.children) {
                    val buyHistoryItem = buySnapshot.getValue(OrderDetails::class.java)
                    buyHistoryItem?.let {
                        // Ensure paymentReceived field exists (for old data compatibility)
                        if (!buySnapshot.hasChild("paymentReceived")) {
                            Log.d("ORDER_STATUS", "Missing paymentReceived field, setting to false for order: ${it.itemPushKey}")
                            // Initialize missing field
                            buySnapshot.ref.child("paymentReceived").setValue(false)
                        }
                        listOfOrderItem.add(it)
                    }
                }
                listOfOrderItem.reverse()
                if (listOfOrderItem.isNotEmpty()){
                    //display the most recent order details
                    setDataInRecentBuyItem()
                    //setup recycler view with previous order details
                    setPreviousBuyItemsRecyclerView()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("ORDER_STATUS", "Failed to load order history: ${error.message}")
            }
        }
        )



    }
    private fun setDataInRecentBuyItem() {
        binding.recentbuyitem.visibility = View.VISIBLE
        val recentOrderItem = listOfOrderItem.firstOrNull()
        recentOrderItem?.let {
            with(binding){
                buyAgainFoodName.text = it.foodNames?.firstOrNull() ?: ""
                buyAgainFoodPrice.text = it.foodPrices?.firstOrNull() ?: " "
                val image = it.foodImages?.firstOrNull()?:""
                val uri = Uri.parse(image)
                Glide.with(requireContext()).load(uri).into(buyAgainFoodImage)

                val isOrderIsAccepted = listOfOrderItem[0].orderAccepted
                val isPaymentReceived = listOfOrderItem[0].paymentReceived
                
                Log.d("ORDER_STATUS", "Order Accepted: $isOrderIsAccepted, Payment Received: $isPaymentReceived")
                
                // Update UI based on order status
                when {
                    isPaymentReceived -> {
                        // Order has been received - show green status, hide button
                        orderStatus.background.setTint(Color.parseColor("#4CAF50")) // Green
                        receivedButton.visibility = View.GONE
                    }
                    isOrderIsAccepted -> {
                        // Order is accepted but not received yet - show orange/yellow status, show button
                        orderStatus.background.setTint(Color.parseColor("#FFA726")) // Orange
                        receivedButton.visibility = View.VISIBLE
                    }
                    else -> {
                        // Order is pending - show gray status, hide button
                        orderStatus.background.setTint(Color.parseColor("#C6C6C6")) // Gray
                        receivedButton.visibility = View.GONE
                    }
                }
            }
        }
    }

    //funct to setup recycler view with previous order details
    private fun setPreviousBuyItemsRecyclerView() {
        val buyAgainFoodName = mutableListOf<String>()
        val buyAgainFoodPrice = mutableListOf<String>()
        val buyAgainFoodImage = mutableListOf<String>()
        val buyAgainFoodQuantity = mutableListOf<Int>()
        
        for (i in 1 until listOfOrderItem.size) {
            listOfOrderItem[i].foodNames?.firstOrNull()?.let {
                buyAgainFoodName.add(it)
                listOfOrderItem[i].foodPrices?.firstOrNull()?.let {
                    buyAgainFoodPrice.add(it)
                    listOfOrderItem[i].foodImages?.firstOrNull()?.let {
                        buyAgainFoodImage.add(it)
                        // Get the first quantity or default to 1
                        val quantity = listOfOrderItem[i].foodQuantities?.firstOrNull() ?: 1
                        buyAgainFoodQuantity.add(quantity)
                    }
                }
            }
        }
        
        if (buyAgainFoodName.isNotEmpty()) {
            val rv = binding.buyAgainRecyclerView
            rv.layoutManager = LinearLayoutManager(requireContext())
            buyAgainAdapter = BuyAgainAdapter(
                buyAgainFoodName,
                buyAgainFoodPrice,
                buyAgainFoodImage,
                buyAgainFoodQuantity,
                requireContext()
            )
            rv.adapter = buyAgainAdapter
        }
    }
}


