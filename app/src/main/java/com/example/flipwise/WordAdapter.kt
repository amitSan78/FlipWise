package com.example.flipwise

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.TextView

// If you want to modify your existing WordAdapter instead of replacing it:

class WordAdapter(
    private val context: Context,
    private val words: List<UiWord>,
    private val selectedPositions: Set<Int> = emptySet(),  // Add this parameter
    private val showCheckboxes: Boolean = false           // Add this parameter
) : BaseAdapter() {

    override fun getCount(): Int = words.size
    override fun getItem(position: Int): UiWord = words[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_word, parent, false)  // Use new layout

        // Add these lines to handle checkbox
        val checkbox = view.findViewById<CheckBox>(R.id.checkbox)
        checkbox.visibility = if (showCheckboxes) View.VISIBLE else View.GONE
        checkbox.isChecked = selectedPositions.contains(position)

        // Your existing code for setting up the word data
        val hiraganaText = view.findViewById<TextView>(R.id.tvHiragana)
        val romajiText = view.findViewById<TextView>(R.id.tvRomaji)
        val englishText = view.findViewById<TextView>(R.id.tvEnglish)

        val word = words[position]
        hiraganaText.text = word.hiragana
        romajiText.text = word.romaji
        englishText.text = word.english

        return view
    }
}
