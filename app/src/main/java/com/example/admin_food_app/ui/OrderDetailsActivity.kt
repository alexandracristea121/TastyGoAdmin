package com.example.admin_food_app.ui

import android.annotation.SuppressLint
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.admin_food_app.R
import com.example.admin_food_app.adapter.OrderDetailsAdapter
import com.example.admin_food_app.databinding.ActivityOrderDetailsBinding
import com.example.admin_food_app.model.OrderDetails
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale

@Suppress("DEPRECATION")
class OrderDetailsActivity : AppCompatActivity() {
    private val binding: ActivityOrderDetailsBinding by lazy {
        ActivityOrderDetailsBinding.inflate(layoutInflater)
    }

    private var userName: String? = null
    private var userLocation: String? = null
    private var restaurantLocation: String? = null
    private var phoneNumber: String? = null
    private var totalPrice: String? = null
    private var foodNames: ArrayList<String> = arrayListOf()
    private var foodImages: ArrayList<String> = arrayListOf()
    private var foodQuantity: ArrayList<Int> = arrayListOf()
    private var foodPrices: ArrayList<String> = arrayListOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }
        getDataFromIntent()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getDataFromIntent() {
        val receivedOrderDetails = intent.getSerializableExtra("UserOrderDetails") as OrderDetails

        receivedOrderDetails.let { orderDetails ->
            userName = orderDetails.userName
            foodNames = orderDetails.foodNames as ArrayList<String>
            foodImages = orderDetails.foodImages as ArrayList<String>
            foodQuantity = orderDetails.foodQuantities as ArrayList<Int>
            phoneNumber = orderDetails.phoneNumber
            foodPrices = orderDetails.foodPrices as ArrayList<String>
            totalPrice = orderDetails.totalPrice?.replace("$", "RON")
            userLocation = orderDetails.userLocation
            restaurantLocation = orderDetails.restaurantLocation

            val (latitude, longitude) = getLatLongFromAddress(userLocation ?: "")
            orderDetails.userLatitude = latitude
            orderDetails.userLongitude = longitude

            val itemPushKey = orderDetails.itemPushkey
            if (!itemPushKey.isNullOrEmpty()) {
                val orderRef = FirebaseDatabase.getInstance().reference
                    .child("OrderDetails")
                    .child(itemPushKey)

                val locationUpdate = mapOf(
                    "userLatitude" to latitude,
                    "userLongitude" to longitude
                )

                orderRef.updateChildren(locationUpdate)
                    .addOnSuccessListener {
                        Log.d("Firebase Update", "Latitude and longitude updated successfully.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firebase Update Error", "Error updating latitude and longitude: ${e.message}")
                    }
            } else {
                Log.e("Firebase Error", "Item push key is null or empty.")
            }

            binding.address.text = "$userLocation \nLat: $latitude, Long: $longitude"

            updateOrderCoordinatesInFirebase(orderDetails)
            binding.name.text = userName
            binding.phone.text = phoneNumber
            binding.totalPay.text = totalPrice
            binding.address.text = userLocation
            setAdapter()
        }
    }

    private fun updateOrderCoordinatesInFirebase(orderDetails: OrderDetails) {
        val database = FirebaseDatabase.getInstance()
        val orderReference = orderDetails.itemPushkey?.let {
            database.reference.child("OrderDetails").child(it)
        }

        orderReference?.apply {
            child("userLatitude").setValue(orderDetails.userLatitude)
            child("userLongitude").setValue(orderDetails.userLongitude)
        }
    }

    private fun setAdapter() {
        binding.orderDetailRecyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = OrderDetailsAdapter(this, foodNames, foodImages, foodQuantity, foodPrices)
        binding.orderDetailRecyclerView.adapter = adapter
    }

    private fun getLatLongFromAddress(address: String): Pair<String?, String?> {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocationName(address, 1)
            if (!addresses.isNullOrEmpty()) {
                val latitude = addresses[0].latitude.toString()
                val longitude = addresses[0].longitude.toString()
                return Pair(latitude, longitude)
            }
        } catch (e: Exception) {
            Log.e("Geocoder Error", "Error getting latitude and longitude: ${e.message}")
        }
        return Pair(null, null)
    }
}