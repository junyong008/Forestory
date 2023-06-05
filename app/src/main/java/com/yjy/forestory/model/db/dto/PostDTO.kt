package com.yjy.forestory.model.db.dto

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Post")
data class PostDTO(
    val image: Bitmap,
    val content: String,
    val tagList: List<String>
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
}