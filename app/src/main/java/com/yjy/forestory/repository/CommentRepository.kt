package com.yjy.forestory.repository

import android.net.Uri
import androidx.annotation.WorkerThread
import com.yjy.forestory.model.db.dao.CommentDAO
import com.yjy.forestory.model.db.dto.CommentDTO

class CommentRepositoryImpl(private val commentDao: CommentDAO): CommentRepository {

    @WorkerThread
    override suspend fun addComment(postId: Int?, writerPicture: Uri?, commentWriter: String?, commentContent: String?): Boolean {

        if (postId == null || writerPicture == null || commentWriter == null || commentContent == null) {
            return false
        }

        val comment = CommentDTO(postId, commentWriter, writerPicture, commentContent)
        commentDao.insert(comment)
        return true
    }
}

interface CommentRepository {
    suspend fun addComment(postId: Int?, writerPicture: Uri?, commentWriter: String?, commentContent: String?): Boolean
}