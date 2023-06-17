package com.yjy.forestory.model.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.yjy.forestory.model.db.dto.CommentDTO

@Dao
interface CommentDAO {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(comment: CommentDTO)

    @Delete
    suspend fun deleteList(comments: List<CommentDTO>)
}