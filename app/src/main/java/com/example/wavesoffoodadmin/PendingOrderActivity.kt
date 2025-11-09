package com.example.wavesoffoodadmin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wavesoffoodadmin.adapter.PendingOrderAdapter
import com.example.wavesoffoodadmin.databinding.ActivityPendingOrderBinding
import com.example.wavesoffoodadmin.model.OrderDetails
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PendingOrderActivity : AppCompatActivity(), PendingOrderAdapter.OnItemClicked {
    private val binding by lazy {
        ActivityPendingOrderBinding.inflate(layoutInflater)
    }

    private var listOfName : MutableList<String> = mutableListOf()
    private var listOfToTalPrice : MutableList<String> = mutableListOf()
    private var listOfImageFirstFoodOrder : MutableList<String> = mutableListOf()
    private var listOfOrderItem : ArrayList<OrderDetails> = arrayListOf()
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseOrderDetails: DatabaseReference
    private var orderListener: ValueEventListener? = null

    companion object {
        private const val TAG = "PendingOrderActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        
        Log.d(TAG, "onCreate: Activity started")
        
        //Initialization of database
        database = FirebaseDatabase.getInstance()
        //Initialization of databaseReference
        databaseOrderDetails = database.reference.child("OrderDetails")

        getOrdersDetails()

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun getOrdersDetails() {
        Log.d(TAG, "getOrdersDetails: Fetching orders from Firebase...")
        
        //Retrieve order details from Firebase database with REAL-TIME updates
        orderListener = databaseOrderDetails.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "onDataChange: Snapshot exists: ${snapshot.exists()}")
                Log.d(TAG, "onDataChange: Children count: ${snapshot.childrenCount}")
                
                // Clear existing data before adding new
                listOfOrderItem.clear()
                listOfName.clear()
                listOfToTalPrice.clear()
                listOfImageFirstFoodOrder.clear()
                
                if (!snapshot.exists()) {
                    Log.w(TAG, "onDataChange: No orders found in OrderDetails")
                    showEmptyState()
                    return
                }
                
                for(orderSnapshot in snapshot.children){
                    val orderDetails = orderSnapshot.getValue(OrderDetails::class.java)
                    
                    // Log detailed order info for debugging
                    Log.d(TAG, "=== Order Details ===")
                    Log.d(TAG, "Key: ${orderSnapshot.key}")
                    Log.d(TAG, "userName: ${orderDetails?.userName}")
                    Log.d(TAG, "userUid: ${orderDetails?.userUid}")
                    Log.d(TAG, "totalPrice: ${orderDetails?.totalPrice}")
                    Log.d(TAG, "phoneNumber: ${orderDetails?.phoneNumber}")
                    Log.d(TAG, "address: ${orderDetails?.address}")
                    Log.d(TAG, "Raw data: ${orderSnapshot.value}")
                    
                    orderDetails?.let {
                        listOfOrderItem.add(it)
                    }
                }

                if (listOfOrderItem.isEmpty()) {
                    showEmptyState()
                } else {
                    hideEmptyState()
                    addDataToListForRecyclerView()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "onCancelled: Database error: ${error.message}", error.toException())
                Toast.makeText(
                    this@PendingOrderActivity,
                    "Failed to load orders: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
                showEmptyState()
            }
        })
    }
    
    private fun showEmptyState() {
        Log.d(TAG, "showEmptyState: Showing empty state")
        Toast.makeText(this, "No pending orders at the moment", Toast.LENGTH_SHORT).show()
        // You can add an empty state view here if you have one in your layout
    }
    
    private fun hideEmptyState() {
        // Hide empty state view if you have one
    }

    private fun addDataToListForRecyclerView() {
        Log.d(TAG, "addDataToListForRecyclerView: Processing ${listOfOrderItem.size} orders")
        
        for(orderItem in listOfOrderItem){
            // Add customer name with fallback options
            val customerName = when {
                !orderItem.userName.isNullOrBlank() -> {
                    Log.d(TAG, "Using userName: ${orderItem.userName}")
                    orderItem.userName!!
                }
                !orderItem.phoneNumber.isNullOrBlank() -> {
                    Log.d(TAG, "Using phoneNumber as name: ${orderItem.phoneNumber}")
                    orderItem.phoneNumber!!
                }
                !orderItem.userUid.isNullOrBlank() -> {
                    Log.d(TAG, "Using userUid as name: ${orderItem.userUid}")
                    "Customer ${orderItem.userUid?.take(8)}"
                }
                else -> {
                    Log.w(TAG, "No customer identifier found, using default")
                    "Unknown Customer"
                }
            }
            listOfName.add(customerName)
            Log.d(TAG, "Added customer: $customerName")
            
            // Add total price
            val price = orderItem.totalPrice ?: "0$"
            listOfToTalPrice.add(price)
            Log.d(TAG, "Added price: $price")
            
            // Add food images
            orderItem.foodImages?.filterNot { it.isEmpty() }?.forEach {
                listOfImageFirstFoodOrder.add(it)
            }
        }
        
        Log.d(TAG, "Lists size - Names: ${listOfName.size}, Prices: ${listOfToTalPrice.size}, Images: ${listOfImageFirstFoodOrder.size}")
        
        if (listOfName.isEmpty()) {
            Log.e(TAG, "⚠️ WARNING: No customer names found!")
            showEmptyState()
        } else {
            setAdpater()
        }
    }

    private fun setAdpater() {
        Log.d(TAG, "setAdapter: Setting up RecyclerView adapter")
        binding.pendingOrderRecyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = PendingOrderAdapter(
            this, 
            listOfName, 
            listOfToTalPrice, 
            listOfImageFirstFoodOrder,
            listOfOrderItem,  // Pass the OrderDetails list
            this
        )
        binding.pendingOrderRecyclerView.adapter = adapter
        Log.d(TAG, "setAdapter: Adapter set with ${listOfName.size} items")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Remove listener to prevent memory leaks
        orderListener?.let {
            databaseOrderDetails.removeEventListener(it)
        }
        Log.d(TAG, "onDestroy: Listener removed")
    }

    override fun onItemClickListener(position: Int){
        val intent = Intent(this, OrderDetailsActivity::class.java)
        val userOrderDetails = listOfOrderItem[position]
        intent.putExtra("UserOrderDetails", userOrderDetails)
        startActivity(intent)
    }

    override fun onItemAcceptClickListener(position: Int) {
         // handle item acceptance and update database
        val childItemPushKey = listOfOrderItem[position].itemPushKey
        val clickItemOrderReference = childItemPushKey?.let {
            database.reference.child("OrderDetails").child(it)
        }
        clickItemOrderReference?.child("AcceptedOrder")?.setValue(true)
        updateOrderAcceptedStatus(position)

    }
 
    override fun onItemDispatchClickListener(position: Int) {
        // ========== DISPATCH FLOW ==========
        // Step 1: Accept order (if not already accepted)
        // Step 2: Copy to CompleteOrder/
        // Step 3: Delete from OrderDetails/
        // Step 4: Remove from local list and update RecyclerView
        // Result: Dashboard auto-updates via real-time listeners! ✨
        
        val order = listOfOrderItem[position]
        val dispatchItemPushKey = order.itemPushKey
        val customerName = order.userName ?: order.phoneNumber ?: "Customer"
        val orderPrice = order.totalPrice ?: "0$"
        
        Log.d(TAG, "========== DISPATCH FLOW START ==========")
        Log.d(TAG, "onItemDispatchClickListener: Dispatching order at position $position")
        Log.d(TAG, "Customer: $customerName")
        Log.d(TAG, "Price: $orderPrice")
        Log.d(TAG, "Order Key: $dispatchItemPushKey")
        
        if (dispatchItemPushKey == null) {
            Log.e(TAG, "❌ ERROR: Order key is null - cannot dispatch")
            Toast.makeText(this, "Error: Order key is missing", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Ensure order is marked as accepted before dispatching
        order.orderAccepted = true
        Log.d(TAG, "Step 1: Set orderAccepted = true ✅")
        
        // Step 2: Copy to CompleteOrder with all data including orderAccepted = true
        val dispatchItemOrderReference = database.reference.child("CompleteOrder").child(dispatchItemPushKey)
        
        Log.d(TAG, "Step 2: Copying order to CompleteOrder/...")
        dispatchItemOrderReference.setValue(order)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Step 2 SUCCESS: Order copied to CompleteOrder")
                Log.d(TAG, "   - Path: CompleteOrder/$dispatchItemPushKey")
                Log.d(TAG, "   - Price: $orderPrice")
                Log.d(TAG, "   - orderAccepted: true")
                
                // Step 3: Delete from OrderDetails
                deleteThisItemFromOrderDetails(dispatchItemPushKey, orderPrice, customerName, position)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Step 2 FAILED: Could not copy to CompleteOrder")
                Log.e(TAG, "   Error: ${e.message}", e)
                Toast.makeText(
                    this, 
                    "Failed to dispatch order: ${e.message}", 
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun deleteThisItemFromOrderDetails(
        dispatchItemPushKey: String, 
        orderPrice: String,
        customerName: String,
        position: Int
    ) {
        Log.d(TAG, "Step 3: Deleting from OrderDetails/...")
        Log.d(TAG, "   - Path: OrderDetails/$dispatchItemPushKey")
        Log.d(TAG, "   - Position in list: $position")
        
        val orderDetailsItemsReference = database.reference.child("OrderDetails").child(dispatchItemPushKey)
        orderDetailsItemsReference.removeValue()
            .addOnSuccessListener {
                Log.d(TAG, "✅ Step 3 SUCCESS: Order removed from OrderDetails")
                
                // Step 4: Remove from local lists and update UI
                Log.d(TAG, "Step 4: Removing from local lists and updating RecyclerView...")
                
                // Remove from all local lists
                if (position >= 0 && position < listOfOrderItem.size) {
                    listOfOrderItem.removeAt(position)
                    Log.d(TAG, "   ✅ Removed from listOfOrderItem at position $position")
                }
                
                if (position >= 0 && position < listOfName.size) {
                    listOfName.removeAt(position)
                    Log.d(TAG, "   ✅ Removed from listOfName at position $position")
                }
                
                if (position >= 0 && position < listOfToTalPrice.size) {
                    listOfToTalPrice.removeAt(position)
                    Log.d(TAG, "   ✅ Removed from listOfToTalPrice at position $position")
                }
                
                if (position >= 0 && position < listOfImageFirstFoodOrder.size) {
                    listOfImageFirstFoodOrder.removeAt(position)
                    Log.d(TAG, "   ✅ Removed from listOfImageFirstFoodOrder at position $position")
                }
                
                // Notify adapter to update RecyclerView
                binding.pendingOrderRecyclerView.adapter?.notifyItemRemoved(position)
                binding.pendingOrderRecyclerView.adapter?.notifyItemRangeChanged(position, listOfOrderItem.size)
                
                Log.d(TAG, "✅ Step 4 SUCCESS: RecyclerView updated")
                Log.d(TAG, "   → Remaining orders: ${listOfOrderItem.size}")
                
                Log.d(TAG, "========== DISPATCH FLOW COMPLETE ==========")
                Log.d(TAG, "")
                Log.d(TAG, "📊 DASHBOARD AUTO-UPDATE:")
                Log.d(TAG, "   ✅ Pending Orders:    -1  (removed from OrderDetails)")
                Log.d(TAG, "   ✅ Completed Orders:  +1  (added to CompleteOrder)")
                Log.d(TAG, "   ✅ Total Earnings:    +$orderPrice")
                Log.d(TAG, "")
                Log.d(TAG, "🎯 Real-time listeners in MainActivity will:")
                Log.d(TAG, "   → Update textView3 (Pending count)")
                Log.d(TAG, "   → Update textView5 (Completed count)")
                Log.d(TAG, "   → Update textView7 (Total earnings)")
                Log.d(TAG, "   → ALL IN REAL-TIME! ⚡")
                Log.d(TAG, "")
                Log.d(TAG, "🎨 UI Updates:")
                Log.d(TAG, "   → Item removed from PendingOrderActivity list INSTANTLY! ✨")
                Log.d(TAG, "   → No need to refresh or go back!")
                Log.d(TAG, "==========================================")
                
                // Show informative toast with customer name and earnings
                Toast.makeText(
                    this,
                    "✅ Order dispatched!\n👤 $customerName\n💰 Earnings: +$orderPrice",
                    Toast.LENGTH_LONG
                ).show()
                
                // Check if list is empty and show empty state
                if (listOfOrderItem.isEmpty()) {
                    Log.d(TAG, "📭 All orders dispatched - showing empty state")
                    showEmptyState()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Step 3 FAILED: Could not delete from OrderDetails")
                Log.e(TAG, "   Error: ${e.message}", e)
                Log.e(TAG, "⚠️  WARNING: Order exists in BOTH OrderDetails AND CompleteOrder!")
                Log.e(TAG, "   Manual cleanup may be required in Firebase Console")
                
                Toast.makeText(
                    this, 
                    "⚠️ Order dispatched but cleanup failed!\nCheck Firebase Console", 
                    Toast.LENGTH_LONG
                ).show()
            }
    }
    
    private fun updateOrderAcceptedStatus(position: Int)
    {
        //update order acceptance in user;s BuyHistory and OrdDetail
        val userIdOfClickedItem = listOfOrderItem[position].userUid
        val pushKeyOfClickedItem = listOfOrderItem[position].itemPushKey
        val buyHistoryReference = database.reference.child("user").child(userIdOfClickedItem!!).child("BuyHistory").child(pushKeyOfClickedItem!!)
        buyHistoryReference.child("AcceptedOrder").setValue(true)
        databaseOrderDetails.child(pushKeyOfClickedItem).child("AcceptedOrder").setValue(true)
        val orderDetailsReference = database.reference.child("OrderDetails")

    }

}


