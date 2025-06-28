package com.example.admin_food_app.model

data class UserModel(
    val name:String?=null,
    val email:String?=null,
    val password:String?=null,
    val nameOfRestaurant:String?=null,
    var address:String?=null,
    var phone:String?=null
)
