package com.yjy.forestory.model

import com.yjy.forestory.model.db.entity.PostWithTagsAndCommentsEntity

data class PostWithTagsAndComments(
    val post: Post,
    val tags: List<Tag>,
    val comments: List<Comment>
) {
    fun toPostWithTagsAndCommentsEntity(): PostWithTagsAndCommentsEntity = PostWithTagsAndCommentsEntity(
        postEntity = post.toPostEntity(),
        tagEntityList = tags.map { it.toTagEntity() },
        commentEntityList = comments.map { it.toCommentEntity() }
    )
}