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
import com.example.flipwise.data.DatabaseProvider
import com.example.flipwise.data.Deck
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.util.Locale

class MyDecksActivity : AppCompatActivity() {

    private lateinit var adapter: ArrayAdapter<String>
    private val display = mutableListOf<String>()  // "🇯🇵 Japanese"
    private val ids = mutableListOf<Int>()         // deck ids (same index as display)

    private val db by lazy { DatabaseProvider.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_decks)

        val listView = findViewById<ListView>(R.id.deckList)
        val fab = findViewById<FloatingActionButton>(R.id.fabAddDeck)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, display)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val deckId = ids[position]
            val deckName = display[position]
            val i = Intent(this, CategoryListActivity::class.java)
            i.putExtra("deckId", deckId)
            i.putExtra("deckName", deckName)
            startActivity(i)
        }

        fab.setOnClickListener { showAddDeckDialog() }

        loadDecks()
    }

    private fun loadDecks() = lifecycleScope.launch {
        val decks = db.deckDao().getAllDecks()
        display.clear(); ids.clear()
        decks.forEach { d ->
            display.add("${flagEmoji(d.code)} ${d.name}")
            ids.add(d.id)
        }
        adapter.notifyDataSetChanged()
    }

    private fun showAddDeckDialog() {
        val nameInput = EditText(this).apply { hint = "Language (e.g. Japanese)" }
        val codeInput = EditText(this).apply { hint = "JP for Japanese" }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
            addView(nameInput); addView(codeInput)
        }
        AlertDialog.Builder(this)
            .setTitle("Add New Deck")
            .setView(container)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString().trim()
                val code = codeInput.text.toString().trim().uppercase(Locale.US)
                if (name.isNotEmpty() && code.length == 2) {
                    lifecycleScope.launch {
                        db.deckDao().insert(Deck(name = name, code = code))
                        loadDecks()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun flagEmoji(countryCode: String): String {
        val cc = countryCode.trim().uppercase(Locale.US)
        if (cc.length != 2 || cc.any { it !in 'A'..'Z' }) return cc
        val first = cc[0].code - 'A'.code + 0x1F1E6
        val second = cc[1].code - 'A'.code + 0x1F1E6
        return String(Character.toChars(first)) + String(Character.toChars(second))
    }
}
