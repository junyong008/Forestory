package com.yjy.forestory.feature.viewPost

import EventObserver
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yjy.forestory.R
import com.yjy.forestory.databinding.FragmentLinearPostListBinding
import com.yjy.forestory.feature.searchPost.SearchActivity
import com.yjy.forestory.model.PostWithTagsAndComments
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

        // 리사이클러뷰 스크롤이 맨 위가 아니라면 맨 위로 가기 버튼 보여주기
        binding.recyclerViewPosts.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                binding.ibuttonGoTop.isVisible = (firstVisibleItemPosition > 0)
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

        // 토스트 메시지 띄우기
        postViewModel.showToast.observe(viewLifecycleOwner, EventObserver {
            mToast?.let { it.cancel() }

            val toastMessage = postViewModel.toastMessage.value
            val toastIcon = postViewModel.toastIcon.value ?: 0
            mToast = StyleableToast.makeText(requireContext(), toastMessage, toastIcon).also { it.show() }
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
            postViewModel.getComments(postWithTagsAndComments)
        }

        // 이미지 클릭 리스너 재정의
        override fun onPostImageClicked(postWithTagsAndComments: PostWithTagsAndComments, imageView: ImageView) {
            val intent = Intent(activity, ImageZoomActivity::class.java)
            intent.putExtra("imageUri", postWithTagsAndComments.post.image.toString())
            startActivity(intent)
        }

        // 게시글 삭제 클릭 리스너 재정의
        override fun onDeletePostClicked(postWithTagsAndComments: PostWithTagsAndComments) {
            postViewModel.deletePostWithTagsAndComments(postWithTagsAndComments)
        }

        // 태그 클릭 리스너 재정의
        override fun onTagChipClicked(tagText: String) {
            val intent = Intent(activity, SearchActivity::class.java)
            intent.putExtra("tag", tagText)
            startActivity(intent)
            activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.stay)
        }
    }
}