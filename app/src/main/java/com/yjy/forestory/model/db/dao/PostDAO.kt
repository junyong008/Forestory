package com.yjy.forestory.model.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yjy.forestory.model.db.dto.PostDTO
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDAO {
    @Query("SELECT * FROM Post")
    fun getAllPosts(): Flow<List<PostDTO>> // Flow 를 사용함으로써 테이블의 데이터가 변경되면 쿼리를 실행해 결과를 반환한다.

    @Insert(onConflict = OnConflictStrategy.IGNORE) // 중복되는 값은 무시
    suspend fun insert(post: PostDTO) // suspend로 코루틴에서 비동기로 실행
}