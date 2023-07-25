package com.yjy.forestory.feature.viewPost

import android.net.Uri
import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.yjy.forestory.model.PostWithTagsAndComments
import com.yjy.forestory.model.repository.PostWithTagsAndCommentsRepository
import com.yjy.forestory.model.repository.SettingRepository
import com.yjy.forestory.util.Event
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // 싱글톤으로 구성하여 게시글의 조회, 수정, 삭제 등을 모두 총괄함.
class PostViewModel @Inject constructor(
    private val postWithTagsAndCommentsRepository: PostWithTagsAndCommentsRepository,
    private val settingRepository: SettingRepository
) : ViewModel() {

    // ---------------------------------- 모든 게시글, 태그, 댓글 조회 : 만약 데이터 복원이 발생하면 PagingData를 새로 가져옴.
    @OptIn(ExperimentalCoroutinesApi::class)
    val postWithTagsAndCommentsList: LiveData<PagingData<PostWithTagsAndComments>> =
        settingRepository.getIsRestoreInProgress().flatMapLatest {
            postWithTagsAndCommentsRepository.getPostWithTagsAndCommentsList().cachedIn(viewModelScope)
        }.asLiveData()

    // ---------------------------------- 특정 게시글 및 댓글 조회
    fun getPostWithTagsAndComments(postId: Int): LiveData<PostWithTagsAndComments?> =
        postWithTagsAndCommentsRepository.getPostWithTagsAndComments(postId).asLiveData()

    // ---------------------------------- 게시글 갯수 조회 및 추가 감지
    private val postCount = postWithTagsAndCommentsRepository.getPostCount().asLiveData()
    private var previousPostCount = -1

    private val _isPostAdded = MediatorLiveData<Event<Boolean>>()
    val isPostAdded: LiveData<Event<Boolean>> get() = _isPostAdded


    init {
        viewModelScope.launch {
            // postCount 전체 개시글의 숫자에 변동이 생기면 이전 게시글의 갯수와 비교하여 게시글이 추가됐는지 감지
            _isPostAdded.addSource(postCount) { newCount ->

                // 초기값은 전체 게시글 숫자로 설정해 새로운 게시글만 감지
                if (previousPostCount == -1) {
                    previousPostCount = newCount
                    return@addSource
                }

                if (newCount > previousPostCount) {
                    _isPostAdded.value = Event(true)
                }
                previousPostCount = newCount
            }
        }
    }

    // ---------------------------------- 게시글 삭제

    private val _isCompleteDeletePost = MutableLiveData<Event<DeletePostResult>>()
    val isCompleteDeletePost: LiveData<Event<DeletePostResult>> get() = _isCompleteDeletePost

    private val _deletedPostImage = MutableLiveData<Uri>()
    val deletedPostImage: LiveData<Uri> get() = _deletedPostImage

    fun deletePostWithTagsAndComments(postWithTagsAndComments: PostWithTagsAndComments) {
        viewModelScope.launch {

            // 만약 댓글을 서버로부터 추가중이라면 삭제 막기
            if (postWithTagsAndComments.post.isAddingComments) {
                _isCompleteDeletePost.value = Event(DeletePostResult.CannotDelete)
                return@launch
            }

            val postImage: Uri = postWithTagsAndComments.post.image

            if (postWithTagsAndCommentsRepository.deletePostWithTagsAndComments(postWithTagsAndComments)) {
                _deletedPostImage.value = postImage
                _isCompleteDeletePost.value = Event(DeletePostResult.Success)
            } else {
                _isCompleteDeletePost.value = Event(DeletePostResult.Failed)
            }
        }
    }

    sealed class DeletePostResult {
        object CannotDelete: DeletePostResult()
        object Success: DeletePostResult()
        object Failed: DeletePostResult()
    }
}