package com.yjy.forestory.feature.searchPost

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.paging.LoadState
import com.yjy.forestory.R
import com.yjy.forestory.databinding.ActivitySearchBinding
import com.yjy.forestory.feature.viewPost.PostActivity
import com.yjy.forestory.model.PostWithTagsAndComments
import com.yjy.forestory.model.Tag
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val searchViewModel: SearchViewModel by viewModels()

    private var searchTag: String? = null

    // 시스템의 뒤로가기 버튼 눌렀을 때
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
            overridePendingTransition(R.anim.stay, R.anim.slide_out_right)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_search)

        binding.searchViewModel = searchViewModel
        binding.lifecycleOwner = this@SearchActivity

        // 뒤로가기 버튼 콜백 등록
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        // 태그 클릭으로 인한 유입
        searchTag = intent.getStringExtra("tag")

        setSearchEdit()
        setRecyclerViewAdapter()
        setOnClickListener()
        setObserver()
    }

    private fun setSearchEdit() {

        // 태그 클릭으로 인한 유입이라면 바로 태그를 검색해서 보여주고, 아니라면 포커스를 잡아 키보드를 띄운다
        if (searchTag != null) {
            searchViewModel.searchText.value = "#$searchTag"
            searchViewModel.searchPostsByTag(searchTag!!)
        } else {
            binding.editSearch.requestFocus()
        }

        // 엔터를 누르면 검색하기
        binding.editSearch.setOnEditorActionListener { view, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                searchViewModel.searchPosts() // 검색. 검색 결과는 바로 리사이클러뷰에 바인딩어댑터로 바인딩된다
                hideKeyboard() // 키보드 숨기기
                true
            } else {
                false
            }
        }

        // 포커스가 잡히면 현재 검색한 태그를 비우기 : 태그 클릭으로 인한 유입시 setOnClickListener가 두번째 클릭부터 동작함. 그래서 이걸로 보충
        binding.editSearch.setOnFocusChangeListener { v, hasFocus ->
            searchViewModel.emptyKeytag()
        }
    }

    private fun setRecyclerViewAdapter() {
        val tagRecyclerViewAdapter = SearchTagAdapter(searchTagItemClickListener)
        binding.recyclerViewTags.adapter = tagRecyclerViewAdapter

        val postRecyclerViewAdapter = SearchPostAdapter(searchPostItemClickListener)
        binding.recyclerViewPosts.adapter = postRecyclerViewAdapter

        postRecyclerViewAdapter.addLoadStateListener { loadState ->
            binding.progressBar.isVisible = loadState.source.refresh is LoadState.Loading
        }
    }

    private fun setOnClickListener() {
        // 닫기버튼 클릭
        binding.ibuttonClose.setOnClickListener {
            onBackPressedCallback.handleOnBackPressed()
        }

        // 검색어 한번에 지우기 버튼 클릭
        binding.ibuttonDeleteText.setOnClickListener {
            binding.editSearch.setText("")
            searchViewModel.emptyKeytag()
        }

        //Chip 닫기 버튼 or 검색창을 누르면 현재 검색한 태그를 비워서 검색이 가능하도록 설정
        binding.chipTag.setOnCloseIconClickListener {
            searchViewModel.emptyKeytag()
        }
        binding.editSearch.setOnClickListener {
            searchViewModel.emptyKeytag()
        }
    }

    private fun setObserver() {
        // 검색어가 있으면 한번에 지우기 버튼을 제공한다
        searchViewModel.searchText.observe(this, Observer {
            binding.ibuttonDeleteText.isVisible = it.isNotEmpty()
        })

        // tag 목록에 항목이 존재할때만 태그 리사이클러뷰를 띄우고 동시에 검색 결과 리사이클러뷰는 감춘다
        searchViewModel.tagList.observe(this, Observer {
            binding.recyclerViewTags.isVisible = !it.isNullOrEmpty()
            binding.recyclerViewPosts.isVisible = it.isNullOrEmpty()
            if (!it.isNullOrEmpty()) { binding.textViewNotfound.isVisible = false }
        })

        // 현재 검색 키워드를 리사이클러뷰 어댑터의 키워드로 설정한다
        searchViewModel.currentKeyword.observe(this, Observer {
            it?.let {
                val postRecyclerViewAdapter = binding.recyclerViewPosts.adapter as SearchPostAdapter
                postRecyclerViewAdapter.keyword = it
            }
        })

        // 검색된 게시글이 존재하지 않으면 없음 안내 메시지를 띄운다
        searchViewModel.searchPostCount.observe(this, Observer {
            binding.textViewNotfound.isVisible = (it <= 0)
        })

        // 현재 태그 검색어로 Chip을 설정
        searchViewModel.currentKeytag.observe(this, Observer {
            binding.chipTag.isVisible = !it.isNullOrEmpty()
            binding.chipTag.text = it
        })
    }

    private fun hideKeyboard() {
        val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        this.currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private val searchTagItemClickListener = object : SearchTagItemClickListener {
        // 태그 클릭 리스너 재정의
        override fun onTagClicked(tag: Tag) {
            searchViewModel.searchPostsByTag(tag.content) // 태그로 게시글을 검색
            hideKeyboard() // 키보드 숨기기
        }
    }

    private val searchPostItemClickListener = object : SearchPostItemClickListener {
        // 포스트 클릭 리스너 재정의
        override fun onPostClicked(postWithTagsAndComments: PostWithTagsAndComments) {
            val intent = Intent(this@SearchActivity, PostActivity::class.java)
            intent.putExtra("postId", postWithTagsAndComments.post.postId)
            intent.putExtra("recursion", true) // 재귀 방지. 검색 -> 태그 클릭 -> 검색 -> 태그 클릭 ...
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.stay)
        }

        // 각 포스트 내의 태그 Chip 클릭 리스너 재정의 : 태그 재검색
        override fun onTagChipClicked(tagText: String) {
            searchViewModel.searchPostsByTag(tagText)
            hideKeyboard()
        }
    }
}