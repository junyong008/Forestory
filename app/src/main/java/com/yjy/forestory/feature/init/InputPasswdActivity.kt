package com.yjy.forestory.feature.init

import EventObserver
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.animation.CycleInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.yjy.forestory.R
import com.yjy.forestory.base.BaseActivity
import com.yjy.forestory.databinding.ActivityInputPasswdBinding
import com.yjy.forestory.feature.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InputPasswdActivity: BaseActivity<ActivityInputPasswdBinding>(R.layout.activity_input_passwd) {

    companion object {
        const val SET_NEW_PASSWORD = 0
        const val CHANGE_PASSWORD = 1
        const val CHECK_PASSWORD = 2
        const val CHECK_BIO_PASSWORD = 3
    }

    private val inputPasswordViewModel: InputPasswordViewModel by viewModels()
    private var mode: Int = SET_NEW_PASSWORD
    private val lightGray: Int by lazy { ContextCompat.getColor(this, R.color.lightgray) }
    private val green: Int by lazy { ContextCompat.getColor(this, R.color.green) }

    override fun initViewModel() {
        binding.inputPasswordViewModel = inputPasswordViewModel
    }

    override val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {

            // 비밀번호 확인창에서 뒤로가기는 무조건 모든 액티비티 종료
            if (mode == CHECK_BIO_PASSWORD || mode == CHECK_PASSWORD) {
                finishAffinity()
            } else {
                finish()
                overridePendingTransition(R.anim.stay, R.anim.fade_out)
            }
        }
    }

    override fun initView(savedInstanceState: Bundle?) {

        // 비밀번호 신규 설정, 비밀번호 재설정, 비밀번호 확인인지 구분
        mode = intent.getIntExtra("mode", SET_NEW_PASSWORD)
        binding.mode = mode

        // 각 모드에 따라 초기 동작 구분
        when(mode) {
            CHANGE_PASSWORD -> binding.textViewInfo.text = getString(R.string.enter_new_password)
            CHECK_BIO_PASSWORD ->  {
                checkBiometricAuthentication()
                binding.ibuttonClose.isVisible = false
            }
            CHECK_PASSWORD -> {
                binding.ibuttonClose.isVisible = false
            }
        }
    }

    // 생체 인증 요구. 인증 성공시 바로 메인 액티비티 이동
    private fun checkBiometricAuthentication() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.setting_screen_lock))
            .setSubtitle("")
            .setNegativeButtonText(getString(R.string.cancel))
            .build()

        val biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {

                // 생체 인증 성공시 바로 메인 액티비티 이동
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    startMainActivity()
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }

    override fun setListener() {

        // 뒤로가기 버튼 클릭
        binding.ibuttonClose.setOnClickListener {
            onBackPressedCallback.handleOnBackPressed()
        }
    }

    override fun setObserver() {

        // 비밀번호 입력에 따라 뷰 변경
        inputPasswordViewModel.passwordDigits.observe(this) { passwordDigits ->

            val digitViews = listOf(binding.digit1, binding.digit2, binding.digit3, binding.digit4)

            for ((index, view) in digitViews.withIndex()) {
                if (index < passwordDigits.size) {
                    view.backgroundTintList = ColorStateList.valueOf(green)
                } else {
                    view.backgroundTintList = ColorStateList.valueOf(lightGray)
                }
            }
        }

        // 1차 비밀번호가 입력돼 있는지에 따라서 안내 텍스트 변경
        inputPasswordViewModel.confirmPasswordDigits.observe(this) {

            val infoText = binding.textViewInfo

            if (!it.isNullOrEmpty()) {
                infoText.text = getString(R.string.enter_password_one_more)
            }
        }
    }

    override fun setEventObserver() {

        // 2차로 입력한 비밀번호가 1차로 입력한 비밀번호와 일치하는지에 따라 이벤트 ui 호출
        inputPasswordViewModel.isConfirmPasswordMatch.observe(this, EventObserver { isMatch ->
            if (isMatch) {
                showToast(getString(R.string.password_set_complete), R.style.successToast)
                onBackPressedCallback.handleOnBackPressed()
            } else {
                shakeView(binding.linearLayoutDigits)
                showToast(getString(R.string.password_unmatch), R.style.errorToast)
            }
        })

        // 비밀번호가 로컬에 저장된 비밀번호와 일치하는지
        inputPasswordViewModel.isPasswordMatch.observe(this, EventObserver { isMatch ->
            if (isMatch) {
                startMainActivity()
            } else {
                shakeView(binding.linearLayoutDigits)
                showToast(getString(R.string.password_unmatch), R.style.errorToast)
            }
        })
    }

    private fun shakeView(view: View) {
        val animator = ObjectAnimator.ofFloat(view, "translationX", -10f, 10f)
        animator.apply {
            repeatCount = 2 // 흔들리는 횟수
            duration = 100 // 애니메이션의 지속 시간 (밀리초)
            interpolator = CycleInterpolator(1f) // 흔들리는 패턴 지정
        }
        animator.start()
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.stay)
        finish()
    }
}