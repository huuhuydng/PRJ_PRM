package com.example.wavesoffoodadmin.utils

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * Helper class for debugging Firebase Database issues
 * Use this to check database connection and data structure
 */
object FirebaseDebugHelper {
    
    private const val TAG = "FirebaseDebugHelper"
    
    /**
     * Check Firebase connection status
     */
    fun checkConnection(onResult: (Boolean) -> Unit) {
        Log.d(TAG, "Checking Firebase connection...")
        
        val connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                Log.d(TAG, "Firebase connected: $connected")
                onResult(connected)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Connection check failed: ${error.message}")
                onResult(false)
            }
        })
    }
    
    /**
     * Debug OrderDetails node
     */
    fun debugOrderDetails() {
        Log.d(TAG, "=== DEBUGGING OrderDetails ===")
        
        val orderDetailsRef = FirebaseDatabase.getInstance().reference.child("OrderDetails")
        orderDetailsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "OrderDetails exists: ${snapshot.exists()}")
                Log.d(TAG, "OrderDetails children count: ${snapshot.childrenCount}")
                
                if (snapshot.exists()) {
                    snapshot.children.forEach { orderSnapshot ->
                        Log.d(TAG, "Order ID: ${orderSnapshot.key}")
                        Log.d(TAG, "Order data: ${orderSnapshot.value}")
                    }
                } else {
                    Log.w(TAG, "⚠️ OrderDetails node is EMPTY!")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "❌ Failed to read OrderDetails: ${error.message}")
            }
        })
    }
    
    /**
     * Debug CompleteOrder node
     */
    fun debugCompleteOrder() {
        Log.d(TAG, "=== DEBUGGING CompleteOrder ===")
        
        val completeOrderRef = FirebaseDatabase.getInstance().reference.child("CompleteOrder")
        completeOrderRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "CompleteOrder exists: ${snapshot.exists()}")
                Log.d(TAG, "CompleteOrder children count: ${snapshot.childrenCount}")
                
                if (snapshot.exists()) {
                    snapshot.children.forEach { orderSnapshot ->
                        Log.d(TAG, "Dispatched Order ID: ${orderSnapshot.key}")
                        Log.d(TAG, "Dispatched Order data: ${orderSnapshot.value}")
                    }
                } else {
                    Log.w(TAG, "⚠️ CompleteOrder node is EMPTY!")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "❌ Failed to read CompleteOrder: ${error.message}")
            }
        })
    }
    
    /**
     * Debug all user orders
     */
    fun debugUserOrders() {
        Log.d(TAG, "=== DEBUGGING User Orders ===")
        
        val usersRef = FirebaseDatabase.getInstance().reference.child("user")
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "Users exist: ${snapshot.exists()}")
                Log.d(TAG, "Users count: ${snapshot.childrenCount}")
                
                if (snapshot.exists()) {
                    snapshot.children.forEach { userSnapshot ->
                        val userId = userSnapshot.key
                        val buyHistory = userSnapshot.child("BuyHistory")
                        Log.d(TAG, "User ID: $userId")
                        Log.d(TAG, "  - BuyHistory count: ${buyHistory.childrenCount}")
                        
                        buyHistory.children.forEach { orderSnapshot ->
                            Log.d(TAG, "    Order: ${orderSnapshot.key}")
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "❌ Failed to read users: ${error.message}")
            }
        })
    }
    
    /**
     * Check Firebase Database Rules
     */
    fun checkDatabaseRules() {
        Log.d(TAG, "=== CHECKING DATABASE RULES ===")
        Log.d(TAG, "Testing read permission on OrderDetails...")
        
        val testRef = FirebaseDatabase.getInstance().reference.child("OrderDetails")
        testRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "✅ READ permission OK")
            }

            override fun onCancelled(error: DatabaseError) {
                when (error.code) {
                    DatabaseError.PERMISSION_DENIED -> {
                        Log.e(TAG, "❌ PERMISSION DENIED - Check Firebase Rules!")
                        Log.e(TAG, "Your Firebase Rules might be blocking read access")
                    }
                    else -> {
                        Log.e(TAG, "❌ Error: ${error.message}")
                    }
                }
            }
        })
    }
    
    /**
     * Print complete database structure
     */
    fun printDatabaseStructure() {
        Log.d(TAG, "=== PRINTING DATABASE STRUCTURE ===")
        
        val rootRef = FirebaseDatabase.getInstance().reference
        rootRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "Root children:")
                snapshot.children.forEach { child ->
                    Log.d(TAG, "  - ${child.key} (${child.childrenCount} items)")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to read root: ${error.message}")
            }
        })
    }
}

