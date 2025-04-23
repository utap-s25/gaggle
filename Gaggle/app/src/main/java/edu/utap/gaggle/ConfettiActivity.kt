package edu.utap.gaggle

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import nl.dionsegijn.konfetti.xml.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import nl.dionsegijn.konfetti.core.models.Size
import java.util.concurrent.TimeUnit

class ConfettiActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confetti)

        val konfettiView = findViewById<KonfettiView>(R.id.konfettiView)
        val streak = intent.getIntExtra("STREAK_COUNT", 1)
        val gaggleTitle = intent.getStringExtra("GAGGLE_TITLE") ?: "Your Gaggle"

        findViewById<TextView>(R.id.streakText).text =
            "🎉 All tasks completed in $gaggleTitle!\n🔥 Current Streak: $streak days!"

        konfettiView.start(
            Party(
                speed = 0f,
                maxSpeed = 30f,
                damping = 0.9f,
                spread = 360,
                colors = listOf(0xFFE57373.toInt(), 0xFF81C784.toInt(), 0xFF64B5F6.toInt()),
                position = Position.Relative(0.5, 0.0),
                shapes = listOf(Shape.Circle, Shape.Square),
                size = listOf(Size.SMALL, Size.LARGE),
                timeToLive = 2000L,
                emitter = Emitter(duration = 3, TimeUnit.SECONDS).perSecond(50)
            )
        )
    }
}
