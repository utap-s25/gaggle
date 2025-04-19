package edu.utap.gaggle

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

class GaggleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        Log.d("FIREBASE", "App initialized: ${FirebaseApp.getInstance().name}")
        Log.d("FIREBASE", "Firestore ref: ${FirebaseFirestore.getInstance()}")
    }
}