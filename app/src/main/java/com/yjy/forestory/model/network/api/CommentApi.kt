package com.yjy.forestory.model.network.api

import com.yjy.forestory.model.network.dto.CommentDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface CommentApi {

    @Multipart
    @POST("getComments.php")
    suspend fun getComments(
        @Part("writerName") writerName: RequestBody,
        @Part("writerGender") writerGender: RequestBody,
        @Part("postContent") postContent: RequestBody,
        @Part image: MultipartBody.Part
    ) : List<CommentDto>
}