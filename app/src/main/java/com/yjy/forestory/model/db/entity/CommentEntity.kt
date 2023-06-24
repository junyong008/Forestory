package com.yjy.forestory.model.db.entity

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yjy.forestory.model.Comment

@Entity(tableName = "Comment")
data class CommentEntity(
    val postId: Int,
    val writerName: String,
    val writerPicture: Uri,
    val content: String
) {
    @PrimaryKey(autoGenerate = true)
    var commentId: Int? = null

    fun toComment(): Comment = Comment(commentId!!, postId, writerName, writerPicture, content)
}