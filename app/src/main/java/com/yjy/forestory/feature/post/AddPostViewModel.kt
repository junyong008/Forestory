package com.yjy.forestory.feature.post

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResult
import androidx.lifecycle.*
import com.theartofdev.edmodo.cropper.CropImage
import com.yjy.forestory.R
import com.yjy.forestory.model.db.dto.PostDTO
import com.yjy.forestory.repository.PostRepository
import com.yjy.forestory.util.Event
import com.yjy.forestory.util.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*
import javax.inject.Inject


@HiltViewModel
class AddPostViewModel @Inject constructor(
    private val imageUtils: ImageUtils,
    private val postRepository: PostRepository
) : ViewModel() {

    companion object { const val MAX_TAG_COUNT = 10 }

    // ---------------------------------- 토스트 메시지 설정
    private val _showToast = MutableLiveData<Event<Boolean>>()
    val showToast: LiveData<Event<Boolean>> get() = _showToast

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    private val _toastIcon = MutableLiveData<Int>()
    val toastIcon: LiveData<Int> get() = _toastIcon

    private fun setToastMsg(icon: Int, msg: String) {
        _toastIcon.value = icon
        _toastMessage.value = msg
        _showToast.value = Event(true)
    }

    // ---------------------------------- 로딩 여부 설정
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading



    // ---------------------------------- 사진 추가 or 삭제 처리
    private val _addPhoto = MutableLiveData<Event<Boolean>>()
    val addPhoto: LiveData<Event<Boolean>> get() = _addPhoto

    private val _deletePhoto = MutableLiveData<Event<Boolean>>()
    val deletePhoto: LiveData<Event<Boolean>> get() = _deletePhoto

    fun addPhoto() {
        // 만약 현재 사진이 있다면, 삭제할 건지 물어보는 dialog 띄우기
        if (_currentPhoto.value != null) {
            _deletePhoto.value = Event(true)
        } else {
            _addPhoto.value = Event(true)
        }
    }

    fun deletePhoto() {
        _currentPhoto.value = null
    }

    // ---------------------------------- 사진 추가 or 삭제 처리[1] : 카메라를 키기 위한 권한 요청 결과 및 이미지 생성 처리
    private val _takePhotoUri = MutableLiveData<Uri?>()

    private fun createTakePhotoIntent(): Intent? {
        _takePhotoUri.value = try {
            imageUtils.createTempImageFile()
        } catch (ex: IOException) {
            null
        }

        var intent: Intent? = null
        _takePhotoUri.value?.let { imageUri ->
            intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            }
        }

        return intent
    }

    private val _openCamera = MutableLiveData<Event<Boolean>>()
    val openCamera: LiveData<Event<Boolean>> get() = _openCamera

    private val _cameraIntent = MutableLiveData<Intent?>()
    val cameraIntent: LiveData<Intent?> get() = _cameraIntent

    fun checkPermission(permissions: Array<String>, granted: ArrayList<String>) {
        if (granted.size == permissions.size) {
            _cameraIntent.value = createTakePhotoIntent()
            _cameraIntent.value?.let {
                _openCamera.value = Event(true)
            }

        } else {
            setToastMsg(R.style.errorToast, "카메라 접근 권한이 거부되었습니다")
        }
    }


    // ---------------------------------- 사진 추가 or 삭제 처리[2] : 카메라 or 갤러리 로부터 결괏값을 받아 Crop처리에 필요한 이미지 Uri 변경
    private val _openCrop = MutableLiveData<Event<Boolean>>()
    val openCrop: LiveData<Event<Boolean>> get() = _openCrop

    private val _cropUri = MutableLiveData<Uri>()
    val cropUri: LiveData<Uri> get() = _cropUri

    fun checkSelectedPhoto(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            var imageUri: Uri?

            // 카메라로 촬영했는지, 갤러리에서 받아왔는지에 따라 Uri를 추출하는 방식이 다름
            if (data?.data != null) {
                imageUri = data.data as Uri
            } else {
                imageUri = _takePhotoUri.value
            }

            imageUri?.let {
                _cropUri.value = it
                _openCrop.value = Event(true)
            }
        }
    }

    // ---------------------------------- 사진 추가 or 삭제 처리[3] : Crop된 이미지를 받아 현재 사진의 Uri 변경
    private val _currentPhoto = MutableLiveData<Uri?>(null)
    val currentPhoto: LiveData<Uri?> get() = _currentPhoto

    fun checkCropppedPhoto(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {

            CropImage.getActivityResult(result.data).uri?.let {
                _currentPhoto.value = it
            }
        }
    }




    // ---------------------------------- 태그 입출력
    val tagList = MutableLiveData<List<String>>()

    private val _tagText = MutableLiveData<String>()
    val tagText: LiveData<String> get() = _tagText

    fun onTagTextChanged(text: CharSequence) {
        if (text.isNotEmpty() && (text.endsWith(" ") || text.endsWith("\n"))) {
            var inputText = text.removeSuffix("\n").trim()

            _tagText.value = ""

            tagList.value?.let {
                if (it.size >= MAX_TAG_COUNT) {
                    setToastMsg(R.style.errorToast, "태그는 최대 ${MAX_TAG_COUNT}개 까지 추가 가능합니다.")
                    return
                }
            }

            if (inputText.isNotEmpty()) {
                addChip(inputText.toString())
            }
        }
    }

    private fun addChip(inputText: String) {
        val currentList = tagList.value.orEmpty().toMutableList()
        currentList.add(inputText)
        tagList.value = currentList
    }


    // ---------------------------------- 내용 양방향 바인딩
    val contentText = MutableLiveData<String>()


    // ---------------------------------- 게시글 추가

    val isReadyToPost = MediatorLiveData(false)

    init {
        // _currentPhoto와 contentText가 변경될때마다 checkReadyToPost() 가 호출되어 isReadyToPost의 상태를 검증한다.
        // "isReadyToPost.value =" 로 갱신하지 않으면 addSource한 데이터의 Value가 저장된다.
        isReadyToPost.addSource(_currentPhoto) { isReadyToPost.value = checkReadyToPost() }
        isReadyToPost.addSource(contentText) { isReadyToPost.value = checkReadyToPost() }
    }

    private fun checkReadyToPost(): Boolean {
        val isCurrentPhotoNull: Boolean = _currentPhoto.value == null
        val isContentTextNull: Boolean = contentText.value.isNullOrEmpty()

        // 모든게 null이 아닐때만 true를 반환
        return when {
            !isCurrentPhotoNull && !isContentTextNull -> true
            else -> false
        }
    }



    private val _isCompleteInsert = MutableLiveData<Event<Boolean>>()
    val isCompleteInsert: LiveData<Event<Boolean>> get() = _isCompleteInsert

    fun addPost() {
        _isLoading.value = true
        viewModelScope.launch {

            // Crop된 이미지를 내부 저장소에 복사 후 Uri를 추출해 해당 이미지 경로를 DB에 저장. Crop된 이미지는 어플 내부 cache에 저장되는 임시 파일임.
            // Bitmap -> ByteArray로 저장하는 방식은 용량이 조금만 커도 OOM 발생하므로 폐지.
            val uploadImage: Uri? = imageUtils.copyImageToInternalStorage(_currentPhoto.value!!)

            if (uploadImage != null) {
                val post = PostDTO(uploadImage!!, contentText.value!!, tagList.value, Date())
                postRepository.insert(post)

                setToastMsg(R.style.successToast, "게시글이 추가됐습니다.")
                _isLoading.value = false
                _isCompleteInsert.value = Event(true)
            } else {
                setToastMsg(R.style.errorToast, "이미지 업로드 실패")
                _isLoading.value = false
            }
        }
    }
}