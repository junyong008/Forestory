package com.yjy.forestory.model.db.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.yjy.forestory.model.db.entity.CommentEntity
import com.yjy.forestory.model.db.entity.PostEntity
import com.yjy.forestory.model.db.entity.PostWithTagsAndCommentsEntity
import com.yjy.forestory.model.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostWithTagsAndCommentsDao {

    // Post 테이블 접근
    @Query("SELECT COUNT(*) FROM Post")
    fun getPostCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPost(postEntity: PostEntity): Long

    @Delete
    suspend fun deletePost(postEntity: PostEntity)

    @Query("UPDATE Post SET isAddingComments = :value WHERE postId = :postId OR :postId IS NULL")
    suspend fun updatePostIsAddingComments(value: Int, postId: Int? = null)


    // Comment 테이블 접근
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCommentList(commentList: List<CommentEntity>)

    @Delete
    suspend fun deleteCommentList(commentList: List<CommentEntity>)


    // Tag 테이블 접근
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTagList(tagEntityList: List<TagEntity>)

    @Delete
    suspend fun deleteTagList(tagEntityList: List<TagEntity>)




    // Transaction 접근 : 데이터의 원자성을 보장. 성공하면 모든 데이터를 정상적으로 처리한거고, 하나라도 실패하면 다 원복시킴
    @Transaction
    @Query("SELECT * FROM Post ORDER BY createDate DESC")
    fun getPostWithTagsAndCommentsList(): PagingSource<Int, PostWithTagsAndCommentsEntity>

    @Transaction
    @Query("SELECT * FROM Post WHERE postId = :postId")
    fun getPostWithTagsAndComments(postId: Int): Flow<PostWithTagsAndCommentsEntity>

    @Transaction
    suspend fun insertPostWithTags(post: PostEntity, tags: List<String>?) {
        val postId = insertPost(post)

        tags?.let {
            val tagEntityList = it.map { tagContent -> TagEntity(postId.toInt(), tagContent) }
            insertTagList(tagEntityList)
        }
    }

    @Delete
    suspend fun deletePostWithTagsAndComments(postWithTagsAndCommentsEntity: PostWithTagsAndCommentsEntity) {
        postWithTagsAndCommentsEntity.commentEntityList?.let { deleteCommentList(it) }
        postWithTagsAndCommentsEntity.tagEntityList?.let { deleteTagList(it) }
        deletePost(postWithTagsAndCommentsEntity.postEntity)
    }
}