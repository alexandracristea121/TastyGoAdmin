package com.example.admin_food_app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.admin_food_app.R
import com.example.admin_food_app.databinding.ActivityAdminProfileBinding
import com.example.admin_food_app.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminProfileActivity : AppCompatActivity() {
    private val binding  : ActivityAdminProfileBinding by lazy {
        ActivityAdminProfileBinding.inflate(layoutInflater)
    }
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var adminReference:DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        auth=FirebaseAuth.getInstance()
        database=FirebaseDatabase.getInstance()
        adminReference=database.reference.child("adminUsers")


        binding.backButton.setOnClickListener {
            finish()
        }

        binding.saveInfoButton.setOnClickListener {
            updateUserData()
        }

        binding.name.isEnabled=false
        binding.address.isEnabled=false
        binding.email.isEnabled=false
        binding.phone.isEnabled=false
        binding.password.isEnabled=false
        binding.saveInfoButton.isEnabled=false

        var isEnable = false
        binding.editButton.setOnClickListener {
            isEnable = !isEnable
            binding.name.isEnabled=isEnable
            binding.address.isEnabled=isEnable
            binding.email.isEnabled=isEnable
            binding.phone.isEnabled=isEnable
            binding.password.isEnabled=isEnable
            if(isEnable){
                binding.name.requestFocus()
            }
            binding.saveInfoButton.isEnabled=isEnable
        }

        retrieveUserData()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun retrieveUserData() {
        val currentUserUid = auth.currentUser?.uid
        if (currentUserUid != null) {
            val userReference = database.reference.child("adminUsers").child(currentUserUid)

            userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val ownerName = snapshot.child("name").getValue(String::class.java)
                        val address = snapshot.child("address").getValue(String::class.java)
                        val email = snapshot.child("email").getValue(String::class.java)
                        val phone = snapshot.child("phone").getValue(String::class.java)
                        val password = snapshot.child("password").getValue(String::class.java)

                        ownerName?.let { binding.name.setText(it) }
                        address?.let { binding.address.setText(it) }
                        email?.let { binding.email.setText(it) }
                        phone?.let { binding.phone.setText(it) }
                        password?.let { binding.password.setText(it) }

                        if (ownerName == null) binding.name.setText("")
                        if (address == null) binding.address.setText("")
                        if (email == null) binding.email.setText("")
                        if (phone == null) binding.phone.setText("")
                        if (password == null) binding.password.setText("")
                    } else {
                        Toast.makeText(this@AdminProfileActivity, "No user data found in Firebase", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AdminProfileActivity, "Failed to load data: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this, "User is not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUserData() {
        val currentUserUid = auth.currentUser?.uid

        if (currentUserUid != null) {
            val updateName = binding.name.text.toString()
            val updateEmail = binding.email.text.toString()
            val updatePassword = binding.password.text.toString()
            val updatePhone = binding.phone.text.toString()
            val updateAddress = binding.address.text.toString()

            val userData = UserModel(
                name = updateName,
                email = updateEmail,
                password = updatePassword,
                phone = updatePhone,
                address = updateAddress
            )

            val userReference = database.reference.child("adminUsers").child(currentUserUid)
            userReference.setValue(userData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Profile Update Failed", Toast.LENGTH_SHORT).show()
                }
        }
    }
}