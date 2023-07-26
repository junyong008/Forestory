package com.yjy.forestory.feature.screenLock

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.yjy.forestory.model.repository.SettingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScreenLockViewModel @Inject constructor(
    private val settingRepository: SettingRepository
) : ViewModel() {

    val password: LiveData<String?> = settingRepository.getPassword().asLiveData()
    val isBioPasswordOn: LiveData<Boolean?> = settingRepository.getIsBioPassword().asLiveData()

    fun deletePassword() {
        viewModelScope.launch {
            settingRepository.setPassword("")
        }
    }

    fun setBioPassword(isOn: Boolean) {
        viewModelScope.launch {
            settingRepository.setIsBioPassword(isOn)
        }
    }
}