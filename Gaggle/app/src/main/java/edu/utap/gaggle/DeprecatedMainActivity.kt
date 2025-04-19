package edu.utap.gaggle

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.identity.util.UUID
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Core user flow navigation and ViewModel logic for Gaggle

sealed class GaggleScreen {
    object UserProfile : GaggleScreen()
    object Preferences : GaggleScreen()
    object Matching : GaggleScreen()
    object ChallengeSetup : GaggleScreen()
    object PostCreation : GaggleScreen()
    object Feed : GaggleScreen()
    object ChallengeComplete : GaggleScreen()
}

// Shared ViewModel to manage app-wide state
class GaggleViewModel : ViewModel() {
    val userProfile = MutableLiveData<UserProfile?>()
    val gagglePreferences = MutableLiveData<GagglePreferences?>()
    val currentGaggle = MutableLiveData<Gaggle?>()
    val challengeRule = MutableLiveData<ChallengeRule?>()
    val challengeState = MutableLiveData<ChallengeState>()
    val posts = MutableLiveData<List<Post>>()

    init {
        challengeState.value = ChallengeState.NotStarted
        posts.value = emptyList()
    }

    fun getNextScreen(): GaggleScreen {
        return when {
            !userHasSetPreferences() -> GaggleScreen.Preferences
            !userHasSetupChallenge() -> GaggleScreen.ChallengeSetup
            else -> GaggleScreen.Feed
        }
    }

    private fun userHasSetPreferences(): Boolean {
        return userProfile.value?.interests?.isNotEmpty() == true
    }

    private fun userHasSetupChallenge(): Boolean {
        return challengeRule.value != null
    }


    fun startChallenge(rule: ChallengeRule) {
        challengeRule.value = rule
        challengeState.value = ChallengeState.InProgress
    }

    fun isInGaggle(): Boolean {
        return currentGaggle.value != null
    }

    fun hasChallengeStarted(): Boolean {
        return challengeState.value != ChallengeState.NotStarted
    }

    fun saveUserProfile(name: String, interests: List<String>) {
        userProfile.value = UserProfile(name, interests)
    }

    fun saveGagglePreferences(challengeLength: Int, groupSize: Int, topics: List<String>) {
        gagglePreferences.value = GagglePreferences(challengeLength, groupSize, topics)
    }

    fun matchUserToGaggle() {
        // Placeholder: Replace with Firebase or algorithmic matching
        val dummyGaggle = Gaggle(
            id = UUID.randomUUID().toString(),
            members = listOf("user1", "user2", "user3")
        )
        currentGaggle.value = dummyGaggle
    }

    fun startChallenge() {
        challengeState.value = ChallengeState.InProgress
    }

    fun completeChallenge() {
        challengeState.value = ChallengeState.Complete
    }

    fun createPost(content: String) {
        val user = userProfile.value?.name ?: return
        val newPost = Post(
            id = UUID.randomUUID().toString(),
            user = user,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        posts.value = posts.value?.plus(newPost)
    }

    fun reactToPost(postId: String, reaction: ReactionType) {
        // Placeholder: Add reaction to post
    }

    fun commentOnPost(postId: String, comment: String) {
        // Placeholder: Add comment to post
    }

    fun getFeed(): LiveData<List<Post>> {
        return posts
    }

    fun loadUserProfile(onLoaded: () -> Unit) {
        Log.d("VIEWMODEL", "getting uid from ${FirebaseAuth.getInstance().currentUser}")
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Log.d("VIEWMODEL", "${uid}")
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val name = doc.getString("name") ?: ""
                    val interests = doc.get("interests") as? List<String> ?: emptyList()
                    userProfile.value = UserProfile(name, interests)
                }
                onLoaded()
            }
            .addOnFailureListener {
                onLoaded() // fallback
            }
    }

}

// Activity Router
class DeprecatedMainActivity : AppCompatActivity() {
    private lateinit var viewModel: GaggleViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.deprecated_activity_main)

        viewModel = ViewModelProvider(this)[GaggleViewModel::class.java]

        // Assume Firebase is used for authentication
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            // No user signed in, go to UserProfile creation
            goTo(GaggleScreen.UserProfile)
        } else {
            // Load user profile from Firestore or SharedPreferences (placeholder logic)
            viewModel.loadUserProfile {
                navigateBasedOnState()
            }
        }
    }

    private fun navigateBasedOnState() {
        val profile = viewModel.userProfile.value
        when {
            profile == null -> goTo(GaggleScreen.UserProfile)
            !viewModel.isInGaggle() -> goTo(GaggleScreen.Preferences)
            !viewModel.hasChallengeStarted() -> goTo(GaggleScreen.ChallengeSetup)
            else -> goTo(GaggleScreen.Feed)
        }
        finish() // prevent returning to splash/main
    }

    private fun goTo(screen: GaggleScreen) {
        val intent = when (screen) {
            GaggleScreen.UserProfile -> Intent(this, UserProfileActivity::class.java)
            GaggleScreen.Preferences -> Intent(this, PreferencesActivity::class.java)
            GaggleScreen.Matching -> Intent(this, MatchingActivity::class.java)
            GaggleScreen.ChallengeSetup -> Intent(this, ChallengeSetupActivity::class.java)
            GaggleScreen.PostCreation -> Intent(this, PostCreationActivity::class.java)
            GaggleScreen.Feed -> Intent(this, FeedActivity::class.java)
            GaggleScreen.ChallengeComplete -> Intent(this, ChallengeCompleteActivity::class.java)
        }
        startActivity(intent)
    }
}


// Supporting Models
data class UserProfile(val name: String, val interests: List<String>)
data class GagglePreferences(val challengeLength: Int, val groupSize: Int, val topics: List<String>)
data class Gaggle(val id: String, val members: List<String>)
data class Post(
    val id: String,
    val user: String,
    val content: String,
    val timestamp: Long,
    val imageUrl: String? = null
)

data class ChallengeRule(
    val title: String,
    val description: String
)

enum class ChallengeState {
    NotStarted,
    InProgress,
    Complete
}

enum class ReactionType {
    LIKE, SUPPORT, FIRE, CLAP
}