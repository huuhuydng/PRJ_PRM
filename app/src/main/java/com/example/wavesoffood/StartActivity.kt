package com.example.wavesoffood

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.wavesoffood.databinding.ActivityStartBinding

class StartActivity : AppCompatActivity() {
    // Khởi tạo binding bằng by lazy để đảm bảo nó chỉ được tạo khi cần
    private val binding: ActivityStartBinding by lazy {
        ActivityStartBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate( savedInstanceState)
        setContentView(binding.root)
        binding.nextButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

    }
}
