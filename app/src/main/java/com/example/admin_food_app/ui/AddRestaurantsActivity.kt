package com.example.admin_food_app.ui

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.admin_food_app.databinding.ActivityAddRestaurantBinding
import com.example.admin_food_app.utils.Config
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest

@Suppress("DEPRECATION")
class AddRestaurantsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddRestaurantBinding
    private lateinit var placesClient: PlacesClient

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddRestaurantBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = ContextCompat.getColor(this, android.R.color.white)
        window.navigationBarColor = ContextCompat.getColor(this, android.R.color.white)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, Config.PLACES_API_KEY)
        }
        placesClient = Places.createClient(this)

        binding.restaurantAddressEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s.toString()
                if (input.isNotEmpty()) {
                    fetchAddressSuggestions(input)
                } else {
                    binding.suggestionsContainer.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.saveButton.setOnClickListener {
            val restaurantName = binding.restaurantNameEditText.text.toString()
            val restaurantAddress = binding.restaurantAddressEditText.text.toString()

            if (restaurantName.isNotEmpty() && restaurantAddress.isNotEmpty()) {
                checkRestaurantExists(restaurantName) { exists ->
                    if (exists) {
                        Toast.makeText(this, "This restaurant already exists.", Toast.LENGTH_SHORT).show()
                    } else {
                        geocodeAddress(restaurantAddress) { latLng ->
                            if (latLng != null) {
                                saveRestaurantToDatabase(restaurantName, latLng.latitude, latLng.longitude)
                            } else {
                                Toast.makeText(this, "Error fetching location for the address", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun checkRestaurantExists(name: String, callback: (Boolean) -> Unit) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("Restaurants")
        val lowerCaseName = name.trim().lowercase()

        databaseReference.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    var exists = false
                    for (restaurantSnapshot in snapshot.children) {
                        val restaurantName = restaurantSnapshot.child("name").getValue(String::class.java)
                        if (restaurantName != null && restaurantName.trim().lowercase() == lowerCaseName) {
                            exists = true
                            break
                        }
                    }
                    callback(exists)
                } else {
                    callback(false)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error checking restaurant existence: ${exception.message}", Toast.LENGTH_SHORT).show()
                callback(false)
            }
    }

    private fun fetchAddressSuggestions(query: String) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                val suggestions = response.autocompletePredictions.map { it.getFullText(null).toString() }
                updateAddressSuggestions(suggestions)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching suggestions: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateAddressSuggestions(suggestions: List<String>) {
        if (suggestions.isNotEmpty()) {
            binding.suggestionsContainer.visibility = View.VISIBLE
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                suggestions
            )
            binding.addressSuggestions.adapter = adapter
            adapter.notifyDataSetChanged()

            binding.addressSuggestions.setOnItemClickListener { parent, _, position, _ ->
                val selectedAddress = parent.getItemAtPosition(position).toString()
                binding.restaurantAddressEditText.setText(selectedAddress)
                clearSuggestions()
            }
        } else {
            clearSuggestions()
        }
    }

    private fun clearSuggestions() {
        binding.suggestionsContainer.visibility = View.GONE
        binding.addressSuggestions.adapter = null
    }

    private fun geocodeAddress(address: String, callback: (LatLng?) -> Unit) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(address)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                if (response.autocompletePredictions.isNotEmpty()) {
                    val prediction = response.autocompletePredictions[0]
                    val placeId = prediction.placeId

                    val placeFields = listOf(Place.Field.LAT_LNG)
                    val placeRequest = FetchPlaceRequest.builder(placeId, placeFields).build()

                    placesClient.fetchPlace(placeRequest)
                        .addOnSuccessListener { fetchPlaceResponse ->
                            val place = fetchPlaceResponse.place
                            val latLng = place.latLng
                            callback(latLng)
                        }
                        .addOnFailureListener {
                            callback(null)
                        }
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    private fun saveRestaurantToDatabase(name: String, latitude: Double, longitude: Double) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val adminUserId = currentUser?.uid

        if (adminUserId == null) {
            Toast.makeText(this, "Error: User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        val databaseReference = FirebaseDatabase.getInstance().getReference("Restaurants")

        val restaurantId = databaseReference.push().key ?: return
        val restaurantData = mapOf(
            "id" to restaurantId,
            "name" to name,
            "latitude" to latitude,
            "longitude" to longitude,
            "adminUserId" to adminUserId
        )

        databaseReference.child(restaurantId).setValue(restaurantData)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Restaurant added successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to add restaurant", Toast.LENGTH_SHORT).show()
                }
            }
    }
}