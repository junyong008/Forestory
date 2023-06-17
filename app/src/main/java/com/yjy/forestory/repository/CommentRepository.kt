package com.yjy.forestory.repository

import android.net.Uri
import androidx.annotation.WorkerThread
import com.yjy.forestory.R
import com.yjy.forestory.model.db.dao.CommentDAO
import com.yjy.forestory.model.db.dto.CommentDTO
import com.yjy.forestory.model.network.RetrofitClient
import com.yjy.forestory.model.network.response.CommentResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

class CommentRepositoryImpl(private val commentDao: CommentDAO): CommentRepository {

    // 서버로부터 인공지능 댓글을 생성해 Comment DB에 저장.
    @WorkerThread
    override suspend fun addComments(parentPostId: Int?, writerName: String?, writerGender: String?, postContent: String?, postImage: MultipartBody.Part?): Int {

        if (parentPostId == null || writerName == null || writerGender == null || postContent == null || postImage == null) {
            return 0
        }

        try {
            // Call 객체를 따로 사용하지 않은 이유
            // 1. 중간에 요청 취소를 제공하지 않는다
            // 2. 애초에 Repository 함수를 코루틴 비동기로 시행하기에 enqueue 를 이용한 비동기 시행이 필요 없다
            // 3. try - catch HttpException으로 요청 실패를 충분히 핸들링 할 수 있다


            // 이미지와 그 외 여러가지를 동시에 보내기에 @Multipart로 구현. 아니라면 @Body로 합치거나 @Field로 구현해도 된다
            val comments: List<CommentResponse> =
                RetrofitClient.commentApi.getComments(
                    writerName.toRequestBody("text/plain".toMediaTypeOrNull()),
                    writerGender.toRequestBody("text/plain".toMediaTypeOrNull()),
                    postContent.toRequestBody("text/plain".toMediaTypeOrNull()),
                    postImage
                )

            if (comments.isEmpty()) {
                return 404
            }

            for (comment in comments) {

                // 이름에 따라 아이콘 별도 적용
                val resourceId = when (comment.name) {
                    "아지" -> R.drawable.ic_dog
                    "코코" -> R.drawable.ic_cat
                    "터미" -> R.drawable.ic_bear
                    "울프" -> R.drawable.ic_wolf
                    "미니" -> R.drawable.ic_fox
                    "콩이" -> R.drawable.ic_rabbit
                    "랑이" -> R.drawable.ic_tiger
                    else -> R.drawable.ic_panda
                }
                val writerPicture = Uri.parse("android.resource://com.yjy.forestory/${resourceId}")

                val commentDto = CommentDTO(parentPostId, comment.name, writerPicture, comment.content)
                commentDao.insert(commentDto)
                //println("댓글 작성자: ${comment.name}, 내용: ${comment.content}")
            }

            return 200

        } catch (e: HttpException) {
            e.printStackTrace()

            return e.code()
        }
    }

    @WorkerThread
    override suspend fun deleteComments(comments: List<CommentDTO>?): Boolean {

        if (comments == null) {
            return false
        }

        commentDao.deleteList(comments)
        return true
    }
}

interface CommentRepository {
    suspend fun addComments(parentPostId: Int?, writerName: String?, writerGender: String?, postContent: String?, postImage: MultipartBody.Part?): Int
    suspend fun deleteComments(comments: List<CommentDTO>?): Boolean
}