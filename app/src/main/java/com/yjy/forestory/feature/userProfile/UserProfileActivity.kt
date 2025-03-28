package com.yjy.forestory.feature.userProfile

import EventObserver
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.github.logansdk.permission.PermissionManager
import com.yjy.forestory.Const.GENDER_FEMALE
import com.yjy.forestory.Const.GENDER_MALE
import com.yjy.forestory.Const.PRIVACY_POLICY_URL
import com.yjy.forestory.R
import com.yjy.forestory.base.BaseActivity
import com.yjy.forestory.databinding.ActivityUserProfileBinding
import com.yjy.forestory.feature.crop.CropActivity
import com.yjy.forestory.feature.crop.CropActivity.Companion.EXTRA_CROPPED_URI
import com.yjy.forestory.feature.main.MainActivity
import com.yjy.forestory.util.CameraGalleryDialog
import com.yjy.forestory.util.CameraGalleryDialogInterface
import com.yjy.forestory.util.ImageUtils
import com.yjy.forestory.util.getParcelableCompat
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
            if (isFirstSet) {
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            } else {
                overridePendingTransition(R.anim.stay, R.anim.fade_out)
            }
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        isFirstSet = intent.getBooleanExtra("isFirstSet", false)

        if (isFirstSet) {
            val spannable = SpannableString(getString(R.string.click_confirm_to_agree_privacy_policy))
            val highlightText = getString(R.string.highlight_privacy_policy)

            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))
                    startActivity(browserIntent)
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = true
                    ds.color = ContextCompat.getColor(this@UserProfileActivity, R.color.green)
                }
            }

            val start = spannable.indexOf(highlightText)
            val end = start + highlightText.length
            spannable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            binding.textViewSighupInfo.apply {
                text = spannable
                movementMethod = LinkMovementMethod.getInstance()
                isVisible = true
            }
        }
    }

    override fun setListener() {
        binding.ibuttonClose.setOnClickListener {
            onBackPressedCallback.handleOnBackPressed()
        }

        binding.circleImageViewAddPhoto.setOnClickListener {
            CameraGalleryDialog().show(supportFragmentManager, CameraGalleryDialog.TAG)
        }

        binding.chipMale.setOnClickListener {
            userProfileViewModel.setCurrentGender(GENDER_MALE)
        }

        binding.chipFemale.setOnClickListener {
            userProfileViewModel.setCurrentGender(GENDER_FEMALE)
        }

        binding.buttonConfirm.setOnClickListener {
            var uploadImage: Uri? = userProfileViewModel.currentUserPicture.value

            uploadImage = if (uploadImage == null) {
                val defaultUserImage: Uri = Uri.parse("android.resource://$packageName/${R.drawable.ic_user}")
                ImageUtils.saveUserProfileToInternalStorage(this, defaultUserImage)
            } else {
                ImageUtils.saveUserProfileToInternalStorage(this, userProfileViewModel.currentUserPicture.value!!)
            }

            val uploadGender = userProfileViewModel.currentUserGender.value
            val uploadName = userProfileViewModel.currentUserName.value

            if (uploadImage != null && uploadGender != null && uploadName != null) {
                userProfileViewModel.confirmProfile(uploadImage, uploadGender, uploadName)
            }
        }
    }

    override fun onCameraClick() {
        val permissions = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
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
                val cropOptions = CropImageOptions().apply {
                    initialCropWindowPaddingRatio = 0f
                    outputCompressQuality = 100
                    guidelines = CropImageView.Guidelines.ON
                    cropShape = CropImageView.CropShape.OVAL
                    aspectRatioX = 1
                    aspectRatioY = 1
                    fixAspectRatio = true
                }

                val cropIntent = Intent(this, CropActivity::class.java).apply {
                    putExtra(CropActivity.EXTRA_IMAGE_URI, it)
                    putExtra(CropActivity.EXTRA_CROP_OPTIONS, cropOptions)
                }

                cropImageLauncher.launch(cropIntent)
            }
        }
    }

    private val cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.takeIf { it.resultCode == Activity.RESULT_OK }
            ?.data
            ?.getParcelableCompat<Uri>(EXTRA_CROPPED_URI)
            ?.let(userProfileViewModel::setCurrentPicture)
    }

    override fun setObserver() {
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
        userProfileViewModel.isCompleteConfirmProfile.observe(this, EventObserver {
            if (isFirstSet) {
                val intent = Intent(this@UserProfileActivity, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.stay)
            } else {
                showToast(getString(R.string.profile_settings_completed), R.style.successToast)
                onBackPressedCallback.handleOnBackPressed()
            }
        })
    }
}