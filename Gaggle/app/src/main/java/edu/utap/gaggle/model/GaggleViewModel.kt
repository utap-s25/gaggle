package edu.utap.gaggle.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase


class GaggleViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val _gaggles = MutableLiveData<List<Gaggle>>()
    val gaggles: LiveData<List<Gaggle>> get() = _gaggles

    private var listenerRegistration: ListenerRegistration? = null

    fun listenToGaggles(preferences: List<String>) {
        listenerRegistration?.remove()

        listenerRegistration = db.collection("gaggles")
            .whereArrayContainsAny("preferences", preferences)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val gaggleList = snapshot.documents.mapNotNull { it.toObject(Gaggle::class.java)?.copy(id = it.id) }
                _gaggles.value = gaggleList
            }
    }

    fun createGaggle(title: String, desc: String, prefs: List<String>, userId: String) {
        val gaggle = Gaggle(
            title = title,
            description = desc,
            categories = prefs,
            members = listOf(userId)
        )
        db.collection("gaggles").add(gaggle)
    }

    fun joinGaggle(gaggleId: String, userId: String) {
        db.collection("gaggles").document(gaggleId)
            .update("members", FieldValue.arrayUnion(userId))
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
