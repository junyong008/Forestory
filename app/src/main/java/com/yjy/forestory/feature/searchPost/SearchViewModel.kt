package com.yjy.forestory.feature.searchPost

import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.yjy.forestory.model.PostWithTagsAndComments
import com.yjy.forestory.model.Tag
import com.yjy.forestory.repository.PostWithTagsAndCommentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class SearchViewModel @Inject constructor(
    private val postWithTagsAndCommentsRepository: PostWithTagsAndCommentsRepository
) : ViewModel() {

    val searchText = MutableLiveData<String>()

    // ---------------------------------- 태그 검색

    private val _tagList = MutableLiveData<List<Tag>>()
    val tagList: LiveData<List<Tag>> get() = _tagList

    fun onSearchTextChanged(text: CharSequence) {
        viewModelScope.launch {

            if (text.startsWith("#")) {
                val inputText = text.substring(1).trim()
                _tagList.value = postWithTagsAndCommentsRepository.getTagList(inputText)
            } else {
                _tagList.value = emptyList()
            }
        }
    }


    // ---------------------------------- 키워드 및 태그로 게시글 검색

    private val _currentKeyword = MutableLiveData<String?>()
    val currentKeyword: LiveData<String?> get() = _currentKeyword

    private val _currentKeytag = MutableLiveData<String?>()
    val currentKeytag: LiveData<String?> get() = _currentKeytag

    // currentKeyword와 currentKeytag의 값이 변경되면 searchPostWithTagsAndCommentsList의 LiveData를 해제하고 갱신
    val searchPostWithTagsAndCommentsList = MediatorLiveData<PagingData<PostWithTagsAndComments>>().apply {
        addSource(_currentKeyword.switchMap { keyword ->
            if (!keyword.isNullOrEmpty()) {
                postWithTagsAndCommentsRepository.getPostWithTagsAndCommentsList(keyword).cachedIn(viewModelScope).asLiveData()
            } else {
                MutableLiveData<PagingData<PostWithTagsAndComments>>()
            }
        }) { value = it }

        addSource(_currentKeytag.switchMap { keytag ->
            if (!keytag.isNullOrEmpty()) {
                postWithTagsAndCommentsRepository.getPostWithTagsAndCommentsListByTag(keytag).cachedIn(viewModelScope).asLiveData()
            } else {
                MutableLiveData<PagingData<PostWithTagsAndComments>>()
            }
        }) { value = it }
    }

    // 찾은 게시글의 갯수
    val searchPostCount = MediatorLiveData<Int>().apply {
        addSource(_currentKeyword.switchMap { keyword ->
            if (!keyword.isNullOrEmpty()) {
                postWithTagsAndCommentsRepository.getPostCount(keyword).asLiveData()
            } else {
                MutableLiveData<Int>()
            }
        }) { value = it }

        addSource(_currentKeytag.switchMap { keytag ->
            if (!keytag.isNullOrEmpty()) {
                postWithTagsAndCommentsRepository.getPostCountByTag(keytag).asLiveData()
            } else {
                MutableLiveData<Int>()
            }
        }) { value = it }
    }


    fun searchPosts() {
        val inputText = searchText.value
        if (!inputText.isNullOrEmpty()) {
            _currentKeytag.value = ""
            _currentKeyword.value = inputText
            _tagList.value = emptyList()
        }
    }

    fun searchPostsByTag(tagText: String) {
        _currentKeyword.value = ""
        _currentKeytag.value = tagText
        _tagList.value = emptyList()
    }

    fun emptyKeytag() {
        _currentKeytag.value = null
    }
}