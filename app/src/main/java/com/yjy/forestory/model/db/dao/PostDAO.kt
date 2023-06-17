package com.yjy.forestory.model.db.dao

import androidx.room.*
import com.yjy.forestory.model.db.dto.PostDTO
import com.yjy.forestory.model.db.dto.PostWithComments
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDAO {
    @Transaction
    @Query("SELECT * FROM Post ORDER BY createDate DESC")
    fun getAllPosts(): Flow<List<PostWithComments>> // Flow 를 사용함으로써 테이블의 데이터가 변경되면 쿼리를 실행해 결과를 반환한다.


    @Delete
    suspend fun delete(post: PostDTO)

    @Query("UPDATE Post SET isAddingComments = :value WHERE postId = :postId OR :postId IS NULL")
    suspend fun updateTempColumn(value: Int, postId: Int? = null)


    @Insert(onConflict = OnConflictStrategy.IGNORE) // 중복되는 값은 무시
    suspend fun insert(post: PostDTO) // suspend로 코루틴에서 비동기로 실행
}