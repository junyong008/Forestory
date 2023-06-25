package com.yjy.forestory.feature.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.yjy.forestory.R
import com.yjy.forestory.databinding.ActivityMainBinding
import com.yjy.forestory.feature.addPost.AddPostActivity
import com.yjy.forestory.feature.searchPost.SearchActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.lifecycleOwner = this@MainActivity

        setOnClickListener()
        setViewPager()
        setTabLayout()
    }

    private fun setOnClickListener() {
        // 게시글 추가 버튼 클릭
        binding.ibuttonAddPost.setOnClickListener {
            val intent = Intent(this, AddPostActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_up, R.anim.stay)
        }

        // 검색 버튼 클릭
        binding.ibuttonSearch.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.stay)
        }
    }

    private fun setViewPager() {
        viewPager = binding.viewPager

        val pagerAdapter = MainViewPagerAdapter(this)
        viewPager.isUserInputEnabled = false // Swipe로 이동하는거 막기
        viewPager.adapter = pagerAdapter
    }

    private fun setTabLayout() {
        tabLayout = binding.tabLayout

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.icon = getDrawable(R.drawable.ic_postlist)
                1 -> tab.icon = getDrawable(R.drawable.ic_postlist_grid)
            }
        }.attach()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
}