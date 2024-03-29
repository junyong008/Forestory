package com.yjy.forestory.util

import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.databinding.BindingAdapter
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.yjy.forestory.R
import com.yjy.forestory.feature.searchPost.SearchPostAdapter
import com.yjy.forestory.feature.searchPost.SearchTagAdapter
import com.yjy.forestory.feature.viewPost.CommentAdapter
import com.yjy.forestory.feature.viewPost.PostAdapter
import com.yjy.forestory.model.Comment
import com.yjy.forestory.model.PostWithTagsAndComments
import com.yjy.forestory.model.Tag
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// BindingAdapter 원칙 규정 : 재사용이 가능한 바인딩. 일회성 바인딩은 각자 뷰에서 LiveData Observe로 처리.
object BindingAdapter {

    // RecyclerView의 PostWithTagsAndComments 형식 List를 등록. Adapter는 각자 뷰(액티비티 or 프레그먼트)에서 사전 등록. (클릭 이벤트 리스너 등록 위해서.)
    @BindingAdapter("postItems")
    @JvmStatic
    fun setPostItems(recyclerView: RecyclerView, pagingData: PagingData<PostWithTagsAndComments>?) {

        pagingData?.let {
            val postAdapter = recyclerView.adapter as PostAdapter

            // lifecycleScope를 통해 submitData()를 호출
            recyclerView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                postAdapter.submitData(it)
            }
        }
    }

    // RecyclerView의 Comment 형식 List를 등록.
    @BindingAdapter("commentItems")
    @JvmStatic
    fun setCommentItems(recyclerView: RecyclerView, commentList: List<Comment>?){

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

    @BindingAdapter("searchTagItems")
    @JvmStatic
    fun setSearchTagItems(recyclerView: RecyclerView, tagList: List<Tag>?){

        tagList?.let {
            val searchTagAdapter = recyclerView.adapter as SearchTagAdapter
            searchTagAdapter.submitList(it)
        }
    }

    @BindingAdapter("searchPostItems")
    @JvmStatic
    fun setSearchPostItems(recyclerView: RecyclerView, pagingData: PagingData<PostWithTagsAndComments>?){

        pagingData?.let {
            val searchPostAdapter = recyclerView.adapter as SearchPostAdapter

            // lifecycleScope를 통해 submitData()를 호출
            recyclerView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                searchPostAdapter.submitData(it)
            }
        }
    }

    // LinearPostList와 PostActivity에서 겹쳐서 바인딩 어댑터로 묶음. 댓글 추가버튼의 상태를 게시글과 댓글 상태에 따라서 변경
    @BindingAdapter("commentAddButtonState")
    @JvmStatic
    fun setCommentAddButtonState(button: AppCompatButton, postWithTagsAndComments: PostWithTagsAndComments?){

        postWithTagsAndComments?.let {
            // 댓글이 있다면 댓글 추가 버튼 숨기기
            if (it.comments.isNotEmpty()) {
                button.visibility = View.GONE
            } else {
                button.visibility = View.VISIBLE
            }

            // 댓글이 없고 만약 추가중이라면 버튼 비활성화
            if (it.comments.isEmpty() && it.post.isAddingComments) {
                button.text = ""
                button.isEnabled = false
            } else {
                button.text = button.context.getString(R.string.notify_forest_friends)
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

    // imageView의 이미지를 Uri로 바인딩 : 그리드뷰에서는 해상도를 조절해 속도 향상
    @BindingAdapter("gridImageUri")
    @JvmStatic
    fun setGridImageUri(imageView: ImageView, imageUri: Uri?) {
        imageUri?.let {
            Glide.with(imageView.context)
                .load(it)
                .placeholder(R.drawable.bg_lightgray_round)
                .override(500, 500)
                .centerCrop()
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
}