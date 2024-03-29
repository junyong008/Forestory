package com.yjy.forestory.di

import android.content.Context
import com.yjy.forestory.model.db.ForestoryDatabase
import com.yjy.forestory.model.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// 보통 용도에 따라 Module을 나누지만, 프로젝트 규모가 크지 않으므로 한개로 통합
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): ForestoryDatabase {
        return ForestoryDatabase.getInstance(context)
    }

    @Singleton
    @Provides
    fun providePostWithTagsAndCommentsRepository(forestoryDatabase: ForestoryDatabase): PostWithTagsAndCommentsRepository {
        return PostWithTagsAndCommentsRepositoryImpl(forestoryDatabase.postWithTagsAndCommentsDao())
    }

    @Singleton
    @Provides
    fun provideUserRepository(@ApplicationContext context: Context): UserRepository {
        return UserRepositoryImpl(context)
    }

    @Singleton
    @Provides
    fun provideSettingRepository(@ApplicationContext context: Context): SettingRepository {
        return SettingRepositoryImpl(context)
    }

    @Singleton
    @Provides
    fun provideTicketRepository(@ApplicationContext context: Context): TicketRepository {
        return TicketRepositoryImpl(context)
    }
}