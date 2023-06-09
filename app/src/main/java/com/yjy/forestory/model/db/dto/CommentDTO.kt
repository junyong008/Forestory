package com.yjy.forestory.model.db.dto

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Comment")
data class CommentDTO(
    val postId: Int,
    val writerName: String,
    val writerPicture: Uri,
    val content: String
) {
    @PrimaryKey(autoGenerate = true)
    var commentId: Int? = null
}