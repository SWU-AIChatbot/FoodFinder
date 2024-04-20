package com.example.foodfinder

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val foodIv = findViewById<ImageView>(R.id.food_iv)

        foodIv.setOnClickListener {   // FoodActiviy로 전환
            val intent = Intent(this, FoodActivity::class.java)
            startActivity(intent)
        }
    }
}