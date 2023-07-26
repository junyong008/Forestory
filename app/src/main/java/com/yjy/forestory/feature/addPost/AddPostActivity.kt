package com.yjy.forestory.feature.addPost

import EventObserver
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.github.logansdk.permission.PermissionManager
import com.google.android.material.chip.Chip
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import com.yjy.forestory.R
import com.yjy.forestory.base.BaseActivity
import com.yjy.forestory.databinding.ActivityAddPostBinding
import com.yjy.forestory.util.*
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class AddPostActivity: BaseActivity<ActivityAddPostBinding>(R.layout.activity_add_post),
    CameraGalleryDialogInterface, ConfirmDialogInterface {


    // Class 상수 및 변수 선언
    companion object {
        private const val CONFIRM_DIALOG_CODE_DELETE_PHOTO = 0
    }

    private val addPostViewModel: AddPostViewModel by viewModels()
    private var loadingDialog: LoadingDialog? = null
    private var tempCameraUri: Uri? = null


    // 바인딩 뷰모델 설정
    override fun initViewModel() {
        binding.addPostViewModel = addPostViewModel
    }


    // 뒤로가기 커스텀
    override val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
            overridePendingTransition(R.anim.stay, R.anim.slide_out_down)
        }
    }


    // 리스너 초기화
    override fun setListener() {

        // 닫기버튼 클릭 리스너
        binding.ibuttonClose.setOnClickListener {
            onBackPressedCallback.handleOnBackPressed()
        }

        // 사진 추가 버튼 클릭 리스너
        binding.ibuttonAddPhoto.setOnClickListener {

            // 기존 사진이 존재한다면 삭제할건지 다이얼로그 제공 아니라면 카메라 or 갤러리 선택 다이얼로그 제공
            if (addPostViewModel.currentPhoto.value != null) {
                ConfirmDialog.newInstance(getString(R.string.confirm_delete_photo), CONFIRM_DIALOG_CODE_DELETE_PHOTO).show(supportFragmentManager, ConfirmDialog.TAG)
            } else {
                CameraGalleryDialog().show(supportFragmentManager, CameraGalleryDialog.TAG)
            }
        }

        // 게시글 작성 버튼 클릭 리스너
        binding.buttonAddpost.setOnClickListener {
            val uploadImage = ImageUtils.copyImageToInternalStorage(this, addPostViewModel.currentPhoto.value!!)
            val uploadContent = addPostViewModel.contentText.value
            val uploadTags = addPostViewModel.tagList.value

            if (uploadImage != null && uploadContent != null) {
                addPostViewModel.addPost(uploadImage, uploadContent, uploadTags)
            }
        }

        // 태그 edit 텍스트 변경 감지 리스너
        binding.editTag.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(text: CharSequence, start: Int, before: Int, count: Int) {
                if (text.isNotEmpty() && (text.endsWith(" ") || text.endsWith("\n"))) {
                    val inputText = text.removeSuffix("\n").trim()

                    binding.editTag.setText("")

                    addPostViewModel.tagList.value?.let { tagList ->
                        if (tagList.size >= addPostViewModel.maxTagCount) {
                            showToast(getString(R.string.max_tag_count_info, addPostViewModel.maxTagCount), R.style.errorToast)
                            return
                        }
                    }

                    if (inputText.isNotEmpty()) {
                        addPostViewModel.addTag(inputText.toString())
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
    }


    // 그 외 함수 및 CallBack
    override fun onConfirmClick(dialogId: Int) {
        when (dialogId) {
            CONFIRM_DIALOG_CODE_DELETE_PHOTO -> {
                addPostViewModel.setCurrentPhoto(null)
            }
        }
    }

    override fun onCameraClick() {

        // 안드로이드 10부터는 WRITE/READ 권한 요청 필요 없음
        val permissions =
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                arrayOf(Manifest.permission.CAMERA)
            } else {
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        PermissionManager.with(this@AddPostActivity, permissions).check { granted, denied, rejected ->

            if (granted.size == permissions.size) {
                tempCameraUri = ImageUtils.createTempImageFile(this)
                tempCameraUri?.let {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, it)
                    selectPhotoResultLauncher.launch(intent)
                }
            } else {
                showToast(getString(R.string.camera_permission_denied), R.style.errorToast)
            }
        }
    }
    override fun onGalleryClick() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        selectPhotoResultLauncher.launch(intent)
    }

    // 사진을 촬영하거나 갤러리에서 선택 된 후 결과 도착
    private val selectPhotoResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            var resultUri: Uri? = null

            // 카메라로 촬영했는지, 갤러리에서 받아왔는지에 따라 Uri를 추출하는 방식이 다름
            if (data?.data != null) {
                resultUri = data.data as Uri
            } else if (tempCameraUri != null) {
                resultUri = tempCameraUri
                tempCameraUri = null
            }

            // 정상적으로 촬영 or 선택된 이미지가 넘어왔으면 Crop 실행
            resultUri?.let {
                val intent = CropImage.activity(it)
                    .setInitialCropWindowPaddingRatio(0F) // 처음 Crop 사이즈 : 꽉 채우기
                    .setOutputCompressQuality(100) // 결과물 압축률 : 원본 유지
                    .setGuidelines(CropImageView.Guidelines.ON) // 가이드라인 : true
                    .getIntent(baseContext)
                cropPhotoResultLauncher.launch(intent)
            }
        }
    }

    // 사진 Crop 후 결과 도착
    private val cropPhotoResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            CropImage.getActivityResult(result.data).uri?.let {
                addPostViewModel.setCurrentPhoto(it)
            }
        }
    }


    // Observer 설정
    override fun setObserver() {

        // BindingAdapter를 이용해 바인딩 하지 않은 이유 : 추후 재사용이 불가능한 바인딩 형태라 판단하여 BindingAdapter의 부담을 덜기 위함.
        addPostViewModel.currentPhoto.observe(this, Observer { inputUri ->
            val ibuttonAddPhoto = binding.ibuttonAddPhoto
            if (inputUri != null) {
                Glide.with(this)
                    .load(inputUri)
                    .centerCrop()
                    .into(ibuttonAddPhoto)
            } else {
                ibuttonAddPhoto.setImageResource(R.drawable.ic_addphoto)
            }
        })

        addPostViewModel.tagList.observe(this, Observer { chipTexts ->
            val chipGroup = binding.chipgroupTags
            chipGroup.removeAllViews()
            chipTexts?.let {
                for (chipText in chipTexts) {
                    val newChip = LayoutInflater.from(chipGroup.context).inflate(R.layout.item_chip, chipGroup, false) as Chip
                    newChip.id = ViewCompat.generateViewId()
                    newChip.text = chipText
                    newChip.setOnCloseIconClickListener {
                        addPostViewModel.removeTag(chipText)
                    }

                    chipGroup.addView(newChip)
                }
            }
        })

        // 로딩 상태 확인하여 로딩 다이얼로그 띄우기
        addPostViewModel.isLoading.observe(this, Observer { isLoading ->
            loadingDialog?.dismiss()

            if (isLoading) {
                loadingDialog = LoadingDialog().also {
                    it.show(this.supportFragmentManager, LoadingDialog.TAG)
                }
            }
        })
    }


    // EventObserver 설정
    override fun setEventObserver() {

        // 게시글 추가 작업 완료 이벤트
        addPostViewModel.isCompleteInsert.observe(this, EventObserver { result ->

            if (result) {
                showToast(getString(R.string.post_added), R.style.successToast)
                onBackPressedCallback.handleOnBackPressed()
            } else {
                showToast(getString(R.string.post_upload_failed), R.style.errorToast)
            }
        })
    }
}