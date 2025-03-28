package com.yjy.forestory.model.network.api

import com.yjy.forestory.model.network.dto.CommentDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface CommentApi {

    @Multipart
    @POST("getComments.php")
    suspend fun getComments(
        @Part("writerName") writerName: RequestBody,
        @Part("writerGender") writerGender: RequestBody,
        @Part("postContent") postContent: RequestBody,
        @Part("language") language: RequestBody,
        @Part image: MultipartBody.Part
    ) : List<CommentDto>
}