package com.yjy.forestory.feature.viewPost

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yjy.forestory.R
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

        val parentPostId = inputData.getInt(PARENT_POST_ID_KEY, 0)

        try {
            // 해당 게시글의 댓글을 현재 추가중임을 DB에 저장하여 명시
            postWithTagsAndCommentsRepository.updatePostIsAddingComments(1, parentPostId)

            // 백그라운드 작업이 시스템에 의해 중지되지 않도록 중요한 작업임을 정의.
            // Progress Notification으로 작업이 처리중임을 알림. 알림 권한이 거부되었으면 알림이 뜨지 않음. 그래도 작업은 시행됨
            setForeground(NotificationHelper.createGettingCommentsForegroundInfo(applicationContext, parentPostId))


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
            val responseCode = postWithTagsAndCommentsRepository.addComments(parentPostId, writerName, writerGender, postContent, language, postImage)

            return handlePostOutcome(responseCode < 300, parentPostId)
        } catch (e: Exception) {
            return handlePostOutcome(false, parentPostId)
        }
    }

    private suspend fun handlePostOutcome(isSuccessful: Boolean, parentPostId: Int): Result {
        // 처리가 완료됐음을 DB에 명시
        postWithTagsAndCommentsRepository.updatePostIsAddingComments(0, parentPostId)

        val (notificationTitle, notificationContent) = if (isSuccessful) {
            Pair(applicationContext.getString(R.string.noti_title_new_comment),
                applicationContext.getString(R.string.noti_content_new_comment))
        } else {
            Pair(applicationContext.getString(R.string.noti_title_failure),
                applicationContext.getString(R.string.noti_content_failure))
        }

        val isNotificationOn = settingRepository.getIsNotificationOn().firstOrNull()
        if (isNotificationOn == true) {
            sendNewCommentNotification(applicationContext, parentPostId, notificationTitle, notificationContent)
        }

        return if (isSuccessful) Result.success() else Result.failure()
    }
}