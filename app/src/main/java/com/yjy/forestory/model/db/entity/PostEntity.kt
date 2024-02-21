package com.yjy.forestory.model.db.entity

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yjy.forestory.model.Post
import java.util.Date

@Entity(tableName = "Post")
data class PostEntity(
    val userName: String,
    val userGender: String,
    val userPicture: Uri,
    val image: Uri,
    val content: String,
    val createDate: Date,
    val isAddingComments: Boolean = false
) {
    @PrimaryKey(autoGenerate = true)
    var postId: Int? = null

    fun toPost(): Post = Post(postId!!, userName, userGender, userPicture, image, content, createDate, isAddingComments)
}