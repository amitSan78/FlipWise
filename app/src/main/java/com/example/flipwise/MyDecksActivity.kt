package com.example.flipwise

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.lifecycleScope
import com.example.flipwise.data.DatabaseProvider
import com.example.flipwise.data.Deck
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.util.Locale

class MyDecksActivity : AppCompatActivity() {

    private lateinit var adapter: SelectableDeckAdapter
    private val display = mutableListOf<String>()  // "🇯🇵 Japanese"
    private val ids = mutableListOf<Int>()         // deck ids
    private val codes = mutableListOf<String>()    // country codes

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
        setContentView(R.layout.activity_my_decks)

        listView = findViewById<ListView>(R.id.deckList)
        val fab = findViewById<FloatingActionButton>(R.id.fabAddDeck)

        // Initialize adapter
        adapter = SelectableDeckAdapter(this, display)
        listView.adapter = adapter

        // Long click to start multi-select
        listView.setOnItemLongClickListener { _, _, position, _ ->
            if (actionMode == null) {
                actionMode = startSupportActionMode(actionModeCallback)
            }
            toggleSelection(position)
            true
        }

        // Single click behavior
        listView.setOnItemClickListener { _, _, position, _ ->
            if (actionMode != null) {
                toggleSelection(position)
            } else {
                // Navigate to categories in this deck
                val deckId = ids[position]
                val deckName = display[position]
                val i = Intent(this, CategoryListActivity::class.java)
                i.putExtra("deckId", deckId)
                i.putExtra("deckName", deckName)
                startActivity(i)
            }
        }

        fab.setOnClickListener { showAddDeckDialog() }
        loadDecks()
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
        adapter = SelectableDeckAdapter(
            this,
            display,
            selectedPositions,
            actionMode != null
        )
        listView.adapter = adapter
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            menuInflater.inflate(R.menu.deck_action_menu, menu)
            updateAdapter() // Show checkboxes
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            // Show edit only if single item selected
            menu.findItem(R.id.action_edit)?.isVisible = selectedPositions.size == 1
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_edit -> {
                    if (selectedPositions.size == 1) {
                        val position = selectedPositions.first()
                        showEditDeckDialog(position)
                    }
                    true
                }
                R.id.action_delete -> {
                    showDeleteConfirmation()
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

    private fun loadDecks() = lifecycleScope.launch {
        val decks = db.deckDao().getAllDecks()
        display.clear()
        ids.clear()
        codes.clear()
        decks.forEach { d ->
            display.add("${flagEmoji(d.code)} ${d.name}")
            ids.add(d.id)
            codes.add(d.code)
        }
        updateAdapter()
    }

    private fun showAddDeckDialog() {
        showDeckDialog(null, null, -1)
    }

    private fun showEditDeckDialog(position: Int) {
        // Extract name without emoji
        val fullName = display[position]
        val name = fullName.substring(fullName.indexOf(' ') + 1)
        showDeckDialog(name, codes[position], ids[position])
    }

    private fun showDeckDialog(existingName: String?, existingCode: String?, deckId: Int) {
        val nameInput = EditText(this).apply {
            hint = "Language (e.g. Japanese)"
            setText(existingName ?: "")
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
        val codeInput = EditText(this).apply {
            hint = "JP for Japanese (optional)"  // Made it clear it's optional
            setText(existingCode ?: "")
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
            addView(nameInput)
            addView(codeInput)
        }

        AlertDialog.Builder(this)
            .setTitle(if (existingName == null) "Add New Deck" else "Edit Deck")
            .setView(container)
            .setPositiveButton(if (existingName == null) "Add" else "Save") { _, _ ->
                val name = capitalizeWords(nameInput.text.toString().trim())
                val codeText = codeInput.text.toString().trim().uppercase(Locale.US)

                // Validate code only if it's provided, otherwise use default
                val code = if (codeText.isEmpty()) {
                    "XX"  // Default code when no country code provided
                } else if (codeText.length == 2 && codeText.all { it in 'A'..'Z' }) {
                    codeText  // Valid country code
                } else {
                    "XX"  // Invalid code, use default
                }

                if (name.isNotEmpty()) {  // Only require name to be provided
                    lifecycleScope.launch {
                        if (existingName == null) {
                            // Add new deck
                            db.deckDao().insert(Deck(name = name, code = code))
                        } else {
                            // Update existing deck
                            db.deckDao().update(Deck(id = deckId, name = name, code = code))
                        }
                        loadDecks()
                        actionMode?.finish()
                    }
                } else {
                    Toast.makeText(this@MyDecksActivity, "Please enter a deck name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Decks")
            .setMessage("Are you sure you want to delete ${selectedPositions.size} deck${if (selectedPositions.size == 1) "" else "s"}?\n\nThis will also delete all categories and words in these decks.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    val idsToDelete = selectedPositions.map { ids[it] }
                    idsToDelete.forEach { id ->
                        db.deckDao().deleteById(id)
                    }
                    loadDecks()
                    actionMode?.finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.deck_list_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_select_all -> {
                if (actionMode == null) {
                    actionMode = startSupportActionMode(actionModeCallback)
                }
                selectedPositions.clear()
                for (i in 0 until display.size) {
                    selectedPositions.add(i)
                }
                updateAdapter()
                actionMode?.title = "${selectedPositions.size} selected"
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun flagEmoji(countryCode: String): String {
        val cc = countryCode.trim().uppercase(Locale.US)

        // Handle default case - show book emoji instead of flag
        if (cc == "XX" || cc.length != 2 || cc.any { it !in 'A'..'Z' }) {
            return "📚"  // Use book emoji as default instead of flag
        }

        val first = cc[0].code - 'A'.code + 0x1F1E6
        val second = cc[1].code - 'A'.code + 0x1F1E6
        return String(Character.toChars(first)) + String(Character.toChars(second))
    }

    override fun onResume() {
        super.onResume()
        loadDecks() // Refresh when returning from category list
    }
}