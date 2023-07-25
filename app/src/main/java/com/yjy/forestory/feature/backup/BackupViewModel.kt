package com.yjy.forestory.feature.backup

import androidx.lifecycle.*
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.yjy.forestory.model.repository.PostWithTagsAndCommentsRepository
import com.yjy.forestory.model.repository.SettingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val postWithTagsAndCommentsRepository: PostWithTagsAndCommentsRepository,
    private val settingRepository: SettingRepository
) : ViewModel() {

    // 백업 혹은 복원이 진행중인지
    val isBackupProgress = settingRepository.getIsBackupInProgress().asLiveData()
    val isRestoreProgress = settingRepository.getIsRestoreInProgress().asLiveData()

    val isBackupOrRestoreInProgress = MediatorLiveData<Boolean>().apply {
        addSource(isBackupProgress) { value = it == true || (isRestoreProgress.value ?: false) }
        addSource(isRestoreProgress) { value = it == true || (isBackupProgress.value ?: false) }
    }

    // 구글 드라이브 계정 정보
    private val _account = MutableLiveData<GoogleSignInAccount?>()
    val account: LiveData<GoogleSignInAccount?> get() = _account

    fun setAccount(account: GoogleSignInAccount?) {
        _account.value = account
    }

    // 게시글 중에서 댓글을 추가하고 있는지 확인하여 복원시 충돌 방지
    suspend fun getIsAddingCommentsExist(): Boolean {
        return postWithTagsAndCommentsRepository.getIsAddingCommentsExist()
    }
}