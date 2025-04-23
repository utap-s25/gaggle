package edu.utap.gaggle.viewmodel

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import java.util.Date
import edu.utap.gaggle.model.Task
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.util.Log

class ProgressViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun saveTaskCompletion(taskId: String, completed: Boolean) {
        viewModelScope.launch{
            val userId = auth.currentUser?.uid ?: return@launch // Ensure user is logged in
            val taskDocRef = firestore.collection("tasks").document(taskId)

            // Update the task document
            taskDocRef.update(mapOf(
                "completed" to completed,
                "userId" to userId,
                "timestamp" to FieldValue.serverTimestamp()
            )).addOnSuccessListener {
                Log.d("ProgressFragment", "Task completion updated for task: $taskId")
                // Optionally: Update UI to show success, etc.
            }
                .addOnFailureListener { e ->
                    Log.w("ProgressFragment", "Error updating task completion for task: $taskId", e)
                    // Optionally: Show error message to the user
                }
        }
    }
}