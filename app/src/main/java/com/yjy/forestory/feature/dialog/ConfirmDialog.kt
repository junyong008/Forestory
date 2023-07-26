package com.yjy.forestory.util

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.yjy.forestory.R
import com.yjy.forestory.databinding.DialogConfirmBinding

class ConfirmDialog: DialogFragment() {

    private lateinit var binding: DialogConfirmBinding
    private lateinit var confirmDialogInterface: ConfirmDialogInterface
    private lateinit var informText: String
    private var dialogId: Int = 0

    companion object {
        private const val ARG_DIALOG_INFORM_TEXT = "dialog_inform_text"
        private const val ARG_DIALOG_ID = "dialog_id"
        const val TAG = "ConfirmDialog"

        // Fragment는 기본 생성자로 생성하지 않으면 Unable to instantiate fragment 오류가 난다. Fragment는 무조건 기본 생성자로 생성하고, 파라미터는 다음과 같이 넘겨준다.
        fun newInstance(informText: String, dialogId: Int): ConfirmDialog {

            // arguments에 생성시 받은 정보들을 저장했다가, Configuration Change가 발생하면 곧바로 이전 arguments를 조회해 불러온다.
            // 다만, 버튼 클릭 이벤트 구현을 위한 interface 같은 경우는 부모를 interface로 사용하고 부모는 interface를 상속받아 버튼 클릭 이벤트를 구현한다.
            // 안내 메시지, 한 부모에서 두개 이상의 동일한 dialog 형식의 클릭 이벤트를 처리하기 위해 만든 dialogId 같은 경우는 그대로 arguments에 저장했다 사용한다.
            val args = Bundle().apply {
                putString(ARG_DIALOG_INFORM_TEXT, informText)
                putInt(ARG_DIALOG_ID, dialogId)
            }

            val dialog = ConfirmDialog()
            dialog.arguments = args
            return dialog
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ConfirmDialogInterface) {
            confirmDialogInterface = context
        }
    }

    // 만약 기존 arguments에 정보들이 있다면 이어 받는다.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            informText = it.getString(ARG_DIALOG_INFORM_TEXT) ?: ""
            dialogId = it.getInt(ARG_DIALOG_ID)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_confirm, container, false)

        // 레이아웃 배경을 투명화
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // 안내 메시지 설정
        binding.textViewInform.text = informText

        // 확인 버튼 클릭
        binding.buttonConfirm.setOnClickListener {
            this.confirmDialogInterface.onConfirmClick(dialogId)
            dismiss()
        }

        // 취소 버튼 클릭
        binding.buttonCancel.setOnClickListener {
            dismiss()
        }

        return binding.root
    }
}

interface ConfirmDialogInterface {
    fun onConfirmClick(dialogId: Int)
}