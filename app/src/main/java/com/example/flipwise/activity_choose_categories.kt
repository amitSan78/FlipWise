package com.example.flipwise

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.flipwise.data.DatabaseProvider
import kotlinx.coroutines.launch

class ChooseCategoriesActivity : AppCompatActivity() {

    private val ids = mutableListOf<Int>()
    private val names = mutableListOf<String>()

    private lateinit var list: ListView
    private var cbAll: CheckBox? = null

    private val db by lazy { DatabaseProvider.get(this) }
    private var deckId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_categories)

        deckId = intent.getIntExtra("deckId", -1)
        title = "Choose Categories"

        list = findViewById(R.id.categoryListMulti)
        list.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        // Optional "Select All" checkbox (add it in XML with id cbSelectAll)
        cbAll = findViewById(R.id.cbSelectAll)

        // Load categories
        lifecycleScope.launch {
            val cats = db.categoryDao().getForDeck(deckId)
            ids.clear(); names.clear()
            cats.forEach { ids.add(it.id); names.add(it.name) }

            list.adapter = ArrayAdapter(
                this@ChooseCategoriesActivity,
                android.R.layout.simple_list_item_multiple_choice,
                names
            )

            // If "Select All" is already checked, mark all
            if (cbAll?.isChecked == true) {
                for (i in 0 until list.count) list.setItemChecked(i, true)
            }
        }

        // Toggle all on checkbox changes
        cbAll?.setOnCheckedChangeListener { _, checked ->
            for (i in 0 until list.count) {
                list.setItemChecked(i, checked)
            }
        }

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            // If "Select All" is checked OR none selected → use all
            val selectedIdx = ids.indices.filter { list.isItemChecked(it) }
            val categoryIds =
                if (cbAll?.isChecked == true || selectedIdx.isEmpty()) ids
                else selectedIdx.map { ids[it] }

            val i = Intent(this, StudyActivity::class.java)
            i.putExtra("deckName", intent.getStringExtra("deckName"))
            i.putIntegerArrayListExtra("categoryIds", ArrayList(categoryIds))
            startActivity(i)
        }
    }
}
