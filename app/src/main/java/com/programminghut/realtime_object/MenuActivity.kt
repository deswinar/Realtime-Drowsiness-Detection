package com.programminghut.realtime_object

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.programminghut.realtime_object.adapters.ImageAdapter

class MenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        getSupportActionBar()?.hide()

        val recyclerView: RecyclerView = findViewById(R.id.recycler)
        val arrayList = ArrayList<Int>()

        // Add multiple images to arraylist.
        arrayList.add(R.drawable.carousel1)
        arrayList.add(R.drawable.carousel2)

        val adapter = ImageAdapter(this, arrayList)
        recyclerView.adapter = adapter

        adapter.setOnItemClickListener(object : ImageAdapter.OnItemClickListener {
            override fun onClick(imageView: ImageView, path: Int) {
                // Do something like opening the image in a new activity or showing it in full screen or something else.
            }
        })

        val btnScan: Button = findViewById(R.id.btn_scan)
        btnScan.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}