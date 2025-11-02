package com.example.wavesoffood

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wavesoffood.adapter.DeliveryAdapter
import com.example.wavesoffood.databinding.ActivityOutForDeliveryBinding

class OutForDeliveryActivity : AppCompatActivity() {
    private val binding: ActivityOutForDeliveryBinding by lazy {
        ActivityOutForDeliveryBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.backButton.setOnClickListener {
            finish()
        }

        val customerNames = arrayListOf(
            "John Doe",
            "Jane Smith",
            "Mike Johnson"
        )
        val moneyStatus = arrayListOf(
            "received",
            "notReceived",
            "Pending"
        )
        val adapter = DeliveryAdapter(customerNames, moneyStatus)
        binding.deliveryRecyclerView.adapter = adapter
        binding.deliveryRecyclerView.layoutManager = LinearLayoutManager(this)
    }
}