package edu.utap.gaggle.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import edu.utap.gaggle.model.Gaggle

class GaggleViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    // preferences used to filter gaggles
    private val _preferences = MutableLiveData<List<String>>()
    val preferences: LiveData<List<String>> get() = _preferences

    // gaggles the user has already joined
    private val _userGaggles = MutableLiveData<Set<String>>()
    val userGaggles: LiveData<Set<String>> get() = _userGaggles

    // raw gaggles from Firestore
    private val _allMatchingGaggles = MutableLiveData<List<Gaggle>>()

    // auto-updated gaggle list
    val gaggles = MediatorLiveData<List<Gaggle>>()

    private var listenerRegistration: ListenerRegistration? = null

    init {
        gaggles.addSource(_preferences) { prefs ->
            listenToGaggles(prefs)
        }

        gaggles.addSource(_allMatchingGaggles) { updateFilteredList() }
        gaggles.addSource(_userGaggles) { updateFilteredList() }

        startListeningToUserGaggles()
    }


    private fun updateFilteredList() {
        val joined = _userGaggles.value ?: emptySet()
        val all = _allMatchingGaggles.value ?: emptyList()
        gaggles.value = all.sortedByDescending { joined.contains(it.id) }
    }

    fun setPreferences(newPrefs: List<String>) {
        _preferences.value = newPrefs
    }

    fun startListeningToUserGaggles() {
        val userId = auth.currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val gaggleIds = snapshot.get("joinedGaggles") as? List<String> ?: emptyList()
                    _userGaggles.value = gaggleIds.toSet()  // Store it as a Set to avoid duplicates
                }
            }
    }

    private fun listenToGaggles(preferences: List<String>) {
        listenerRegistration?.remove()

        if (preferences.isEmpty()) {
            _allMatchingGaggles.value = emptyList()
            return
        }

        listenerRegistration = db.collection("gaggles")
            .whereArrayContainsAny("categories", preferences)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val gaggleList = snapshot.documents.mapNotNull { it.toObject(Gaggle::class.java)?.copy(id = it.id) }
                _allMatchingGaggles.value = gaggleList
            }
    }

    fun joinGaggle(gaggleId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        _userGaggles.value = _userGaggles.value?.plus(gaggleId) ?: setOf(gaggleId)
        if (gaggleId.isBlank()) {
            Log.e("joinGaggle", "Invalid gaggleId: '$gaggleId'")
            return false
        }

        db.collection("gaggles").document(gaggleId)
            .update("members", FieldValue.arrayUnion(uid))
        db.collection("users").document(uid)
            .update("joinedGaggles", FieldValue.arrayUnion(gaggleId))
        return true
    }

    fun leaveGaggle(gaggleId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        _userGaggles.value = _userGaggles.value?.minus(gaggleId) ?: setOf(gaggleId)

        db.collection("gaggles").document(gaggleId)
            .update("members", FieldValue.arrayRemove(uid))
        db.collection("users").document(uid)
            .update("joinedGaggles", FieldValue.arrayRemove(gaggleId))
        return true
    }

    fun createGaggle(title: String, desc: String, prefs: List<String>, tasks: List<String>) {
        val userId = auth.currentUser?.uid ?: return
        val gaggleRef = db.collection("gaggles").document()  // generate a doc ref

        val gaggle = Gaggle(
            id = gaggleRef.id,
            title = title,
            description = desc,
            categories = prefs,
            members = listOf(userId),
            tasks = tasks
        )

        gaggleRef.set(gaggle).addOnSuccessListener {
            db.collection("users").document(userId)
                .update("joinedGaggles", FieldValue.arrayUnion(gaggleRef.id))
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}