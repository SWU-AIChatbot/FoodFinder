package com.example.foodfinder


import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

const val REST_API_KEY = "5427ec0138708f549ccea79fef0981de"

interface SearchNetWorkInterface {
    @Headers("Authorization: KakaoAK ${REST_API_KEY}")
    @GET("/v2/search/image")
    suspend fun getKakaoImageResponse(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ) : Response<KakaoImageResponse>
}
data class KakaoImageResponse(
    val documents: List<KakaoImageDocument>?,
    val meta: KakaoImageMeta
)


data class KakaoImageMeta(
    val total_count:Int,
    val pageable_count:Int,
    val is_end:Boolean
)

data class KakaoImageDocument(
    val collection: String,
    val thumbnail_url: String,
    val image_url: String,
    val width: Int,
    val height: Int,
    val display_sitename: String,
    val doc_url: String,
    val datetime: String
)
