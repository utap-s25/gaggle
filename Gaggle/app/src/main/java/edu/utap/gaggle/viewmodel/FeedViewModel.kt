package edu.utap.gaggle.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import edu.utap.gaggle.model.FeedItem
import edu.utap.gaggle.model.GaggleMemberGroup
import edu.utap.gaggle.model.MemberIcon
import java.text.SimpleDateFormat
import java.util.*

class FeedViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _feedItems = MutableLiveData<List<FeedItem>>()
    val feedItems: LiveData<List<FeedItem>> = _feedItems

    private val _gaggleMemberGroups = MutableLiveData<List<GaggleMemberGroup>>()
    val gaggleMemberGroups: LiveData<List<GaggleMemberGroup>> = _gaggleMemberGroups

    private val listeners = mutableListOf<ListenerRegistration>()
    private val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    init {
        listenToFeed()
    }

    fun refreshFeed() {
        listeners.forEach { it.remove() }
        listeners.clear()

        _feedItems.value = emptyList()
        _gaggleMemberGroups.value = emptyList()

        listenToFeed()
    }

    private fun listenToFeed() {
        val user = auth.currentUser ?: return
        val userId = user.uid

        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val joinedGaggles = userDoc.get("joinedGaggles") as? List<String> ?: emptyList()
                Log.d("FeedViewModel", "Joined gaggles: $joinedGaggles")

                if (joinedGaggles.isEmpty()) {
                    _feedItems.value = emptyList()
                    _gaggleMemberGroups.value = emptyList()
                    return@addOnSuccessListener
                }

                val allMemberIds = mutableSetOf<String>()
                val tasksToLoad = mutableListOf<() -> Unit>()

                joinedGaggles.forEach { gaggleId ->
                    tasksToLoad.add {
                        db.collection("gaggles").document(gaggleId).get()
                            .addOnSuccessListener { gaggleDoc ->
                                val members = gaggleDoc.get("members") as? List<String> ?: emptyList()
                                allMemberIds.addAll(members)

                                allMemberIds.add(userId)

                                Log.d("FeedViewModel", "All member IDs: $allMemberIds")

                                if (allMemberIds.isNotEmpty()) {
                                    listenToAllMemberTasks(allMemberIds.toList())
                                }
                            }
                    }
                }
                tasksToLoad.forEach { it() }

                joinedGaggles.forEach { gaggleId ->
                    loadGaggleMembers(gaggleId)
                }
                loadFeed()
            }
    }

    private fun listenToAllMemberTasks(memberIds: List<String>) {
        val feedItemsMap = mutableMapOf<String, FeedItem>() // Shared map: key = taskTitle+userId+gaggleTitle

        memberIds.forEach { memberId ->
            val userTasksRef = db.collection("tasks").document(memberId).collection("userTasks")

            val listener = userTasksRef
                .whereEqualTo("date", today)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener

                    snapshot.documentChanges.forEach { change ->
                        val doc = change.document
                        val taskTitle = doc.getString("title") ?: return@forEach
                        val gaggleTitle = doc.getString("gaggleTitle") ?: "Unnamed Gaggle"
                        val taskUserId = doc.getString("userId") ?: return@forEach
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        val key = "$taskTitle|$taskUserId|$gaggleTitle" // Unique key for this feed item

                        val userRef = db.collection("users").document(taskUserId)

                        when (change.type) {
                            DocumentChange.Type.ADDED,
                            DocumentChange.Type.MODIFIED -> {
                                val isCompleted = doc.getBoolean("completed") ?: false
                                if (isCompleted) {
                                    userRef.get().addOnSuccessListener { userDoc ->
                                        val username = userDoc.getString("username") ?: "Unknown"
                                        feedItemsMap[key] = FeedItem(
                                            userName = username,
                                            gaggleTitle = gaggleTitle,
                                            taskTitle = taskTitle,
                                            date = today,
                                            timestamp = timestamp,
                                            completed = true
                                        )
                                        _feedItems.postValue(feedItemsMap.values.sortedByDescending { it.timestamp })
                                    }
                                }
                            }
                            DocumentChange.Type.REMOVED -> {
                                userRef.get().addOnSuccessListener { userDoc ->
                                    val username = userDoc.getString("username") ?: "Unknown"
                                    val removalKey = "$taskTitle|$taskUserId|$gaggleTitle"
                                    feedItemsMap.remove(removalKey)
                                    _feedItems.postValue(feedItemsMap.values.sortedByDescending { it.timestamp })
                                }
                            }
                        }
                    }
                }

            listeners.add(listener)
        }
    }



    private fun loadGaggleMembers(gaggleId: String) {
        val gaggleDocRef = db.collection("gaggles").document(gaggleId)

        gaggleDocRef.get().addOnSuccessListener { gaggleSnapshot ->
            val members = gaggleSnapshot.get("members") as? List<String> ?: return@addOnSuccessListener
            val gaggleTitle = gaggleSnapshot.getString("title") ?: "Unnamed Gaggle"
            val memberIcons = mutableListOf<MemberIcon>()

            if (members.isEmpty()) return@addOnSuccessListener

            var fetchedCount = 0
            members.forEach { memberId ->
                db.collection("users").document(memberId).get()
                    .addOnSuccessListener { userDoc ->
                        val username = userDoc.getString("username") ?: "Unknown"
                        val profileImageUrl = userDoc.getString("profileImageUrl")
                        memberIcons.add(MemberIcon(memberId, username, profileImageUrl))

                        fetchedCount++
                        if (fetchedCount == members.size) {
                            val updatedGroups = _gaggleMemberGroups.value?.toMutableList() ?: mutableListOf()
                            val existingIndex = updatedGroups.indexOfFirst { it.gaggleTitle == gaggleTitle }
                            if (existingIndex != -1) {
                                updatedGroups[existingIndex] = GaggleMemberGroup(gaggleTitle, memberIcons)
                            } else {
                                updatedGroups.add(GaggleMemberGroup(gaggleTitle, memberIcons))
                            }
                            _gaggleMemberGroups.value = updatedGroups
                            Log.d("FeedViewModel", "Updated gaggle member groups: $updatedGroups")
                        }
                    }
            }
        }
    }


    fun renderGaggleHeaders(): List<GaggleMemberGroup> {
        return _gaggleMemberGroups.value ?: emptyList()
    }

    fun loadFeed() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val joinedGaggles = userDoc.get("joinedGaggles") as? List<String> ?: emptyList()
                val username = userDoc.getString("username") ?: "Someone"

                if (joinedGaggles.isEmpty()) {
                    _feedItems.value = emptyList()
                    return@addOnSuccessListener
                }

                val newItems = mutableListOf<FeedItem>()

                joinedGaggles.forEach { gaggleId ->
                    db.collection("gaggles").document(gaggleId).get()
                        .addOnSuccessListener { gaggleDoc ->
                            val gaggleTitle = gaggleDoc?.getString("title") ?: "Unnamed Gaggle"
                            val taskTitles = gaggleDoc?.get("tasks") as? List<String> ?: emptyList()

                            taskTitles.forEach { title ->
                                val taskId = "${title}_$today"
                                db.collection("tasks")
                                    .document(userId)
                                    .collection("userTasks")
                                    .document(taskId)
                                    .get()
                                    .addOnSuccessListener { taskDoc ->
                                        val completed = taskDoc?.getBoolean("completed") ?: false
                                        val timestamp = taskDoc?.getLong("timestamp") ?: System.currentTimeMillis()
                                        newItems.add(
                                            FeedItem(
                                                userName = username,
                                                gaggleTitle = gaggleTitle,
                                                taskTitle = title,
                                                date = today,
                                                timestamp = timestamp,
                                                completed = completed
                                            )
                                        )
                                        _feedItems.postValue(newItems.sortedByDescending { it.timestamp })
                                    }
                            }
                        }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        listeners.forEach { it.remove() }
        listeners.clear()
    }
}
