package com.example.flipwise

import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.flipwise.data.DatabaseProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class WordListActivity : AppCompatActivity() {

    private lateinit var adapter: WordAdapter
    private val words = mutableListOf<UiWord>() // mutable list for adapter

    private var categoryId: Int = -1   // <- use Int to match DAO
    private val db by lazy { DatabaseProvider.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_word_list)

        // Read extras from intent
        title = intent.getStringExtra("categoryName") ?: "Words"
        categoryId = intent.getIntExtra("categoryId", -1)

        if (categoryId == -1) {
            Toast.makeText(this, "Invalid category", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val listView = findViewById<ListView>(R.id.wordList)
        val fab = findViewById<FloatingActionButton>(R.id.fabAddWord)

        adapter = WordAdapter(this, words)
        listView.adapter = adapter

        fab.setOnClickListener { showAddWordDialog() }

        loadWords()
    }

    private fun loadWords() = lifecycleScope.launch {
        // DB entity: com.example.flipwise.data.Word
        val data = db.wordDao().getForCategory(categoryId)
        words.clear()
        words.addAll(data.map { UiWord(it.hiragana, it.romaji, it.translation) })
        adapter.notifyDataSetChanged()
    }

    private fun showAddWordDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }

        val wordEt   = EditText(this).apply { hint = "The Word (e.g. さかな)" }
        val romajiEt = EditText(this).apply { hint = "Romaji (e.g. sakana)" }
        val transEt  = EditText(this).apply { hint = "Translation (e.g. fish)" }

        container.addView(wordEt)
        container.addView(romajiEt)
        container.addView(transEt)

        AlertDialog.Builder(this)
            .setTitle("Add Word")
            .setView(container)
            .setPositiveButton("Add") { _, _ ->
                val word = wordEt.text.toString().trim()
                val romaji = romajiEt.text.toString().trim()
                val translation = transEt.text.toString().trim()

                // All three required
                if (word.isNotEmpty() && romaji.isNotEmpty() && translation.isNotEmpty()) {
                    lifecycleScope.launch {
                        db.wordDao().insert(
                            com.example.flipwise.data.Word(
                                categoryId = categoryId,   // Int matches DAO
                                hiragana   = word,
                                romaji     = romaji,
                                translation= translation
                            )
                        )
                        loadWords()
                    }
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
