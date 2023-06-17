package com.yjy.forestory.feature.post

import androidx.lifecycle.*
import com.yjy.forestory.Const.Companion.GENDER_MALE
import com.yjy.forestory.R
import com.yjy.forestory.model.db.dto.PostWithComments
import com.yjy.forestory.repository.CommentRepository
import com.yjy.forestory.repository.PostRepository
import com.yjy.forestory.repository.UserRepository
import com.yjy.forestory.util.Event
import com.yjy.forestory.util.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LinearPostListViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val commentRepository: CommentRepository,
    private val imageUtils: ImageUtils
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

   /* val userName: LiveData<String?> = userRepository.getUserName().asLiveData()




    fun changeUserName(newUserName: String) {
        viewModelScope.launch {
            userRepository.setUserName(newUserName)
        }
    }*/





    // DB의 초기값을 설정해줘야 하는 열을 모두 초기화해준다.
    // 왜 댓글을 추가하고 있다는 사실을 DB에 저장하고 관리해야 하는가?
    // 1. 리사이클러뷰에서 댓글 추가 버튼을 누르면 해당 버튼을 비활성화하고 중복으로 요청을 못하게 해야한다.
    // 2. 비활성화는 클릭시 처리하고 리스너를 등록하면 되지만, recyclerView Adapter는 액티비티 혹은 프레그먼트에 의해 중속되므로 액티비티 혹은 프레그먼트가 Configuration Change가 일어나면 초기화가 되버린다
    // 3. 고로, DB에 임시값을 추가하여 댓글을 추가하는 로직의 주체인 ViewModel에서 임시값을 다룬다.
    // 4. 만약 이렇게 매번 임시값으로 초기화를 해주지 않으면, 댓글 추가 도중 어플이 종료되거나 뷰모델의 생명주기가 끝나면 댓글이 추가되지 않았음에도 계속 비활성화 될것이다. 고로 매번 초기화를 해준다.
    // 5. 혹은 데이터베이스가 생성될때 callBack으로 초기화를 해줘도 될것 같다. 하지만, 그럴땐 뷰모델도 싱글톤으로 구성해야할 것 이다.
    //    어플 실행시 데이터베이스에서 초기화를 해줬는데 뷰모델이 댓글을 불러오다 생명주기가 모종의 이유로 끝나면 어플을 다시 실행하지 않는 한 계속 비활성화 처리가 될 것이기 때문이다. 고로 뷰모델에서 초기화를 진행한다.
    init {
        viewModelScope.launch {
            postRepository.updateTempColumn(0)
        }
    }

    // Flow를 이용해 DB내 데이터가 변경되면 자동으로 비동기적으로 데이터를 가져와 갱신.
    // List<CommentDTO> 와 PostDTO가 묶여져 있으므로, PostDB나 CommentDB에 새로운 항목이 생기면 갱신된다.
    val allPosts: LiveData<List<PostWithComments>> = postRepository.getAllPosts().asLiveData()





    fun getComments(postWithComments: PostWithComments) {
        viewModelScope.launch {
            val post = postWithComments.post

            // 해당 게시글의 댓글을 현재 추가중임을 DB에 저장하여 명시
            postRepository.updateTempColumn(1, post.postId!!)

            // 댓글을 불러오는데 필요한 요소들 정의
            val parentPostId = post.postId
            val writerName = userRepository.getUserName().firstOrNull()
            val writerGender = GENDER_MALE
            val postContent = post.content
            val postImage = imageUtils.uriToMultipart(post.image)

            // 결과를 받아와서 성공 실패 유무 보여주기
            val responseCode = commentRepository.addComments(parentPostId, writerName, writerGender, postContent, postImage)
            if (responseCode != 200) {
                setToastMsg(R.style.errorToast, "ERROR : $responseCode")
            }

            // 처리가 완료됐음을 DB에 명시
            postRepository.updateTempColumn(0, post.postId!!)
        }
    }

}