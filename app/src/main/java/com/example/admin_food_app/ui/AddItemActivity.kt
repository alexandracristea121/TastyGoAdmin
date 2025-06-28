package com.example.admin_food_app.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.admin_food_app.R
import com.example.admin_food_app.databinding.ActivityAddItemBinding
import com.example.admin_food_app.model.MenuItems
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class AddItemActivity : AppCompatActivity() {
    private lateinit var foodName: String
    private lateinit var foodPrice: String
    private lateinit var foodDescription: String
    private lateinit var foodIngredient: String
    private var foodImage: Uri? = null
    private lateinit var selectedRestaurantId: String
    private lateinit var selectedCategory: String
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private val binding: ActivityAddItemBinding by lazy {
        ActivityAddItemBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.white)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        fetchRestaurantData()
        setupCategorySpinner()

        binding.AddItemButton.setOnClickListener {
            foodName = binding.foodName.text.toString().trim()
            foodPrice = binding.foodPrice.text.toString().trim()
            foodDescription = binding.description.text.toString().trim()
            foodIngredient = binding.ingredient.text.toString().trim()

            if (foodName.isNotBlank() && foodPrice.isNotBlank() && foodDescription.isNotBlank() && foodIngredient.isNotBlank()) {
                uploadData()
                Toast.makeText(this, "Item Added Successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Please fill all the details", Toast.LENGTH_SHORT).show()
            }
        }

        binding.selectImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun uploadData() {
        val restaurantRef = database.getReference("Restaurants").child(selectedRestaurantId).child("menu")
        val newItemKey = restaurantRef.push().key

        if (foodImage != null) {
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("menu_images/${newItemKey}.jpg")
            val uploadTask = imageRef.putFile(foodImage!!)

            uploadTask.addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    val newItem = MenuItems(
                        newItemKey,
                        foodName = foodName,
                        foodPrice = foodPrice,
                        foodDescription = foodDescription,
                        foodIngredient = foodIngredient,
                        foodImage = downloadUrl.toString(),
                        restaurantName = selectedRestaurantId,
                        category = selectedCategory
                    )

                    newItemKey?.let { key ->
                        restaurantRef.child(key).setValue(newItem).addOnSuccessListener {
                            Toast.makeText(this, "Data uploaded successfully", Toast.LENGTH_SHORT).show()
                        }
                            .addOnFailureListener {
                                Toast.makeText(this, "Data upload failed", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Image Upload Failed", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            binding.selectedImage.setImageURI(uri)
            foodImage = uri
        }
    }

    private fun fetchRestaurantData() {
        val restaurantRef = database.getReference("Restaurants")
        val currentUserId = auth.currentUser?.uid ?: ""

        restaurantRef.get().addOnSuccessListener { snapshot ->
            val restaurantList = mutableListOf<String>()
            val restaurantIds = mutableListOf<String>()

            for (restaurantSnapshot in snapshot.children) {
                val restaurantName = restaurantSnapshot.child("name").getValue(String::class.java)
                val restaurantId = restaurantSnapshot.key
                val adminUserId = restaurantSnapshot.child("adminUserId").getValue(String::class.java)

                if (restaurantName != null && restaurantId != null && adminUserId != null && adminUserId == currentUserId) {
                    restaurantList.add(restaurantName)
                    restaurantIds.add(restaurantId)
                }
            }

            if (restaurantList.isEmpty()) {
                Toast.makeText(this, "No restaurants found for this user.", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, restaurantList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.restaurantSpinner.adapter = adapter
            binding.restaurantSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, id: Long) {
                    selectedRestaurantId = restaurantIds[position] // Set selected restaurant ID
                }

                override fun onNothingSelected(parentView: AdapterView<*>) {
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load restaurants", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCategorySpinner() {
        val categories = listOf(
            "Pizza", "Pasta", "Burgers", "Sandwiches", "Salads",
            "Sushi", "Mexican", "Asian", "Desserts", "Drinks",
            "Soups", "Snacks", "Fast Food", "Breakfast",
            "Grilled Food", "Vegan", "Vegetarian", "Seafood", "BBQ"
        )

        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = categoryAdapter

        binding.categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, id: Long) {
                selectedCategory = categories[position]
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
            }
        }
    }
}