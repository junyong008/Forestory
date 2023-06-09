package com.yjy.forestory.feature.post

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.yjy.forestory.model.db.dto.PostWithComments
import com.yjy.forestory.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LinearPostListViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {



   /* val userName: LiveData<String?> = userRepository.getUserName().asLiveData()




    fun changeUserName(newUserName: String) {
        viewModelScope.launch {
            userRepository.setUserName(newUserName)
        }
    }*/



    // Flow를 이용해 DB내 데이터가 변경되면 자동으로 비동기적으로 데이터를 가져와 갱신.
    val allPosts: LiveData<List<PostWithComments>> = postRepository.getAllPosts().asLiveData()


}