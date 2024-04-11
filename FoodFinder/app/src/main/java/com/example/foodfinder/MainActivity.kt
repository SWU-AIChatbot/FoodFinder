package com.example.foodfinder

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var menu_Iv: ImageView
    private lateinit var food_Iv: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // menu_Iv를 findViewById로 초기화
        menu_Iv = findViewById(R.id.menu_iv)
        food_Iv = findViewById(R.id.food_iv)

        menu_Iv.setOnClickListener {
            val intent = Intent(this@MainActivity, MenuInfoActivity::class.java)
            startActivity(intent)
        }

        food_Iv.setOnClickListener {
            val intent = Intent(this@MainActivity, FoodInfoActivity::class.java)
            startActivity(intent)
        }

    }
}