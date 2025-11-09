package com.example.wavesoffoodadmin

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.wavesoffoodadmin.databinding.ActivityCreateUserBinding
import com.example.wavesoffoodadmin.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class CreateUserActivity : AppCompatActivity() {
    private val binding : ActivityCreateUserBinding by lazy {
        ActivityCreateUserBinding.inflate(layoutInflater)
    }
    
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private var progressDialog: ProgressDialog? = null
    
    companion object {
        private const val TAG = "CreateUserActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        databaseReference = database.reference
        
        // Setup click listeners
        setupClickListeners()
    }
    
    /**
     * Setup click listeners
     */
    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            finish()
        }
        
        binding.createUserButton.setOnClickListener {
            createNewAdminUser()
        }
    }
    
    /**
     * Create new admin user
     */
    private fun createNewAdminUser() {
        val name = binding.name.text.toString().trim()
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString().trim()
        
        // Validate inputs
        if (!validateInputs(name, email, password)) {
            return
        }
        
        showProgressDialog("Creating new admin...")
        
        // Create user with Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    user?.let {
                        // Save admin data to Firebase Database
                        saveAdminUserData(it.uid, name, email, password)
                    } ?: run {
                        hideProgressDialog()
                        Toast.makeText(this, "Admin creation failed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    hideProgressDialog()
                    val errorMessage = task.exception?.message ?: "Unknown error"
                    Log.e(TAG, "Error creating admin: $errorMessage")
                    Toast.makeText(this, "Failed to create admin: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    /**
     * Save admin user data to Firebase Database in "admin" node
     */
    private fun saveAdminUserData(userId: String, name: String, email: String, password: String) {
        val userModel = UserModel(
            name = name,
            email = email,
            password = password
        )
        
        // Save to "admin" node instead of "user" node
        databaseReference.child("admin").child(userId).setValue(userModel)
            .addOnSuccessListener {
                hideProgressDialog()
                Log.d(TAG, "✅ Admin created successfully: $email")
                Toast.makeText(this, "✅ Admin created successfully!", Toast.LENGTH_SHORT).show()
                clearInputFields()
            }
            .addOnFailureListener { e ->
                hideProgressDialog()
                Log.e(TAG, "❌ Error saving admin data: ${e.message}")
                Toast.makeText(this, "Admin created but data save failed: ${e.message}", Toast.LENGTH_LONG).show()
                // Still clear fields even if save failed
                clearInputFields()
            }
    }
    
    /**
     * Validate input fields
     */
    private fun validateInputs(name: String, email: String, password: String): Boolean {
        if (name.isEmpty()) {
            binding.name.error = "Name is required"
            binding.name.requestFocus()
            return false
        }
        
        if (email.isEmpty()) {
            binding.email.error = "Email is required"
            binding.email.requestFocus()
            return false
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.email.error = "Invalid email format"
            binding.email.requestFocus()
            return false
        }
        
        if (password.isEmpty()) {
            binding.password.error = "Password is required"
            binding.password.requestFocus()
            return false
        }
        
        if (password.length < 6) {
            binding.password.error = "Password must be at least 6 characters"
            binding.password.requestFocus()
            return false
        }
        
        return true
    }
    
    /**
     * Clear input fields after successful creation
     */
    private fun clearInputFields() {
        binding.name.text?.clear()
        binding.email.text?.clear()
        binding.password.text?.clear()
    }
    
    /**
     * Show progress dialog
     */
    private fun showProgressDialog(message: String) {
        progressDialog = ProgressDialog(this)
        progressDialog?.setMessage(message)
        progressDialog?.setCancelable(false)
        progressDialog?.show()
    }
    
    /**
     * Hide progress dialog
     */
    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        hideProgressDialog()
    }
}

