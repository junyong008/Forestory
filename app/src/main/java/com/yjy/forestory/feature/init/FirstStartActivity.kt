package com.yjy.forestory.feature.init

import android.content.Intent
import android.os.Bundle
import com.yjy.forestory.R
import com.yjy.forestory.base.BaseActivity
import com.yjy.forestory.databinding.ActivityFirstStartBinding
import com.yjy.forestory.feature.userProfile.UserProfileActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FirstStartActivity: BaseActivity<ActivityFirstStartBinding>(R.layout.activity_first_start) {

    override fun initView(savedInstanceState: Bundle?) {

        // 간단한 애니메이션 효과
        val title = binding.textView
        val description = binding.textView2

        title.alpha = 0f
        description.alpha = 0f
        description.translationY = -100f

        val titleAnimationDuration = 1000L // 애니메이션의 지속 시간 (밀리초)
        val titleAnimationDelay = 100L // 애니메이션의 지연 시간 (밀리초)
        val descriptionAnimationDuration = 500L // 애니메이션의 지속 시간 (밀리초)
        val descriptionAnimationDelay = 0L // 애니메이션의 지연 시간 (밀리초)

        title.animate().alpha(1f).setDuration(titleAnimationDuration).setStartDelay(titleAnimationDelay).withEndAction {
            description.animate().alpha(1f).translationYBy(100f).setDuration(descriptionAnimationDuration).setStartDelay(descriptionAnimationDelay).start()
        }
    }

    override fun setListener() {

       binding.buttonStart.setOnClickListener {
           val intent = Intent(this@FirstStartActivity, UserProfileActivity::class.java)
           intent.putExtra("isFirstSet", true)
           startActivity(intent)
           overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
       }
    }
}
