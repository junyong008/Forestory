package com.yjy.forestory.repository

import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.yjy.forestory.model.db.dao.PostDAO
import com.yjy.forestory.model.db.dto.PostDTO
import com.yjy.forestory.model.db.dto.PostWithComments
import kotlinx.coroutines.flow.Flow
import java.util.*

// 왜 PostDAO의 의존성을 주입받는데 @Inject를 사용하지 않았느냐 -> AppModule에 이미 Singleton으로 구성하면서 의존성을 주입받기에.
class PostRepositoryImpl(private val postDao: PostDAO): PostRepository {

    override fun getPosts(): Flow<PagingData<PostWithComments>> {
        return Pager(
            config = PagingConfig(
                pageSize = 15, // 페이지당 불러올 항목 갯수
                enablePlaceholders = false // 미리 모든 항목들을 불러와서 null처리 해둘것 인지.
                /*
                initialLoadSize = 기본값(pageSize * 3) : 초기 데이터 로드시 한번에 가져올 항목갯수. pageSize보다 작게 설정한다면 pageSize로 대체됨
                prefetchDistance = 기본값(pageSize) : RecyclerView가 아이템을 미리 가져와야 하는 거리
                 */
            ),
            pagingSourceFactory = {
                postDao.getPosts()
            }
        ).flow
    }

    override fun getPost(postId: Int): Flow<PostWithComments> {
        return postDao.getPost(postId)
    }

    override fun getPostCount(): Flow<Int> {
        return postDao.getPostCount()
    }

    @WorkerThread
    override suspend fun updateTempColumn(value: Int, postId: Int?) {
        postDao.updateTempColumn(value, postId)
    }


    // 유효성 검사를 Repository에서 처리함으로 데이터 입력의 안정성을 높이고 호출자로 하여금 데이터 유효성 검사를 하지 않게 해서 중복 코드를 줄이고 유지보수의 용이성을 높인다!
    @WorkerThread
    override suspend fun addPost(userName: String?, userPicture: Uri?, postImage: Uri?, postContent: String?, tagList: List<String>?): Boolean {

        if (userName == null || userPicture == null || postImage == null || postContent == null) {
            return false
        }

        val post = PostDTO(userName, userPicture, postImage, postContent, tagList, Date())
        postDao.insert(post)
        return true
    }

    @WorkerThread
    override suspend fun deletePost(post: PostDTO?): Boolean {

        if (post == null) {
            return false
        }

        postDao.delete(post)
        return true
    }
}

interface PostRepository {
    fun getPosts(): Flow<PagingData<PostWithComments>>
    fun getPost(postId: Int): Flow<PostWithComments>
    fun getPostCount(): Flow<Int>
    suspend fun updateTempColumn(value: Int, postId: Int? = null)
    suspend fun addPost(userName: String?, userPicture: Uri?, postImage: Uri?, postContent: String?, tagList: List<String>?): Boolean
    suspend fun deletePost(post: PostDTO?): Boolean
}