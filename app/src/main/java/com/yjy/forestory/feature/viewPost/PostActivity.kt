package com.yjy.forestory.feature.viewPost

import EventObserver
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yjy.forestory.R
import com.yjy.forestory.databinding.ActivityPostBinding
import com.yjy.forestory.feature.searchPost.SearchActivity
import com.yjy.forestory.model.PostWithTagsAndComments
import com.yjy.forestory.util.BindingAdapter.setCommentAddButtonState
import com.yjy.forestory.util.BindingAdapter.setCommentItems
import com.yjy.forestory.util.BindingAdapter.setFormattedDateTime
import com.yjy.forestory.util.BindingAdapter.setImageUri
import com.yjy.forestory.util.ImageUtils
import dagger.hilt.android.AndroidEntryPoint
import io.github.muddz.styleabletoast.StyleableToast
import javax.inject.Inject

@AndroidEntryPoint
class PostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostBinding
    @Inject lateinit var postViewModel: PostViewModel

    private var postId: Int = -1
    private var isRecursion: Boolean = false
    private var mToast: StyleableToast? = null

    // 시스템의 뒤로가기 버튼 눌렀을 때
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // 이미지뷰가 시야에 보이면 축소 애니메이션 적용, 아니라면 fade out 시킨다
            val scrollBounds = Rect()
            binding.recyclerViewComments.getHitRect(scrollBounds)
            if (binding.imageViewPost.getGlobalVisibleRect(scrollBounds) && postId != -1) {
                finishAfterTransition()
            } else {
                window.sharedElementsUseOverlay = false
                window.sharedElementExitTransition = null
                window.sharedElementEnterTransition = null
                finish()
                overridePendingTransition(R.anim.stay, R.anim.fade_out)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_post)

        binding.postViewModel = postViewModel
        binding.lifecycleOwner = this@PostActivity

        // 뒤로가기 버튼 콜백 등록
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        // 이미지가 그려진 후 애니메이션을 적용
        postponeEnterTransition()
        binding.imageViewPost.doOnPreDraw {
            startPostponedEnterTransition()
        }

        // 어떤 게시글을 보여줄지 Id를 받아온다. 정상적으로 못받아왔다면 뒤로가기
        postId = intent.getIntExtra("postId", -1)
        if (postId == -1) {
            onBackPressedCallback.handleOnBackPressed()
        }

        // 재귀 인지 확인. 검색 -> 태그 클릭 -> 검색 -> 태그 클릭 ... 재귀를 방지하기 위함
        isRecursion = intent.getBooleanExtra("recursion", false)

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
        postViewModel.getPostWithTagsAndComments(postId).observe(this) { postWithTagsAndComments ->

            // 게시글 정보를 불러오지 못했으면 뒤로가기
            if (postWithTagsAndComments == null) {
                postId = -1
                onBackPressedCallback.handleOnBackPressed()
                return@observe
            }

            // 바인딩
            setImageUri(binding.circleImageViewUserPicture, postWithTagsAndComments.post.userPicture)
            binding.textViewUserName.text = postWithTagsAndComments.post.userName
            setFormattedDateTime(binding.textViewCreateDate, postWithTagsAndComments.post.createDate)
            setImageUri(binding.imageViewPost, postWithTagsAndComments.post.image)
            binding.textViewContent.text = postWithTagsAndComments.post.content
            setCommentItems(binding.recyclerViewComments, postWithTagsAndComments.comments)
            setCommentAddButtonState(binding.buttonAddComment, postWithTagsAndComments)
            binding.progressBar.isVisible = postWithTagsAndComments.comments.isEmpty() && postWithTagsAndComments.post.isAddingComments

            val chipGroup = binding.chipgroupTags
            val chipTexts = postWithTagsAndComments.tags
            
            chipGroup.removeAllViews()
            for (chipText in chipTexts) {

                val newChip = LayoutInflater.from(chipGroup.context).inflate(R.layout.item_readonly_chip, chipGroup, false) as Chip
                newChip.id = ViewCompat.generateViewId()
                newChip.text = chipText.content
                if (!isRecursion) {
                    newChip.setOnClickListener {

                        // Chip 태그 클릭시 해당 태그 검색.
                        val intent = Intent(this, SearchActivity::class.java)
                        intent.putExtra("tag", chipText.content)
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.stay)
                    }
                }
                chipGroup.addView(newChip)
            }


            // 이미지 클릭 리스너
            binding.imageViewPost.setOnClickListener {
                val intent = Intent(this, ImageZoomActivity::class.java)
                intent.putExtra("imageUri", postWithTagsAndComments.post.image.toString())
                startActivity(intent)
            }

            // 게시글 메뉴 버튼 클릭 리스너
            binding.ibuttonMenu.setOnClickListener {
                showMenuDialog(it, postWithTagsAndComments)
            }

            // 댓글 추가 버튼 클릭 리스너
            binding.buttonAddComment.setOnClickListener {
                val post = postWithTagsAndComments.post
                val parentPostId = post.postId
                val postContent = post.content
                ImageUtils.uriToMultipart(this, post.image)?.let { postImage ->
                    postViewModel.getComments(parentPostId, postContent, postImage)
                }
            }
        }

    }

    private fun setEventObserver() {

        // 댓글 추가 결과 처리
        postViewModel.isCompleteGetComments.observe(this, EventObserver { responseCode ->
            if (responseCode != 200) {
                mToast?.cancel()
                mToast = StyleableToast.makeText(this, getString(R.string.add_comment_failure, responseCode), R.style.errorToast).also { it.show() }
            }
        })

        // 게시글 삭제 결과 처리
        postViewModel.isCompleteDeletePost.observe(this, EventObserver { result ->
            mToast?.cancel()
            val toastMessage = when (result) {
                is PostViewModel.DeletePostResult.CannotDelete -> getString(R.string.notify_forest_friends)
                is PostViewModel.DeletePostResult.Success -> getString(R.string.delete_success)
                else -> getString(R.string.delete_failure)
            }
            mToast = StyleableToast.makeText(this, toastMessage, R.style.errorToast).also { it.show() }
        })
    }

    private fun showMenuDialog(view: View, postWithTagsAndComments: PostWithTagsAndComments) {
        val menuItems = arrayOf(getString(R.string.delete)) // 메뉴 항목 배열

        MaterialAlertDialogBuilder(view.context)
            .setItems(menuItems) { dialog, which ->
                when (which) {
                    0 -> {
                        // "삭제하기" 메뉴 항목 클릭 처리
                        showDeleteConfirmationDialog(view, postWithTagsAndComments)
                    }
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun showDeleteConfirmationDialog(view: View, postWithTagsAndComments: PostWithTagsAndComments) {
        MaterialAlertDialogBuilder(view.context)
            .setMessage(getString(R.string.confirm_delete_message))
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                postViewModel.deletePostWithTagsAndComments(postWithTagsAndComments)
                dialog.dismiss()
            }
            .show()
    }
}