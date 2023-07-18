package com.yjy.forestory.feature.setting

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.yjy.forestory.R
import com.yjy.forestory.base.BaseActivity
import com.yjy.forestory.databinding.ActivityLanguageSettingBinding
import com.yjy.forestory.feature.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LanguageSettingActivity: BaseActivity<ActivityLanguageSettingBinding>(R.layout.activity_language_setting) {

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
            val radioButton = when(mainViewModel.getCurrentLanguage()) {
                "ko" -> R.id.radio_kor
                "en-US" -> R.id.radio_eng
                else -> R.id.radio_kor
            }
            binding.radioGroup.check(radioButton)
        }
    }

    override fun setListener() {

        // 뒤로가기 버튼 클릭
        binding.ibuttonClose.setOnClickListener {
            onBackPressedCallback.handleOnBackPressed()
        }

        // 선택지에 따라 언어 변경
        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_kor -> changeLanguage("ko")
                R.id.radio_eng -> changeLanguage("en-US")
            }
        }
    }

    private fun changeLanguage(language: String) {
        mainViewModel.setLanguage(language)

        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(language)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
}