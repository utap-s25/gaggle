package edu.utap.gaggle

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.progressindicator.CircularProgressIndicator

class MatchingActivity : AppCompatActivity() {
    private lateinit var viewModel: GaggleViewModel
    private lateinit var matchingStatusText: TextView
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var matchButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_matching)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(GaggleViewModel::class.java)

        // Initialize Views
        matchingStatusText = findViewById(R.id.matchingStatusText)
        progressIndicator = findViewById(R.id.progressIndicator)
        matchButton = findViewById(R.id.matchButton)

        // Show loading indicator while matching
        showLoadingState()

        // Simulate the matching process
        matchButton.setOnClickListener {
            matchGaggle()
        }

        // Observe matching result
        viewModel.currentGaggle.observe(this) { gaggle ->
            if (gaggle != null) {
                showMatchedGaggle(gaggle)
            }
        }
    }

    private fun showLoadingState() {
        matchingStatusText.text = "Matching you to a Gaggle..."
        progressIndicator.visibility = CircularProgressIndicator.VISIBLE
        matchButton.visibility = Button.GONE
    }

    private fun showMatchedGaggle(gaggle: Gaggle) {
        matchingStatusText.text = "Matched with Gaggle: ${gaggle.id} with ${gaggle.members.size} members"
        progressIndicator.visibility = CircularProgressIndicator.GONE
        matchButton.visibility = Button.VISIBLE

        // When match is successful, show the button to proceed to challenge setup
        matchButton.text = "Proceed to Challenge Setup"
        matchButton.setOnClickListener {
            navigateToChallengeSetup()
        }
    }

    private fun matchGaggle() {
        // This is where the logic of matching the user to a Gaggle would go
        // Simulate matching with dummy data for now
        val matchedGaggle = Gaggle("Group_1", listOf("user1", "user2", "user3"))
        viewModel.currentGaggle.value = matchedGaggle
    }

    private fun navigateToChallengeSetup() {
        val intent = Intent(this, ChallengeSetupActivity::class.java)
        startActivity(intent)
        finish() // Close MatchingActivity
    }
}
