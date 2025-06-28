package com.example.admin_food_app.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.admin_food_app.R
import com.example.admin_food_app.databinding.ActivitySignUpBinding
import com.example.admin_food_app.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {

    private lateinit var userName: String
    private lateinit var email: String
    private lateinit var password: String
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private val binding: ActivitySignUpBinding by lazy {
        ActivitySignUpBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        binding.createUserButton.setOnClickListener {
            userName = binding.name.text.toString().trim()
            email = binding.emailOrPhone.text.toString().trim()
            password = binding.password.text.toString().trim()

            when {
                userName.isBlank() -> {
                    binding.name.error = "User name cannot be empty"
                }
                email.isBlank() -> {
                    binding.emailOrPhone.error = "Email cannot be empty"
                }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    binding.emailOrPhone.error = "Enter a valid email address"
                }
                password.isBlank() -> {
                    binding.password.error = "Password cannot be empty"
                }
                password.length < 6 -> {
                    binding.password.error = "Password must be at least 6 characters long"
                }
                !password.matches(Regex(".*[A-Z].*")) -> {
                    binding.password.error = "Password must contain at least one uppercase letter"
                }
                !password.matches(Regex(".*\\d.*")) -> {
                    binding.password.error = "Password must contain at least one number"
                }
                else -> {
                    createAccount(email, password)
                }
            }
        }

        binding.alreadyHaveAccountButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        val locationList = arrayOf("Timisoara", "Oradea", "Cluj", "Deva")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, locationList)
        val autoCompleteTextView = binding.listOfLocation
        autoCompleteTextView.setAdapter(adapter)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun createAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show()
                saveUserData()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                val exception = task.exception
                if (exception is FirebaseAuthUserCollisionException) {
                    Toast.makeText(this, "Email already in use", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Account Creation Failed", Toast.LENGTH_SHORT).show()
                }
                Log.d("Account", "createAccount: Failure", exception)
            }
        }
    }

    private fun saveUserData() {
        userName = binding.name.text.toString().trim()
        email = binding.emailOrPhone.text.toString().trim()
        password = binding.password.text.toString().trim()

        val user = UserModel(userName, email, password)
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        database = FirebaseDatabase.getInstance().reference
        database.child("adminUsers").child(userId).setValue(user)
    }
}