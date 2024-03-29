package com.yjy.forestory.feature.viewPost

import EventObserver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.RecyclerView
import com.yjy.forestory.R
import com.yjy.forestory.base.BaseFragment
import com.yjy.forestory.databinding.FragmentGridPostListBinding
import com.yjy.forestory.feature.main.RecyclerViewScrollListener
import com.yjy.forestory.model.PostWithTagsAndComments
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GridPostListFragment: BaseFragment<FragmentGridPostListBinding>(R.layout.fragment_grid_post_list) {

    @Inject lateinit var postViewModel: PostViewModel
    private var isPostClickable = true
    private var recyclerViewScrollListener: RecyclerViewScrollListener? = null

    override fun initViewModel() {
        binding.postViewModel = postViewModel
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is RecyclerViewScrollListener) {
            recyclerViewScrollListener = context
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.recyclerViewPosts.adapter = PostAdapter(postItemClickListener, false)
    }

    override fun setListener() {
        // 리사이클러뷰 Adapter의 로딩 상태를 감지하여 프로그레스 보여주기
        (binding.recyclerViewPosts.adapter as PostAdapter).addLoadStateListener { loadState ->
            binding.progressBar.isVisible = loadState.source.refresh is LoadState.Loading
        }

        // 리사이클러뷰의 스크롤 변경 이벤트를 액티비티 리스너에 전달
        binding.recyclerViewPosts.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                recyclerViewScrollListener?.onScrollChanged(recyclerView.computeVerticalScrollOffset())
            }
        })
    }

    override fun setEventObserver() {

        // 게시글이 새로 추가되면 스크롤을 맨 위로 이동
        postViewModel.isPostAdded.observe(viewLifecycleOwner, EventObserver {
            lifecycleScope.launch {
                delay(100) // DB의 변경이 리사이클러뷰에 반영되는 시간을 고려하여 약간의 딜레이를 부여
                binding.recyclerViewPosts.smoothScrollToPosition(0)
            }
        })
    }

    private val postItemClickListener = object : PostItemClickListener {
        // 이미지 클릭 리스너 재정의
        override fun onPostImageClicked(postWithTagsAndComments: PostWithTagsAndComments, imageView: ImageView) {
            if (isPostClickable) {
                isPostClickable = false
                val intent = Intent(activity, PostActivity::class.java)
                intent.putExtra("postId", postWithTagsAndComments.post.postId)

                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), imageView, "postImage")
                requireActivity().startActivity(intent, options.toBundle())

                imageView.postDelayed({ isPostClickable = true }, 500L)
            }
        }

        override fun onGetCommentClicked(postWithTagsAndComments: PostWithTagsAndComments) {}
        override fun onOptionClicked(postWithTagsAndComments: PostWithTagsAndComments, imageButton: ImageButton) {}
        override fun onTagChipClicked(tagText: String) {}
    }
}