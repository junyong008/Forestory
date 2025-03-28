package com.yjy.forestory.model.repository

import android.net.Uri
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.yjy.forestory.model.PostWithTagsAndComments
import com.yjy.forestory.model.Tag
import com.yjy.forestory.model.db.dao.PostWithTagsAndCommentsDao
import com.yjy.forestory.model.db.entity.CommentEntity
import com.yjy.forestory.model.db.entity.PostEntity
import com.yjy.forestory.model.network.RetrofitClient
import com.yjy.forestory.model.network.dto.CommentDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.util.Date

class PostWithTagsAndCommentsRepositoryImpl(private val postWithTagsAndCommentsDao: PostWithTagsAndCommentsDao):
    PostWithTagsAndCommentsRepository {

    // Post
    override fun getPostCount(keyword: String?): Flow<Int> {
        return postWithTagsAndCommentsDao.getPostCount(keyword)
    }

    override fun getPostCountByTag(keytag: String?): Flow<Int> {
        return postWithTagsAndCommentsDao.getPostCountByTag(keytag)
    }

    @WorkerThread
    override suspend fun updatePostIsAddingComments(value: Int, postId: Int?) {
        postWithTagsAndCommentsDao.updatePostIsAddingComments(value, postId)
    }

    override suspend fun getIsAddingCommentsExist(): Boolean {
        return postWithTagsAndCommentsDao.isAddingCommentsExist()
    }



    // Comment
    @WorkerThread
    override suspend fun addComments(parentPostId: Int?, writerName: String?, writerGender: String?, postContent: String?, language: String?, postImage: MultipartBody.Part?): Int {
        if (parentPostId == null || writerName == null || writerGender == null || postContent == null || language == null || postImage == null) {
            return 0
        }

        try {
            val commentDtoList: List<CommentDto> = RetrofitClient.commentApi.getComments(
                writerName.toRequestBody("text/plain".toMediaTypeOrNull()),
                writerGender.toRequestBody("text/plain".toMediaTypeOrNull()),
                postContent.toRequestBody("text/plain".toMediaTypeOrNull()),
                language.toRequestBody("text/plain".toMediaTypeOrNull()),
                postImage
            )

            if (commentDtoList.isEmpty()) {
                return 404
            }

            val commentEntityList: List<CommentEntity> = commentDtoList.map { commentDto ->
                commentDto.toCommentEntity(parentPostId)
            }

            postWithTagsAndCommentsDao.insertCommentList(commentEntityList)
            return 200
        } catch (e: HttpException) {
            Log.d("AddComments", "HttpException: $e")
            e.printStackTrace()

            return e.code()
        } catch (e: Exception) {
            Log.d("AddComments", "Exception: $e")
            e.printStackTrace()

            return 500
        }
    }

    @WorkerThread
    override suspend fun getTagList(keyword: String): List<Tag> {
        return postWithTagsAndCommentsDao.getTagList(keyword).map { it.toTag() }
    }

    override fun getPostWithTagsAndCommentsList(keyword: String?): Flow<PagingData<PostWithTagsAndComments>> {
        return Pager(
            config = PagingConfig(
                pageSize = 15,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                postWithTagsAndCommentsDao.getPostWithTagsAndCommentsList(keyword)
            }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toPostWithTagsAndComments() }
        }
    }

    override fun getPostWithTagsAndCommentsListByTag(keytag: String?): Flow<PagingData<PostWithTagsAndComments>> {
        return Pager(
            config = PagingConfig(
                pageSize = 15,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                postWithTagsAndCommentsDao.getPostWithTagsAndCommentsListByTag(keytag)
            }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toPostWithTagsAndComments() }
        }
    }

    override fun getPostWithTagsAndComments(postId: Int): Flow<PostWithTagsAndComments?> {
        return postWithTagsAndCommentsDao.getPostWithTagsAndComments(postId).map { it.toPostWithTagsAndComments() }
    }

    @WorkerThread
    override suspend fun insertPostWithTags(userName: String?, userGender: String?, userPicture: Uri?, postImage: Uri?, postContent: String?, tagList: List<String>?): Boolean {

        // null 체크를 Repository에서 진행함으로써 중복 코드를 줄이고 뷰모델에서의 호출할때 자연스러운 코드 흐름으로 가독성을 가진다
        if (userName == null || userGender == null || userPicture == null || postImage == null || postContent == null) {
            return false
        }

        // List<String>을 Repository에서 List<TagEntity>로 변환하지 않는 이유 : postId가 필요하며 데이터의 원자성을 보장하기 위해. 게시글을 추가하다 강제 종료되면 태그가 입력 안되는 등의 오류를 방지하기 위함
        val postEntity = PostEntity(userName, userGender, userPicture, postImage, postContent, Date())
        postWithTagsAndCommentsDao.insertPostWithTags(postEntity, tagList)
        return true
    }

    @WorkerThread
    override suspend fun deletePostWithTagsAndComments(postWithTagsAndComments: PostWithTagsAndComments?): Boolean {

        if (postWithTagsAndComments == null) {
            return false
        }

        postWithTagsAndCommentsDao.deletePostWithTagsAndComments(postWithTagsAndComments.toPostWithTagsAndCommentsEntity())
        return true
    }
}

interface PostWithTagsAndCommentsRepository {
    // Post
    fun getPostCount(keyword: String? = null): Flow<Int>
    fun getPostCountByTag(keytag: String?): Flow<Int>
    suspend fun updatePostIsAddingComments(value: Int, postId: Int? = null)
    suspend fun getIsAddingCommentsExist(): Boolean

    // Comment
    suspend fun addComments(parentPostId: Int?, writerName: String?, writerGender: String?, postContent: String?, language: String?, postImage: MultipartBody.Part?): Int

    // Tag
    suspend fun getTagList(keyword: String): List<Tag>

    // Transaction
    fun getPostWithTagsAndCommentsList(keyword: String? = null): Flow<PagingData<PostWithTagsAndComments>>
    fun getPostWithTagsAndCommentsListByTag(keytag: String?): Flow<PagingData<PostWithTagsAndComments>>
    fun getPostWithTagsAndComments(postId: Int): Flow<PostWithTagsAndComments?>
    suspend fun insertPostWithTags(userName: String?, userGender: String?, userPicture: Uri?, postImage: Uri?, postContent: String?, tagList: List<String>?): Boolean
    suspend fun deletePostWithTagsAndComments(postWithTagsAndComments: PostWithTagsAndComments?): Boolean
}