package com.yjy.forestory.repository

import androidx.annotation.WorkerThread
import com.yjy.forestory.model.db.dao.PostDAO
import com.yjy.forestory.model.db.dto.PostDTO
import kotlinx.coroutines.flow.Flow

// 왜 PostDAO의 의존성을 주입받는데 @Inject를 사용하지 않았느냐 -> AppModule에 이미 Singleton으로 구성하면서 의존성을 주입받기에.
class PostRepositoryImpl(private val postDao: PostDAO): PostRepository {

    override fun getAllPosts(): Flow<List<PostDTO>> {
        return postDao.getAllPosts()
    }

    @WorkerThread
    override suspend fun insert(post: PostDTO) {
        postDao.insert(post)
    }
}

interface PostRepository {
    fun getAllPosts(): Flow<List<PostDTO>>
    suspend fun insert(post: PostDTO)
}