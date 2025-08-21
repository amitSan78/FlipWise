package com.example.flipwise
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.TextView


class SelectableCategoryAdapter(
    private val context: Context,
    private val items: List<String>,
    private val selectedPositions: Set<Int> = emptySet(),
    private val showCheckboxes: Boolean = false
) : BaseAdapter() {

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): String = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_deck_selectable, parent, false)

        val checkbox = view.findViewById<CheckBox>(R.id.checkbox)
        val textView = view.findViewById<TextView>(R.id.textView)

        textView.text = items[position]
        checkbox.visibility = if (showCheckboxes) View.VISIBLE else View.GONE
        checkbox.isChecked = selectedPositions.contains(position)

        return view
    }
}