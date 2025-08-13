package com.example.flipwise

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnMyDecks).setOnClickListener {
            startActivity(Intent(this, MyDecksActivity::class.java))
        }

        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            // we'll wire this later (choose deck/categories)
        }
    }
}
