package com.example.admin_food_app.ui

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import android.widget.AdapterView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.admin_food_app.databinding.ManageExistingRestaurantsBinding
import com.example.admin_food_app.utils.Config
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest

import com.google.firebase.auth.FirebaseAuth

@Suppress("DEPRECATION", "NAME_SHADOWING")
class ManageExistingRestaurantsActivity : AppCompatActivity() {

    private lateinit var binding: ManageExistingRestaurantsBinding
    private lateinit var placesClient: PlacesClient
    private lateinit var database: FirebaseDatabase
    private var isAddressPrefilled = false
    private lateinit var auth: FirebaseAuth

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ManageExistingRestaurantsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = ContextCompat.getColor(this, android.R.color.white)
        window.navigationBarColor = ContextCompat.getColor(this, android.R.color.white)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, Config.PLACES_API_KEY)
        }
        placesClient = Places.createClient(this)

        fetchCurrentAdminUserIdAndFetchRestaurantData()

        binding.restaurantAddressEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s.toString()

                if (!isAddressPrefilled && input.isNotEmpty()) {
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

        binding.restaurantSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, id: Long) {
                val selectedRestaurant = binding.restaurantSpinner.selectedItem.toString()
                fetchRestaurantDetails(selectedRestaurant)
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {

            }
        }

        binding.saveRestaurantButton.setOnClickListener {
            val selectedRestaurantName = binding.restaurantSpinner.selectedItem.toString()
            val updatedRestaurantName = binding.restaurantNameEditText.text.toString()
            val updatedAddress = binding.restaurantAddressEditText.text.toString()

            if (updatedAddress.isNotEmpty()) {
                geocodeAddress(updatedAddress) { latLng ->
                    if (latLng != null) {
                        updateRestaurantDetailsInDatabase(selectedRestaurantName, updatedRestaurantName, updatedAddress, latLng)
                    } else {
                        Toast.makeText(this, "Error fetching location for the address", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please fill in the address", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun fetchCurrentAdminUserIdAndFetchRestaurantData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val adminUserId = currentUser.uid

            fetchRestaurantData(adminUserId)
        } else {
            Toast.makeText(this, "No logged-in user found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchRestaurantData(adminUserId: String) {
        database.reference.child("Restaurants")
            .orderByChild("adminUserId").equalTo(adminUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val restaurantList = mutableListOf<String>()

                    if (snapshot.exists()) {
                        for (restaurantSnapshot in snapshot.children) {
                            val restaurantName = restaurantSnapshot.child("name").getValue(String::class.java)
                            if (restaurantName != null) {
                                restaurantList.add(restaurantName)
                            }
                        }

                        if (restaurantList.isEmpty()) {
                            Toast.makeText(this@ManageExistingRestaurantsActivity, "No restaurants found for this admin", Toast.LENGTH_SHORT).show()
                        } else {
                            val adapter = ArrayAdapter(this@ManageExistingRestaurantsActivity, android.R.layout.simple_spinner_item, restaurantList)
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            binding.restaurantSpinner.adapter = adapter
                        }
                    } else {
                        Toast.makeText(this@ManageExistingRestaurantsActivity, "No restaurants found for this admin", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ManageExistingRestaurantsActivity, "Failed to load restaurants", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun fetchRestaurantDetails(restaurantName: String) {
        database.reference.child("Restaurants")
            .orderByChild("name").equalTo(restaurantName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (restaurantSnapshot in snapshot.children) {
                        val restaurantAddress = restaurantSnapshot.child("address").getValue(String::class.java)
                        val restaurantName = restaurantSnapshot.child("name").getValue(String::class.java)
                        if (restaurantAddress != null && restaurantName != null) {
                            binding.restaurantAddressEditText.setText(restaurantAddress)
                            binding.restaurantNameEditText.setText(restaurantName)
                            isAddressPrefilled = true
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ManageExistingRestaurantsActivity, "Failed to fetch restaurant details", Toast.LENGTH_SHORT).show()
                }
            })
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
                isAddressPrefilled = false
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

    private fun updateRestaurantDetailsInDatabase(
        selectedRestaurantName: String,
        updatedRestaurantName: String,
        updatedAddress: String,
        latLng: LatLng
    ) {
        database.reference.child("Restaurants").orderByChild("name").equalTo(selectedRestaurantName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (restaurantSnapshot in snapshot.children) {
                        val restaurantId = restaurantSnapshot.child("id").getValue(String::class.java)
                        if (restaurantId != null) {
                            val updates = mapOf<String, Any>(
                                "Restaurants/$restaurantId/name" to updatedRestaurantName,
                                "Restaurants/$restaurantId/address" to updatedAddress,
                                "Restaurants/$restaurantId/latitude" to latLng.latitude,
                                "Restaurants/$restaurantId/longitude" to latLng.longitude
                            )

                            database.reference.updateChildren(updates).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(this@ManageExistingRestaurantsActivity, "Restaurant updated successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this@ManageExistingRestaurantsActivity, "Failed to update restaurant", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ManageExistingRestaurantsActivity, "Failed to update restaurant details", Toast.LENGTH_SHORT).show()
                }
            })
    }
}