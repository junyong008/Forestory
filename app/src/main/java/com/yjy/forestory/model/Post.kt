package com.yjy.forestory.model

import android.net.Uri
import com.yjy.forestory.model.db.entity.PostEntity
import java.util.*

data class Post(
    val postId: Int,
    val userName: String,
    val userPicture: Uri,
    val image: Uri,
    val content: String,
    val createDate: Date,
    val isAddingComments: Boolean = false
) {
    fun toPostEntity(): PostEntity {
        val postEntity = PostEntity(userName, userPicture, image, content, createDate, isAddingComments)
        postEntity.postId = postId

        return postEntity
    }
}