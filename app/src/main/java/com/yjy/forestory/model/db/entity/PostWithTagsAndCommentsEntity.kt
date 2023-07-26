package com.yjy.forestory.model.db.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.yjy.forestory.model.PostWithTagsAndComments

data class PostWithTagsAndCommentsEntity (
    @Embedded
    val postEntity: PostEntity,

    @Relation(
        parentColumn = "postId",
        entityColumn = "postId",
        entity = TagEntity::class
    )
    val tagEntityList: List<TagEntity>,

    @Relation(
        parentColumn = "postId",
        entityColumn = "postId",
        entity = CommentEntity::class
    )
    val commentEntityList: List<CommentEntity>
) {
    fun toPostWithTagsAndComments(): PostWithTagsAndComments = PostWithTagsAndComments(
        post = postEntity.toPost(),
        tags = tagEntityList.map { it.toTag() },
        comments = commentEntityList.map { it.toComment() }
    )
}