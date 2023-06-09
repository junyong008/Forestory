package com.yjy.forestory.model.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yjy.forestory.model.db.dao.CommentDAO
import com.yjy.forestory.model.db.dao.PostDAO
import com.yjy.forestory.model.db.dto.CommentDTO
import com.yjy.forestory.model.db.dto.PostDTO

// 추후 DB의 수정이 추가돼서 배포할 상황이 생기면, DB의 version을 올리고 인스턴스를 만들때 기존 DB 스키마를 사용하고 있는 유저들을 위해 마이그레이션을 통해 스키마의 형태를 일치하게 맞춰주어야한다.
// 혹은 CoroutineScope를 같이 넘겨 Callback 함수를 등록해 기존 DB 스키마를 싹 비워버리는 방법도 있지만, 데이터가 날라가므로 권장되지 않음.
@Database(entities = [PostDTO::class, CommentDTO::class], version = 1, exportSchema = false)
@TypeConverters(Converter::class)
abstract class ForestoryDatabase: RoomDatabase() {

    abstract fun postDao(): PostDAO
    abstract fun commentDao(): CommentDAO


    companion object {
        @Volatile   // 메인 메모리에 할당

        // DB 인스턴스가 존재하면 만들지 않고, 없으면 새로 만든다. SingleTon 패턴으로 구성
        private var INSTANCE: ForestoryDatabase ?= null
        fun getInstance(context: Context): ForestoryDatabase {
            if (INSTANCE == null) {
                synchronized(ForestoryDatabase::class) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        ForestoryDatabase::class.java,
                        "forestory_database"
                    ).build()
                }
            }
            return INSTANCE as ForestoryDatabase
        }

    }
}