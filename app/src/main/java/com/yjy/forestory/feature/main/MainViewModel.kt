package com.yjy.forestory.feature.main

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.yjy.forestory.model.repository.PostWithTagsAndCommentsRepository
import com.yjy.forestory.model.repository.SettingRepository
import com.yjy.forestory.model.repository.TicketRepository
import com.yjy.forestory.model.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val settingRepository: SettingRepository,
    private val ticketRepository: TicketRepository,
    private val postWithTagsAndCommentsRepository: PostWithTagsAndCommentsRepository
) : ViewModel() {

    // 사용자 정보 조회
    val userName: LiveData<String?> = userRepository.getUserName().asLiveData()
    val userPicture: LiveData<Uri?> = userRepository.getUserPicture().asLiveData()


    // 게시글 갯수 조회
    val postCount = postWithTagsAndCommentsRepository.getPostCount().asLiveData()


    // 티켓 갯수 조회 및 변경
    val tickets = ticketRepository.getTicket().asLiveData()
    val freeTickets = ticketRepository.getFreeTicket().asLiveData()

    suspend fun getCurrentTicket(): Int? {
        return ticketRepository.getTicket().firstOrNull()
    }
    suspend fun getCurrentFreeTicket(): Int? {
        return ticketRepository.getFreeTicket().firstOrNull()
    }
    fun setTicket(count: Int) {
        viewModelScope.launch {
            ticketRepository.setTicket(count)
        }
    }
    fun setFreeTicket(count: Int) {
        viewModelScope.launch {
            ticketRepository.setFreeTicket(count)
        }
    }

    // 메인 화면 인삿말 fold 여부
    private val _isTitleFolded = MutableLiveData<Boolean?>(null)
    val isTitleFolded: LiveData<Boolean?> get() = _isTitleFolded

    fun foldTitle() {
        _isTitleFolded.value = true
    }


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