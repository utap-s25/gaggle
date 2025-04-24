package edu.utap.gaggle.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import edu.utap.gaggle.model.FeedItem
import edu.utap.gaggle.model.Gaggle
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

    // Function to listen to the current user's tasks and the tasks of other users in the same gaggles
    private fun listenToFeed() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Get the list of gaggles the user has joined
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                val joinedGaggles = userDoc.get("joinedGaggles") as? List<String> ?: return@addOnSuccessListener

                // Listen to the current user's tasks
                listenToUserTasks(userId)

                // Listen to tasks from users in the same gaggles
                joinedGaggles.forEach { gaggleId ->
                    listenToGaggleTasks(userId, gaggleId)
                }
            }
    }

    private fun listenToUserTasks(userId: String) {
        db.collection("tasks")
            .document(userId)
            .collection("userTasks")
            .whereEqualTo("date", formattedDate)
            .addSnapshotListener { snapshot, error ->
                Log.d("FeedViewModel", "Listening to user tasks")
                if (error != null || snapshot == null) return@addSnapshotListener

                val currentItems = _feedItems.value?.toMutableList() ?: mutableListOf()
                val updatedMap = currentItems.associateBy { "${it.taskTitle}|${it.userName}|${it.gaggleTitle}" }.toMutableMap()

                snapshot.documentChanges.forEach { change ->
                    val doc = change.document
                    val taskTitle = doc.getString("title") ?: return@forEach
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val gaggleTitle = doc.getString("gaggleTitle") ?: "Unnamed Gaggle"
                    val taskUserId = doc.getString("userId") ?: return@forEach

                    val userDocRef = db.collection("users").document(taskUserId)

                    when (change.type) {
                        DocumentChange.Type.ADDED,
                        DocumentChange.Type.MODIFIED -> {
                            val isCompleted = doc.getBoolean("completed") ?: false
                            if (!isCompleted) {
                                // Remove item if it exists
                                userDocRef.get().addOnSuccessListener { userDoc ->
                                    val userName = userDoc.getString("username") ?: "Unknown"
                                    val taskId = "$taskTitle|$userName|$gaggleTitle"
                                    updatedMap.remove(taskId)
                                    _feedItems.postValue(updatedMap.values.sortedByDescending { it.timestamp })
                                }
                                return@forEach
                            }

                            userDocRef.get().addOnSuccessListener { userDoc ->
                                val userName = userDoc.getString("username") ?: "Unknown"
                                val taskId = "$taskTitle|$userName|$gaggleTitle"

                                val newItem = FeedItem(
                                    userName = userName,
                                    gaggleTitle = gaggleTitle,
                                    taskTitle = taskTitle,
                                    date = formattedDate,
                                    timestamp = timestamp,
                                    completed = true
                                )

                                if (!updatedMap.containsKey(taskId) || (updatedMap[taskId]?.timestamp ?: 0) < timestamp) {
                                    updatedMap[taskId] = newItem
                                    _feedItems.postValue(updatedMap.values.sortedByDescending { it.timestamp })
                                }
                            }
                        }

                        DocumentChange.Type.REMOVED -> {
                            userDocRef.get().addOnSuccessListener { userDoc ->
                                val userName = userDoc.getString("username") ?: "Unknown"
                                val taskId = "$taskTitle|$userName|$gaggleTitle"
                                updatedMap.remove(taskId)
                                _feedItems.postValue(updatedMap.values.sortedByDescending { it.timestamp })
                            }
                        }
                    }
                }
            }
    }


    private fun listenToGaggleTasks(currentUserId: String, gaggleId: String) {
        val gaggleDocRef = db.collection("gaggles").document(gaggleId)
        Log.d("FeedViewModel", "Listening to tasks in gaggle: $gaggleId")

        gaggleDocRef.get().addOnSuccessListener { gaggleSnapshot ->
            val taskTitles = gaggleSnapshot.get("tasks") as? List<String> ?: return@addOnSuccessListener
            val gaggleTitle = gaggleSnapshot.getString("title") ?: "Unnamed Gaggle"
            val members = gaggleSnapshot.get("members") as? List<String> ?: return@addOnSuccessListener

            Log.d("FeedViewModel", "Members: $members")
            members.filter { it.isNotBlank() && it != currentUserId }.forEach { userId ->
                taskTitles.forEach { taskTitle ->
                    val taskDocId = "${taskTitle}_$formattedDate"
                    val userTaskRef = db.collection("tasks")
                        .document(userId)
                        .collection("userTasks")
                        .document(taskDocId)

                    val listener = userTaskRef.addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                        val completed = snapshot.getBoolean("completed") == true
                        val timestamp = snapshot.getLong("timestamp") ?: System.currentTimeMillis()

                        // Fetch username from users/{userId}
                        db.collection("users").document(userId).get()
                            .addOnSuccessListener { userDoc ->
                                val userName = userDoc.getString("username") ?: "Someone"

                                // Create a unique task identifier
                                val taskId = "$taskTitle|$userName|$gaggleTitle"

                                val updatedMap = _feedItems.value
                                    ?.associateBy { "${it.taskTitle}|${it.userName}|${it.gaggleTitle}" }
                                    ?.toMutableMap() ?: mutableMapOf()

                                if (completed) {
                                    val newItem = FeedItem(
                                        userName = userName,
                                        gaggleTitle = gaggleTitle,
                                        taskTitle = taskTitle,
                                        date = formattedDate,
                                        timestamp = timestamp,
                                        completed = true
                                    )

                                    if (!updatedMap.containsKey(taskId) || (updatedMap[taskId]?.timestamp ?: 0) < timestamp) {
                                        updatedMap[taskId] = newItem
                                        Log.d("FeedViewModel", "Added/Updated feed item: $taskId")
                                    }
                                } else {
                                    // If unchecked, remove from map
                                    if (updatedMap.remove(taskId) != null) {
                                        Log.d("FeedViewModel", "Removed feed item: $taskId")
                                    }
                                }

                                // Update the LiveData with the sorted values
                                _feedItems.postValue(updatedMap.values.sortedByDescending { it.timestamp })
                            }
                            .addOnFailureListener { e ->
                                Log.e("FeedViewModel", "Failed to fetch username for $userId", e)
                            }
                    }
                    listeners.add(listener)
                }
            }
        }
    }



    // Clean up listeners when ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        listeners.forEach { it.remove() }
    }
}
