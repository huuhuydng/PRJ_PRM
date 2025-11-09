package com.example.wavesoffoodadmin

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.wavesoffoodadmin.databinding.ActivityAdminProfileBinding
import com.example.wavesoffoodadmin.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminProfileActivity : AppCompatActivity() {
    private val binding : ActivityAdminProfileBinding by lazy {
        ActivityAdminProfileBinding.inflate(layoutInflater)
    }
    
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private var progressDialog: ProgressDialog? = null
    private var isEditMode = false
    
    companion object {
        private const val TAG = "AdminProfileActivity"
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
        
        // Set fields to non-editable initially
        setFieldsEditable(false)
        
        // Load admin profile data
        loadAdminProfile()
    }
    
    /**
     * Setup click listeners for buttons
     */
    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            finish()
        }
        
        binding.editButton.setOnClickListener {
            toggleEditMode()
        }
        
        binding.saveButton.setOnClickListener {
            saveAdminProfile()
        }
    }
    
    /**
     * Enable edit mode
     */
    private fun enableEditMode() {
        Log.d(TAG, "Enabling edit mode")
        isEditMode = true
        setFieldsEditable(true)
        updateEditButton()
        binding.name.requestFocus()
        Toast.makeText(this, "Edit mode enabled", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Disable edit mode
     */
    private fun disableEditMode() {
        Log.d(TAG, "Disabling edit mode")
        isEditMode = false
        setFieldsEditable(false)
        updateEditButton()
    }
    
    /**
     * Update edit button appearance based on edit mode
     */
    private fun updateEditButton() {
        if (isEditMode) {
            binding.editButton.text = "Cancel"
            binding.editButton.setTextColor(getColor(R.color.error))
        } else {
            binding.editButton.text = "Edit"
            binding.editButton.setTextColor(getColor(R.color.black))
        }
    }
    
    /**
     * Toggle edit mode (deprecated - keeping for compatibility)
     */
    private fun toggleEditMode() {
        if (isEditMode) {
            // Cancel edit mode
            disableEditMode()
            loadAdminProfile() // Reload original data
        } else {
            // Enable edit mode
            enableEditMode()
        }
    }
    
    /**
     * Enable/disable fields for editing
     */
    private fun setFieldsEditable(enabled: Boolean) {
        binding.name.isEnabled = enabled
        binding.address.isEnabled = enabled
        binding.email.isEnabled = enabled
        binding.phone.isEnabled = enabled
        binding.password.isEnabled = enabled
    }
    
    /**
     * Load admin profile data from Firebase
     */
    private fun loadAdminProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        showProgressDialog("Loading profile...")
        
        val userId = currentUser.uid
        
        // Try to load from "user" node first
        loadFromUserNode(userId)
    }
    
    /**
     * Load profile from "user" node in Firebase
     */
    private fun loadFromUserNode(userId: String) {
        databaseReference.child("user").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    hideProgressDialog()
                    
                    if (snapshot.exists()) {
                        val userModel = snapshot.getValue(UserModel::class.java)
                        userModel?.let {
                            displayAdminProfile(it)
                        } ?: run {
                            displayDefaultProfile()
                        }
                    } else {
                        // If not found in "user" node, try to get from Firebase Auth
                        displayDefaultProfile()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    hideProgressDialog()
                    Log.e(TAG, "Error loading profile: ${error.message}")
                    Toast.makeText(this@AdminProfileActivity, "Failed to load profile", Toast.LENGTH_SHORT).show()
                    displayDefaultProfile()
                }
            })
    }
    
    /**
     * Display admin profile data
     */
    private fun displayAdminProfile(userModel: UserModel) {
        binding.name.setText(userModel.name ?: "")
        binding.email.setText(auth.currentUser?.email ?: "")
        binding.address.setText(userModel.address ?: "")
        binding.phone.setText(userModel.phone ?: "")
        binding.password.setText("••••••••") // Don't show actual password
    }
    
    /**
     * Display default profile from Firebase Auth
     */
    private fun displayDefaultProfile() {
        val currentUser = auth.currentUser
        currentUser?.let {
            binding.name.setText(it.displayName ?: "")
            binding.email.setText(it.email ?: "")
            binding.address.setText("")
            binding.phone.setText("")
            binding.password.setText("••••••••")
        }
    }
    
    /**
     * Save admin profile to Firebase
     */
    private fun saveAdminProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Validate inputs
        if (!validateProfileInputs()) {
            return
        }
        
        showProgressDialog("Saving profile...")
        
        val userId = currentUser.uid
        val name = binding.name.text.toString().trim()
        val email = binding.email.text.toString().trim()
        val address = binding.address.text.toString().trim()
        val phone = binding.phone.text.toString().trim()
        val password = binding.password.text.toString().trim()
        
        // Create/update UserModel
        val userModel = UserModel(
            name = name,
            email = email,
            password = password.ifEmpty { null }, // Only save if changed
            address = address.ifEmpty { null },
            phone = phone.ifEmpty { null }
        )
        
        // Save to Firebase
        databaseReference.child("user").child(userId).setValue(userModel)
            .addOnSuccessListener {
                hideProgressDialog()
                
                // Update email in Firebase Auth if changed
                if (email != currentUser.email) {
                    currentUser.updateEmail(email)
                        .addOnSuccessListener {
                            Log.d(TAG, "Profile and email updated successfully")
                            Toast.makeText(this, "✅ Profile updated successfully!", Toast.LENGTH_SHORT).show()
                            disableEditMode() // Exit edit mode
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error updating email: ${e.message}")
                            Toast.makeText(this, "Profile saved but email update failed", Toast.LENGTH_LONG).show()
                            disableEditMode()
                        }
                } else {
                    Log.d(TAG, "Profile updated successfully")
                    Toast.makeText(this, "✅ Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    disableEditMode() // Exit edit mode
                }
            }
            .addOnFailureListener { e ->
                hideProgressDialog()
                Log.e(TAG, "Error saving profile: ${e.message}")
                Toast.makeText(this, "❌ Failed to save profile: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
    
    /**
     * Validate profile inputs
     */
    private fun validateProfileInputs(): Boolean {
        val name = binding.name.text.toString().trim()
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString().trim()
        
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
        
        // Password validation (only if changed)
        if (password.isNotEmpty() && password != "••••••••" && password.length < 6) {
            binding.password.error = "Password must be at least 6 characters"
            binding.password.requestFocus()
            return false
        }
        
        return true
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
    
    /**
     * Override back button to handle cancel when in edit mode
     */
    override fun onBackPressed() {
        if (isEditMode) {
            // Cancel edit mode without saving
            disableEditMode()
            loadAdminProfile() // Reload original data
            Toast.makeText(this, "Changes discarded", Toast.LENGTH_SHORT).show()
        } else {
            super.onBackPressed()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        hideProgressDialog()
    }
}