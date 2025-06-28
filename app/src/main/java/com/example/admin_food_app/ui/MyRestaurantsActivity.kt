package com.example.admin_food_app.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.admin_food_app.databinding.ActivityMyRestaurantsBinding

@Suppress("DEPRECATION")
class MyRestaurantsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyRestaurantsBinding

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyRestaurantsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = ContextCompat.getColor(this, android.R.color.white)
        window.navigationBarColor = ContextCompat.getColor(this, android.R.color.white)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.addRestaurantButton.setOnClickListener {
            val intent = Intent(this, AddRestaurantsActivity::class.java)
            startActivity(intent)
        }

        binding.existingRestaurantsButton.setOnClickListener {
            val intent = Intent(this, ManageExistingRestaurantsActivity::class.java)
            startActivity(intent)
        }

        binding.manageMenuItemsButton.setOnClickListener {
            val intent = Intent(this, AddItemActivity::class.java)
            startActivity(intent)
        }
    }
}