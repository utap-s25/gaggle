package edu.utap.gaggle

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // debugging Goal Input
        val intent = Intent(this, GoalInputActivity::class.java)
        startActivity(intent)
        finish()
    }
}
