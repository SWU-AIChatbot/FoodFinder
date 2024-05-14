package com.example.foodfinder

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Response // Response import 추가

class NewActivity : BottomSheetDialogFragment() {

    private lateinit var newRecyclerView: RecyclerView
    private lateinit var adapter: ImageAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.new_activity_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        newRecyclerView = view.findViewById(R.id.newRecyclerView)

        val keyword = arguments?.getString("keyword") ?: ""
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
                if (response.isSuccessful) {
                    val kakaoImageResponse = response.body()
                    Log.d("keyword", "$kakaoImageResponse")

                    if (kakaoImageResponse != null) {
                        // UI 업데이트는 Main 스레드에서 해야 함
                        withContext(Dispatchers.Main) {
                            // RecyclerView에 데이터 설정
                            val layoutManager = GridLayoutManager(requireContext(), 2) // 가로로 2개의 아이템을 배치하는 레이아웃 매니저
                            newRecyclerView.layoutManager = layoutManager
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

