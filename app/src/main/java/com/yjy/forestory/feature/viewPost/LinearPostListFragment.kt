package com.yjy.forestory.feature.viewPost

import EventObserver
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yjy.forestory.R
import com.yjy.forestory.databinding.FragmentLinearPostListBinding
import com.yjy.forestory.feature.searchPost.SearchActivity
import com.yjy.forestory.model.PostWithTagsAndComments
import com.yjy.forestory.util.ImageUtils
import dagger.hilt.android.AndroidEntryPoint
import io.github.muddz.styleabletoast.StyleableToast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LinearPostListFragment : Fragment() {

    // 바인딩, 뷰모델 정의
    private lateinit var binding: FragmentLinearPostListBinding
    @Inject lateinit var postViewModel: PostViewModel

    private var mToast: StyleableToast? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_linear_post_list, container, false)

        binding.lifecycleOwner = this@LinearPostListFragment
        binding.postViewModel = postViewModel

        setRecyclerViewAdapter()
        setOnClickListener()
        setObserver()
        setEventObserver()

        return binding.root
    }


    private fun setRecyclerViewAdapter() {
        /* 리사이클러뷰 Adapter 등록, postItemClickListener 리스너를 등록하여 클릭 이벤트 처리
        왜 DialogFragment는 직접 인터페이스 리스너를 상속받아 override 했는가 하면.. -> 이건 Configuration Change가 발생하더라도 다시 onCreateView에서 어댑터를 연결하면서 리스너가 등록된다.
        DialogFragment는 onCreate에서 할당하는게 아니기에, 띄워져 있는 상태에서 CC 가 발생하면 리스너 등록이 안된다.*/
        val recyclerViewAdapter = PostAdapter(postItemClickListener, true)
        binding.recyclerViewPosts.adapter = recyclerViewAdapter

        // 리사이클러뷰 Adapter의 로딩 상태를 감지하여 프로그레스 보여주기
        recyclerViewAdapter.addLoadStateListener { loadState ->
            binding.progressBar.isVisible = loadState.source.refresh is LoadState.Loading
        }

        // 리사이클러뷰 스크롤이 맨 위가 아니고 스크롤을 위로 올리고 있다면 맨 위로 가기 버튼 보여주기
        binding.recyclerViewPosts.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var isButtonVisible = false

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                val shouldShowButton = (firstVisibleItemPosition > 0 && dy < 0)
                if (shouldShowButton != isButtonVisible) {
                    isButtonVisible = shouldShowButton
                    if (isButtonVisible) {
                        binding.ibuttonGoTop.animate()
                            .alpha(1.0f)
                            .setDuration(200)
                            .setListener(null)
                            .translationY(0f)
                            .start()
                        binding.ibuttonGoTop.visibility = View.VISIBLE
                    } else {
                        binding.ibuttonGoTop.animate()
                            .alpha(0.0f)
                            .setDuration(200)
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    binding.ibuttonGoTop.visibility = View.GONE
                                }
                            })
                            .translationY(binding.ibuttonGoTop.height.toFloat())
                            .start()
                    }
                }
            }
        })
    }

    private fun setOnClickListener() {
        // 게시글 맨 위로 가기 버튼
        binding.ibuttonGoTop.setOnClickListener {
            binding.recyclerViewPosts.scrollToPosition(0)
        }
    }

    private fun setObserver() {
        // 게시글이 존재하지 않으면 안내 메시지를 띄운다
        postViewModel.postCount.observe(viewLifecycleOwner, Observer {
            binding.imageViewInfo.isVisible = (it <= 0)
            binding.textViewInfo.isVisible = (it <= 0)
        })
    }

    private fun setEventObserver() {

        // 댓글 추가 결과 처리
        postViewModel.isCompleteGetComments.observe(viewLifecycleOwner, EventObserver { responseCode ->
            if (responseCode != 200) {
                mToast?.cancel()
                mToast = StyleableToast.makeText(requireContext(), getString(R.string.add_comment_failure, responseCode), R.style.errorToast).also { it.show() }
            }
        })

        // 게시글 삭제 결과 처리
        postViewModel.isCompleteDeletePost.observe(viewLifecycleOwner, EventObserver { result ->
            mToast?.cancel()
            val toastMessage = when (result) {
                is PostViewModel.DeletePostResult.CannotDelete -> getString(R.string.notify_forest_friends)
                is PostViewModel.DeletePostResult.Success -> getString(R.string.delete_success)
                else -> getString(R.string.delete_failure)
            }
            mToast = StyleableToast.makeText(requireContext(), toastMessage, R.style.errorToast).also { it.show() }
        })

        // 스크롤 맨 위로 이동
        postViewModel.isPostAdded.observe(viewLifecycleOwner, EventObserver {
            lifecycleScope.launch {
                delay(100) // DB의 변경이 리사이클러뷰에 반영되는 시간을 고려하여 약간의 딜레이를 부여
                binding.recyclerViewPosts.smoothScrollToPosition(0)
            }
        })

    }

    private val postItemClickListener = object : PostItemClickListener {
        // 댓글 추가 버튼 클릭 리스너 재정의
        override fun onGetCommentClicked(postWithTagsAndComments: PostWithTagsAndComments) {
            val post = postWithTagsAndComments.post
            val parentPostId = post.postId
            val postContent = post.content
            ImageUtils.uriToMultipart(requireContext(), post.image)?.let {  postImage ->
                postViewModel.getComments(parentPostId, postContent, postImage)
            }
        }

        // 이미지 클릭 리스너 재정의
        override fun onPostImageClicked(postWithTagsAndComments: PostWithTagsAndComments, imageView: ImageView) {
            val intent = Intent(activity, ImageZoomActivity::class.java)
            intent.putExtra("imageUri", postWithTagsAndComments.post.image.toString())
            startActivity(intent)
        }

        // 게시글 옵션 클릭 리스너 재정의
        override fun onOptionClicked(postWithTagsAndComments: PostWithTagsAndComments, imageButton: ImageButton) {
            showMenuDialog(imageButton, postWithTagsAndComments)
        }

        // 태그 클릭 리스너 재정의
        override fun onTagChipClicked(tagText: String) {
            val intent = Intent(activity, SearchActivity::class.java)
            intent.putExtra("tag", tagText)
            startActivity(intent)
            activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.stay)
        }
    }

    // 옵션 메뉴를 띄우고 해당 아이템 클릭시 리스너에 알림
    private fun showMenuDialog(view: View, postWithTagsAndComments: PostWithTagsAndComments) {
        val menuItems = arrayOf(getString(R.string.delete)) // 메뉴 항목 배열

        MaterialAlertDialogBuilder(view.context)
            .setItems(menuItems) { dialog, which ->
                when (which) {
                    // "삭제하기" 메뉴 항목 클릭 처리
                    0 -> { showDeleteConfirmationDialog(view, postWithTagsAndComments) }
                }
                dialog.dismiss()
            }
            .show()
    }

    // 삭제 버튼은 한번 더 확인 다이얼로그를 띄움
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