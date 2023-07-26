package com.yjy.forestory.feature.setting

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.yjy.forestory.R
import com.yjy.forestory.base.BaseActivity
import com.yjy.forestory.databinding.ActivityThemeSettingBinding
import com.yjy.forestory.feature.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ThemeSettingActivity: BaseActivity<ActivityThemeSettingBinding>(R.layout.activity_theme_setting) {

    private val mainViewModel: MainViewModel by viewModels()

    override val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
            overridePendingTransition(R.anim.stay, R.anim.fade_out)
        }
    }

    override fun initView(savedInstanceState: Bundle?) {

        // 현재 설정값에 따른 뷰 초기화
        lifecycleScope.launch {
            val radioButton = when(mainViewModel.getCurrentTheme()) {
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> R.id.radio_system
                AppCompatDelegate.MODE_NIGHT_NO -> R.id.radio_light
                AppCompatDelegate.MODE_NIGHT_YES -> R.id.radio_dark
                else -> R.id.radio_system
            }
            binding.radioGroup.check(radioButton)
        }
    }

    override fun setListener() {

        // 뒤로가기 버튼 클릭
        binding.ibuttonClose.setOnClickListener {
            onBackPressedCallback.handleOnBackPressed()
        }

        // 선택지에 따라 테마 변경
        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.radio_system -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                R.id.radio_light -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.radio_dark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            mainViewModel.setTheme(theme)
            AppCompatDelegate.setDefaultNightMode(theme)
        }
    }
}