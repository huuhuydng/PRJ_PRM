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
        
        Log.d("ORDER_STATUS", "🔘 User clicked Received button for order: $itemPushKey")
        
        // Disable button to prevent multiple clicks
        binding.receivedButton.isEnabled = false
        
        // Update Firebase - this will handle both BuyHistory and CompleteOrder
        updateUserBuyHistory(itemPushKey)
    }
    
    private fun updateUserBuyHistory(itemPushKey: String) {
        val userOrderReference = database.reference
            .child("user")
            .child(userId)
            .child("BuyHistory")
            .child(itemPushKey)
        
        Log.d("ORDER_STATUS", "💾 Updating BuyHistory/$itemPushKey with paymentReceived=true")
        
        userOrderReference.child("paymentReceived").setValue(true)
            .addOnSuccessListener {
                Log.d("ORDER_STATUS", "✅ Firebase BuyHistory updated successfully")
                
                // Now update CompleteOrder to ensure sync
                database.reference
                    .child("CompleteOrder")
                    .child(itemPushKey)
                    .child("paymentReceived")
                    .setValue(true)
                    .addOnSuccessListener {
                        Log.d("ORDER_STATUS", "✅ Firebase CompleteOrder updated successfully")
                        
                        // Only update local UI after Firebase confirms both updates
                        updateLocalDataAndUI()
                    }
                    .addOnFailureListener { e ->
                        Log.e("ORDER_STATUS", "❌ Failed to update CompleteOrder: ${e.message}")
                        // Still update UI since BuyHistory was successful
                        updateLocalDataAndUI()
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("ORDER_STATUS", "❌ Failed to update BuyHistory: ${exception.message}")
                showToast("Failed to update history: ${exception.message}")
                binding.receivedButton.isEnabled = true
            }
    }
    
    private fun updateLocalDataAndUI() {
        Log.d("ORDER_STATUS", "🎨 Updating local data and UI")
        
        // Update local data
        val receivedOrder = listOfOrderItem.firstOrNull()
        receivedOrder?.paymentReceived = true
        
        // Remove from Recent Buy (first position) and move to Previously Buy
        if (receivedOrder != null) {
            listOfOrderItem.removeAt(0)
            // Add to the end or keep in list (it will show in Previously Buy RecyclerView)
            listOfOrderItem.add(receivedOrder)
        }
        
        // Refresh UI - this will update both Recent Buy and Previously Buy sections
        refreshHistoryUI()
        
        showToast("✅ Order received and moved to Previously Buy!")
        
        // Re-enable button for next order (if any)
        binding.receivedButton.isEnabled = true
    }
    
    private fun refreshHistoryUI() {
        // Check if there are still orders
        if (listOfOrderItem.isEmpty()) {
            // No orders at all - hide Recent Buy section
            binding.recentbuyitem.visibility = View.GONE
            binding.receivedButton.visibility = View.GONE
            showToast("No orders in history")
            return
        }
        
        // Check first item to see if it should be shown as Recent Buy
        val firstOrder = listOfOrderItem.firstOrNull()
        if (firstOrder != null && !firstOrder.paymentReceived) {
            // There's an active order (not yet received) - show as Recent Buy
            setDataInRecentBuyItem()
        } else {
            // All orders are received - hide Recent Buy section
            binding.recentbuyitem.visibility = View.GONE
            binding.receivedButton.visibility = View.GONE
        }
        
        // Always refresh Previously Buy list
        setPreviousBuyItemsRecyclerView()
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
                listOfOrderItem.clear() // Clear old data first
                Log.d("ORDER_STATUS", "📚 Loading BuyHistory - total orders: ${snapshot.childrenCount}")
                
                for (buySnapshot in snapshot.children) {
                    val buyHistoryItem = buySnapshot.getValue(OrderDetails::class.java)
                    buyHistoryItem?.let {
                        Log.d("ORDER_STATUS", "📦 Order ${it.itemPushKey}: orderAccepted=${it.orderAccepted}, paymentReceived=${it.paymentReceived}")
                        
                        // Ensure paymentReceived field exists (for old data compatibility)
                        if (!buySnapshot.hasChild("paymentReceived")) {
                            Log.d("ORDER_STATUS", "⚠️ Missing paymentReceived field, setting to false for order: ${it.itemPushKey}")
                            // Initialize missing field
                            buySnapshot.ref.child("paymentReceived").setValue(false)
                            it.paymentReceived = false
                        }
                        // Add the item as read from user's BuyHistory
                        listOfOrderItem.add(it)
                    }
                }
                listOfOrderItem.reverse()
                
                Log.d("ORDER_STATUS", "📋 After loading: ${listOfOrderItem.size} orders in list")
                if (listOfOrderItem.isNotEmpty()) {
                    Log.d("ORDER_STATUS", "🔝 First order: ${listOfOrderItem[0].itemPushKey}, paymentReceived=${listOfOrderItem[0].paymentReceived}")
                }
                
                if (listOfOrderItem.isNotEmpty()){
                    // Before showing UI, try to sync the latest acceptance/payment status
                    // from the central CompleteOrder node (Admin may update there).
                    val recent = listOfOrderItem[0]
                    syncCompleteOrderStatusFor(recent) {
                        // After syncing, update UI and previous items list
                        setDataInRecentBuyItem()
                        setPreviousBuyItemsRecyclerView()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("ORDER_STATUS", "Failed to load order history: ${error.message}")
            }
        }
        )



    }

    /**
     * Syncs the latest orderAccepted/paymentReceived flags from
     * CompleteOrder/{itemPushKey} into the provided OrderDetails instance.
     * Calls onComplete() when done (success or failure) so the UI can refresh.
     * 
     * Priority: BuyHistory (already loaded) is the source of truth for paymentReceived.
     * We only sync orderAccepted from CompleteOrder (Admin updates).
     */
    private fun syncCompleteOrderStatusFor(item: OrderDetails, onComplete: () -> Unit) {
        val key = item.itemPushKey
        if (key.isNullOrEmpty()) {
            Log.w("ORDER_STATUS", "Cannot sync - itemPushKey is null or empty")
            onComplete()
            return
        }

        Log.d("ORDER_STATUS", "🔄 Syncing order status from CompleteOrder/$key")
        Log.d("ORDER_STATUS", "📦 Before sync - orderAccepted: ${item.orderAccepted}, paymentReceived: ${item.paymentReceived}")

        database.reference.child("CompleteOrder").child(key).get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    val accepted = snap.child("orderAccepted").getValue(Boolean::class.java)
                    val payment = snap.child("paymentReceived").getValue(Boolean::class.java)
                    
                    Log.d("ORDER_STATUS", "✅ CompleteOrder data found - orderAccepted: $accepted, paymentReceived: $payment")
                    
                    // Only update orderAccepted from CompleteOrder (Admin's responsibility)
                    if (accepted != null) {
                        item.orderAccepted = accepted
                    }
                    
                    // For paymentReceived, trust what's already in BuyHistory
                    // (User app is the one that updates this field)
                    // Only sync if BuyHistory doesn't have it yet
                    if (item.paymentReceived == false && payment == true) {
                        Log.d("ORDER_STATUS", "⚠️ CompleteOrder shows received but BuyHistory doesn't - syncing")
                        item.paymentReceived = payment
                    }
                    
                    Log.d("ORDER_STATUS", "📦 After sync - orderAccepted: ${item.orderAccepted}, paymentReceived: ${item.paymentReceived}")

                    // Only update BuyHistory with orderAccepted (not paymentReceived)
                    // because paymentReceived is managed by user app
                    if (accepted != null && accepted != item.orderAccepted) {
                        val userOrderRef = database.reference.child("user").child(userId).child("BuyHistory").child(key)
                        Log.d("ORDER_STATUS", "💾 Syncing orderAccepted to BuyHistory: $accepted")
                        userOrderRef.child("orderAccepted").setValue(accepted)
                            .addOnSuccessListener { 
                                Log.d("ORDER_STATUS", "✅ BuyHistory orderAccepted synced")
                            }
                            .addOnFailureListener { e ->
                                Log.e("ORDER_STATUS", "❌ Failed to sync BuyHistory: ${e.message}")
                            }
                    }
                } else {
                    Log.w("ORDER_STATUS", "⚠️ CompleteOrder/$key does not exist yet")
                }
                onComplete()
            }
            .addOnFailureListener {
                // If sync fails, still proceed to show whatever we have
                Log.e("ORDER_STATUS", "❌ Failed to fetch CompleteOrder/$key: ${it.message}")
                onComplete()
            }
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
                
                Log.d("ORDER_STATUS", "🎨 UI Update - Order Accepted: $isOrderIsAccepted, Payment Received: $isPaymentReceived")
                
                // Update UI based on order status
                when {
                    isPaymentReceived -> {
                        // Order has been received - show green status, hide button
                        Log.d("ORDER_STATUS", "✅ Status: RECEIVED - showing green, hiding button")
                        orderStatus.background.setTint(Color.parseColor("#4CAF50")) // Green
                        receivedButton.visibility = View.GONE
                    }
                    isOrderIsAccepted -> {
                        // Order is accepted but not received yet - show orange/yellow status, show button
                        Log.d("ORDER_STATUS", "📦 Status: DISPATCHED - showing orange, SHOWING BUTTON")
                        orderStatus.background.setTint(Color.parseColor("#FFA726")) // Orange
                        receivedButton.visibility = View.VISIBLE
                    }
                    else -> {
                        // Order is pending - show gray status, hide button
                        Log.d("ORDER_STATUS", "⏳ Status: PENDING - showing gray, hiding button")
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
        
        // Determine starting index based on whether first item is Recent Buy or not
        val startIndex = if (listOfOrderItem.firstOrNull()?.paymentReceived == false) {
            // First item is active/Recent Buy, so Previously Buy starts from index 1
            1
        } else {
            // All items are received, start from index 0
            0
        }
        
        Log.d("ORDER_STATUS", "📋 Setting up Previously Buy RecyclerView, startIndex: $startIndex, total items: ${listOfOrderItem.size}")
        
        for (i in startIndex until listOfOrderItem.size) {
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
        
        Log.d("ORDER_STATUS", "📋 Previously Buy items count: ${buyAgainFoodName.size}")
        
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
        } else {
            // Clear the adapter if no items
            binding.buyAgainRecyclerView.adapter = null
        }
    }
}


