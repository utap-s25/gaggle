package edu.utap.gaggle.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import edu.utap.gaggle.model.FeedItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FeedViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _feedItems = MutableLiveData<List<FeedItem>>()
    val feedItems: LiveData<List<FeedItem>> = _feedItems

    private val formattedDate = LocalDate.now().toString() // YYYY-MM-DD

    private val listeners = mutableListOf<ListenerRegistration>()

    init {
        listenToFeed()
    }

    private fun listenToFeed() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                val joinedGaggles = userDoc.get("joinedGaggles") as? List<String> ?: return@addOnSuccessListener

                // Add own tasks first
                listenToOwnTasks(userId)

                // Add tasks from other users in joined gaggles
                for (gaggleId in joinedGaggles) {
                    db.collection("gaggles")
                        .document(gaggleId)
                        .get()
                        .addOnSuccessListener { gaggleDoc ->
                            val gaggleTitle = gaggleDoc.getString("title") ?: "Unnamed Gaggle"
                            val usersInGaggle = gaggleDoc.get("users") as? List<String> ?: return@addOnSuccessListener

                            for (gaggleUserId in usersInGaggle) {
                                if (gaggleUserId == userId) continue // Skip own tasks

                                val listener = db.collection("tasks")
                                    .document(gaggleId)
                                    .collection(gaggleUserId)
                                    .document("userTasks")
                                    .collection(formattedDate)
                                    .addSnapshotListener { snapshot, error ->
                                        if (error != null || snapshot == null) return@addSnapshotListener

                                        val updatedList = _feedItems.value?.toMutableList() ?: mutableListOf()

                                        for (doc in snapshot.documents) {
                                            val completed = doc.getBoolean("completed") ?: false
                                            if (!completed) continue

                                            val taskTitle = doc.getString("taskTitle") ?: continue
                                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                            val userName = doc.getString("userName") ?: "Someone"

                                            val item = FeedItem(
                                                userName = userName,
                                                gaggleTitle = gaggleTitle,
                                                taskTitle = taskTitle,
                                                date = formattedDate,
                                                timestamp = timestamp
                                            )

                                            // Remove any duplicates based on timestamp + taskTitle
                                            updatedList.removeAll { it.timestamp == item.timestamp && it.taskTitle == item.taskTitle }
                                            updatedList.add(item)
                                        }

                                        _feedItems.value = updatedList.sortedByDescending { it.timestamp }
                                    }

                                listeners.add(listener)
                            }
                        }
                }
            }
    }

    private fun listenToOwnTasks(userId: String) {
        val todayDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        db.collection("tasks")
            .document(userId)
            .collection("userTasks")
            .whereEqualTo("date", todayDate)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Handle the error if any
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val feedItems = mutableListOf<FeedItem>()
                    for (document in snapshot) {
                        val feedItem = document.toObject(FeedItem::class.java)
                        feedItems.add(feedItem)
                    }
                    _feedItems.value = feedItems
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        listeners.forEach { it.remove() }
    }
}
