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
import com.yjy.forestory.databinding.FragmentLinearPostListBinding
import com.yjy.forestory.model.db.dto.PostWithComments
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

        // 리사이클러뷰 Adapter 등록, postItemClickListener 리스너를 등록하여 클릭 이벤트 처리
        // 왜 DialogFragment는 직접 인터페이스 리스너를 상속받아 override 했는가 하면.. -> 이건 Configuration Change가 발생하더라도 다시 onCreateView에서 어댑터를 연결하면서 리스너가 등록된다.
        // DialogFragment는 onCreate에서 할당하는게 아니기에, 띄워져 있는 상태에서 CC 가 발생하면 리스너 등록이 안된다.
        val recyclerViewAdapter = PostAdapter(postItemClickListener, true)
        binding.recyclerViewPosts.adapter = recyclerViewAdapter

        // 리사이클러뷰 Adapter의 로딩 상태를 감지하여 프로그레스 보여주기
        recyclerViewAdapter.addLoadStateListener { loadState ->
            binding.progressBar.isVisible = loadState.source.refresh is LoadState.Loading
        }

        setEventObserver()

        return binding.root
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
        override fun onGetCommentClicked(postWithComments: PostWithComments) {
            postViewModel.getComments(postWithComments)
        }

        // 이미지 클릭 리스너 재정의
        override fun onPostImageClicked(postWithComments: PostWithComments) {
            val intent = Intent(activity, ImageZoomActivity::class.java)
            intent.putExtra("imageUri", postWithComments.post.image.toString())
            startActivity(intent)
        }

        // 게시글 삭제 클릭 리스너 재정의
        override fun onDeletePostClicked(postWithComments: PostWithComments) {
            postViewModel.deletePost(postWithComments)
        }
    }
}