package com.example.wavesoffood.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.wavesoffood.R
import com.example.wavesoffood.databinding.FragmentProfileBinding
import com.example.wavesoffood.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private var isEditMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUserData()
        setEditMode(false) // Initial state: disabled and faded

        binding.editButton.setOnClickListener {
            isEditMode = !isEditMode
            setEditMode(isEditMode)
            if(isEditMode){
                Toast.makeText(requireContext(), "You can now edit your profile", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Editing disabled", Toast.LENGTH_SHORT).show()
            }
        }

        binding.saveInforButton.setOnClickListener {
            val name = binding.name.text.toString()
            val email = binding.email.text.toString()
            val address = binding.address.text.toString()
            val phone = binding.phone.text.toString()
            updateUserData(name, email, address, phone)
        }
        
        binding.logoutButton.setOnClickListener {
            logoutUser()
        }
    }
    
    private fun logoutUser() {
        // Show confirmation
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                // Call MainActivity's logout function
                (activity as? com.example.wavesoffood.MainActivity)?.logoutUser()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setEditMode(enabled: Boolean) {
        val alpha = if (enabled) 1.0f else 0.5f
        binding.apply {
            name.isEnabled = enabled
            address.isEnabled = enabled
            email.isEnabled = enabled
            phone.isEnabled = enabled

            nameContainer.alpha = alpha
            addressContainer.alpha = alpha
            emailContainer.alpha = alpha
            phoneContainer.alpha = alpha
        }
    }

    private fun updateUserData(
        name: String,
        email: String,
        address: String,
        phone: String
    ) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userReference = database.getReference("user").child(userId)
            val userData = hashMapOf(
                "name" to name,
                "address" to address,
                "email" to email,
                "phone" to phone
            )
            userReference.setValue(userData).addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated successfully😊", Toast.LENGTH_SHORT).show()
                isEditMode = false
                setEditMode(false) // Disable fields after saving
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Profile update failed😭", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setUserData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userReference = database.getReference("user").child(userId)
            userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userProfile = snapshot.getValue(UserModel::class.java)
                        if (userProfile != null) {
                            binding.name.setText(userProfile.name)
                            binding.address.setText(userProfile.address)
                            binding.email.setText(userProfile.email)
                            binding.phone.setText(userProfile.phone)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load user data.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}