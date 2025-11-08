package com.example.wavesoffoodadmin.model

import com.google.firebase.database.PropertyName

data class UserModel(
    // Map "username" from Firebase to "name" property in code
    @PropertyName("username")
    var name: String? = null,
    var email: String? = null,
    var password: String? = null,
    var address: String? = null,
    var phone: String? = null
)




