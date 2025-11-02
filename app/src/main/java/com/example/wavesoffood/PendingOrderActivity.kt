package com.example.wavesoffood

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wavesoffood.adapter.PendingOrderAdapter
import com.example.wavesoffood.databinding.ActivityPendingOrderBinding

class PendingOrderActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityPendingOrderBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.backButton.setOnClickListener {
            finish()
        }

        val orderedCustomerNames = arrayListOf(
            "John Doe",
            "Jane Smith",
            "Mike Johnson"
        )
        val orderedQuantity = arrayListOf(
            "8",
            "6",
            "5"
        )
        val orderedFoodImage = arrayListOf(
            R.drawable.menu1,
            R.drawable.menu2,
            R.drawable.menu3
        )
        val adapter = PendingOrderAdapter(orderedCustomerNames, orderedQuantity, orderedFoodImage, this)
        binding.pendingOrderRecyclerView.adapter = adapter
        binding.pendingOrderRecyclerView.layoutManager = LinearLayoutManager(this)
    }

}