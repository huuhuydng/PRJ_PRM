package com.example.wavesoffood

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wavesoffood.adaptar.RecentBuyAdapter
import com.example.wavesoffood.databinding.ActivityRecentOrderItemsBinding
import com.example.wavesoffood.model.OrderDetails

class RecentOrderItems : AppCompatActivity() {

    private  val binding : ActivityRecentOrderItemsBinding by lazy {
        ActivityRecentOrderItemsBinding.inflate(layoutInflater)
    }
    private  lateinit var allFoodNames : ArrayList<String>
    private  lateinit var allFoodPrices : ArrayList<String>
    private  lateinit var allFoodImages : ArrayList<String>
    private  lateinit var allFoodQuantites : ArrayList<Int>




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }


        val recentOrderItems= intent.getSerializableExtra("RecentBuyOrderItem") as ArrayList<OrderDetails>
        recentOrderItems?.let {orderDetails ->
            if (orderDetails.isNotEmpty()){
                val recentOrderItem = orderDetails[0]
                allFoodNames = recentOrderItem.foodNames as ArrayList<String>
                allFoodImages = recentOrderItem.foodImages as ArrayList<String>
                allFoodPrices = recentOrderItem.foodPrices as ArrayList<String>
                allFoodQuantites = recentOrderItem.foodQuantities as ArrayList<Int>
            }
        }
        setAdapters()
    }

    private fun setAdapters() {
        val rv = binding.recyclerViewRecentBuy
        rv.layoutManager = LinearLayoutManager(this)
        val adapter = RecentBuyAdapter(this,allFoodNames,allFoodImages,allFoodPrices,allFoodQuantites)
        rv.adapter = adapter

    }
}