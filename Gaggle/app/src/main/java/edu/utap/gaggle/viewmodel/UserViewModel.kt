package edu.utap.gaggle.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import edu.utap.gaggle.model.UserPreferences

class UserViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // LiveData to hold the current user preferences
    private val _preferences = MutableLiveData<UserPreferences>()
    val preferences: LiveData<UserPreferences> get() = _preferences

    // Function to load preferences from Firestore
    fun loadPreferences() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val prefs = document.toObject(UserPreferences::class.java)
                    _preferences.value = prefs
                }
            }
            .addOnFailureListener { exception ->
                Log.e("UserViewModel", "Error loading preferences: ", exception)
            }
    }

    // Function to update preferences in Firestore
    fun updatePreferences(prefs: UserPreferences) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .set(mapOf(
                "wantsPhysical" to prefs.wantsPhysical,
                "wantsMental" to prefs.wantsMental,
                "wantsCreative" to prefs.wantsCreative,
                "wantsSocial" to prefs.wantsSocial
            ), SetOptions.merge())  // Use merge to update fields without overwriting other data
            .addOnSuccessListener {
                _preferences.value = prefs  // Update LiveData with new preferences
            }
            .addOnFailureListener { exception ->
                Log.e("UserViewModel", "Error saving preferences: ", exception)
            }
    }
}

