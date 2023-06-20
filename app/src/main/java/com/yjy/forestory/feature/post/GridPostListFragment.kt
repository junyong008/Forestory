package com.yjy.forestory.feature.post

import EventObserver
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.yjy.forestory.R
import com.yjy.forestory.databinding.FragmentGridPostListBinding
import com.yjy.forestory.model.db.dto.PostWithComments
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GridPostListFragment : Fragment() {

    private lateinit var binding: FragmentGridPostListBinding
    @Inject lateinit var postViewModel: PostViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_grid_post_list, container, false)

        binding.lifecycleOwner = this@GridPostListFragment
        binding.postViewModel = postViewModel

        binding.recyclerViewPosts.adapter = PostAdapter(postItemClickListener, false)

        // 리사이클러뷰 Adapter의 로딩 상태를 감지하여 프로그레스 보여주기
        val recyclerViewAdapter = binding.recyclerViewPosts.adapter as PostAdapter
        recyclerViewAdapter.addLoadStateListener { loadState ->
            binding.progressBar.isVisible = loadState.source.refresh is LoadState.Loading
        }

        postViewModel.isPostAdded.observe(viewLifecycleOwner, EventObserver {
            lifecycleScope.launch {
                delay(100) // DB의 변경이 리사이클러뷰에 반영되는 시간을 고려하여 약간의 딜레이를 부여
                binding.recyclerViewPosts.smoothScrollToPosition(0)
            }
        })

        return binding.root
    }

    private val postItemClickListener = object : PostItemClickListener {
        // 이미지 클릭 리스너 재정의
        override fun onPostImageClicked(postWithComments: PostWithComments) {
            val intent = Intent(activity, PostActivity::class.java)
            intent.putExtra("postId", postWithComments.post.postId)
            startActivity(intent)
            activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.stay)
        }

        override fun onGetCommentClicked(postWithComments: PostWithComments) {}
        override fun onDeletePostClicked(postWithComments: PostWithComments) {}
    }
}