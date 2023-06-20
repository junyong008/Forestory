package com.yjy.forestory.util

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.databinding.BindingAdapter
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.yjy.forestory.R
import com.yjy.forestory.feature.post.CommentAdapter
import com.yjy.forestory.feature.post.PostAdapter
import com.yjy.forestory.model.db.dto.CommentDTO
import com.yjy.forestory.model.db.dto.PostWithComments
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// BindingAdapter 원칙 규정 : 재사용이 가능한 바인딩. 일회성 바인딩은 각자 뷰에서 LiveData Observe로 처리.
object BindingAdapter {

    // RecyclerView의 PostWithComments 형식 List를 등록. Adapter는 각자 뷰(액티비티 or 프레그먼트)에서 사전 등록. (클릭 이벤트 리스너 등록 위해서.)
    @BindingAdapter("postItems")
    @JvmStatic
    fun setPostItems(recyclerView: RecyclerView, pagingData: PagingData<PostWithComments>?) {
        pagingData?.let {
            val postAdapter = recyclerView.adapter as PostAdapter // 이 부분은 PostAdapter를 PagingDataAdapter로 변경해야 합니다.

            // lifecycleScope를 통해 submitData()를 호출
            recyclerView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                postAdapter.submitData(it)
            }
        }
    }

    // RecyclerView의 CommentDTO 형식 List를 등록.
    @BindingAdapter("commentItems")
    @JvmStatic
    fun setCommentItems(recyclerView: RecyclerView, commentList: List<CommentDTO>?){

        commentList?.let {

            // 연결돼 있는 어댑터가 없다면 연결. 클릭 이벤트 리스너 등록이 필요 없으므로 바인딩어댑터에서 등록하는 것임.
            if (recyclerView.adapter == null) {
                val commentAdapter = CommentAdapter()
                recyclerView.adapter = commentAdapter
            }

            val commentAdapter = recyclerView.adapter as CommentAdapter
            commentAdapter.submitList(it)
        }
    }

    // LinearPostList와 PostActivity에서 겹쳐서 바인딩 어댑터로 묶음. 댓글 추가버튼의 상태를 게시글과 댓글 상태에 따라서 변경
    @BindingAdapter("commentAddButtonState")
    @JvmStatic
    fun setCommentAddButtonState(button: AppCompatButton, postWithComments: PostWithComments){

        val commentAddButtonText = "숲속 친구들에게 알리기"
        postWithComments?.let {
            // 댓글이 있다면 댓글 추가 버튼 숨기기
            if (postWithComments.comments.isNotEmpty()) {
                button.visibility = View.GONE
            } else {
                button.visibility = View.VISIBLE
            }

            // 댓글이 없고 만약 추가중이라면 버튼 비활성화
            if (postWithComments.comments.isEmpty() && postWithComments.post.isAddingComments) {
                button.setText("")
                button.isEnabled = false
            } else {
                button.setText(commentAddButtonText)
                button.isEnabled = true
            }
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
    fun setReadOnlyChips(chipGroup: ChipGroup, chipTexts: List<String>?) {

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