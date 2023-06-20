package com.yjy.forestory.feature.post

import EventObserver
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yjy.forestory.R
import com.yjy.forestory.databinding.ActivityPostBinding
import com.yjy.forestory.model.db.dto.PostWithComments
import com.yjy.forestory.util.BindingAdapter.setCommentAddButtonState
import com.yjy.forestory.util.BindingAdapter.setCommentItems
import com.yjy.forestory.util.BindingAdapter.setFormattedDateTime
import com.yjy.forestory.util.BindingAdapter.setImageUri
import com.yjy.forestory.util.BindingAdapter.setReadOnlyChips
import dagger.hilt.android.AndroidEntryPoint
import io.github.muddz.styleabletoast.StyleableToast
import javax.inject.Inject

@AndroidEntryPoint
class PostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostBinding
    @Inject lateinit var postViewModel: PostViewModel

    private var postId: Int = -1
    private var mToast: StyleableToast? = null

    // 시스템의 뒤로가기 버튼 눌렀을 때
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
            overridePendingTransition(R.anim.stay, R.anim.slide_out_right)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_post)

        binding.postViewModel = postViewModel
        binding.lifecycleOwner = this@PostActivity

        // 뒤로가기 버튼 콜백 등록
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        // 어떤 게시글을 보여줄지 Id를 받아온다. 정상적으로 못받아왔다면 뒤로가기
        postId = intent.getIntExtra("postId", -1)
        if (postId == -1) {
            onBackPressedCallback.handleOnBackPressed()
        }

        setOnClickListener()
        setObserver()
        setEventObserver()
    }

    private fun setOnClickListener() {
        // 뒤로가기 버튼 클릭
        binding.ibuttonClose.setOnClickListener {
            onBackPressedCallback.handleOnBackPressed()
        }
    }

    private fun setObserver() {

        // 특정 게시글 정보를 받아와서 뷰를 업데이트 한다
        postViewModel.getPost(postId).observe(this) { postWithComments ->

            // 게시글 정보를 불러오지 못했으면 뒤로가기
            if (postWithComments == null) {
                onBackPressedCallback.handleOnBackPressed()
                return@observe
            }

            // 바인딩
            setImageUri(binding.circleImageViewUserPicture, postWithComments.post.userPicture)
            binding.textViewUserName.text = postWithComments.post.userName
            setFormattedDateTime(binding.textViewCreateDate, postWithComments.post.createDate)
            setImageUri(binding.imageViewPost, postWithComments.post.image)
            setReadOnlyChips(binding.chipgroupTags, postWithComments.post.tagList)
            binding.textViewContent.text = postWithComments.post.content
            setCommentItems(binding.recyclerViewComments, postWithComments.comments)
            setCommentAddButtonState(binding.buttonAddComment, postWithComments)
            binding.progressBar.isVisible = postWithComments.comments.isEmpty() && postWithComments.post.isAddingComments

            // 이미지 클릭 리스너
            binding.imageViewPost.setOnClickListener {
                val intent = Intent(this, ImageZoomActivity::class.java)
                intent.putExtra("imageUri", postWithComments.post.image.toString())
                startActivity(intent)
            }

            // 게시글 메뉴 버튼 클릭 리스너
            binding.ibuttonMenu.setOnClickListener {
                showMenuDialog(it, postWithComments)
            }

            // 댓글 추가 버튼 클릭 리스너
            binding.buttonAddComment.setOnClickListener {
                postViewModel.getComments(postWithComments)
            }
        }

    }

    private fun setEventObserver() {

        // 토스트 메시지 띄우기
        postViewModel.showToast.observe(this, EventObserver {
            mToast?.let { it.cancel() }

            val toastMessage = postViewModel.toastMessage.value
            val toastIcon = postViewModel.toastIcon.value ?: 0
            mToast = StyleableToast.makeText(this, toastMessage, toastIcon).also { it.show() }
        })

    }

    private fun showMenuDialog(view: View, postWithComments: PostWithComments) {
        val menuItems = arrayOf("삭제하기") // 메뉴 항목 배열

        MaterialAlertDialogBuilder(view.context)
            .setItems(menuItems) { dialog, which ->
                when (which) {
                    0 -> {
                        // "삭제하기" 메뉴 항목 클릭 처리
                        showDeleteConfirmationDialog(view, postWithComments)
                    }
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun showDeleteConfirmationDialog(view: View, postWithComments: PostWithComments) {
        // 통일된 사용자 경험을 위해 [확인 / 취소] 순서로 변경
        MaterialAlertDialogBuilder(view.context)
            .setMessage("게시글을 삭제하시겠습니까?")
            .setNegativeButton("확인") { dialog, _ ->
                postViewModel.deletePost(postWithComments)
                dialog.dismiss()
            }
            .setPositiveButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}