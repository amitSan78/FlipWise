package com.example.flipwise

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.lifecycleScope
import com.example.flipwise.data.Category
import com.example.flipwise.data.DatabaseProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class CategoryListActivity : AppCompatActivity() {

    private lateinit var adapter: SelectableCategoryAdapter
    private val display = mutableListOf<String>()
    private val ids = mutableListOf<Int>()
    private val db by lazy { DatabaseProvider.get(this) }

    private var deckId: Int = -1

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
        setContentView(R.layout.activity_category_list)

        deckId = intent.getIntExtra("deckId", -1)
        title = intent.getStringExtra("deckName") ?: "Categories"

        listView = findViewById<ListView>(R.id.categoryList)
        val fab = findViewById<FloatingActionButton>(R.id.fabAddCategory)

        adapter = SelectableCategoryAdapter(this, display)
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
                // Navigate to words in this category
                val categoryId = ids[position]
                val categoryName = display[position]
                val i = Intent(this, WordListActivity::class.java)
                i.putExtra("categoryId", categoryId)
                i.putExtra("categoryName", categoryName)
                startActivity(i)
            }
        }

        fab.setOnClickListener { showAddCategoryDialog() }
        loadCategories()
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
        adapter = SelectableCategoryAdapter(
            this,
            display,
            selectedPositions,
            actionMode != null
        )
        listView.adapter = adapter
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            menuInflater.inflate(R.menu.category_action_menu, menu)
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
                        showEditCategoryDialog(position)
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

    private fun loadCategories() = lifecycleScope.launch {
        val cats = db.categoryDao().getForDeck(deckId)
        display.clear()
        ids.clear()
        cats.forEach { c ->
            display.add(c.name)
            ids.add(c.id)
        }
        updateAdapter()
    }

    private fun showAddCategoryDialog() {
        showCategoryDialog(null, -1)
    }

    private fun showEditCategoryDialog(position: Int) {
        showCategoryDialog(display[position], ids[position])
    }

    private fun showCategoryDialog(existingName: String?, categoryId: Int) {
        val nameInput = EditText(this).apply {
            hint = "Category name (e.g. Food)"
            setText(existingName ?: "")
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
            addView(nameInput)
        }

        AlertDialog.Builder(this)
            .setTitle(if (existingName == null) "Add Category" else "Edit Category")
            .setView(container)
            .setPositiveButton(if (existingName == null) "Add" else "Save") { _, _ ->
                val name = capitalizeWords(nameInput.text.toString().trim())
                if (name.isNotEmpty()) {
                    lifecycleScope.launch {
                        if (existingName == null) {
                            // Add new category
                            db.categoryDao().insert(Category(deckId = deckId, name = name))
                        } else {
                            // Update existing category
                            db.categoryDao().update(Category(id = categoryId, deckId = deckId, name = name))
                        }
                        loadCategories()
                        actionMode?.finish()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Categories")
            .setMessage("Are you sure you want to delete ${selectedPositions.size} categor${if (selectedPositions.size == 1) "y" else "ies"}?\n\nThis will also delete all words in these categories.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    val idsToDelete = selectedPositions.map { ids[it] }
                    idsToDelete.forEach { id ->
                        db.categoryDao().deleteById(id)
                    }
                    loadCategories()
                    actionMode?.finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.category_list_menu, menu)
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

    override fun onResume() {
        super.onResume()
        loadCategories() // Refresh when returning from word list
    }
}