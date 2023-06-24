package com.yjy.forestory.model.network.dto

import android.net.Uri
import com.yjy.forestory.model.db.entity.CommentEntity

data class CommentDto(
    val name: String,
    val content: String
) {
    fun toCommentEntity(postId: Int, writerPicture: Uri): CommentEntity = CommentEntity(postId, name, writerPicture, content)
}