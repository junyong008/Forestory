package com.yjy.forestory.feature.post

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GridPostListViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is asdsafd Fragment"
    }
    val text: LiveData<String> = _text
}