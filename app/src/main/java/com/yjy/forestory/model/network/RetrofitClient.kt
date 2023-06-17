package com.yjy.forestory.model.network

import com.yjy.forestory.Const.Companion.BASE_URL
import com.yjy.forestory.model.network.api.CommentApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 로그 메시지의 포맷을 설정
    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC  // 로그 레벨 BASIC
    }

    // OkHttpClient -> HTTP 통신의 결과를 로그 메시지로 출력
    private val client = OkHttpClient.Builder()
        .connectTimeout(300, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .addInterceptor(logger)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val commentApi: CommentApi by lazy {
        retrofit.create(CommentApi::class.java)
    }

}