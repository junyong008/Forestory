package com.yjy.forestory.model.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingRepositoryImpl(private val context: Context): SettingRepository {

    private val KEY_THEME = intPreferencesKey("setting_theme")
    private val KEY_LANGUAGE = stringPreferencesKey("setting_language")
    private val KEY_NOTIFICATION = booleanPreferencesKey("setting_notification")

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
}

interface SettingRepository {
    fun getTheme(): Flow<Int?>
    suspend fun setTheme(theme: Int)
    fun getLanguage(): Flow<String?>
    suspend fun setLanguage(language: String)
    fun getIsNotificationOn(): Flow<Boolean?>
    suspend fun setIsNotificationOn(isOn: Boolean)
}