package com.yjy.forestory.model.db.dto

import androidx.room.Embedded
import androidx.room.Relation

data class PostWithComments(
    @Embedded val post: PostDTO,
    @Relation(
        parentColumn = "postId",
        entityColumn = "postId",
        entity = CommentDTO::class
    )
    val comments: List<CommentDTO>
)