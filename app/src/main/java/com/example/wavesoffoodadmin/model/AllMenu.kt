package com.example.wavesoffoodadmin.model

data class AllMenu(
    val foodName :String? = null,
    val foodPrice :String? = null,
    val foodDescription :String? = null,
    val foodImage :String? = null,
    val foodIngredient :String? = null,
    var key: String? = null  // Firebase push key for deletion
)
