package com.yjy.forestory.util

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.yjy.forestory.R

class LoadingDialog: DialogFragment() {

    companion object {
        const val TAG = "LoadingDialog"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // LoadingDialog는 단순히 보여주기만 위함이므로 굳이 데이터바인딩은 미사용
        val view = inflater.inflate(R.layout.dialog_loading, container, false)

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // 배경 투명화
        dialog?.setCancelable(false) // 배경 클릭해도 닫히지 않게

        return view
    }
}