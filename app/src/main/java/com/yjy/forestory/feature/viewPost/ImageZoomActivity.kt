package com.yjy.forestory.feature.viewPost

import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import com.yjy.forestory.R
import com.yjy.forestory.base.BaseActivity
import com.yjy.forestory.databinding.ActivityImageZoomBinding

class ImageZoomActivity: BaseActivity<ActivityImageZoomBinding>(R.layout.activity_image_zoom) {

    override fun initView(savedInstanceState: Bundle?) {

        // Uri를 받아서 photoView에 띄우기
        intent.getStringExtra("imageUri")?.let {
            binding.photoView.setImageURI(Uri.parse(it))
        }

        // 액션바의 제목 및 뒤로가기 버튼 설정
        supportActionBar?.setTitle(R.string.title_image_zoom)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    // 액션바의 뒤로 가기 버튼을 눌렀을 때의 동작 정의
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}