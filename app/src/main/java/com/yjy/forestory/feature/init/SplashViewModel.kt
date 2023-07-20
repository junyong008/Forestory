package com.yjy.forestory.feature.init

import androidx.lifecycle.ViewModel
import com.yjy.forestory.model.repository.SettingRepository
import com.yjy.forestory.model.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val settingRepository: SettingRepository
) : ViewModel() {

    // 유저 프로필이 유효한지 검사
    suspend fun getIsUserProfileValid(): Boolean {
        return ( userRepository.getUserName().firstOrNull() != null &&
                userRepository.getUserPicture().firstOrNull() != null &&
                userRepository.getUserGender().firstOrNull() != null)
    }

    // 어플 접속 비밀번호, 혹은 생체 인증이 있는지
    suspend fun getIsPasswordExist(): Boolean {
        return !settingRepository.getPassword().firstOrNull().isNullOrEmpty()
    }

    suspend fun getIsBioPasswordExist(): Boolean? {
        return settingRepository.getIsBioPassword().firstOrNull()
    }
}