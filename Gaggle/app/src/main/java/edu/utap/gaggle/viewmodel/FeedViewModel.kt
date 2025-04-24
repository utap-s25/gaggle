package edu.utap.gaggle.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import edu.utap.gaggle.model.FeedItem
import edu.utap.gaggle.model.Gaggle
import edu.utap.gaggle.model.GaggleMemberGroup
import edu.utap.gaggle.model.MemberIcon
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class FeedViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _feedItems = MutableLiveData<List<FeedItem>>()
    val feedItems: LiveData<List<FeedItem>> = _feedItems
    private val _gaggleMemberGroups = MutableLiveData<List<GaggleMemberGroup>>()
    val gaggleMemberGroups: LiveData<List<GaggleMemberGroup>> = _gaggleMemberGroups


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
                    loadGaggleMembers(gaggleId)
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

    private fun loadGaggleMembers(gaggleId: String) {
        val gaggleDocRef = db.collection("gaggles").document(gaggleId)
        val groupList = mutableListOf<GaggleMemberGroup>()

        gaggleDocRef.get().addOnSuccessListener { gaggleSnapshot ->
            val members = gaggleSnapshot.get("members") as? List<String> ?: return@addOnSuccessListener
            val gaggleTitle = gaggleSnapshot.getString("title") ?: "Unnamed Gaggle"

            val memberIcons = mutableListOf<MemberIcon>()
            members.forEach { memberId ->
                db.collection("users").document(memberId).get()
                    .addOnSuccessListener { userDoc ->
                        val username = userDoc.getString("username") ?: "Unknown"
                        val profileImageUrl = userDoc.getString("profileImageUrl")
                        memberIcons.add(MemberIcon(memberId, username, profileImageUrl))

                        if (memberIcons.size == members.size) {
                            groupList.add(GaggleMemberGroup(gaggleTitle, memberIcons))
                            _gaggleMemberGroups.value = groupList
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

    // Group feed items by gaggle and return a map of gaggle title to its member group
    fun renderGaggleHeaders(): List<GaggleMemberGroup> {
        val items = _feedItems.value ?: return emptyList()
        val groups = _gaggleMemberGroups.value ?: return emptyList()

        // Only return groups that actually have feed items
        val gagglesWithItems = items.map { it.gaggleTitle }.toSet()
        return groups.filter { it.gaggleTitle in gagglesWithItems }
    }

    fun loadFeed() {
        val currentUserId = auth.currentUser?.uid ?: return

        Log.d("FeedViewModel", "Loading feed for user: $currentUserId")
        db.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener { userDoc ->
                val joinedGaggles = userDoc.get("joinedGaggles") as? List<String> ?: emptyList()
                val username = userDoc.getString("username") ?: "Someone"

                if (joinedGaggles.isEmpty()) {
                    _feedItems.value = emptyList()
                    return@addOnSuccessListener
                }

                val newFeedItems = mutableListOf<FeedItem>()
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                // Create a list of tasks to be fetched from Firestore
                val gaggleFetchTasks = joinedGaggles.map { gaggleId ->
                    Log.d("FeedViewModel", "Fetching tasks for gaggle: $gaggleId")
                    db.collection("gaggles").document(gaggleId)
                        .get()
                        .continueWithTask { task ->
                            if (!task.isSuccessful) {
                                return@continueWithTask Tasks.forException(task.exception ?: Exception("Failed to load gaggle"))
                            }

                            val gaggleDoc = task.result
                            val gaggleName = gaggleDoc?.getString("name") ?: "Unnamed Gaggle"
                            val taskTitles = gaggleDoc?.get("tasks") as? List<String> ?: emptyList()

                            val userTaskFetches = taskTitles.map { title ->
                                val taskId = "${title}_$today"
                                Log.d("FeedViewModel", "Fetching task: $taskId from user $currentUserId")
                                db.collection("tasks")
                                    .document(currentUserId)
                                    .collection("userTasks")
                                    .document(taskId)
                                    .get()
                                    .continueWith { userTaskDocTask ->
                                        val userTaskDoc = userTaskDocTask.result
                                        val isCompleted = userTaskDoc?.getBoolean("completed") ?: false
                                        val timestamp = userTaskDoc?.getLong("timestamp") ?: System.currentTimeMillis()

                                        FeedItem(
                                            userName = username,
                                            gaggleTitle = gaggleName,
                                            taskTitle = title,
                                            date = today,
                                            timestamp = timestamp,
                                            completed = isCompleted
                                        )
                                    }
                            }

                            Tasks.whenAllSuccess<FeedItem>(userTaskFetches).continueWith {
                                it.result ?: emptyList()
                            }
                        }
                }

                // Using `Tasks.whenAllSuccess` to gather all gaggle results
                Tasks.whenAllSuccess<List<FeedItem>>(gaggleFetchTasks)
                    .addOnSuccessListener { feedItems ->
                        _feedItems.value = feedItems.flatten().sortedByDescending { it.timestamp }
                    }
            }
            .addOnFailureListener {
                Log.e("FeedViewModel", "Failed to load feed", it)
            }
    }




    // Clean up listeners when ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        listeners.forEach { it.remove() }
    }
}
