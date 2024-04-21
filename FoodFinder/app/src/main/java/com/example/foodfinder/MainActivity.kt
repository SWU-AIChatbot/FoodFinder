package com.example.foodfinder

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView

class MainActivity : AppCompatActivity() {

    private lateinit var menu_Iv: ImageView
    private lateinit var food_Iv: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 음식 이미지 뷰 초기화
        val foodIv = findViewById<ImageView>(R.id.food_iv)

        // 음식 이미지 클릭 시 FoodActivity로 전환
        foodIv.setOnClickListener {
            // FoodActivity로 이동하는 Intent 생성
            val intent = Intent(this, FoodActivity::class.java)
            // Intent로 새 액티비티 시작
            startActivity(intent)
        }

        val menuIv = findViewById<ImageView>(R.id.menu_iv)

        menuIv.setOnClickListener {   // MenuActiviy로 전환
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
        }

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