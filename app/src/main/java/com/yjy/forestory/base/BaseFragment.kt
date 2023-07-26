package com.yjy.forestory.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import io.github.muddz.styleabletoast.StyleableToast

abstract class BaseFragment<T: ViewDataBinding>(@LayoutRes val layoutResId: Int): Fragment() {

    protected lateinit var binding: T
    private var mToast: StyleableToast? = null

    // 토스트 메시지
    protected fun showToast(message: String, iconResId: Int) {
        mToast?.cancel()
        mToast = StyleableToast.makeText(requireContext(), message, iconResId).apply { show() }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = DataBindingUtil.inflate(inflater, layoutResId, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        initViewModel()
        initView(savedInstanceState)
        setListener()
        setObserver()
        setEventObserver()

        return binding.root
    }

    protected open fun initViewModel() {}
    protected open fun initView(savedInstanceState: Bundle?) {}
    protected open fun setListener() {}
    protected open fun setObserver() {}
    protected open fun setEventObserver() {}
}