package edu.utap.gaggle

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class ChallengeSetupActivity : AppCompatActivity() {

    private val viewModel: GaggleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenge_setup)

        val titleInput: EditText = findViewById(R.id.challengeTitleInput)
        val descriptionInput: EditText = findViewById(R.id.challengeDescInput)
        val startButton: Button = findViewById(R.id.startChallengeButton)

        startButton.setOnClickListener {
            val title = titleInput.text.toString().trim()
            val description = descriptionInput.text.toString().trim()

            if (title.isNotEmpty() && description.isNotEmpty()) {
                val challengeRule = ChallengeRule(title, description)
                viewModel.startChallenge(challengeRule)

                val intent = Intent(this, PostCreationActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}
