package com.example.flipwise

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.flipwise.data.Category
import com.example.flipwise.data.DatabaseProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class CategoryListActivity : AppCompatActivity() {

    private lateinit var adapter: ArrayAdapter<String>
    private val display = mutableListOf<String>()
    private val ids = mutableListOf<Int>()
    private val db by lazy { DatabaseProvider.get(this) }

    private var deckId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_list)

        deckId = intent.getIntExtra("deckId", -1)
        title = intent.getStringExtra("deckName") ?: "Categories"

        val listView = findViewById<ListView>(R.id.categoryList)
        val fab = findViewById<FloatingActionButton>(R.id.fabAddCategory)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, display)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val categoryId = ids[position]
            val categoryName = display[position]
            val i = Intent(this, WordListActivity::class.java)
            i.putExtra("categoryId", categoryId)
            i.putExtra("categoryName", categoryName)
            startActivity(i)
        }

        fab.setOnClickListener { showAddCategoryDialog() }

        loadCategories()
    }

    private fun loadCategories() = lifecycleScope.launch {
        val cats = db.categoryDao().getForDeck(deckId)
        display.clear(); ids.clear()
        cats.forEach { c ->
            display.add(c.name)
            ids.add(c.id)
        }
        adapter.notifyDataSetChanged()
    }

    private fun showAddCategoryDialog() {
        val nameInput = EditText(this).apply { hint = "Category name (e.g. Food)" }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
            addView(nameInput)
        }
        AlertDialog.Builder(this)
            .setTitle("Add Category")
            .setView(container)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    lifecycleScope.launch {
                        db.categoryDao().insert(Category(deckId = deckId, name = name))
                        loadCategories()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
