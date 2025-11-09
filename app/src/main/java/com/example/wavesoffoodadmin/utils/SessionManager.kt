package com.example.wavesoffoodadmin.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages user session data using SharedPreferences
 * Stores user information, preferences, and login state
 */
class SessionManager(context: Context) {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    private val editor: SharedPreferences.Editor = prefs.edit()
    
    companion object {
        private const val PREF_NAME = "user_prefs"
        
        // Keys for storing data
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_LOGIN_TIME = "login_time"
        private const val KEY_REMEMBER_ME = "remember_me"
    }
    
    /**
     * Save user login session
     */
    fun createLoginSession(
        userId: String,
        email: String,
        userName: String? = null,
        rememberMe: Boolean = false
    ) {
        editor.putString(KEY_USER_ID, userId)
        editor.putString(KEY_USER_EMAIL, email)
        editor.putString(KEY_USER_NAME, userName)
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
        editor.putBoolean(KEY_REMEMBER_ME, rememberMe)
        editor.apply()
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    /**
     * Get user ID
     */
    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }
    
    /**
     * Get user email
     */
    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }
    
    /**
     * Get user name
     */
    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }
    
    /**
     * Get login time
     */
    fun getLoginTime(): Long {
        return prefs.getLong(KEY_LOGIN_TIME, 0)
    }
    
    /**
     * Check if remember me is enabled
     */
    fun isRememberMeEnabled(): Boolean {
        return prefs.getBoolean(KEY_REMEMBER_ME, false)
    }
    
    /**
     * Update user name
     */
    fun updateUserName(userName: String) {
        editor.putString(KEY_USER_NAME, userName)
        editor.apply()
    }
    
    /**
     * Clear all session data (logout)
     */
    fun clearSession() {
        editor.clear()
        editor.apply()
    }
    
    /**
     * Get session details as a map
     */
    fun getSessionDetails(): Map<String, Any?> {
        return mapOf(
            "userId" to getUserId(),
            "email" to getUserEmail(),
            "userName" to getUserName(),
            "isLoggedIn" to isLoggedIn(),
            "loginTime" to getLoginTime(),
            "rememberMe" to isRememberMeEnabled()
        )
    }
    
    /**
     * Check if session is expired (after 30 days)
     */
    fun isSessionExpired(): Boolean {
        val loginTime = getLoginTime()
        if (loginTime == 0L) return true
        
        val currentTime = System.currentTimeMillis()
        val daysSinceLogin = (currentTime - loginTime) / (1000 * 60 * 60 * 24)
        
        return daysSinceLogin > 30
    }
}

