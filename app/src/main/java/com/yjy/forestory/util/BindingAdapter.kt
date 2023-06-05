package com.yjy.forestory.util

import android.view.LayoutInflater
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.yjy.forestory.R
import com.yjy.forestory.feature.home.PostAdapter
import com.yjy.forestory.model.db.dto.PostDTO

object BindingAdapter {

    // postItems 어댑터 아이템 연결, 갱신
    @BindingAdapter("postItems")
    @JvmStatic
    fun setItems(recyclerView: RecyclerView, items : ArrayList<PostDTO>?){

        items?.let {
            val postAdapter = recyclerView.adapter as PostAdapter

            // 자동 갱신 : 리스트의 주소가 변경된다는걸 가정하고 비교하며 갱신하므로, 테스트로 임의의 내부 리스트를 전달하면 갱신이 안됨. 고로 테스트용으로 .toMutableList() 작성.
            postAdapter.submitList(items.toMutableList())
        }
    }

    // postImage 이미지 바인딩
    @BindingAdapter("postImage")
    @JvmStatic
    fun setImage(imageView: ImageView, imageUrl: Any){
        Glide.with(imageView.context)
            .load(imageUrl)
            .override(200,200)
            .circleCrop().into(imageView)
    }

    // chipGroup의 chip을 바인딩 : String List를 받아서 Chip의 내용을 모두 지우고 재정립
    @BindingAdapter("chips")
    @JvmStatic
    fun setChips(chipGroup: ChipGroup, chipTexts: MutableList<String>?) {

        chipGroup.removeAllViews()

        chipTexts?.let {
            for (chipText in chipTexts) {
                val newChip = LayoutInflater.from(chipGroup.context).inflate(R.layout.item_chip, chipGroup, false) as Chip
                newChip.id = ViewCompat.generateViewId()
                newChip.text = chipText
                newChip.setOnCloseIconClickListener {
                    chipGroup.removeView(newChip)
                    chipTexts.remove(chipText)
                }

                chipGroup.addView(newChip)
            }
        }
    }
}