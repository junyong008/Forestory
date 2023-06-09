package com.yjy.forestory.util

import android.net.Uri
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.yjy.forestory.R
import com.yjy.forestory.feature.post.PostAdapter
import com.yjy.forestory.model.db.dto.PostWithComments
import java.text.SimpleDateFormat
import java.util.*

// BindingAdapter 원칙 규정 : 재사용이 가능한 바인딩. 일회성 바인딩은 각자 뷰에서 LiveData Observe로 처리.
object BindingAdapter {

    // RecyclerView의 PostDTO 형식 List를 등록. Adapter는 각자 뷰(액티비티 or 프레그먼트)에서 사전 등록. (클릭 이벤트 리스너 등록 위해서.)
    @BindingAdapter("postItems")
    @JvmStatic
    fun setItems(recyclerView: RecyclerView, postList : List<PostWithComments>?){

        postList?.let {
            val postAdapter = recyclerView.adapter as PostAdapter

            // 자동 갱신 : 리스트의 주소가 변경된다는걸 가정하고 비교하며 갱신하므로, 테스트로 임의의 내부 리스트를 전달하면 갱신이 안됨. 고로 테스트용으로 .toMutableList() 작성.
            postAdapter.submitList(it.toMutableList())
        }
    }

    // imageView의 이미지를 Uri로 바인딩
    @BindingAdapter("imageUri")
    @JvmStatic
    fun setImageUri(imageView: ImageView, imageUri: Uri?) {
        imageUri?.let {
            Glide.with(imageView.context)
                .load(it)
                .into(imageView)
        }
    }

    // textView의 텍스트를 Date로 바인딩
    @BindingAdapter("formattedDateTime")
    @JvmStatic
    fun setFormattedDateTime(textView: TextView, date: Date?) {
        date?.let {
            val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(it)
            textView.text = formattedDate
        }
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

    // chipGroup의 chip을 바인딩 : 읽기 전용 Chip 바인딩
    @BindingAdapter("readOnlyChips")
    @JvmStatic
    fun setReadOnlyChips(chipGroup: ChipGroup, chipTexts: MutableList<String>?) {

        chipGroup.removeAllViews()

        chipTexts?.let {
            for (chipText in chipTexts) {
                val newChip = LayoutInflater.from(chipGroup.context).inflate(R.layout.item_readonly_chip, chipGroup, false) as Chip
                newChip.id = ViewCompat.generateViewId()
                newChip.text = chipText

                chipGroup.addView(newChip)
            }
        }
    }
}