package com.yjy.forestory.model

import com.yjy.forestory.model.db.entity.CommentEntity

data class Comment(
    val commentId: Int,
    val postId: Int,
    val writerName: String,
    val content: String
) {
    fun toCommentEntity(): CommentEntity {
        val commentEntity = CommentEntity(postId, writerName, content)
        commentEntity.commentId = commentId

        return commentEntity
    }
}