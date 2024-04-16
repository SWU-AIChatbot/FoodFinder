package com.example.foodfinder

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val menuIv = findViewById<ImageView>(R.id.menu_iv)

        menuIv.setOnClickListener {   // MenuActiviy로 전환
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
        }
    }
}