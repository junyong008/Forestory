package com.yjy.forestory.feature.setting

import EventObserver
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.github.logansdk.permission.PermissionManager
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import com.yjy.forestory.Const.GENDER_FEMALE
import com.yjy.forestory.Const.GENDER_MALE
import com.yjy.forestory.R
import com.yjy.forestory.base.BaseActivity
import com.yjy.forestory.databinding.ActivityUserProfileBinding
import com.yjy.forestory.util.CameraGalleryDialog
import com.yjy.forestory.util.CameraGalleryDialogInterface
import com.yjy.forestory.util.ImageUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserProfileActivity: BaseActivity<ActivityUserProfileBinding>(R.layout.activity_user_profile),
    CameraGalleryDialogInterface {

    private val userProfileViewModel: UserProfileViewModel by viewModels()
    private var tempCameraUri: Uri? = null
    private var isFirstSet: Boolean = false

    override fun initViewModel() {
        binding.userProfileViewModel = userProfileViewModel
    }

    override val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
            overridePendingTransition(R.anim.stay, R.anim.fade_out)
        }
    }

    override fun initView(savedInstanceState: Bundle?) {

        // 초기 설정인지 확인. 초기 설정이라면 확인버튼을 누를때 바로 MainActivity로 이동하게끔 한다
        isFirstSet = intent.getBooleanExtra("isFirstSet", false)
    }

    override fun setListener() {

        // 뒤로가기 버튼 클릭
        binding.ibuttonClose.setOnClickListener {
            onBackPressedCallback.handleOnBackPressed()
        }

        // 프로필 사진 편집
        binding.circleImageViewAddPhoto.setOnClickListener {
            CameraGalleryDialog().show(supportFragmentManager, CameraGalleryDialog.TAG)
        }

        // 남성 or 여성 선택
        binding.chipMale.setOnClickListener {
            userProfileViewModel.setCurrentGender(GENDER_MALE)
        }
        binding.chipFemale.setOnClickListener {
            userProfileViewModel.setCurrentGender(GENDER_FEMALE)
        }

        // 확인 버튼 클릭
        binding.buttonConfirm.setOnClickListener {

            var uploadImage: Uri? = userProfileViewModel.currentUserPicture.value

            // 프로필 사진을 설정하지 않았다면 기본 프로필 사진을 따로 저장하여 설정
            if (uploadImage == null) {
                val defaultUserImage: Uri = Uri.parse("android.resource://$packageName/${R.drawable.ic_user}")
                uploadImage = ImageUtils.copyImageToInternalStorage(this, defaultUserImage)
            }

            val uploadGender = userProfileViewModel.currentUserGender.value
            val uploadName = userProfileViewModel.currentUserName.value

            if (uploadImage != null && uploadGender != null && uploadName != null) {
                userProfileViewModel.confirmProfile(uploadImage, uploadGender, uploadName)
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

        PermissionManager.with(this, permissions).check { granted, _, _ ->

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
                    .setCropShape(CropImageView.CropShape.OVAL) // Crop 모양 : 원
                    .setAspectRatio(1, 1)
                    .getIntent(baseContext)
                cropPhotoResultLauncher.launch(intent)
            }
        }
    }

    // 사진 Crop 후 결과 도착
    private val cropPhotoResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            CropImage.getActivityResult(result.data).uri?.let {
                userProfileViewModel.setCurrentPicture(it)
            }
        }
    }

    override fun setObserver() {

        // 프로필 사진과 이름은 각각 바인딩 어댑터, 양방향 바인딩으로 연결됨. 성별만 따로 아래와 같이 연결

        // 현재 성별
        userProfileViewModel.currentUserGender.observe(this) { gender ->
            when(gender) {
                GENDER_MALE -> {
                    binding.chipMale.isSelected = true
                    binding.chipFemale.isSelected = false
                }
                GENDER_FEMALE -> {
                    binding.chipMale.isSelected = false
                    binding.chipFemale.isSelected = true
                }
            }
        }
    }

    override fun setEventObserver() {

        // 프로필 수정을 완료했을때
        userProfileViewModel.isCompleteConfirmProfile.observe(this, EventObserver {
            showToast(getString(R.string.profile_settings_completed), R.style.successToast)
            onBackPressedCallback.handleOnBackPressed()
        })
    }
}