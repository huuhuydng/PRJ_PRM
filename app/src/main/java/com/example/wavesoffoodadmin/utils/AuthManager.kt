package com.example.wavesoffoodadmin.utils

import android.content.Context
import android.content.Intent
import com.example.wavesoffoodadmin.LoginActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/**
 * Singleton class to manage authentication operations
 * Handles Firebase Auth and Google Sign-In
 */
object AuthManager {
    
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var googleSignInClient: GoogleSignInClient? = null
    
    /**
     * Initialize Google Sign-In Client
     * Call this in your Application class or before using logout
     */
    fun initialize(context: Context, webClientId: String) {
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, googleSignInOptions)
    }
    
    /**
     * Get current authenticated user
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
    
    /**
     * Check if user is logged in
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
    
    /**
     * Perform complete logout
     * - Signs out from Firebase
     * - Signs out from Google
     * - Clears session data
     */
    fun logout(
        context: Context,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        try {
            // Sign out from Firebase
            auth.signOut()
            
            // Sign out from Google
            googleSignInClient?.signOut()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    clearUserSession(context)
                    onSuccess()
                } else {
                    clearUserSession(context)
                    onSuccess() // Still proceed even if Google sign out fails
                }
            } ?: run {
                // If Google client not initialized, just clear session
                clearUserSession(context)
                onSuccess()
            }
        } catch (e: Exception) {
            onError(e)
        }
    }
    
    /**
     * Clear all user session data
     */
    private fun clearUserSession(context: Context) {
        val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
        
        // Clear any other app-specific cached data here
    }
    
    /**
     * Navigate to login screen and clear activity stack
     */
    fun navigateToLogin(context: Context) {
        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }
    
    /**
     * Get user email
     */
    fun getUserEmail(): String? {
        return auth.currentUser?.email
    }
    
    /**
     * Get user UID
     */
    fun getUserId(): String? {
        return auth.currentUser?.uid
    }
    
    /**
     * Get user display name
     */
    fun getUserDisplayName(): String? {
        return auth.currentUser?.displayName
    }
    
    /**
     * Check if user email is verified
     */
    fun isEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified ?: false
    }
}

