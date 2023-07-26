package com.yjy.forestory.model

import com.yjy.forestory.model.db.entity.TagEntity

data class Tag(
    val tagId: Int,
    val postId: Int,
    val content: String
) {
    fun toTagEntity(): TagEntity {
        val tagEntity = TagEntity(postId, content)
        tagEntity.tagId = tagId

        return tagEntity
    }
}