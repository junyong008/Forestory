package com.yjy.forestory.model.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TicketRepositoryImpl(private val context: Context): TicketRepository {

    companion object {
        private val KEY_TICKET = intPreferencesKey("ticket")
        private val KEY_FREE_TICKET = intPreferencesKey("free_ticket")
        private val KEY_TICKET_NEED_CONSUME = stringPreferencesKey("ticket_need_consume")
    }

    // context를 입력받아 직접적으로 사용하진 않지만, 코드 가독성을 위해 Context.userDataStore를 클래스 내에 선언하고 바로 불러와 사용.
    private val Context.ticketDataStore: DataStore<Preferences> by preferencesDataStore(name = "ticket_preferences")
    private val dataStore = context.ticketDataStore

    override fun getTicket(): Flow<Int?> {
        return dataStore.data.map { preferences ->
            preferences[KEY_TICKET]
        }
    }
    override suspend fun setTicket(count: Int) {
        dataStore.edit { settings ->
            settings[KEY_TICKET] = count
        }
    }

    override fun getFreeTicket(): Flow<Int?> {
        return dataStore.data.map { preferences ->
            preferences[KEY_FREE_TICKET]
        }
    }
    override suspend fun setFreeTicket(count: Int) {
        dataStore.edit { settings ->
            settings[KEY_FREE_TICKET] = count
        }
    }

    override fun getTicketNeedConsume(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[KEY_TICKET_NEED_CONSUME]
        }
    }
    override suspend fun setTicketNeedConsume(productId: String) {
        dataStore.edit { settings ->
            settings[KEY_TICKET_NEED_CONSUME] = productId
        }
    }
}



interface TicketRepository {
    fun getTicket(): Flow<Int?>
    suspend fun setTicket(count: Int)
    fun getFreeTicket(): Flow<Int?>
    suspend fun setFreeTicket(count: Int)
    fun getTicketNeedConsume(): Flow<String?>
    suspend fun setTicketNeedConsume(productId: String)
}