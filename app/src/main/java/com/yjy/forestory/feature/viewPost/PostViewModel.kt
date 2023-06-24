package com.yjy.forestory.feature.viewPost

import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.yjy.forestory.Const.Companion.GENDER_MALE
import com.yjy.forestory.R
import com.yjy.forestory.model.PostWithTagsAndComments
import com.yjy.forestory.repository.PostWithTagsAndCommentsRepository
import com.yjy.forestory.repository.UserRepository
import com.yjy.forestory.util.Event
import com.yjy.forestory.util.ImageUtils
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // 싱글톤으로 구성하여 게시글의 조회, 수정, 삭제 등을 모두 총괄함.
class PostViewModel @Inject constructor(
    private val imageUtils: ImageUtils,
    private val userRepository: UserRepository,
    private val postWithTagsAndCommentsRepository: PostWithTagsAndCommentsRepository
) : ViewModel() {

    // ---------------------------------- 토스트 메시지 설정
    private val _showToast = MutableLiveData<Event<Boolean>>()
    val showToast: LiveData<Event<Boolean>> get() = _showToast

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    private val _toastIcon = MutableLiveData<Int>()
    val toastIcon: LiveData<Int> get() = _toastIcon

    private fun setToastMsg(icon: Int, msg: String) {
        _toastIcon.value = icon
        _toastMessage.value = msg
        _showToast.value = Event(true)
    }

    // ---------------------------------- 모든 게시글, 태그, 댓글 조회
    val postWithTagsAndCommentsList: LiveData<PagingData<PostWithTagsAndComments>> =
        postWithTagsAndCommentsRepository.getPostWithTagsAndCommentsList().cachedIn(viewModelScope).asLiveData()

    // ---------------------------------- 특정 게시글 및 댓글 조회
    fun getPostWithTagsAndComments(postId: Int): LiveData<PostWithTagsAndComments?> =
        postWithTagsAndCommentsRepository.getPostWithTagsAndComments(postId).asLiveData()

    // ---------------------------------- 게시글 갯수 조회 및 추가 감지
    val postCount = postWithTagsAndCommentsRepository.getPostCount().asLiveData()
    private var previousPostCount = -1

    private val _isPostAdded = MediatorLiveData<Event<Boolean>>()
    val isPostAdded: LiveData<Event<Boolean>> get() = _isPostAdded


    init {
        viewModelScope.launch {
            /* DB의 초기값을 설정해줘야 하는 열을 모두 초기화해준다.
                 왜 댓글을 추가하고 있다는 사실을 DB에 저장하고 관리해야 하는가?
                 1. 리사이클러뷰에서 댓글 추가 버튼을 누르면 해당 버튼을 비활성화하고 중복으로 요청을 못하게 해야한다.
                 2. 비활성화는 클릭시 처리하고 리스너를 등록하면 되지만, recyclerView Adapter는 액티비티 혹은 프레그먼트에 의해 중속되므로 액티비티 혹은 프레그먼트가 Configuration Change가 일어나면 초기화가 되버린다
                 3. 고로, DB에 임시값을 추가하여 댓글을 추가하는 로직의 주체인 ViewModel에서 임시값을 다룬다.
                 4. 만약 이렇게 매번 임시값으로 초기화를 해주지 않으면, 댓글 추가 도중 어플이 종료되거나 뷰모델의 생명주기가 끝나면 댓글이 추가되지 않았음에도 계속 비활성화 될것이다. 고로 매번 초기화를 해준다.*/
            postWithTagsAndCommentsRepository.updatePostIsAddingComments(0)

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






    // ---------------------------------- 게시글의 정보를 받아서 서버로부터 AI의 반응을 받아와 댓글을 추가
    fun getComments(postWithTagsAndComments: PostWithTagsAndComments) {
        viewModelScope.launch {
            val post = postWithTagsAndComments.post

            // 해당 게시글의 댓글을 현재 추가중임을 DB에 저장하여 명시
            postWithTagsAndCommentsRepository.updatePostIsAddingComments(1, post.postId)

            // 댓글을 불러오는데 필요한 요소들 정의
            val parentPostId = post.postId
            val writerName = userRepository.getUserName().firstOrNull()
            val writerGender = GENDER_MALE
            val postContent = post.content
            val postImage = imageUtils.uriToMultipart(post.image)

            // 결과를 받아와서 성공 실패 유무 보여주기
            val responseCode = postWithTagsAndCommentsRepository.addComments(parentPostId, writerName, writerGender, postContent, postImage)
            if (responseCode != 200) {
                setToastMsg(R.style.errorToast, "ERROR : $responseCode")
            }

            // 처리가 완료됐음을 DB에 명시
            postWithTagsAndCommentsRepository.updatePostIsAddingComments(0, post.postId)
        }
    }

    // ---------------------------------- 게시글 삭제
    fun deletePostWithTagsAndComments(postWithTagsAndComments: PostWithTagsAndComments) {

        // 만약 댓글을 서버로부터 추가중이라면 삭제 막기
        if (postWithTagsAndComments.post.isAddingComments) {
            setToastMsg(R.style.errorToast, "숲속 친구들에게 알리고있는 게시글 입니다")
            return
        }

        viewModelScope.launch {
            if (postWithTagsAndCommentsRepository.deletePostWithTagsAndComments(postWithTagsAndComments)) {
                setToastMsg(R.style.successToast, "삭제되었습니다")
            }
        }
    }
}