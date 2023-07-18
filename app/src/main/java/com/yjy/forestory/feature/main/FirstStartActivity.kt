package com.yjy.forestory.feature.main

import android.content.Intent
import com.yjy.forestory.R
import com.yjy.forestory.base.BaseActivity
import com.yjy.forestory.databinding.ActivityFirstStartBinding
import com.yjy.forestory.feature.setting.UserProfileActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FirstStartActivity: BaseActivity<ActivityFirstStartBinding>(R.layout.activity_first_start) {

    override fun setListener() {

       binding.buttonStart.setOnClickListener {
           val intent = Intent(this@FirstStartActivity, UserProfileActivity::class.java)
           intent.putExtra("isFirstSet", true)
           startActivity(intent)
           overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
       }
    }
}
