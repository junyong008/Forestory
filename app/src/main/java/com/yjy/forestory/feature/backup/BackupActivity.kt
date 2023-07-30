package com.yjy.forestory.feature.backup

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.material.snackbar.Snackbar
import com.yjy.forestory.R
import com.yjy.forestory.base.BaseActivity
import com.yjy.forestory.databinding.ActivityBackupBinding
import com.yjy.forestory.feature.dialog.ConfirmDialog
import com.yjy.forestory.feature.dialog.ConfirmDialogInterface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BackupActivity: BaseActivity<ActivityBackupBinding>(R.layout.activity_backup),
    ConfirmDialogInterface {

    companion object {
        private const val CONFIRM_DIALOG_CODE_UNLINK = 0
        private const val CONFIRM_DIALOG_CODE_BACKUP = 1
        private const val CONFIRM_DIALOG_CODE_RESTORE = 2
    }

    private val backupViewModel: BackupViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient

    override val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {

            // 백업 혹은 복원이 진행중이라면 나가지 못하게 하기. 처리 도중 댓글을 불러온다거나 게시글을 삭제하는 등 예외 방지
            if (backupViewModel.isBackupOrRestoreInProgress.value == true) {
                Snackbar.make(binding.root, getString(R.string.can_not_exit_while_backup_restore), Snackbar.LENGTH_SHORT).show()
                return
            }

            finish()
            overridePendingTransition(R.anim.stay, R.anim.fade_out)
        }
    }

    override fun initView(savedInstanceState: Bundle?) {

        // 구글 드라이브 Client API 초기화
        googleSignInClient = initializeGoogleDriveClient()

        // 기존 연동 계정 가져오기
        val account = GoogleSignIn.getLastSignedInAccount(this)
        backupViewModel.setAccount(account)
    }

    private fun initializeGoogleDriveClient(): GoogleSignInClient {

        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
            .build()

        return GoogleSignIn.getClient(this, googleSignInOptions)
    }


    override fun setListener() {

        // 뒤로가기 버튼 클릭
        binding.ibuttonClose.setOnClickListener {
            onBackPressedCallback.handleOnBackPressed()
        }

        // 연동 버튼 클릭
        binding.buttonLinkage.setOnClickListener {
            link()
        }

        // 연동 해제 버튼 클릭
        binding.buttonAccountSignOut.setOnClickListener {
            unlink()
        }

        // 백업 버튼 클릭 : 백그라운드로 백업 작업
        binding.menuBackUp.setOnClickListener {
            lifecycleScope.launch {

                if (backupViewModel.getIsAddingCommentsExist()) {
                    showToast(getString(R.string.notify_post_exist_shared), R.style.errorToast)
                } else {
                    ConfirmDialog.newInstance(getString(R.string.confirm_want_to_backup), CONFIRM_DIALOG_CODE_BACKUP).show(supportFragmentManager, ConfirmDialog.TAG)
                }
            }
        }

        // 복원 버튼 클릭 : 백그라운드로 복원 작업
        binding.menuRestore.setOnClickListener {
            lifecycleScope.launch {

                if (backupViewModel.getIsAddingCommentsExist()) {
                    showToast(getString(R.string.notify_post_exist_shared), R.style.errorToast)
                } else {
                    ConfirmDialog.newInstance(getString(R.string.confirm_want_to_restore), CONFIRM_DIALOG_CODE_RESTORE).show(supportFragmentManager, ConfirmDialog.TAG)
                }
            }
        }
    }

    override fun setObserver() {

        // 구글 드라이브 계정
        backupViewModel.account.observe(this) { account ->
            // 공통적으로 변하는 부분 처리
            binding.layoutAccountInfo.isVisible = (account != null)
            binding.buttonLinkage.isVisible = (account == null)

            // View의 활성화 및 비활성화 처리
            val isClickable = (account != null)
            binding.menuBackUp.isClickable = isClickable
            binding.menuRestore.isClickable = isClickable
            binding.textViewBackUp.isEnabled = isClickable
            binding.textViewDownload.isEnabled = isClickable

            // 계정 정보가 있는 경우
            account?.let {

                // email과 photo를 설정
                binding.textViewAccountEmail.text = it.email
                Glide.with(this).load(it.photoUrl).into(binding.circleImageViewAccountPicture)
            }
        }

        // 백업 혹은 복원이 진행중인지에 따라서 뷰 변화
        backupViewModel.isBackupOrRestoreInProgress.observe(this) { inProgress ->
            inProgress?.let {
                binding.buttonAccountSignOut.isEnabled = !inProgress
                binding.layoutProgress.isVisible = inProgress
                binding.menuBackUp.visibility = if(inProgress) View.INVISIBLE else View.VISIBLE
                binding.menuRestore.visibility = if(inProgress) View.INVISIBLE else View.VISIBLE
            }
        }
        backupViewModel.isBackupProgress.observe(this) { inProgress ->
            if (inProgress == true) {
                binding.textViewInfoProgress.text = getString(R.string.noti_title_backup)
            }
        }
        backupViewModel.isRestoreProgress.observe(this) { inProgress ->
            if (inProgress == true) {
                binding.textViewInfoProgress.text = getString(R.string.noti_title_restore)
            }
        }
    }



    // 구글 드라이브와 계정 연동
    private fun link() {
        val signInIntent = googleSignInClient.signInIntent
        driveSignInResultLauncher.launch(signInIntent)
    }

    // 구글 드라이브 연동 결과 도착
    private val driveSignInResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {

            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                // 로그인 성공
                val account = task.getResult(ApiException::class.java)
                backupViewModel.setAccount(account)
                showToast(getString(R.string.connected_google_drive), R.style.successToast)
            } catch (e: ApiException) {
                // 로그인 실패
                showToast(getString(R.string.fail_connect_google_drive), R.style.errorToast)
            }
        }
    }

    // 구글 드라이브 연동 해제
    private fun unlink() {
        ConfirmDialog.newInstance(getString(R.string.confirm_disconnect_account), CONFIRM_DIALOG_CODE_UNLINK).show(supportFragmentManager, ConfirmDialog.TAG)
    }


    override fun onConfirmClick(dialogId: Int) {
        when (dialogId) {
            // 연동 해제 여부 다이얼로그 결과 도착
            CONFIRM_DIALOG_CODE_UNLINK -> {

                googleSignInClient.signOut().addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // 로그아웃 성공
                        backupViewModel.setAccount(null)
                        showToast(getString(R.string.disconnected_google_drive), R.style.successToast)
                    } else {
                        // 로그아웃 실패
                        showToast(getString(R.string.fail_disconnect_google_drive), R.style.errorToast)
                    }
                }
            }

            // 백업 진행 여부 다이얼로그 결과 도착
            CONFIRM_DIALOG_CODE_BACKUP -> {

                val inputData = Data.Builder().putString("workType", "backup").build()

                val backupWorkRequest = OneTimeWorkRequestBuilder<BackupWorker>().setInputData(inputData).build()
                WorkManager.getInstance(this).enqueue(backupWorkRequest)

                Snackbar.make(binding.root, getString(R.string.backup_start), Snackbar.LENGTH_SHORT).show()
            }

            // 복원 진행 여부 다이얼로그 결과 도착
            CONFIRM_DIALOG_CODE_RESTORE -> {

                val inputData = Data.Builder().putString("workType", "restore").build()

                val backupWorkRequest = OneTimeWorkRequestBuilder<BackupWorker>().setInputData(inputData).build()
                WorkManager.getInstance(this).enqueue(backupWorkRequest)

                Snackbar.make(binding.root, getString(R.string.restore_start), Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}