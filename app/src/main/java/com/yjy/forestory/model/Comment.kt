package com.yjy.forestory.model

import android.net.Uri
import com.yjy.forestory.model.db.entity.CommentEntity

data class Comment(
    val commentId: Int,
    val postId: Int,
    val writerName: String,
    val writerPicture: Uri,
    val content: String
) {
    fun toCommentEntity(): CommentEntity {
        val commentEntity = CommentEntity(postId, writerName, writerPicture, content)
        commentEntity.commentId = commentId

        return commentEntity
    }
}