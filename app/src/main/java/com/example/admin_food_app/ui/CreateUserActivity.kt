package com.example.admin_food_app.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.admin_food_app.databinding.ActivityCreateUserBinding
import com.example.admin_food_app.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class CreateUserActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private val binding: ActivityCreateUserBinding by lazy {
        ActivityCreateUserBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        binding.createUserButton.setOnClickListener {
            val userName = binding.name.text.toString().trim()
            val email = binding.emailOrPhone.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (userName.isBlank() || email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show()
            } else {
                createAccount(userName, email, password)
            }
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun createAccount(userName: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "User created successfully", Toast.LENGTH_SHORT).show()
                saveUserData(userName, email, password)
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                val exception = task.exception
                if (exception is FirebaseAuthUserCollisionException) {
                    Toast.makeText(this, "Email already in use", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "User creation failed", Toast.LENGTH_SHORT).show()
                }
                Log.d("CreateUser", "createAccount: Failure", exception)
            }
        }
    }

    private fun saveUserData(userName: String, email: String, password: String) {
        val userId = auth.currentUser!!.uid
        val user = UserModel(userName, "", email, password)
        database.child("adminUsers").child(userId).setValue(user)
    }
}