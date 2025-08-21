package com.example.flipwise

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.lifecycleScope
import com.example.flipwise.data.Category
import com.example.flipwise.data.DatabaseProvider
import com.example.flipwise.data.Word
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class WordListActivity : AppCompatActivity() {

    private lateinit var adapter: WordAdapter
    private val words = mutableListOf<UiWord>()
    private val wordIds = mutableListOf<Int>() // Track actual word IDs

    private var categoryId: Int = -1
    private val db by lazy { DatabaseProvider.get(this) }

    // Multi-select mode
    private var actionMode: ActionMode? = null
    private val selectedPositions = mutableSetOf<Int>()

    private lateinit var listView: ListView

    private fun capitalizeFirstLetter(text: String): String {
        return if (text.isEmpty()) text else text.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
    }

    private fun capitalizeWords(text: String): String {
        return text.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_word_list)

        title = intent.getStringExtra("categoryName") ?: "Words"
        categoryId = intent.getIntExtra("categoryId", -1)

        if (categoryId == -1) {
            Toast.makeText(this, "Invalid category", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        listView = findViewById<ListView>(R.id.wordList)
        val fab = findViewById<FloatingActionButton>(R.id.fabAddWord)

        // Initialize adapter
        adapter = WordAdapter(this, words)
        listView.adapter = adapter

        // Long click to start multi-select
        listView.setOnItemLongClickListener { _, _, position, _ ->
            if (actionMode == null) {
                actionMode = startSupportActionMode(actionModeCallback)
            }
            toggleSelection(position)
            true
        }

        // Single click during selection mode
        listView.setOnItemClickListener { _, _, position, _ ->
            if (actionMode != null) {
                toggleSelection(position)
            } else {
                // Edit word
                showEditWordDialog(position)
            }
        }

        fab.setOnClickListener { showAddWordDialog() }
        loadWords()
    }

    private fun toggleSelection(position: Int) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position)
        } else {
            selectedPositions.add(position)
        }

        updateAdapter()
        actionMode?.title = "${selectedPositions.size} selected"

        if (selectedPositions.isEmpty()) {
            actionMode?.finish()
        }
    }

    private fun updateAdapter() {
        adapter = WordAdapter(
            this,
            words,
            selectedPositions,
            actionMode != null
        )
        listView.adapter = adapter
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            menuInflater.inflate(R.menu.word_action_menu, menu)
            updateAdapter() // Show checkboxes
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_delete -> {
                    showDeleteConfirmation()
                    true
                }
                R.id.action_transfer -> {
                    showTransferDialog()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null
            selectedPositions.clear()
            updateAdapter() // Hide checkboxes
        }
    }

    private fun loadWords() = lifecycleScope.launch {
        val data = db.wordDao().getForCategory(categoryId)
        words.clear()
        wordIds.clear()
        data.forEach { word ->
            words.add(UiWord(word.hiragana, word.romaji, word.translation))
            wordIds.add(word.id)
        }
        updateAdapter()
    }

    private fun showAddWordDialog() {
        showWordDialog(null, -1)
    }

    private fun showEditWordDialog(position: Int) {
        showWordDialog(words[position], wordIds[position])
    }

    private fun showWordDialog(existingWord: UiWord?, wordId: Int) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }

        val wordEt = EditText(this).apply {
            hint = "The Word (e.g. さかな)"
            setText(existingWord?.hiragana ?: "")
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        val romajiEt = EditText(this).apply {
            hint = "Romaji (e.g. sakana)"
            setText(existingWord?.romaji ?: "")
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        val transEt = EditText(this).apply {
            hint = "Translation (e.g. fish)"
            setText(existingWord?.english ?: "")
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }

        container.addView(wordEt)
        container.addView(romajiEt)
        container.addView(transEt)

        AlertDialog.Builder(this)
            .setTitle(if (existingWord == null) "Add Word" else "Edit Word")
            .setView(container)
            .setPositiveButton(if (existingWord == null) "Add" else "Save") { _, _ ->
                val word = capitalizeFirstLetter(wordEt.text.toString().trim())
                val romaji = romajiEt.text.toString().trim()
                val translation = capitalizeFirstLetter(transEt.text.toString().trim())

                if (word.isNotEmpty() && romaji.isNotEmpty() && translation.isNotEmpty()) {
                    lifecycleScope.launch {
                        if (existingWord == null) {
                            // Add new word
                            db.wordDao().insert(
                                Word(
                                    categoryId = categoryId,
                                    hiragana = word,
                                    romaji = romaji,
                                    translation = translation
                                )
                            )
                        } else {
                            // Update existing word
                            db.wordDao().update(
                                Word(
                                    id = wordId,
                                    categoryId = categoryId,
                                    hiragana = word,
                                    romaji = romaji,
                                    translation = translation
                                )
                            )
                        }
                        loadWords()
                    }
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Words")
            .setMessage("Are you sure you want to delete ${selectedPositions.size} word(s)?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    val idsToDelete = selectedPositions.map { wordIds[it] }
                    idsToDelete.forEach { id ->
                        db.wordDao().deleteById(id)
                    }
                    loadWords()
                    actionMode?.finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTransferDialog() {
        lifecycleScope.launch {
            // Get all decks for transfer options
            val decks = db.deckDao().getAllDecks()

            if (decks.isEmpty()) {
                Toast.makeText(this@WordListActivity, "No other decks available", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val deckNames = decks.map { "${it.name}" }.toTypedArray()
            val selectedDeck = arrayOf(-1) // Store selected deck index

            AlertDialog.Builder(this@WordListActivity)
                .setTitle("Choose Target Deck")
                .setSingleChoiceItems(deckNames, -1) { _, which ->
                    selectedDeck[0] = which
                }
                .setPositiveButton("Next") { _, _ ->
                    if (selectedDeck[0] >= 0) {
                        val targetDeckId = decks[selectedDeck[0]].id
                        showCategorySelectionDialog(targetDeckId)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showCategorySelectionDialog(targetDeckId: Int) {
        lifecycleScope.launch {
            val categories = db.categoryDao().getForDeck(targetDeckId)

            if (categories.isEmpty()) {
                Toast.makeText(this@WordListActivity, "Target deck has no categories", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val categoryNames = categories.map { it.name }.toTypedArray()
            val selectedCategory = arrayOf(-1)

            AlertDialog.Builder(this@WordListActivity)
                .setTitle("Choose Target Category")
                .setSingleChoiceItems(categoryNames, -1) { _, which ->
                    selectedCategory[0] = which
                }
                .setPositiveButton("Transfer") { _, _ ->
                    if (selectedCategory[0] >= 0) {
                        val targetCategoryId = categories[selectedCategory[0]].id
                        performTransfer(targetCategoryId)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun performTransfer(targetCategoryId: Int) {
        lifecycleScope.launch {
            val wordsToTransfer = selectedPositions.map { position ->
                val wordId = wordIds[position]
                val uiWord = words[position]
                Word(
                    id = wordId,
                    categoryId = targetCategoryId,
                    hiragana = uiWord.hiragana,
                    romaji = uiWord.romaji,
                    translation = uiWord.english
                )
            }

            wordsToTransfer.forEach { word ->
                db.wordDao().update(word)
            }

            Toast.makeText(this@WordListActivity,
                "${wordsToTransfer.size} words transferred successfully",
                Toast.LENGTH_SHORT).show()

            loadWords()
            actionMode?.finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.word_list_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_select_all -> {
                if (actionMode == null) {
                    actionMode = startSupportActionMode(actionModeCallback)
                }
                selectedPositions.clear()
                for (i in 0 until words.size) {
                    selectedPositions.add(i)
                }
                updateAdapter()
                actionMode?.title = "${selectedPositions.size} selected"
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}