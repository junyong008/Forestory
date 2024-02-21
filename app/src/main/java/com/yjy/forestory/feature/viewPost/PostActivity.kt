package com.yjy.forestory.feature.viewPost

import EventObserver
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.yjy.forestory.R
import com.yjy.forestory.base.BaseActivity
import com.yjy.forestory.databinding.ActivityPostBinding
import com.yjy.forestory.feature.purchase.PurchaseActivity
import com.yjy.forestory.feature.searchPost.SearchActivity
import com.yjy.forestory.model.PostWithTagsAndComments
import com.yjy.forestory.util.TicketDialog
import com.yjy.forestory.util.TicketDialogInterface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PostActivity: BaseActivity<ActivityPostBinding>(R.layout.activity_post), TicketDialogInterface {

    @Inject lateinit var postViewModel: PostViewModel
    private var postId: Int = -1
    private var isRecursion: Boolean = false

    override val onBackPressedCallback = object : OnBackPressedCallback(true) {
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

    override fun initView(savedInstanceState: Bundle?) {

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
    }

    override fun setListener() {

        // 뒤로가기 버튼 클릭
        binding.ibuttonClose.setOnClickListener {
            onBackPressedCallback.handleOnBackPressed()
        }
    }

    override fun setObserver() {

        // 특정 게시글 정보를 받아와서 뷰를 업데이트 한다
        postViewModel.getPostWithTagsAndComments(postId).observe(this) { postWithTagsAndComments ->

            // 게시글 정보를 불러오지 못했으면 뒤로가기
            if (postWithTagsAndComments == null) {
                postId = -1
                onBackPressedCallback.handleOnBackPressed()
                return@observe
            }

            // 바인딩
            binding.postWithTagsAndComments = postWithTagsAndComments
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
                lifecycleScope.launch {
                    val currentTicketCount = postViewModel.getCurrentTicket()
                    val currentFreeTicketCount = postViewModel.getCurrentFreeTicket()

                    if (currentTicketCount != null && currentFreeTicketCount != null) {
                        TicketDialog.newInstance(currentTicketCount, currentFreeTicketCount, postWithTagsAndComments, this@PostActivity).show(supportFragmentManager, TicketDialog.TAG)
                    }
                }
            }
        }
    }

    // 댓글 받아오기 버튼 다이얼로그 처리
    override fun onDeliverClick(isNeedToCharge: Boolean, postWithTagsAndComments: PostWithTagsAndComments) {

        // 티켓 충전이 필요하면 충전 액티비티로 이동
        if (isNeedToCharge) {
            val intent = Intent(this, PurchaseActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.stay)

            return
        }

        // CommentWorker 를 통해 백그라운드에서 댓글을 서버로 부터 받아오기
        val post = postWithTagsAndComments.post
        val userName = post.userName
        val userGender = post.userGender
        val parentPostId = post.postId
        val postContent = post.content
        val postImage = post.image.toString()

        val inputData = workDataOf(
            CommentWorker.USER_NAME_KEY to userName,
            CommentWorker.USER_GENDER_KEY to userGender,
            CommentWorker.PARENT_POST_ID_KEY to parentPostId,
            CommentWorker.POST_CONTENT_KEY to postContent,
            CommentWorker.POST_IMAGE_KEY to postImage
        )

        val commentWorkRequest = OneTimeWorkRequestBuilder<CommentWorker>().setInputData(inputData).build()
        WorkManager.getInstance(this@PostActivity).enqueue(commentWorkRequest)

        postViewModel.useTicket()

        Snackbar.make(binding.root, getString(R.string.delivering_news_message), Snackbar.LENGTH_SHORT).show()
    }

    override fun setEventObserver() {

        // 게시글 삭제 결과 처리
        postViewModel.isCompleteDeletePost.observe(this, EventObserver { result ->
            when (result) {
                is PostViewModel.DeletePostResult.CannotDelete -> {
                    showToast(getString(R.string.notify_post_being_shared), R.style.errorToast)
                }
                is PostViewModel.DeletePostResult.Success -> {
                    // 삭제 성공시 내부 저장소에 저장된 게시글 이미지를 삭제하여 내부 공간 절약
                    val deletedPostImage: Uri = postViewModel.deletedPostImage.value!!
                    baseContext.contentResolver.delete(deletedPostImage, null, null)

                    showToast(getString(R.string.delete_success), R.style.successToast)
                }
                else -> {
                    showToast(getString(R.string.delete_failure), R.style.errorToast)
                }
            }
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