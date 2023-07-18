package com.yjy.forestory.feature.main

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.yjy.forestory.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity: AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()


        lifecycleScope.launch {

            // 유저 프로필 정보가 모두 입력되지 않았다면 최초 실행으로 간주, 이동
            val intent = when {
                mainViewModel.getIsUserProfileValid() -> Intent(
                    this@SplashActivity,
                    MainActivity::class.java
                )
                else -> Intent(this@SplashActivity, FirstStartActivity::class.java)
            }

            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.stay)
            finish()
        }
    }
}