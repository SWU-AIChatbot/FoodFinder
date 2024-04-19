package com.example.foodfinder

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class FoodInfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_info)

        val imageView: ImageView = findViewById(R.id.menu_img_iv)
        val resultTextView: TextView = findViewById(R.id.menuname_kr_tv)

        // MainActivity로부터 이미지와 결과 텍스트를 가져옴
        val image: Bitmap? = intent.getParcelableExtra("image")
        val resultText: String? = intent.getStringExtra("resultText")

        // 이미지와 결과를 화면에 표시
        imageView.setImageBitmap(image)
        resultTextView.text = resultText
    }
}