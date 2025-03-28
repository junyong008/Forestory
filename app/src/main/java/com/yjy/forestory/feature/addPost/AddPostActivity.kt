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
import com.bumptech.glide.Glide
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.github.logansdk.permission.PermissionManager
import com.google.android.material.chip.Chip
import com.yjy.forestory.R
import com.yjy.forestory.base.BaseActivity
import com.yjy.forestory.databinding.ActivityAddPostBinding
import com.yjy.forestory.feature.crop.CropActivity
import com.yjy.forestory.feature.crop.CropActivity.Companion.EXTRA_CROPPED_URI
import com.yjy.forestory.feature.dialog.ConfirmDialog
import com.yjy.forestory.feature.dialog.ConfirmDialogInterface
import com.yjy.forestory.util.CameraGalleryDialog
import com.yjy.forestory.util.CameraGalleryDialogInterface
import com.yjy.forestory.util.ImageUtils
import com.yjy.forestory.util.LoadingDialog
import com.yjy.forestory.util.getParcelableCompat
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class AddPostActivity: BaseActivity<ActivityAddPostBinding>(R.layout.activity_add_post),
    CameraGalleryDialogInterface, ConfirmDialogInterface {

    private val addPostViewModel: AddPostViewModel by viewModels()
    private var loadingDialog: LoadingDialog? = null
    private var tempCameraUri: Uri? = null

    override fun initViewModel() {
        binding.addPostViewModel = addPostViewModel
    }

    override val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
            overridePendingTransition(R.anim.stay, R.anim.slide_out_down)
        }
    }

    override fun setListener() {
        binding.ibuttonClose.setOnClickListener {
            onBackPressedCallback.handleOnBackPressed()
        }

        binding.ibuttonAddPhoto.setOnClickListener {
            if (addPostViewModel.currentPhoto.value != null) {
                ConfirmDialog.newInstance(getString(R.string.confirm_delete_photo), CONFIRM_DIALOG_CODE_DELETE_PHOTO).show(supportFragmentManager, ConfirmDialog.TAG)
            } else {
                CameraGalleryDialog().show(supportFragmentManager, CameraGalleryDialog.TAG)
            }
        }

        binding.buttonAddpost.setOnClickListener {
            val uploadImage = ImageUtils.copyImageToInternalStorage(this, addPostViewModel.currentPhoto.value!!)
            val uploadContent = addPostViewModel.contentText.value
            val uploadTags = addPostViewModel.tagList.value

            if (uploadImage != null && uploadContent != null) {
                addPostViewModel.addPost(uploadImage, uploadContent, uploadTags)
            }
        }

        binding.editTag.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(text: CharSequence, start: Int, before: Int, count: Int) {
                if (text.isNotEmpty() && (text.endsWith(" ") || text.endsWith("\n"))) {
                    val inputText = text.removeSuffix("\n").trim().toString()

                    binding.editTag.setText("")

                    addPostViewModel.tagList.value?.let { tagList ->
                        if (tagList.size >= addPostViewModel.maxTagCount) {
                            showToast(getString(R.string.max_tag_count_info, addPostViewModel.maxTagCount), R.style.errorToast)
                            return
                        }
                        if (addPostViewModel.containsTag(inputText)) {
                            showToast(getString(R.string.duplicate_tag_exist), R.style.errorToast)
                            return
                        }
                    }

                    if (inputText.isNotEmpty()) {
                        addPostViewModel.addTag(inputText)
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onConfirmClick(dialogId: Int) {
        when (dialogId) {
            CONFIRM_DIALOG_CODE_DELETE_PHOTO -> {
                addPostViewModel.setCurrentPhoto(null)
            }
        }
    }

    override fun onCameraClick() {
        val permissions = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.CAMERA)
        } else {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        PermissionManager.with(this@AddPostActivity, permissions).check { granted, _, _ ->
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

            if (data?.data != null) {
                resultUri = data.data as Uri
            } else if (tempCameraUri != null) {
                resultUri = tempCameraUri
                tempCameraUri = null
            }

            resultUri?.let {
                val cropOptions = CropImageOptions().apply {
                    initialCropWindowPaddingRatio = 0f
                    outputCompressQuality = 100
                    guidelines = CropImageView.Guidelines.ON
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
            ?.let(addPostViewModel::setCurrentPhoto)
    }

    override fun setObserver() {
        addPostViewModel.currentPhoto.observe(this) { inputUri ->
            val ibuttonAddPhoto = binding.ibuttonAddPhoto
            if (inputUri != null) {
                Glide.with(this)
                    .load(inputUri)
                    .centerCrop()
                    .into(ibuttonAddPhoto)
            } else {
                ibuttonAddPhoto.setImageResource(R.drawable.ic_addphoto)
            }
        }

        addPostViewModel.tagList.observe(this) { chipTexts ->
            val chipGroup = binding.chipgroupTags
            chipGroup.removeAllViews()
            chipTexts?.let {
                for (chipText in chipTexts) {
                    val newChip = LayoutInflater.from(chipGroup.context)
                        .inflate(R.layout.item_chip, chipGroup, false) as Chip
                    newChip.id = ViewCompat.generateViewId()
                    newChip.text = chipText
                    newChip.setOnCloseIconClickListener {
                        addPostViewModel.removeTag(chipText)
                    }

                    chipGroup.addView(newChip)
                }
            }
        }

        addPostViewModel.isLoading.observe(this) { isLoading ->
            loadingDialog?.dismiss()

            if (isLoading) {
                loadingDialog = LoadingDialog().also {
                    it.show(this.supportFragmentManager, LoadingDialog.TAG)
                }
            }
        }
    }

    override fun setEventObserver() {
        addPostViewModel.isCompleteInsert.observe(this, EventObserver { result ->
            if (result) {
                showToast(getString(R.string.post_added), R.style.successToast)
                onBackPressedCallback.handleOnBackPressed()
            } else {
                showToast(getString(R.string.post_upload_failed), R.style.errorToast)
            }
        })
    }

    companion object {
        private const val CONFIRM_DIALOG_CODE_DELETE_PHOTO = 0
    }
}