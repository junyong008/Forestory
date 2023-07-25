package com.yjy.forestory.model.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingRepositoryImpl(private val context: Context): SettingRepository {

    companion object {
        private val KEY_THEME = intPreferencesKey("setting_theme")
        private val KEY_LANGUAGE = stringPreferencesKey("setting_language")
        private val KEY_NOTIFICATION = booleanPreferencesKey("setting_notification")
        private val KEY_PASSWORD = stringPreferencesKey("setting_password")
        private val KEY_BIO_PASSWORD = booleanPreferencesKey("setting_bio_password")
        private val KEY_BACKUP_PROGRESS = booleanPreferencesKey("setting_backup_progress")
        private val KEY_RESTORE_PROGRESS = booleanPreferencesKey("setting_restore_progress")
    }

    private val Context.settingDataStore: DataStore<Preferences> by preferencesDataStore(name = "setting_preferences")
    private val dataStore = context.settingDataStore


    override fun getTheme(): Flow<Int?> {
        return dataStore.data.map { preferences ->
            preferences[KEY_THEME]
        }
    }
    override suspend fun setTheme(theme: Int) {
        dataStore.edit { settings ->
            settings[KEY_THEME] = theme
        }
    }

    override fun getLanguage(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[KEY_LANGUAGE]
        }
    }
    override suspend fun setLanguage(language: String) {
        dataStore.edit { settings ->
            settings[KEY_LANGUAGE] = language
        }
    }

    override fun getIsNotificationOn(): Flow<Boolean?> {
        return dataStore.data.map { preferences ->
            preferences[KEY_NOTIFICATION]
        }
    }
    override suspend fun setIsNotificationOn(isOn: Boolean) {
        dataStore.edit { settings ->
            settings[KEY_NOTIFICATION] = isOn
        }
    }

    override fun getPassword(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[KEY_PASSWORD]
        }
    }
    override suspend fun setPassword(password: String) {
        dataStore.edit { settings ->
            settings[KEY_PASSWORD] = password
        }
    }

    override fun getIsBioPassword(): Flow<Boolean?> {
        return dataStore.data.map { preferences ->
            preferences[KEY_BIO_PASSWORD]
        }
    }
    override suspend fun setIsBioPassword(isOn: Boolean) {
        dataStore.edit { settings ->
            settings[KEY_BIO_PASSWORD] = isOn
        }
    }

    override fun getIsBackupInProgress(): Flow<Boolean?> {
        return dataStore.data.map { preferences ->
            preferences[KEY_BACKUP_PROGRESS]
        }
    }
    override suspend fun setIsBackupInProgress(isProgress: Boolean) {
        dataStore.edit { settings ->
            settings[KEY_BACKUP_PROGRESS] = isProgress
        }
    }

    override fun getIsRestoreInProgress(): Flow<Boolean?> {
        return dataStore.data.map { preferences ->
            preferences[KEY_RESTORE_PROGRESS]
        }
    }
    override suspend fun setIsRestoreInProgress(isProgress: Boolean) {
        dataStore.edit { settings ->
            settings[KEY_RESTORE_PROGRESS] = isProgress
        }
    }
}

interface SettingRepository {
    fun getTheme(): Flow<Int?>
    suspend fun setTheme(theme: Int)
    fun getLanguage(): Flow<String?>
    suspend fun setLanguage(language: String)
    fun getIsNotificationOn(): Flow<Boolean?>
    suspend fun setIsNotificationOn(isOn: Boolean)
    fun getPassword(): Flow<String?>
    suspend fun setPassword(password: String)
    fun getIsBioPassword(): Flow<Boolean?>
    suspend fun setIsBioPassword(isOn: Boolean)
    fun getIsBackupInProgress(): Flow<Boolean?>
    suspend fun setIsBackupInProgress(isProgress: Boolean)
    fun getIsRestoreInProgress(): Flow<Boolean?>
    suspend fun setIsRestoreInProgress(isProgress: Boolean)
}