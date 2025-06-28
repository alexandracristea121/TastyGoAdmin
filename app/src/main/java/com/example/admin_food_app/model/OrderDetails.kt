package com.example.admin_food_app.model

import java.io.Serializable

class OrderDetails() : Serializable {

    var adminUserId: String? = null
    var userUid: String? = null
    var userName: String? = null
    var foodNames: MutableList<String>? = null
    var foodImages: MutableList<String>? = null
    var foodPrices: MutableList<String>? = null
    var foodQuantities: MutableList<Int>? = null
    var userLocation: String? = null
    var userLatitude: String? = null
    var userLongitude: String? = null
    var restaurantLocation: String? = null
    var totalPrice: String? = null
    var phoneNumber: String? = null
    var paymentReceived: Boolean = false
    var itemPushkey: String? = null
    var orderDelivered: Boolean = false

}
