package com.example.foodfinder

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Response // Response import 추가

class NewActivity : AppCompatActivity() {
    private lateinit var newRecyclerView: RecyclerView
    private lateinit var adapter: ImageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_activity_layout)
        Log.d("keyword", "newActivity")

        newRecyclerView = findViewById(R.id.newRecyclerView)

        val keyword = intent.getStringExtra("keyword") ?: ""
        Log.d("keyword", "$keyword")


        val retrofit = Retrofit.Builder()
            .baseUrl("https://dapi.kakao.com") // 카카오 API의 기본 URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(SearchNetWorkInterface::class.java)

        lifecycleScope.launch {
            try {
                // API 호출
                val response = service.getKakaoImageResponse(query = keyword)
                Log.d("keyword", "$response")

                // 응답 처리

                if (response.isSuccessful) { // null 체크로 요청 성공 여부 확인
                    // UI 업데이트는 Main 스레드에서 해야 함
                    val kakaoImageResponse = response.body() // response의 body를 가져옴
                    Log.d("keyword", "$kakaoImageResponse")

                    if (kakaoImageResponse != null) {
                        // UI 업데이트는 Main 스레드에서 해야 함
                        withContext(Dispatchers.Main) {
                            // RecyclerView에 데이터 설정
                            adapter = ImageAdapter(kakaoImageResponse.documents ?: emptyList())
                            newRecyclerView.adapter = adapter
                            Log.d("keyword", "$newRecyclerView.adapter")
                        }
                    }
                } else {
                    // API 호출에 실패한 경우
                    Log.e("NewActivity", "API call failed: Response is null")
                }
            } catch (e: Exception) {
                // 예외 처리
                Log.e("NewActivity", "Exception: ${e.message}", e)
            }
        }


    }
}
