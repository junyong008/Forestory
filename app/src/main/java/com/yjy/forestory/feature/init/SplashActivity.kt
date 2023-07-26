package com.yjy.forestory.feature.init

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.yjy.forestory.R
import com.yjy.forestory.feature.main.MainActivity
import com.yjy.forestory.feature.screenLock.InputPasswdActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity: AppCompatActivity() {

    private val splashViewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()

        lifecycleScope.launch {

            // 유저 프로필 정보가 모두 입력되지 않았다면 최초 실행으로 간주, 이동
            if (!splashViewModel.getIsUserProfileValid()) {
                val intent = Intent(this@SplashActivity, FirstStartActivity::class.java)
                startActivity(intent)
                finish()
                return@launch
            }


            // 어플 실행 비밀번호, 생체인증이 있다면 인증 요구
            val checkPasswordMode: Int? =
            if (splashViewModel.getIsBioPasswordExist() == true) {
                InputPasswdActivity.CHECK_BIO_PASSWORD
            } else if (splashViewModel.getIsPasswordExist()) {
                InputPasswdActivity.CHECK_PASSWORD
            } else {
                null
            }

            checkPasswordMode?.let {
                val intent = Intent(this@SplashActivity, InputPasswdActivity::class.java)
                intent.putExtra("mode", it)
                startActivity(intent)
                finish()
                return@launch
            }


            // 메인 액티비티 실행
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.stay)
            finish()
        }
    }
}