package com.example.foodfinder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ImageAdapter(private val dataset: List<KakaoImageDocument>?) :
    RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val document = dataset?.getOrNull(position)

        // document가 null이 아닌 경우에만 Glide를 사용하여 이미지 로드 및 표시
        document?.let {
            Glide.with(holder.itemView)
                .load(it.thumbnail_url) // 썸네일 URL 사용
                .centerCrop() // 이미지를 ImageView에 맞게 잘라서 표시
                .error(R.drawable.loop) // 이미지 로딩에 실패한 경우 대체 이미지 표시
                .into(holder.imageView)
        }
    }

    override fun getItemCount(): Int {
        return dataset?.size ?: 0
    }
}
