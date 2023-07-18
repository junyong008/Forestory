package com.yjy.forestory.feature.viewPost

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yjy.forestory.model.repository.PostWithTagsAndCommentsRepository
import com.yjy.forestory.model.repository.SettingRepository
import com.yjy.forestory.model.repository.UserRepository
import com.yjy.forestory.util.ImageUtils
import com.yjy.forestory.util.NotificationHelper
import com.yjy.forestory.util.NotificationHelper.sendNewCommentNotification
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull

@HiltWorker
class CommentWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val postWithTagsAndCommentsRepository: PostWithTagsAndCommentsRepository,
    private val userRepository: UserRepository,
    private val settingRepository: SettingRepository
): CoroutineWorker(context, params) {

    companion object {
        const val PARENT_POST_ID_KEY = "parent_post_id"
        const val POST_CONTENT_KEY = "post_content"
        const val POST_IMAGE_KEY = "post_image"
    }

    override suspend fun doWork(): Result {

        // 해당 게시글의 댓글을 현재 추가중임을 DB에 저장하여 명시
        val parentPostId = inputData.getInt(PARENT_POST_ID_KEY, 0)
        postWithTagsAndCommentsRepository.updatePostIsAddingComments(1, parentPostId)

        // 백그라운드 작업이 시스템에 의해 중지되지 않도록 중요한 작업임을 정의.
        // Progress Notification으로 작업이 처리중임을 알림. 알림 권한이 거부되었으면 알림이 뜨지 않음. 그래도 작업은 시행됨
        setForeground(NotificationHelper.createForegroundInfo(applicationContext, parentPostId))


        // 댓글을 불러오는데 필요한 요소들 정의
        val writerName = userRepository.getUserName().firstOrNull()
        val writerGender = userRepository.getUserGender().firstOrNull()
        val postContent = inputData.getString(POST_CONTENT_KEY)
        val postImage = Uri.parse(inputData.getString(POST_IMAGE_KEY)).let {
            ImageUtils.uriToMultipart(applicationContext, it)
        }
        val language = when (settingRepository.getLanguage().firstOrNull()) {
            "ko" -> "한국어"
            "en-US" -> "영어"
            else -> "한국어"
        }

        // 댓글을 불러오고 결과 통신 코드를 받아옴
        val responseCode: Int = postWithTagsAndCommentsRepository.addComments(parentPostId, writerName, writerGender, postContent, language, postImage)

        // 처리가 완료됐음을 DB에 명시
        postWithTagsAndCommentsRepository.updatePostIsAddingComments(0, parentPostId)

        return if (responseCode < 300) {

            // 성공적으로 댓글을 받아왔으면 Notification 으로 알림. 알림 설정이 켜져있을때만
            val isNotificationOn = settingRepository.getIsNotificationOn().firstOrNull()
            if (isNotificationOn == true) {
                sendNewCommentNotification(applicationContext, parentPostId)
            }

            Result.success()
        } else {
            Result.failure()
        }
    }
}