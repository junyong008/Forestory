package com.yjy.forestory.util

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.yjy.forestory.R
import com.yjy.forestory.databinding.DialogTicketBinding
import com.yjy.forestory.model.PostWithTagsAndComments

class TicketDialog: DialogFragment() {

    private lateinit var binding: DialogTicketBinding
    private lateinit var ticketDialogInterface: TicketDialogInterface
    private lateinit var postWithTagsAndComments: PostWithTagsAndComments
    private var ticketCount: Int = 0
    private var freeTicketCount: Int = 0

    companion object {
        const val TAG = "TicketDialog"

        fun newInstance(ticketCount: Int, freeTicketCount: Int, postWithTagsAndComments: PostWithTagsAndComments, listener: TicketDialogInterface): TicketDialog {
            val dialog = TicketDialog().apply {
                this.ticketCount = ticketCount
                this.freeTicketCount = freeTicketCount
                this.postWithTagsAndComments = postWithTagsAndComments
                this.ticketDialogInterface = listener
            }

            return dialog
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_ticket, container, false)

        // 레이아웃 배경을 투명화
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // 현재 티켓 갯수 표기
        binding.currentTicket.text = getString(R.string.tickets_count, ticketCount)

        // 안내 메시지 설정
        val isNeedToCharge = if (freeTicketCount > 0) {
            binding.buttonConfirm.text = getString(R.string.try_for_free, freeTicketCount)
            false
        } else if (ticketCount > 0) {
            binding.buttonConfirm.text = getString(R.string.deliver)
            false
        } else {
            binding.textViewQuestion.text = getString(R.string.not_enough_tickets)
            binding.currentTicket.text = getString(R.string.tickets_count, ticketCount)
            binding.buttonConfirm.text = getString(R.string.recharge)
            true
        }

        // 확인 버튼 클릭
        binding.buttonConfirm.setOnClickListener {
            this.ticketDialogInterface.onDeliverClick(isNeedToCharge, postWithTagsAndComments)
            dismiss()
        }

        // 취소 버튼 클릭
        binding.buttonCancel.setOnClickListener {
            dismiss()
        }

        return binding.root
    }

    // Configuration Change가 발생하면 그냥 닫히도록. PostWithTagsAndComments 객체를 받아 유지하기 까다로움. 약간의 트릭
    override fun onPause() {
        super.onPause()
        dismiss()
    }
}

interface TicketDialogInterface {
    fun onDeliverClick(isNeedToCharge: Boolean, postWithTagsAndComments: PostWithTagsAndComments)
}