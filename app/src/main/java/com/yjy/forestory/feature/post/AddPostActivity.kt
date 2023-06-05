package com.yjy.forestory.feature.post

import EventObserver
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.github.logansdk.permission.PermissionManager
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import com.yjy.forestory.R
import com.yjy.forestory.databinding.ActivityAddPostBinding
import com.yjy.forestory.util.*
import dagger.hilt.android.AndroidEntryPoint
import io.github.muddz.styleabletoast.StyleableToast


@AndroidEntryPoint
class AddPostActivity : AppCompatActivity(), CameraGalleryDialogInterface, ConfirmDialogInterface {

    private lateinit var binding: ActivityAddPostBinding
    private val addPostViewModel: AddPostViewModel by viewModels()

    private var mToast: StyleableToast? = null
    private val loadingDialog = LoadingDialog()

    companion object {
        private const val CONFIRM_DIALOG_CODE_DELETE_PHOTO = 0
    }

    // 시스템의 뒤로가기 버튼 눌렀을 때
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            setResult(Activity.RESULT_CANCELED)
            finish()
            overridePendingTransition(R.anim.stay, R.anim.slide_out_down)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_post)

        binding.addPostViewModel = addPostViewModel
        binding.lifecycleOwner = this@AddPostActivity

        // 뒤로가기 버튼 콜백 등록
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        setOnClickListener()
        setObserver()
        setEventObserver()
    }

    // ViewModel을 거치지 않고 바로 뷰의 변화를 필요로 하는 경우 아래와 같이 등록.
    // xml에서 onClick으로 바로 ViewModel의 함수를 연결하는것은 ViewModel을 거쳐 비지니스 로직을 처리해야 할때 그렇게 하는것.
    private fun setOnClickListener() {
        // 닫기버튼 클릭
        binding.ibuttonClose.setOnClickListener {
            onBackPressedCallback.handleOnBackPressed()
        }
    }

    private fun setObserver() {

        // BindingAdapter를 이용해 바인딩 하지 않은 이유 : 추후 재사용이 불가능한 바인딩 형태라 판단하여 BindingAdapter의 부담을 덜기 위함.
        addPostViewModel.currentPhoto.observe(this, Observer { inputUri ->
            if (inputUri != null) {
                binding.ibuttonAddPhoto.apply {
                    setPadding(0)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setImageURI(inputUri)
                }
            } else {
                binding.ibuttonAddPhoto.apply {
                    setPadding(500)
                    scaleType = ImageView.ScaleType.CENTER
                    setImageDrawable(ContextCompat.getDrawable(baseContext, R.drawable.ic_addphoto))
                }
            }
        })

        addPostViewModel.isLoading.observe(this, Observer { isLoading ->
            val checkPrevDialog = supportFragmentManager.findFragmentByTag(LoadingDialog.TAG) // 기존 Dialog가 없는데도 dismiss하면 IllegalStateException이 뜨므로, 이전 Dialog가 있는지 검사
            if (isLoading) {
                loadingDialog.show(this.supportFragmentManager, LoadingDialog.TAG)
            } else if (checkPrevDialog != null) {
                loadingDialog.dismiss()
            }
        })
    }

    private fun setEventObserver() {


        // 다이얼로그로 카메라 촬영 / 갤러리 선택 제공
        addPostViewModel.addPhoto.observe(this, EventObserver {
            val checkPrevDialog = supportFragmentManager.findFragmentByTag(CameraGalleryDialog.TAG) // 중복 방지로 이전에 생성된 DialogFragment가 없을때만 새로 생성
            if (checkPrevDialog == null) {
                val dialog = CameraGalleryDialog()
                dialog.show(supportFragmentManager, CameraGalleryDialog.TAG)
            }
        })

        // 다이얼로그로 사진 삭제할건지 선택 제공
        addPostViewModel.deletePhoto.observe(this, EventObserver {
            val checkPrevDialog = supportFragmentManager.findFragmentByTag(ConfirmDialog.TAG)
            if (checkPrevDialog == null) {
                val dialog = ConfirmDialog.newInstance("사진을 삭제하시겠습니까?", CONFIRM_DIALOG_CODE_DELETE_PHOTO)
                dialog.show(this.supportFragmentManager, ConfirmDialog.TAG)
            }
        })

        // 토스트 메시지 띄우기
        addPostViewModel.showToast.observe(this, EventObserver {
            mToast?.let { it.cancel() }

            val toastMessage = addPostViewModel.toastMessage.value
            val toastIcon = addPostViewModel.toastIcon.value ?: 0
            mToast = StyleableToast.makeText(this@AddPostActivity, toastMessage, toastIcon).also { it.show() }
        })

        // 카메라 촬영 실행
        addPostViewModel.openCamera.observe(this, EventObserver {
            selectPhotoResultLauncher.launch(addPostViewModel.cameraIntent.value)
        })

        // 받아온 이미지 Crop 실행
        addPostViewModel.openCrop.observe(this, EventObserver {
            addPostViewModel.cropUri.value?.let {
                val intent = CropImage.activity(it)
                    .setInitialCropWindowPaddingRatio(0F) // 처음 Crop 사이즈 : 꽉 채우기
                    .setOutputCompressQuality(100) // 결과물 압축률 : 원본 유지
                    .setGuidelines(CropImageView.Guidelines.ON) // 가이드라인 : true
                    .getIntent(baseContext)

                cropPhotoResultLauncher.launch(intent)
            }
        })

        // 게시글 추가 완료시
        addPostViewModel.isCompleteInsert.observe(this, EventObserver {
            onBackPressedCallback.handleOnBackPressed()
        })
    }

    override fun onCameraClick() {
        val permissions = arrayOf(Manifest.permission.CAMERA)
        PermissionManager.with(this@AddPostActivity, permissions).check { granted, denied, rejected ->
            addPostViewModel.checkPermission(
                permissions = permissions,
                granted = granted
            )
        }
    }
    override fun onGalleryClick() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        selectPhotoResultLauncher.launch(intent)
    }

    override fun onConfirmClick(dialogId: Int) {
        when (dialogId) {
            CONFIRM_DIALOG_CODE_DELETE_PHOTO -> {
                addPostViewModel.deletePhoto()
            }
        }
    }


    // 사진을 촬영하거나 갤러리에서 선택 된 후 결과 도착
    private val selectPhotoResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        addPostViewModel.checkSelectedPhoto(result)
    }

    // 사진 Crop 후 결과 도착
    private val cropPhotoResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        addPostViewModel.checkCropppedPhoto(result)
    }
}