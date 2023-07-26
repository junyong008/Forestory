package com.yjy.forestory.base

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import io.github.muddz.styleabletoast.StyleableToast

abstract class BaseActivity<T: ViewDataBinding>(@LayoutRes val layoutResId: Int): AppCompatActivity() {

    protected lateinit var binding: T
    private var mToast: StyleableToast? = null

    // 토스트 메시지
    protected fun showToast(message: String, iconResId: Int) {
        mToast?.cancel()
        mToast = StyleableToast.makeText(this, message, iconResId).apply { show() }
    }

    // 시스템의 뒤로가기 버튼 눌렀을 때
    protected open val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, layoutResId)
        binding.lifecycleOwner = this

        // 뒤로가기 버튼 콜백 등록
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        initViewModel()
        initView(savedInstanceState)
        setListener()
        setObserver()
        setEventObserver()
    }

    protected open fun initViewModel() {}
    protected open fun initView(savedInstanceState: Bundle?) {}
    protected open fun setListener() {}
    protected open fun setObserver() {}
    protected open fun setEventObserver() {}
}