package com.yjy.forestory.feature.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.yjy.forestory.R
import com.yjy.forestory.databinding.FragmentLinearPostListBinding
import com.yjy.forestory.model.db.dto.PostDTO
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LinearPostListFragment : Fragment() {

    // 바인딩, 뷰모델 정의
    private lateinit var binding: FragmentLinearPostListBinding
    private val linearPostListViewModel: LinearPostListViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_linear_post_list, container, false)

        binding.lifecycleOwner = this@LinearPostListFragment
        binding.linearPostListViewModel = linearPostListViewModel

        // 리사이클러뷰 Adapter 등록, postItemClickListener 리스너를 등록하여 클릭 이벤트 처리
        // 왜 DialogFragment는 직접 인터페이스 리스너를 상속받아 override 했는가 하면.. -> 이건 Configuration Change가 발생하더라도 다시 onCreateView에서 어댑터를 연결하면서 리스너가 등록된다.
        // DialogFragment는 onCreate에서 할당하는게 아니기에, 띄워져 있는 상태에서 CC 가 발생하면 리스너 등록이 안된다.
        binding.recyclerViewPosts.adapter = PostAdapter(postItemClickListener, true)

        return binding.root
    }

    private val postItemClickListener = object : PostItemClickListener {
        override fun onGetCommentClicked(post: PostDTO) {

        }
    }
}