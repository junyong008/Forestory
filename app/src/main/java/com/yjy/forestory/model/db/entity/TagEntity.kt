package com.yjy.forestory.model.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yjy.forestory.model.Tag

@Entity(tableName = "Tag")
data class TagEntity(
    val postId: Int,
    val content: String
) {
    @PrimaryKey(autoGenerate = true)
    var tagId: Int? = null

    fun toTag(): Tag = Tag(tagId!!, postId, content)
}