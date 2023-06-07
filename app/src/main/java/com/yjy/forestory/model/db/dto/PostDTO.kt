package com.yjy.forestory.model.db.dto

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "Post")
data class PostDTO(
    val image: Uri,
    val content: String,
    val tagList: List<String>?,
    val createDate: Date
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
}