package com.example.foodfinder

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Response
class MainActivity : AppCompatActivity() {

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


    }
}