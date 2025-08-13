package com.example.flipwise

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class WordAdapter(
    context: Context,
    private val items: MutableList<UiWord>
) : ArrayAdapter<UiWord>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_word, parent, false)

        val w = items[position]
        v.findViewById<TextView>(R.id.tvHiragana).text = w.hiragana
        v.findViewById<TextView>(R.id.tvRomaji).text = w.romaji
        v.findViewById<TextView>(R.id.tvEnglish).text = w.english
        return v
    }
}
