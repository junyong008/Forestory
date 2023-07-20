package com.yjy.forestory.model.repository

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl(private val context: Context): UserRepository {

    companion object {
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_PICTURE = stringPreferencesKey("user_picture")
        private val KEY_USER_GENDER = stringPreferencesKey("user_gender")
    }

    // context를 입력받아 직접적으로 사용하진 않지만, 코드 가독성을 위해 Context.userDataStore를 클래스 내에 선언하고 바로 불러와 사용.
    private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")
    private val dataStore = context.userDataStore

    override fun getUserName(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[KEY_USER_NAME]
        }
    }

    override suspend fun setUserName(userName: String) {
        dataStore.edit { settings ->
            settings[KEY_USER_NAME] = userName
        }
    }

    override fun getUserPicture(): Flow<Uri?> {
        return dataStore.data.map { preferences ->
            val pictureUriString = preferences[KEY_USER_PICTURE]
            if (pictureUriString != null) {
                Uri.parse(pictureUriString)
            } else {
                null
            }
        }
    }

    override suspend fun setUserPicture(uri: Uri) {
        val pictureUriString = uri.toString()
        dataStore.edit { settings ->
            settings[KEY_USER_PICTURE] = pictureUriString
        }
    }

    override fun getUserGender(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[KEY_USER_GENDER]
        }
    }

    override suspend fun setUserGender(userGender: String) {
        dataStore.edit { settings ->
            settings[KEY_USER_GENDER] = userGender
        }
    }
}



interface UserRepository {
    fun getUserName(): Flow<String?>
    suspend fun setUserName(userName: String)
    fun getUserPicture(): Flow<Uri?>
    suspend fun setUserPicture(uri: Uri)
    fun getUserGender(): Flow<String?>
    suspend fun setUserGender(userGender: String)
}