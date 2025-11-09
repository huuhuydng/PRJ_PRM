package com.example.wavesoffoodadmin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.wavesoffoodadmin.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {
    private val binding : ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    
    // Firebase listeners for real-time statistics
    private var pendingOrdersListener: ValueEventListener? = null
    private var completedOrdersListener: ValueEventListener? = null
    private var earningsListener: ValueEventListener? = null
    
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        
        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance()
        databaseReference = database.reference
        
        // Configure Google Sign In
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
        
        // Load real-time statistics
        loadPendingOrdersCount()
        loadCompletedOrdersCount()
        loadTotalEarnings()
        binding.addMenu.setOnClickListener {
            val intent = Intent(this, AddItemActivity::class.java)
            startActivity(intent)
        }

        binding.allItemMenu.setOnClickListener {
            val intent = Intent(this, AllItemActivity::class.java)
            startActivity(intent)
        }

        binding.outForDeliveryButton.setOnClickListener {
            val intent = Intent(this, OutForDeliveryActivity::class.java)
            startActivity(intent)
        }

        binding.profile.setOnClickListener {
            val intent = Intent(this, AdminProfileActivity::class.java)
            startActivity(intent)
        }

        binding.createUser.setOnClickListener {
            val intent = Intent(this, CreateUserActivity::class.java)
            startActivity(intent)
        }

        binding.pendingOrderTextView.setOnClickListener {
            val intent = Intent(this, PendingOrderActivity::class.java)
            startActivity(intent)
        }
        
        // Logout button click listener
        binding.cardView2.setOnClickListener {
            showLogoutDialog()
        }
    }
    
    /**
     * Show confirmation dialog before logout
     */
    private fun showLogoutDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to logout?")
        
        builder.setPositiveButton("Yes") { dialog, _ ->
            performLogout()
            dialog.dismiss()
        }
        
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        
        val dialog = builder.create()
        dialog.show()
    }
    
    /**
     * Perform logout operations:
     * 1. Sign out from Firebase Auth
     * 2. Sign out from Google Sign-In
     * 3. Clear user session data
     * 4. Navigate to Login screen
     */
    private fun performLogout() {
        try {
            // Sign out from Firebase
            auth.signOut()
            
            // Sign out from Google
            googleSignInClient.signOut().addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Clear any cached data if needed
                    clearUserSession()
                    
                    // Show success message
                    Toast.makeText(
                        this,
                        "Logged out successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Navigate to Login screen
                    navigateToLogin()
                } else {
                    // Even if Google sign out fails, still navigate to login
                    Toast.makeText(
                        this,
                        "Logged out",
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToLogin()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Logout failed: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * Clear any user session data (SharedPreferences, cached data, etc.)
     */
    private fun clearUserSession() {
        // Clear SharedPreferences if you're using them
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
        
        // Clear any other cached user data here if needed
    }
    
    /**
     * Navigate to Login screen and clear activity stack
     */
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        // Clear the activity stack so user can't go back after logout
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    /**
     * Check if user is authenticated on activity resume
     */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "========================================")
        Log.d(TAG, "📱 onResume: MainActivity is resuming...")
        Log.d(TAG, "========================================")
        
        // Check user authentication
        checkUserAuthentication()
        
        // Force refresh statistics when returning to MainActivity
        // This ensures we get the latest data from Firebase
        Log.d(TAG, "🔄 Triggering force refresh of statistics...")
        refreshStatistics()
    }
    
    /**
     * Verify user authentication status
     */
    private fun checkUserAuthentication() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // User is not authenticated, redirect to login
            Log.w(TAG, "⚠️ User not authenticated - redirecting to login")
            navigateToLogin()
        } else {
            Log.d(TAG, "✅ User authenticated: ${currentUser.email ?: currentUser.uid}")
        }
    }
    
    /**
     * Load pending orders count from Firebase with real-time updates
     */
    private fun loadPendingOrdersCount() {
        val pendingOrdersRef = databaseReference.child("OrderDetails")
        
        Log.d(TAG, "📊 Setting up real-time listener: Pending Orders")
        
        pendingOrdersListener = pendingOrdersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount.toInt()
                binding.textView3.text = count.toString()
                Log.d(TAG, "🔄 REAL-TIME UPDATE: Pending Orders = $count")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "❌ Error loading pending orders: ${error.message}")
                binding.textView3.text = "0"
            }
        })
    }
    
    /**
     * Load completed orders count from Firebase with real-time updates
     */
    private fun loadCompletedOrdersCount() {
        val completedOrdersRef = databaseReference.child("CompleteOrder")
        
        Log.d(TAG, "📊 Setting up real-time listener: Completed Orders")
        
        completedOrdersListener = completedOrdersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount.toInt()
                binding.textView5.text = count.toString()
                Log.d(TAG, "🔄 REAL-TIME UPDATE: Completed Orders = $count")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "❌ Error loading completed orders: ${error.message}")
                binding.textView5.text = "0"
            }
        })
    }
    
    /**
     * Load total earnings from Firebase with real-time updates
     * Supports multiple price formats: "150$", "1,500$", "$100", "50"
     */
    private fun loadTotalEarnings() {
        val completeOrderRef = databaseReference.child("CompleteOrder")
        
        Log.d(TAG, "📊 Setting up real-time listener: Total Earnings")
        
        earningsListener = completeOrderRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalEarnings = 0.0
                var orderCount = 0
                
                for (orderSnapshot in snapshot.children) {
                    val totalPrice = orderSnapshot.child("totalPrice").getValue(String::class.java)
                    totalPrice?.let {
                        // Parse price: supports "150$", "1,500$", "$100", "50"
                        val price = parsePrice(it)
                        totalEarnings += price
                        orderCount++
                        
                        // Log individual order price for debugging (only first 5)
                        if (orderCount <= 5) {
                            Log.d(TAG, "   Order #$orderCount: $it → \$$price")
                        }
                    }
                }
                
                if (orderCount > 5) {
                    Log.d(TAG, "   ... and ${orderCount - 5} more orders")
                }
                
                // Format total earnings with $ sign
                val formattedEarnings = String.format("%.0f$", totalEarnings)
                binding.textView7.text = formattedEarnings
                Log.d(TAG, "🔄 REAL-TIME UPDATE: Total Earnings = $formattedEarnings (from $orderCount orders)")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "❌ Error loading total earnings: ${error.message}")
                binding.textView7.text = "0$"
            }
        })
    }
    
    /**
     * Parse price string to double
     * Supports formats: "150$", "1,500$", "$100", "50"
     */
    private fun parsePrice(priceString: String): Double {
        return try {
            // Remove $ sign, commas, and whitespace
            val cleaned = priceString.replace("$", "")
                .replace(",", "")
                .replace(" ", "")
                .trim()
            
            cleaned.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse price: $priceString", e)
            0.0
        }
    }
    
    /**
     * Force refresh statistics by reading directly from Firebase
     * This ensures we get the latest data when returning to MainActivity
     */
    private fun refreshStatistics() {
        Log.d(TAG, "========== FORCE REFRESH START ==========")
        Log.d(TAG, "🔄 Force refreshing all statistics from Firebase...")
        
        // Force read Pending Orders directly from Firebase
        databaseReference.child("OrderDetails").get()
            .addOnSuccessListener { snapshot ->
                val count = snapshot.childrenCount.toInt()
                binding.textView3.text = count.toString()
                Log.d(TAG, "✅ Force refresh SUCCESS - Pending Orders: $count")
                Log.d(TAG, "   → Updated textView3 with new count")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Force refresh FAILED - Pending Orders: ${e.message}")
            }
        
        // Force read Completed Orders directly from Firebase
        databaseReference.child("CompleteOrder").get()
            .addOnSuccessListener { snapshot ->
                val count = snapshot.childrenCount.toInt()
                binding.textView5.text = count.toString()
                Log.d(TAG, "✅ Force refresh SUCCESS - Completed Orders: $count")
                Log.d(TAG, "   → Updated textView5 with new count")
                
                // Calculate total earnings while we have the snapshot
                var totalEarnings = 0.0
                var orderCount = 0
                
                Log.d(TAG, "💰 Calculating total earnings from $count completed orders...")
                
                for (orderSnapshot in snapshot.children) {
                    val totalPrice = orderSnapshot.child("totalPrice").getValue(String::class.java)
                    totalPrice?.let {
                        val price = parsePrice(it)
                        totalEarnings += price
                        orderCount++
                        
                        // Log first 3 orders for debugging
                        if (orderCount <= 3) {
                            Log.d(TAG, "   Order #$orderCount: $it → \$$price")
                        }
                    }
                }
                
                if (orderCount > 3) {
                    Log.d(TAG, "   ... and ${orderCount - 3} more orders")
                }
                
                val formattedEarnings = String.format("%.0f$", totalEarnings)
                binding.textView7.text = formattedEarnings
                Log.d(TAG, "✅ Force refresh SUCCESS - Total Earnings: $formattedEarnings")
                Log.d(TAG, "   → Calculated from $orderCount orders")
                Log.d(TAG, "   → Updated textView7 with new total")
                Log.d(TAG, "========== FORCE REFRESH COMPLETE ==========")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Force refresh FAILED - Completed Orders: ${e.message}")
                Log.e(TAG, "========== FORCE REFRESH FAILED ==========")
            }
    }
    
    /**
     * Remove Firebase listeners to prevent memory leaks
     */
    override fun onDestroy() {
        super.onDestroy()
        
        // Remove all listeners
        pendingOrdersListener?.let {
            databaseReference.child("OrderDetails").removeEventListener(it)
        }
        completedOrdersListener?.let {
            databaseReference.child("CompleteOrder").removeEventListener(it)
        }
        earningsListener?.let {
            databaseReference.child("CompleteOrder").removeEventListener(it)
        }
        
        Log.d(TAG, "onDestroy: All listeners removed")
    }
}