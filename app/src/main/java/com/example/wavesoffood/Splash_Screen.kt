package com.example.wavesoffood

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class Splash_Screen : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)
        
        auth = FirebaseAuth.getInstance()
        
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserAuthentication()
        }, 3000)
    }
    
    private fun checkUserAuthentication() {
        val currentUser = auth.currentUser
        
        if (currentUser != null) {
            // User đã đăng nhập -> vào MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        } else {
            // User chưa đăng nhập -> vào StartActivity
            val intent = Intent(this, StartActivity::class.java)
            startActivity(intent)
        }
        finish()
    }
}