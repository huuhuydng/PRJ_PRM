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
        val adapter = PendingOrderAdapter(this, listOfName, listOfToTalPrice, listOfImageFirstFoodOrder, this)
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
        // handle item dispatch and update database
        val order = listOfOrderItem[position]
        val dispatchItemPushKey = order.itemPushKey
        val customerName = order.userName ?: order.phoneNumber ?: "Customer"
        val orderPrice = order.totalPrice ?: "0$"
        
        Log.d(TAG, "onItemDispatchClickListener: Dispatching order at position $position")
        Log.d(TAG, "Dispatching order: $customerName, Price: $orderPrice, Key: $dispatchItemPushKey")
        
        if (dispatchItemPushKey == null) {
            Log.e(TAG, "❌ Cannot dispatch: order key is null")
            Toast.makeText(this, "Error: Order key is missing", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Set orderAccepted to true before dispatching
        order.orderAccepted = true
        Log.d(TAG, "✅ Set orderAccepted = true before dispatch")
        
        // Step 1: Update AcceptedOrder in OrderDetails (if not already set)
        val orderDetailsRef = database.reference.child("OrderDetails").child(dispatchItemPushKey)
        orderDetailsRef.child("AcceptedOrder").setValue(true)
            .addOnSuccessListener {
                Log.d(TAG, "✅ AcceptedOrder set to true in OrderDetails")
            }
        
        // Step 2: Copy to CompleteOrder with orderAccepted = true
        val dispatchItemOrderReference = database.reference.child("CompleteOrder").child(dispatchItemPushKey)
        dispatchItemOrderReference.setValue(order)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Order copied to CompleteOrder with orderAccepted = true")
                // Step 3: Delete from OrderDetails
                deleteThisItemFromOrderDetails(dispatchItemPushKey, orderPrice)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to copy order to CompleteOrder: ${e.message}")
                Toast.makeText(this, "Failed to dispatch order: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteThisItemFromOrderDetails(dispatchItemPushKey: String, orderPrice: String) {
        Log.d(TAG, "deleteThisItemFromOrderDetails: Removing from OrderDetails, Price: $orderPrice")
        
        val orderDetailsItemsReference = database.reference.child("OrderDetails").child(dispatchItemPushKey)
        orderDetailsItemsReference.removeValue()
            .addOnSuccessListener {
                Log.d(TAG, "✅ Order removed from OrderDetails")
                Log.d(TAG, "📊 Dashboard will auto-update:")
                Log.d(TAG, "   - Pending Orders: -1")
                Log.d(TAG, "   - Completed Orders: +1")
                Log.d(TAG, "   - Total Earnings: +$orderPrice")
                
                // Show informative toast with earnings
                Toast.makeText(
                    this,
                    "Order dispatched! Earnings: +$orderPrice",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to remove order from OrderDetails: ${e.message}")
                Toast.makeText(this, "Order dispatch failed: ${e.message}", Toast.LENGTH_SHORT).show()
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


