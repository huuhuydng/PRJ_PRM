package com.example.wavesoffood

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.wavesoffood.databinding.ActivitySignBinding
import com.example.wavesoffood.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignActivity : AppCompatActivity() {
    private val binding: ActivitySignBinding by lazy {
        ActivitySignBinding.inflate(layoutInflater)
    }
    
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        
        // Create account button click
        binding.button3.setOnClickListener {
            val name = binding.editName.text.toString().trim()
            val email = binding.editMailOrPhone.text.toString().trim()
            val password = binding.editPassword.text.toString().trim()
            
            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            } else {
                createAccount(name, email, password)
            }
        }
        
        // Already have account button click
        binding.alreadyhavebutton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun createAccount(name: String, email: String, password: String) {
        // Show loading
        binding.button3.isEnabled = false
        binding.button3.text = "Creating..."
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.button3.isEnabled = true
                binding.button3.text = "Create Account"
                
                if (task.isSuccessful) {
                    // Account created successfully
                    val user = auth.currentUser
                    Log.d("SIGNUP", "Account created: ${user?.email}")
                    
                    // Save user data to database
                    saveUserData(name, email)
                } else {
                    // Failed to create account
                    Log.e("SIGNUP", "Account creation failed: ${task.exception?.message}")
                    Toast.makeText(
                        this, 
                        "Sign up failed: ${task.exception?.message}", 
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
    
    private fun saveUserData(name: String, email: String) {
        val userId = auth.currentUser?.uid ?: return
        val user = UserModel(name, email, "", "", "")
        
        database.reference.child("user").child(userId).setValue(user)
            .addOnSuccessListener {
                Log.d("SIGNUP", "User data saved successfully")
                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                
                // Navigate to MainActivity
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { exception ->
                Log.e("SIGNUP", "Failed to save user data: ${exception.message}")
                Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show()
            }
    }
}