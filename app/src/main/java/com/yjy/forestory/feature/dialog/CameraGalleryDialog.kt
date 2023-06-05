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
import com.yjy.forestory.databinding.DialogCameraGalleryBinding

class CameraGalleryDialog: DialogFragment() {

    private lateinit var binding: DialogCameraGalleryBinding
    private var cameraGalleryDialogInterface: CameraGalleryDialogInterface? = null

    companion object {
        const val TAG = "CameraGalleryDialog"
    }

    // Dialog 생성시 이벤트를 처리할 interface를 생성자로 받으면(물론 newInstance로), Configuration Change가 발생했을 시 받았던 interface가 Destroy 돼버린다. 처리가 불가능해진다.
    // 이걸 arguments로 저장했다가 onCreate시 interface 형식이기에 저장도 안되고, 과거 interface는 이미 Destroy 됐기에 안정적인 방식이 아니다.
    // 고로 다음과 같이 onAttach시 바로 부모의 context를 받아 interface로 사용하고, 부모에선 해당 dialog의 interface를 상속받아 클릭 이벤트를 구현한다.
    // Dialog가 띄워진 상태에서 Configuration Change가 발생하더라도 바로 부모를 interface로 새로 등록하기에 안정적이다.
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is CameraGalleryDialogInterface) {
            cameraGalleryDialogInterface = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_camera_gallery, container, false)

        // 레이아웃 배경을 투명화
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // 닫기 버튼 클릭
        binding.ibuttonClose.setOnClickListener {
            dismiss()
        }

        // 카메라 버튼 클릭
        binding.buttonCamera.setOnClickListener {
            this.cameraGalleryDialogInterface?.onCameraClick()
            dismiss()
        }

        // 갤러리 버튼 클릭
        binding.buttonGallery.setOnClickListener {
            this.cameraGalleryDialogInterface?.onGalleryClick()
            dismiss()
        }

        return binding.root
    }
}

interface CameraGalleryDialogInterface {
    fun onCameraClick()
    fun onGalleryClick()
}