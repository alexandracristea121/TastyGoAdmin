import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.FirebaseDatabase
import java.io.IOException
import java.util.*

class LocationUtil {

    fun getLatLngFromAddress(context: Context, address: String): LatLng? {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocationName(address, 1)
            addresses?.firstOrNull()?.let { LatLng(it.latitude, it.longitude) }
        } catch (e: IOException) {
            null
        }
    }

    fun fetchOrderDetails(orderId: String, context: Context, callback: (LatLng?, LatLng?) -> Unit) {
        val orderRef = FirebaseDatabase.getInstance().reference.child("orders").child(orderId)

        orderRef.get().addOnSuccessListener { snapshot ->
            val restaurantAddress = snapshot.child("restaurantLocation").value as? String
            val userAddress = snapshot.child("userLocation").value as? String

            val restaurantLatLng = restaurantAddress?.let { getLatLngFromAddress(context, it) }
            val userLatLng = userAddress?.let { getLatLngFromAddress(context, it) }

            callback(restaurantLatLng, userLatLng)
        }.addOnFailureListener {
            callback(null, null)
        }
    }

    fun findNearestCourier(orderId: String, context: Context, callback: (String?) -> Unit) {
        fetchOrderDetails(orderId, context) { restaurantLatLng, userLatLng ->
            if (restaurantLatLng == null || userLatLng == null) {
                Log.d("findNearestCourier", "Order details not found. Restaurant or User location is null.")
                callback(null)
                return@fetchOrderDetails
            }

            val couriersRef = FirebaseDatabase.getInstance().reference.child("couriers")
            couriersRef.get().addOnSuccessListener { snapshot ->
                var nearestCourierId: String? = null
                var minTotalDistance = Float.MAX_VALUE

                for (courierSnapshot in snapshot.children) {
                    val latitude = courierSnapshot.child("latitude").getValue(Double::class.java)
                    val longitude = courierSnapshot.child("longitude").getValue(Double::class.java)
                    val courierId = courierSnapshot.key

                    if (latitude != null && longitude != null && courierId != null) {
                        val courierLatLng = LatLng(latitude, longitude)

                        val distanceToRestaurant = calculateDistance(courierLatLng, restaurantLatLng)
                        val distanceToUser = calculateDistance(restaurantLatLng, userLatLng)
                        val totalDistance = distanceToRestaurant + distanceToUser

                        Log.d(
                            "findNearestCourier",
                            "Courier $courierId: Distance to restaurant = $distanceToRestaurant m, " +
                                    "Distance to user = $distanceToUser m, Total = $totalDistance m"
                        )

                        if (totalDistance < minTotalDistance) {
                            minTotalDistance = totalDistance
                            nearestCourierId = courierId
                            Log.d("findNearestCourier", "New nearest courier: $courierId with total distance $minTotalDistance m")
                        }
                    } else {
                        Log.d("findNearestCourier", "Invalid courier data for ${courierSnapshot.key}")
                    }
                }

                if (nearestCourierId != null) {
                    Log.d("findNearestCourier", "Nearest courier found: $nearestCourierId with total distance $minTotalDistance m")
                } else {
                    Log.d("findNearestCourier", "No couriers found.")
                }

                callback(nearestCourierId)
            }.addOnFailureListener { exception ->
                Log.e("findNearestCourier", "Failed to fetch couriers: ${exception.message}")
                callback(null)
            }
        }
    }

    fun updateCourierStatusAndAddLocations(courierId: String, restaurantLocation: LatLng?, userLocation: LatLng?, status: String) {
        val courierRef = FirebaseDatabase.getInstance().reference.child("couriers").child(courierId)

        courierRef.child("status").setValue(status)
        if (restaurantLocation != null) {
            courierRef.child("restaurantLatitude").setValue(restaurantLocation.latitude)
            courierRef.child("restaurantLongitude").setValue(restaurantLocation.longitude)
        }
        if (userLocation != null) {
            courierRef.child("userLatitude").setValue(userLocation.latitude)
            courierRef.child("userLongitude").setValue(userLocation.longitude)
        }
    }

    fun calculateDistance(from: LatLng, to: LatLng?): Float {
        val results = FloatArray(1)
        if (to != null) {
            Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, results)
        }
        return results[0]
    }

    fun getAddressFromLatLng(context: Context, latLng: LatLng, callback: (String) -> Unit) {
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            val address = addresses?.firstOrNull()?.getAddressLine(0) ?: "Unknown Location"
            callback(address)
        } catch (e: IOException) {
            callback("Unknown Location")
        }
    }
}