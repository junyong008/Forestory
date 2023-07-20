package com.yjy.forestory.feature.init

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yjy.forestory.feature.init.InputPasswdActivity.Companion.CHECK_BIO_PASSWORD
import com.yjy.forestory.feature.init.InputPasswdActivity.Companion.CHECK_PASSWORD
import com.yjy.forestory.model.repository.SettingRepository
import com.yjy.forestory.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InputPasswordViewModel @Inject constructor(
    private val settingRepository: SettingRepository
) : ViewModel() {

    private val _passwordDigits = MutableLiveData<List<Int>>()
    val passwordDigits: LiveData<List<Int>> = _passwordDigits

    // 버퍼 변수
    private val _confirmPasswordDigits = MutableLiveData<List<Int>>()
    val confirmPasswordDigits: LiveData<List<Int>> = _confirmPasswordDigits


    // 비밀번호 신규 등록 or 변경 용 : 두번째 입력한 비밀번호가 기존에 입력한 비밀번호와 일치하는지
    private val _isConfirmPasswordMatch = MutableLiveData<Event<Boolean>>()
    val isConfirmPasswordMatch: LiveData<Event<Boolean>> = _isConfirmPasswordMatch

    // 비밀번호 일치 확인 용 : 입력한 비밀번호가 로컬에 저장된 비밀번호와 일치하는지
    private val _isPasswordMatch = MutableLiveData<Event<Boolean>>()
    val isPasswordMatch: LiveData<Event<Boolean>> = _isPasswordMatch


    init {
        _passwordDigits.value = emptyList()
        _confirmPasswordDigits.value = emptyList()
    }

    fun addDigit(mode: Int, digit: Int) {
        viewModelScope.launch {
            val currentDigits = _passwordDigits.value.orEmpty().toMutableList()
            val confirmDigits = _confirmPasswordDigits.value.orEmpty().toMutableList()

            if (currentDigits.size == 3 && confirmDigits.isNotEmpty()) {

                // 현 비밀번호가 4자리 꽉차고, 버퍼 변수에도 비밀번호가 있다면 둘이 일치 여부를 확인해서 비밀번호 신규 설정 or 변경
                currentDigits.add(digit)
                if (currentDigits == confirmDigits) {
                    val inputPassword: String = confirmDigits.joinToString()
                    settingRepository.setPassword(inputPassword)
                    _isConfirmPasswordMatch.value = Event(true)
                } else {
                    _passwordDigits.value = emptyList()
                    _isConfirmPasswordMatch.value = Event(false)
                }
            } else if (currentDigits.size == 3) {

                // 현 비밀번호가 4자리 꽉 찼을때
                currentDigits.add(digit)

                if (mode == CHECK_PASSWORD || mode == CHECK_BIO_PASSWORD) {

                    // mode가 비밀번호 일치 확인이라면 기존 저장된 비밀번호와의 일치 여부 확인
                    val inputPassword: String = currentDigits.joinToString()
                    val savedPassword = settingRepository.getPassword().firstOrNull()

                    if (inputPassword == savedPassword) {
                        _isPasswordMatch.value = Event(true)
                    } else {
                        _passwordDigits.value = emptyList()
                        _isPasswordMatch.value = Event(false)
                    }
                } else {

                    // 그 외 새로운 비밀번호 설정, 변경이라면 입력된 비밀번호를 일종의 버퍼 변수에 저장
                    _confirmPasswordDigits.value = currentDigits
                    _passwordDigits.value = emptyList()
                }
            } else {

                // 번호를 입력받아 적재
                currentDigits.add(digit)
                _passwordDigits.value = currentDigits
            }
        }
    }

    fun removeDigit() {
        val currentDigits = _passwordDigits.value.orEmpty().toMutableList()

        if (currentDigits.isNotEmpty() && currentDigits.size != 4) {
            currentDigits.removeAt(currentDigits.size - 1)
            _passwordDigits.value = currentDigits
        }
    }
}