@file:Suppress("DEPRECATION")

package com.example.flipwise

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_study)

        title = "Study"

        // Bind
        front = findViewById(R.id.frontSide)
        back = findViewById(R.id.backSide)
        tvWord = findViewById(R.id.tvWord)
        tvRomaji = findViewById(R.id.tvRomaji)
        tvTranslation = findViewById(R.id.tvTranslation)
        btnToggleRomaji = findViewById(R.id.btnToggleRomaji)
        btnEasy = findViewById(R.id.btnEasy)
        btnStruggle = findViewById(R.id.btnStruggle)

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

        findViewById<View>(R.id.cardContainer).setOnTouchListener { _, ev ->
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
        if (!showingFront) flip() // ensure we start on front
    }


    private fun flip() {
        // quick flip without heavy animation (we can add 3D later)
        if (showingFront) {
            front.visibility = View.GONE
            back.visibility = View.VISIBLE
        } else {
            back.visibility = View.GONE
            front.visibility = View.VISIBLE
        }
        showingFront = !showingFront
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

        // If we’d land on an identical item (rare), bump forward one
        if (insertAt < queue.size && queue.elementAt(insertAt).word == current.word) {
            insertAt = (insertAt + 1).coerceAtMost(queue.size)
        }

        // Insert back into the queue
        val list = queue.toMutableList()
        list.add(insertAt, current)
        queue.clear(); queue.addAll(list)

        // Reset to front next card if needed and continue
        if (!showingFront) flip()
        showTop()
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        detector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }
}
