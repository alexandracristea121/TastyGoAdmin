package com.example.admin_food_app

import android.app.Application
import com.google.firebase.FirebaseApp

class AdminFoodApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
} 