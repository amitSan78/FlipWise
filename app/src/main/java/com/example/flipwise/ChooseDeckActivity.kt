package com.example.flipwise

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.flipwise.data.DatabaseProvider
import kotlinx.coroutines.launch

class ChooseDeckActivity : AppCompatActivity() {

    private val db by lazy { DatabaseProvider.get(this) }
    private lateinit var list: ListView
    private val deckIds = mutableListOf<Int>()
    private val deckNames = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_deck)
        title = "Choose Deck"

        list = findViewById(R.id.deckListChoose)

        lifecycleScope.launch {
            val decks = db.deckDao().getAll()  // make sure you have this DAO method
            if (decks.isEmpty()) {
                Toast.makeText(this@ChooseDeckActivity, "No decks yet. Add one first.", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            deckIds.clear(); deckNames.clear()
            decks.forEach { d ->
                deckIds.add(d.id)
                deckNames.add(d.name) // or "${flagEmoji(d.code)} ${d.name}" if you store a code
            }
            list.adapter = ArrayAdapter(
                this@ChooseDeckActivity,
                android.R.layout.simple_list_item_1,
                deckNames
            )
        }

        list.setOnItemClickListener { _, _, pos, _ ->
            val i = Intent(this, ChooseCategoriesActivity::class.java)
            i.putExtra("deckId", deckIds[pos])
            i.putExtra("deckName", deckNames[pos])
            startActivity(i)
        }
    }
}
