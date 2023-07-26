package com.yjy.forestory.feature.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.yjy.forestory.R
import com.yjy.forestory.model.repository.SettingRepository
import com.yjy.forestory.util.NotificationHelper
import com.yjy.forestory.util.NotificationHelper.sendBackupCompleteNotification
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import java.io.FileOutputStream
import java.util.*

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settingRepository: SettingRepository
): CoroutineWorker(context, params) {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleDriveService: Drive
    private val dbName = "forestory_database"
    private val dbPath by lazy { applicationContext.getDatabasePath(dbName).path }
    private val dbPathShm by lazy { "${applicationContext.getDatabasePath(dbName).path}-shm" }
    private val dbPathWal by lazy { "${applicationContext.getDatabasePath(dbName).path}-wal" }

    override suspend fun doWork(): Result {

        val workType = inputData.getString("workType")

        try {

            // 작업중 임을 저장
            when(workType) {
                "backup" -> settingRepository.setIsBackupInProgress(true)
                "restore" -> settingRepository.setIsRestoreInProgress(true)
            }

            // 구글 드라이브 Client API 초기화
            googleSignInClient = initializeGoogleDriveClient()

            // 기존 연동 계정 가져오기
            val account = GoogleSignIn.getLastSignedInAccount(applicationContext)

            // googleDriveService 설정
            val googleAccountCredential = GoogleAccountCredential.usingOAuth2(applicationContext, listOf(
                Scopes.DRIVE_FILE
            ))
            googleAccountCredential.selectedAccount = account?.account

            val transport = AndroidHttp.newCompatibleTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()

            googleDriveService = Drive.Builder(transport, jsonFactory, googleAccountCredential)
                .setApplicationName(applicationContext.getString(R.string.app_name))
                .build()

            // 작업 및 결과 핸들링
            when(workType) {
                "backup" -> {
                    // notificaion 설정
                    setForeground(NotificationHelper.createBackupForegroundInfo(applicationContext, true))

                    emptyDriveFolder()
                    imageBackUp()
                    dbBackUp()
                    return handleBackupOutcome(true)
                }
                "restore" -> {
                    // notificaion 설정
                    setForeground(NotificationHelper.createBackupForegroundInfo(applicationContext, false))

                    return handleRestoreOutcome(restoreAll())
                }
                else -> { return Result.failure() }
            }
        } catch (e: Exception) {
            return when(workType) {
                "backup" -> { handleBackupOutcome(false) }
                "restore" -> { handleRestoreOutcome(RestoreResult.Failure) }
                else -> { Result.failure() }
            }
        }
    }

    private suspend fun handleBackupOutcome(isSuccessful: Boolean): Result {

        // 백업이 끝났음을 명시
        settingRepository.setIsBackupInProgress(false)

        val notificationMessage = if (isSuccessful) {
            applicationContext.getString(R.string.success_backup)
        } else {
            applicationContext.getString(R.string.fail_backup)
        }

        val isNotificationOn = settingRepository.getIsNotificationOn().firstOrNull()
        if (isNotificationOn == true) {
            sendBackupCompleteNotification(applicationContext, notificationMessage)
        }

        return if (isSuccessful) Result.success() else Result.failure()
    }

    private suspend fun handleRestoreOutcome(restoreResult: RestoreResult): Result {

        // 복원이 끝났음을 명시
        settingRepository.setIsRestoreInProgress(false)

        val notificationMessage = when(restoreResult) {
            RestoreResult.Success -> applicationContext.getString(R.string.success_restore)
            RestoreResult.NoData -> applicationContext.getString(R.string.nodata_restore)
            else -> applicationContext.getString(R.string.fail_restore)
        }

        val isNotificationOn = settingRepository.getIsNotificationOn().firstOrNull()
        if (isNotificationOn == true) {
            sendBackupCompleteNotification(applicationContext, notificationMessage)
        }

        return if (restoreResult == RestoreResult.Success) Result.success() else Result.failure()
    }

    private fun initializeGoogleDriveClient(): GoogleSignInClient {

        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
            .build()

        return GoogleSignIn.getClient(applicationContext, googleSignInOptions)
    }

    // 구글 드라이브 안 파일들을 모두 삭제
    private fun emptyDriveFolder() {

        var pageToken: String? = null
        do {
            val result = googleDriveService.files().list()
                .setSpaces("appDataFolder")
                .setFields("nextPageToken, files(id, name)")
                .setPageSize(100)
                .setPageToken(pageToken)
                .execute()

            for (file in result.files) {
                googleDriveService.files().delete(file.id).execute()
            }

            pageToken = result.nextPageToken
        } while (pageToken != null)
    }

    // RoomDB 데이터를 구글 드라이브에 백업
    private fun dbBackUp() {

        // 구글 드라이브에 저장될 파일 메타데이터 설정
        val storageFile = File().apply {
            parents = Collections.singletonList("appDataFolder")
            name = "forestorydb"
        }
        val storageFileShm = File().apply {
            parents = Collections.singletonList("appDataFolder")
            name = "forestorydb-shm"
        }
        val storageFileWal = File().apply {
            parents = Collections.singletonList("appDataFolder")
            name = "forestorydb-wal"
        }

        // 로컬의 데이터베이스 파일 경로 설정
        val dbFilePath = java.io.File(dbPath)
        val dbShmFilePath = java.io.File(dbPathShm)
        val dbWalFilePath = java.io.File(dbPathWal)

        // 파일의 내용과 함께 FileContent 생성
        val mediaContent = FileContent("", dbFilePath)
        val mediaContentShm = FileContent("", dbShmFilePath)
        val mediaContentWal = FileContent("", dbWalFilePath)

        // 구글 드라이브 서비스를 이용해 파일 업로드
        googleDriveService.files().apply {
            create(storageFile, mediaContent).execute()
            create(storageFileShm, mediaContentShm).execute()
            create(storageFileWal, mediaContentWal).execute()
        }
    }

    // 어플 내부 저장소에 저장된 이미지 파일들을 백업
    private fun imageBackUp() {

        val internalFilesPath = applicationContext.filesDir.listFiles()

        internalFilesPath?.forEach { file ->

            val fileName = file.name

            // jpg 이미지 파일이 아니면 패스
            if (fileName.substringAfterLast(".") != "jpg") {
                return@forEach
            }

            // 구글 드라이브에 저장될 파일 메타데이터 설정
            val storageFile = File().apply {
                parents = Collections.singletonList("appDataFolder")
                name = fileName
            }

            // 로컬의 파일 경로 설정
            val filePath = java.io.File(applicationContext.filesDir, fileName)

            // 파일의 내용과 함께 FileContent 생성
            val mediaContent = FileContent("image/jpeg", filePath)

            googleDriveService.files().create(storageFile, mediaContent).execute() // 구글 드라이브 서비스를 이용해 파일 업로드
        }
    }

    // 드라이브에 저장된 DB 와 이미지를 모두 받아옴
    private fun restoreAll(): RestoreResult {

        // 구글 드라이브에 백업 파일이 존재하는지 확인
        val fileList = googleDriveService.files().list()
            .setSpaces("appDataFolder")
            .setFields("nextPageToken, files(id, name)")
            .setPageSize(10)
            .execute()

        // 존재하지 않으면 파일 없음 반환
        if (fileList.files.isEmpty()) {
            return RestoreResult.NoData
        }

        // 기존 로컬 DB를 삭제
        java.io.File(dbPath).listFiles()?.forEach { it.delete() }

        // 기존 로컬 사진들을 삭제 : 유저 프로필 사진은 제외
        applicationContext.filesDir.listFiles { _, name -> name.endsWith(".jpg") && !name.contains("Profile") }?.forEach { it.delete() }


        var pageToken: String? = null

        do {
            val result = googleDriveService.files().list()
                .setSpaces("appDataFolder")
                .setFields("nextPageToken, files(id, name)")
                .setPageSize(10)
                .setPageToken(pageToken)
                .execute()

            for (file in result.files) {
                println("Found file: ${file.name} (${file.id})")

                // 파일 복원
                when (file.name) {
                    "forestorydb" -> {
                        val outputStream = FileOutputStream(dbPath)
                        googleDriveService.files().get(file.id).executeMediaAndDownloadTo(outputStream)
                    }
                    "forestorydb-shm" -> {
                        val outputStream = FileOutputStream(dbPathShm)
                        googleDriveService.files().get(file.id).executeMediaAndDownloadTo(outputStream)
                    }
                    "forestorydb-wal" -> {
                        val outputStream = FileOutputStream(dbPathWal)
                        googleDriveService.files().get(file.id).executeMediaAndDownloadTo(outputStream)
                    }
                    else -> { // 이미지 파일 복원
                        if (file.name.substringAfterLast(".") == "jpg") {
                            val outputStream = FileOutputStream(java.io.File(applicationContext.filesDir, file.name))
                            googleDriveService.files().get(file.id).executeMediaAndDownloadTo(outputStream)
                        }
                    }
                }
            }

            pageToken = result.nextPageToken
        } while (pageToken != null)

        return RestoreResult.Success
    }

    sealed class RestoreResult {
        object Success: RestoreResult()
        object Failure: RestoreResult()
        object NoData: RestoreResult()
    }
}
