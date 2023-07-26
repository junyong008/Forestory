package com.yjy.forestory.feature.screenLock

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.core.view.isVisible
import com.yjy.forestory.R
import com.yjy.forestory.base.BaseActivity
import com.yjy.forestory.databinding.ActivityScreenLockSettingBinding
import com.yjy.forestory.feature.screenLock.InputPasswdActivity.Companion.CHANGE_PASSWORD
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScreenLockSettingActivity: BaseActivity<ActivityScreenLockSettingBinding>(R.layout.activity_screen_lock_setting) {

    private val screenLockViewModel: ScreenLockViewModel by viewModels()

    override val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
            overridePendingTransition(R.anim.stay, R.anim.fade_out)
        }
    }

    override fun initView(savedInstanceState: Bundle?) {

        // 객체의 상태가 액티비티 재생성에 의해 자동으로 저장되는 것을 막아 의도치 않은 동작을 방지
        binding.switchPassword.isSaveEnabled = false
        binding.switchBioPassword.isSaveEnabled = false

        // 생체 인증 가능 여부를 확인해서 제공
        if (!isBiometricAvailableForDevice()) {
            binding.imageViewBioError.isVisible = true
            binding.switchBioPassword.isVisible = false
        }
    }

    // 기기가 생체 인증을 사용할 수 있는지 확인
    private fun isBiometricAvailableForDevice(): Boolean {
        val biometricManager = BiometricManager.from(this)

        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    override fun setListener() {

        // 뒤로가기 버튼 클릭
        binding.ibuttonClose.setOnClickListener {
            onBackPressedCallback.handleOnBackPressed()
        }

        // 비밀번호 스위치 클릭
        binding.switchPassword.setOnClickListener {

            val switchPassword = binding.switchPassword
            val isChecked = switchPassword.isChecked

            if (isChecked && screenLockViewModel.password.value.isNullOrEmpty()) {
                // 비밀번호가 없다면 설정창으로 이동

                switchPassword.isChecked = false

                val intent = Intent(this@ScreenLockSettingActivity, InputPasswdActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.stay)
            } else if (!isChecked) {
                screenLockViewModel.deletePassword()
            }
        }

        // 비밀번호 재설정 클릭
        binding.menuChangePassword.setOnClickListener {
            val intent = Intent(this@ScreenLockSettingActivity, InputPasswdActivity::class.java)
            intent.putExtra("mode", CHANGE_PASSWORD)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.stay)
        }

        // 생체 인증 스위치 클릭
        binding.switchBioPassword.setOnClickListener {
            val isChecked = binding.switchBioPassword.isChecked
            screenLockViewModel.setBioPassword(isChecked)
        }
    }

    override fun setObserver() {

        // 비밀번호의 존재 유무에 따라 스위치 ON OFF, 비밀번호 재설정/생체 인증 버튼 시각화
        screenLockViewModel.password.observe(this) { password ->
            binding.switchPassword.isChecked = !password.isNullOrEmpty()
            binding.menuChangePassword.isVisible = !password.isNullOrEmpty()
            binding.menuBioPassword.isVisible = !password.isNullOrEmpty()

            // 비밀번호가 없으면 생체 인증도 OFF
            if (password.isNullOrEmpty()) {
                screenLockViewModel.setBioPassword(false)
            }
        }

        // 생체 인증 유무에 따라 스위치 ON OFF
        screenLockViewModel.isBioPasswordOn.observe(this) {
            it?.let { isOn ->
                binding.switchBioPassword.isChecked = isOn
            }
        }
    }
}