package com.yjy.forestory.model.db.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.yjy.forestory.model.db.dto.PostDTO
import com.yjy.forestory.model.db.dto.PostWithComments
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDAO {
    @Transaction
    @Query("SELECT * FROM Post ORDER BY createDate DESC")
    fun getPosts(): PagingSource<Int, PostWithComments>

    @Transaction
    @Query("SELECT * FROM Post WHERE postId = :postId")
    fun getPost(postId: Int): Flow<PostWithComments>

    @Query("SELECT COUNT(*) FROM Post")
    fun getPostCount(): Flow<Int>

    @Delete
    suspend fun delete(post: PostDTO)

    @Query("UPDATE Post SET isAddingComments = :value WHERE postId = :postId OR :postId IS NULL")
    suspend fun updateTempColumn(value: Int, postId: Int? = null)

    @Insert(onConflict = OnConflictStrategy.IGNORE) // 중복되는 값은 무시
    suspend fun insert(post: PostDTO) // suspend로 코루틴에서 비동기로 실행
}