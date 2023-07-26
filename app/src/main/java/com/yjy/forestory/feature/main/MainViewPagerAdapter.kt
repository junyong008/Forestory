package com.yjy.forestory.feature.main

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.yjy.forestory.feature.viewPost.GridPostListFragment
import com.yjy.forestory.feature.viewPost.LinearPostListFragment

class MainViewPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> LinearPostListFragment()
            1 -> GridPostListFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}
