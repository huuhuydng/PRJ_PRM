package com.example.wavesoffoodadmin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.wavesoffoodadmin.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private val binding : ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        
        // Configure Google Sign In
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
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
        checkUserAuthentication()
    }
    
    /**
     * Verify user authentication status
     */
    private fun checkUserAuthentication() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // User is not authenticated, redirect to login
            navigateToLogin()
        }
    }
}