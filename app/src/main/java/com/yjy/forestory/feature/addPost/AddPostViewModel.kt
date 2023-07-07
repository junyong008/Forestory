package com.yjy.forestory.feature.addPost

import android.net.Uri
import androidx.lifecycle.*
import com.yjy.forestory.model.repository.PostWithTagsAndCommentsRepository
import com.yjy.forestory.model.repository.UserRepository
import com.yjy.forestory.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AddPostViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val postWithTagsAndCommentsRepository: PostWithTagsAndCommentsRepository
) : ViewModel() {

    // ---------------------------------- 로딩 여부 설정
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading


    // ---------------------------------- 추가할 사진
    private val _currentPhoto = MutableLiveData<Uri?>(null)
    val currentPhoto: LiveData<Uri?> get() = _currentPhoto

    fun setCurrentPhoto(uri: Uri?) {
        _currentPhoto.value = uri
    }


    // ---------------------------------- 태그
    private val _tagList = MutableLiveData<List<String>>()
    val tagList: LiveData<List<String>> get() = _tagList

    val maxTagCount = 10

    fun addTag(inputText: String) {
        val currentList = _tagList.value.orEmpty().toMutableList()
        currentList.add(inputText)
        _tagList.value = currentList
    }

    fun removeTag(inputText: String) {
        val currentList = _tagList.value.orEmpty().toMutableList()
        currentList.remove(inputText)
        _tagList.value = currentList
    }


    // ---------------------------------- 내용 양방향 바인딩
    val contentText = MutableLiveData<String>()


    // ---------------------------------- 게시글 추가
    val isReadyToPost = MediatorLiveData(false)

    init {
        // _currentPhoto와 contentText가 변경될때마다 checkReadyToPost() 가 호출되어 isReadyToPost의 상태를 검증한다.
        // "isReadyToPost.value =" 로 갱신하지 않으면 addSource한 데이터의 Value가 저장된다.
        isReadyToPost.addSource(_currentPhoto) { isReadyToPost.value = checkReadyToPost() }
        isReadyToPost.addSource(contentText) { isReadyToPost.value = checkReadyToPost() }
    }

    private fun checkReadyToPost(): Boolean {
        val isCurrentPhotoNull: Boolean = _currentPhoto.value == null
        val isContentTextNull: Boolean = contentText.value.isNullOrEmpty()

        // 모든게 null이 아닐때만 true를 반환
        return when {
            !isCurrentPhotoNull && !isContentTextNull -> true
            else -> false
        }
    }

    private val _isCompleteInsert = MutableLiveData<Event<Boolean>>()
    val isCompleteInsert: LiveData<Event<Boolean>> get() = _isCompleteInsert

    fun addPost(uploadImage: Uri, uploadContent: String, uploadTags: List<String>?) {
        viewModelScope.launch {
            _isLoading.value = true

            // 사용자 이름과 프로필 사진을 가져오기.
            val userName = userRepository.getUserName().firstOrNull()
            val userProfile = userRepository.getUserPicture().firstOrNull()

            val result: Boolean = postWithTagsAndCommentsRepository.insertPostWithTags(userName, userProfile, uploadImage, uploadContent, uploadTags)
            _isCompleteInsert.value = Event(result)
            _isLoading.value = false
        }
    }
}