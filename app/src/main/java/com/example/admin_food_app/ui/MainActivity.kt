package com.example.admin_food_app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.admin_food_app.R
import com.example.admin_food_app.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        binding.profile.setOnClickListener {
            val intent = Intent(this, AdminProfileActivity::class.java)
            startActivity(intent)
        }

        binding.myRestaurants.setOnClickListener {
            val intent = Intent(this, MyRestaurantsActivity::class.java)
            startActivity(intent)
        }

        binding.outForDeliveryButton.setOnClickListener {
            val intent = Intent(this, OutForDeliveryActivity::class.java)
            startActivity(intent)
        }

        binding.orderManagement.setOnClickListener {
            val intent = Intent(this, OrderManagementActivity::class.java)
            startActivity(intent)
        }

        binding.logout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        observePendingOrders()
        observeCompletedOrders()
        observeWholeTimeEarning()
    }

    private fun observePendingOrders() {
        val currentUserId = auth.currentUser?.uid ?: return
        val ordersReference: DatabaseReference = database.reference.child("orders")

        ordersReference.orderByChild("adminUserId").equalTo(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(snapshot: DataSnapshot) {
                    var pendingOrderCount = 0

                    for (orderSnapshot in snapshot.children) {
                        val orderDelivered = orderSnapshot.child("orderDelivered").getValue(Boolean::class.java) ?: false

                        if (!orderDelivered) {
                            pendingOrderCount++
                        }
                    }

                    binding.pendingOrders.text = pendingOrderCount.toString()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun observeCompletedOrders() {
        val currentUserId = auth.currentUser?.uid ?: return
        val ordersReference: DatabaseReference = database.reference.child("orders")

        ordersReference.orderByChild("adminUserId").equalTo(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(snapshot: DataSnapshot) {
                    var completedOrderCount = 0
                    for (orderSnapshot in snapshot.children) {
                        val orderDelivered = orderSnapshot.child("orderDelivered").getValue(Boolean::class.java) ?: false
                        if (orderDelivered) {
                            completedOrderCount++
                        }
                    }
                    binding.completeOrders.text = completedOrderCount.toString()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun observeWholeTimeEarning() {
        val currentUserId = auth.currentUser?.uid ?: return
        val ordersReference: DatabaseReference = database.reference.child("orders")

        ordersReference.orderByChild("adminUserId").equalTo(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                @SuppressLint("DefaultLocale")
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalEarnings = 0.0
                    for (orderSnapshot in snapshot.children) {
                        val orderDelivered = orderSnapshot.child("orderDelivered").getValue(Boolean::class.java) ?: false

                        if (orderDelivered) {
                            val totalPrice = orderSnapshot.child("totalPrice").getValue(String::class.java)
                            totalPrice?.let {
                                totalEarnings += it.toDoubleOrNull() ?: 0.0
                            }
                        }
                    }

                    binding.wholeTimeEarning.text = String.format("%.2f RON", totalEarnings)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}