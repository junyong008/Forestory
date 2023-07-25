package com.yjy.forestory.feature.main

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.yjy.forestory.model.repository.PostWithTagsAndCommentsRepository
import com.yjy.forestory.model.repository.SettingRepository
import com.yjy.forestory.model.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val settingRepository: SettingRepository,
    private val postWithTagsAndCommentsRepository: PostWithTagsAndCommentsRepository
) : ViewModel() {

    // 사용자 정보 조회
    val userName: LiveData<String?> = userRepository.getUserName().asLiveData()
    val userPicture: LiveData<Uri?> = userRepository.getUserPicture().asLiveData()


    // 게시글 갯수 조회
    val postCount = postWithTagsAndCommentsRepository.getPostCount().asLiveData()


    // 데이터를 복원 혹은 백업 중인지
    suspend fun getIsBackupOrRestoreInProgress(): Boolean {
        return (settingRepository.getIsBackupInProgress().firstOrNull() == true || settingRepository.getIsRestoreInProgress().firstOrNull() == true)
    }


    // 어플 실행시 설정값에 따른 UI 업데이트를 위한 설정값 접근 함수
    suspend fun getCurrentTheme(): Int? {
        return settingRepository.getTheme().firstOrNull()
    }

    fun setTheme(mode: Int) {
        viewModelScope.launch {
            settingRepository.setTheme(mode)
        }
    }

    suspend fun getCurrentLanguage(): String? {
        return settingRepository.getLanguage().firstOrNull()
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            settingRepository.setLanguage(language)
        }
    }

    suspend fun getCurrentIsNotificationOn(): Boolean? {
        return settingRepository.getIsNotificationOn().firstOrNull()
    }

    fun setIsNotificationOn(IsOn: Boolean) {
        viewModelScope.launch {
            settingRepository.setIsNotificationOn(IsOn)
        }
    }
}