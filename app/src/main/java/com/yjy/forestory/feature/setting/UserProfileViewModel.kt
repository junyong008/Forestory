package com.yjy.forestory.feature.setting

import android.net.Uri
import androidx.lifecycle.*
import com.yjy.forestory.model.repository.UserRepository
import com.yjy.forestory.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    // 현재 액티비티의 데이터를 저장, 최초 실행시 현재 설정값을 받아와 뷰 초기화
    val currentUserName = MutableLiveData<String?>()

    private val _currentUserPicture = MutableLiveData<Uri?>()
    val currentUserPicture: LiveData<Uri?> get() = _currentUserPicture

    private val _currentUserGender = MutableLiveData<String?>()
    val currentUserGender: LiveData<String?> get() = _currentUserGender


    private val _isReadyToConfirm = MediatorLiveData(false)
    val isReadyToConfirm: MediatorLiveData<Boolean> get() = _isReadyToConfirm

    init {
        _isReadyToConfirm.addSource(currentUserName) { _isReadyToConfirm.value = checkReadyToConfirm() }
        _isReadyToConfirm.addSource(_currentUserGender) { _isReadyToConfirm.value = checkReadyToConfirm() }

        viewModelScope.launch {
            currentUserName.value = userRepository.getUserName().firstOrNull()
            _currentUserPicture.value = userRepository.getUserPicture().firstOrNull()
            _currentUserGender.value = userRepository.getUserGender().firstOrNull()
        }
    }

    private fun checkReadyToConfirm(): Boolean {
        val isCurrentUserNameNull: Boolean = currentUserName.value.isNullOrEmpty()
        val isCurrentUserGenderNull: Boolean = _currentUserGender.value.isNullOrEmpty()

        // 모든게 null이 아닐때만 true를 반환
        return when {
            !isCurrentUserNameNull && !isCurrentUserGenderNull -> true
            else -> false
        }
    }

    fun setCurrentPicture(picture: Uri) {
        _currentUserPicture.value = picture
    }

    fun setCurrentGender(gender: String) {
        _currentUserGender.value = gender
    }


    private val _isCompleteConfirmProfile = MutableLiveData<Event<Boolean>>()
    val isCompleteConfirmProfile: LiveData<Event<Boolean>> get() = _isCompleteConfirmProfile

    fun confirmProfile(uploadImage: Uri, uploadGender: String, uploadName: String) {
        viewModelScope.launch {
            userRepository.apply {
                setUserPicture(uploadImage)
                setUserGender(uploadGender)
                setUserName(uploadName)
            }

            _isCompleteConfirmProfile.value = Event(true)
        }
    }
}