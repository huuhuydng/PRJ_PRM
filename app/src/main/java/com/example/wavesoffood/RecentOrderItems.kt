package com.example.wavesoffood

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wavesoffood.adaptar.RecentBuyAdapter
import com.example.wavesoffood.databinding.ActivityRecentOrderItemsBinding
import com.example.wavesoffood.model.OrderDetails

class RecentOrderItems : AppCompatActivity() {

    private val binding: ActivityRecentOrderItemsBinding by lazy {
        ActivityRecentOrderItemsBinding.inflate(layoutInflater)
    }
    private var allFoodNames: ArrayList<String> = arrayListOf()
    private var allFoodImages: ArrayList<String> = arrayListOf()
    private var allFoodPrices: ArrayList<String> = arrayListOf()
    private var allFoodQuantities: ArrayList<Int> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }

        val recentOrderItems = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("RecentBuyOrderItem", OrderDetails::class.java)
        } else {
            intent.getParcelableArrayListExtra<OrderDetails>("RecentBuyOrderItem")
        }

        recentOrderItems?.forEach { orderDetail ->
            orderDetail.foodNames?.let { allFoodNames.addAll(it) }
            orderDetail.foodImages?.let { allFoodImages.addAll(it) }
            orderDetail.foodPrices?.let { allFoodPrices.addAll(it) }
            orderDetail.foodQuantities?.let { allFoodQuantities.addAll(it) }
        }

        setAdapter()
    }

    private fun setAdapter() {
        val rv = binding.recyclerViewRecentBuy // Corrected ID
        rv.layoutManager = LinearLayoutManager(this)
        val adapter =
            RecentBuyAdapter(this, allFoodNames, allFoodImages, allFoodPrices, allFoodQuantities)
        rv.adapter = adapter
    }
}
