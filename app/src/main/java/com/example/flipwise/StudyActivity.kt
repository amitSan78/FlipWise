@file:Suppress("DEPRECATION")

package com.example.flipwise

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.lifecycle.lifecycleScope
import com.example.flipwise.data.DatabaseProvider
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.random.Random


data class StudyCard(val word: String, val romaji: String, val translation: String)

class StudyActivity : AppCompatActivity() {

    private val db by lazy { DatabaseProvider.get(this) }

    // UI
    private lateinit var cardContainer: View
    private lateinit var front: View
    private lateinit var back: View
    private lateinit var tvWord: TextView
    private lateinit var tvRomaji: TextView
    private lateinit var tvTranslation: TextView
    private lateinit var btnToggleRomaji: Button
    private lateinit var btnEasy: Button
    private lateinit var btnStruggle: Button

    // Queue
    private val queue = ArrayDeque<StudyCard>()
    private var showingFront = true
    private lateinit var detector: GestureDetectorCompat
    // Recent history to avoid immediate repeats
    private val recent = ArrayDeque<String>() // store a key (word or id)
    private val RECENT_WINDOW = 3       // how many to remember

    // Animation
    private var isFlipping = false
    private var originalElevation = 0f


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_study)

        title = "Study"

        // Bind
        cardContainer = findViewById(R.id.cardContainer)
        front = findViewById(R.id.frontSide)
        back = findViewById(R.id.backSide)
        tvWord = findViewById(R.id.tvWord)
        tvRomaji = findViewById(R.id.tvRomaji)
        tvTranslation = findViewById(R.id.tvTranslation)
        btnToggleRomaji = findViewById(R.id.btnToggleRomaji)
        btnEasy = findViewById(R.id.btnEasy)
        btnStruggle = findViewById(R.id.btnStruggle)

        // Set up the card for 3D flipping
        setupCardForFlipping()

        detector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            // must return true so we receive further events
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                flip()
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null || e2 == null) return false
                val dy = e2.y - e1.y
                if (kotlin.math.abs(dy) > 200) {
                    flip()
                    return true
                }
                return false
            }
        })

        cardContainer.setOnTouchListener { _, ev ->
            detector.onTouchEvent(ev)
            true
        }

        btnToggleRomaji.setOnClickListener {
            tvRomaji.visibility =
                if (tvRomaji.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            btnToggleRomaji.text = if (tvRomaji.visibility == View.VISIBLE) "Hide Romaji" else "Show Romaji"
        }

        btnStruggle.setOnClickListener { markCurrent(struggle = true) }
        btnEasy.setOnClickListener { markCurrent(struggle = false) }

        // Load data
        val ids = intent.getIntegerArrayListExtra("categoryIds") ?: arrayListOf()
        lifecycleScope.launch {
            val words = db.wordDao().getForCategories(ids)
            val cards = words.map { StudyCard(it.hiragana, it.romaji, it.translation) }
            // randomize start order
            queue.clear()
            queue.addAll(cards.shuffled())
            showTop()
        }
    }

    private fun setupCardForFlipping() {
        // Set the distance of the camera from the card container for 3D effect
        val scale = resources.displayMetrics.density
        val cameraDistance = 8000 * scale

        cardContainer.cameraDistance = cameraDistance

        // Store original elevation to restore later
        originalElevation = cardContainer.elevation

        // The back side needs to be pre-rotated 180° so it appears correctly
        // when the container is flipped
        back.rotationY = 180f

        // Initially show front, hide back
        front.visibility = View.VISIBLE
        back.visibility = View.GONE
    }

    private fun showTop() {
        if (queue.isEmpty()) {
            finish() // or show a "Done!" dialog then finish
            return
        }

        // Avoid showing the same card immediately again
        // If top is in recent and there are more cards, rotate once
        if (queue.size > 1 && recent.contains(queue.first().word)) {
            val skipped = queue.removeFirst()
            queue.addLast(skipped)
            // If rotating still leaves a recent on top, that's fine—we only rotate once
        }

        val c = queue.first()
        tvWord.text = c.word
        tvRomaji.text = c.romaji
        tvTranslation.text = c.translation
        tvRomaji.visibility = View.GONE
        btnToggleRomaji.text = "Show Romaji"

        // Reset to front side without animation
        if (!showingFront) {
            resetToFront()
        }
    }

    private fun resetToFront() {
        // Reset to front immediately without animation
        showingFront = true
        cardContainer.rotationY = 0f
        front.visibility = View.VISIBLE
        back.visibility = View.GONE
    }

    private fun flip() {
        // Prevent multiple flips at once
        if (isFlipping) return

        isFlipping = true

        val flipDuration = 300L // milliseconds

        if (showingFront) {
            // Flip from front to back
            flipToBack(flipDuration)
        } else {
            // Flip from back to front
            flipToFront(flipDuration)
        }

        showingFront = !showingFront
    }

    private fun flipToBack(duration: Long) {
        // Temporarily remove elevation to prevent shadow artifacts during rotation
        cardContainer.elevation = 0f

        // At 90 degrees, switch content visibility
        cardContainer.animate()
            .rotationY(90f)
            .setDuration(duration / 2)
            .withEndAction {
                // Switch from front to back content
                front.visibility = View.GONE
                back.visibility = View.VISIBLE

                // Continue rotating to 180 degrees
                cardContainer.animate()
                    .rotationY(180f)
                    .setDuration(duration / 2)
                    .withEndAction {
                        // Restore elevation after flip is complete
                        cardContainer.elevation = originalElevation
                        isFlipping = false
                    }
                    .start()
            }
            .start()
    }

    private fun flipToFront(duration: Long) {
        // Temporarily remove elevation to prevent shadow artifacts during rotation
        cardContainer.elevation = 0f

        // Continue rotating from 180 to 270 degrees
        cardContainer.animate()
            .rotationY(270f)
            .setDuration(duration / 2)
            .withEndAction {
                // At 270 degrees, switch content from back to front
                back.visibility = View.GONE
                front.visibility = View.VISIBLE

                // Complete the rotation to 360 degrees
                cardContainer.animate()
                    .rotationY(360f)
                    .setDuration(duration / 2)
                    .withEndAction {
                        // Reset rotation to 0 and restore elevation
                        cardContainer.rotationY = 0f
                        cardContainer.elevation = originalElevation
                        isFlipping = false
                    }
                    .start()
            }
            .start()
    }

    private fun markCurrent(struggle: Boolean) {
        if (queue.isEmpty()) return
        val current = queue.removeFirst()

        // Record into recent history (bounded)
        recent.addLast(current.word)
        while (recent.size > RECENT_WINDOW) recent.removeFirst()

        // ---- SRS spacing rules (tweakable) ----
        // "Struggle": bring back soon but not immediate
        // "Easy": bring back much later
        val minGap = if (struggle) 4 else 25   // at least this far away
        val maxGap = if (struggle) 7 else 60   // roughly not farther than this

        // Target gap within [minGap, maxGap], but constrained by queue size
        val targetGap = if (queue.isEmpty()) 0
        else Random.nextInt(minGap, maxGap + 1).coerceAtMost(queue.size)

        // Start with targetGap, nudge by ±1 randomly to avoid patterns
        var insertAt = (targetGap + Random.nextInt(-1, 2))
            .coerceIn(0, queue.size)

        // If we'd land on an identical item (rare), bump forward one
        if (insertAt < queue.size && queue.elementAt(insertAt).word == current.word) {
            insertAt = (insertAt + 1).coerceAtMost(queue.size)
        }

        // Insert back into the queue
        val list = queue.toMutableList()
        list.add(insertAt, current)
        queue.clear(); queue.addAll(list)

        // Reset to front next card if needed and continue
        if (!showingFront) {
            resetToFront()
        }
        showTop()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        detector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }
}