package edu.utap.gaggle

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider

class ChallengeCompleteActivity : AppCompatActivity() {

    private lateinit var viewModel: GaggleViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenge_complete)

        viewModel = ViewModelProvider(this)[GaggleViewModel::class.java]

        findViewById<Button>(R.id.returnToFeedButton).setOnClickListener {
            viewModel.challengeState.value = ChallengeState.NotStarted // Reset for next challenge
            val intent = Intent(this, FeedActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}