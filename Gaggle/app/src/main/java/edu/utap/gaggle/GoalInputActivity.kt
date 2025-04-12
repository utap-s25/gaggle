package edu.utap.gaggle

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class GoalInputActivity : AppCompatActivity() {

    private lateinit var goalEditText: EditText
    private lateinit var durationGroup: RadioGroup
    private lateinit var submitGoalButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_input)

        goalEditText = findViewById(R.id.goalEditText)
        durationGroup = findViewById(R.id.durationGroup)
        submitGoalButton = findViewById(R.id.submitGoalButton)

        submitGoalButton.setOnClickListener {
            val goalText = goalEditText.text.toString().trim()
            val selectedDurationId = durationGroup.checkedRadioButtonId

            if (goalText.isEmpty() || selectedDurationId == -1) {
                Toast.makeText(this, "Please enter a goal and select duration", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val duration = when (selectedDurationId) {
                R.id.sevenDays -> 7
                R.id.fourteenDays -> 14
                R.id.thirtyDays -> 30
                else -> 0
            }

            // placeholder, should save goal & duration for firebase or next activity
            // TODO: replace with startActivity(Intent(this, NextActivity::class.java))
            Toast.makeText(this, "Goal set: $goalText ($duration days)", Toast.LENGTH_SHORT).show()
        }
    }
}
