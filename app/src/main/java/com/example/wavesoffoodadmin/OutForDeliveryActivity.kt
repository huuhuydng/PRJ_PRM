package com.example.wavesoffoodadmin

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wavesoffoodadmin.adapter.DeliveryAdapter
import com.example.wavesoffoodadmin.databinding.ActivityOutForDeliveryBinding
import com.example.wavesoffoodadmin.model.OrderDetails
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class OutForDeliveryActivity : AppCompatActivity() {
    private val binding: ActivityOutForDeliveryBinding by lazy {
        ActivityOutForDeliveryBinding.inflate(layoutInflater)
    }

    private lateinit var database: FirebaseDatabase
    private var listOfCompleteOrderList: ArrayList<OrderDetails> = arrayListOf()
    private var orderListener: ValueEventListener? = null

    companion object {
        private const val TAG = "OutForDeliveryActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        
        Log.d(TAG, "onCreate: Activity started")
        
        binding.backButton.setOnClickListener {
            finish()
        }
        //retrieve and display complete order list
        retrieveCompleteOrderList()

    }

    private fun retrieveCompleteOrderList() {
        Log.d(TAG, "retrieveCompleteOrderList: Fetching dispatched orders from Firebase...")

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance()
        val completeOrderReference = database.reference.child("CompleteOrder")
            .orderByChild("currentTime")
        
        // Use real-time listener instead of single value event
        orderListener = completeOrderReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "onDataChange: Snapshot exists: ${snapshot.exists()}")
                Log.d(TAG, "onDataChange: Children count: ${snapshot.childrenCount}")
                
                //clear the list before populating it with new data
                listOfCompleteOrderList.clear()
                
                if (!snapshot.exists()) {
                    Log.w(TAG, "onDataChange: No dispatched orders found in CompleteOrder")
                    showEmptyState()
                    return
                }
                
                for (orderSnapshot in snapshot.children) {
                    val completeOrder = orderSnapshot.getValue(OrderDetails::class.java)
                    Log.d(TAG, "Dispatched order found: ${completeOrder?.userName}, Payment: ${completeOrder?.paymentReceived}")
                    completeOrder?.let {
                        listOfCompleteOrderList.add(it)
                    }
                }
                
                //reverse the list to display latest first
                listOfCompleteOrderList.reverse()

                if (listOfCompleteOrderList.isEmpty()) {
                    showEmptyState()
                } else {
                    hideEmptyState()
                    setDataIntoRecyclerView()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "onCancelled: Database error: ${error.message}", error.toException())
                Toast.makeText(
                    this@OutForDeliveryActivity,
                    "Failed to load dispatched orders: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
                showEmptyState()
            }
        })
    }
    
    private fun showEmptyState() {
        Log.d(TAG, "showEmptyState: Showing empty state")
        Toast.makeText(this, "No dispatched orders at the moment", Toast.LENGTH_SHORT).show()
    }
    
    private fun hideEmptyState() {
        // Hide empty state view if you have one
    }

    private fun setDataIntoRecyclerView() {
        Log.d(TAG, "setDataIntoRecyclerView: Setting up RecyclerView with ${listOfCompleteOrderList.size} orders")
        
        //Initialization list to hold customers name and payment status
        val customerName = mutableListOf<String>()
        val moneyStatus = mutableListOf<Boolean>()

        for (order in listOfCompleteOrderList) {
            order.userName?.let {
                customerName.add(it)
                Log.d(TAG, "Added customer: $it, Payment received: ${order.paymentReceived}")
            }
            moneyStatus.add(order.paymentReceived)
        }

        val adapter = DeliveryAdapter(customerName, moneyStatus)
        binding.deliveryRecyclerView.adapter = adapter
        binding.deliveryRecyclerView.layoutManager = LinearLayoutManager(this)
        Log.d(TAG, "setDataIntoRecyclerView: Adapter set with ${customerName.size} items")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Remove listener to prevent memory leaks
        orderListener?.let {
            database.reference.child("CompleteOrder").removeEventListener(it)
        }
        Log.d(TAG, "onDestroy: Listener removed")
    }
}
