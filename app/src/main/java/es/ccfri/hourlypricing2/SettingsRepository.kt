package es.ccfri.hourlypricing2

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val includeDeliveryKey = booleanPreferencesKey("include_delivery")
    private val deliveryTypeKey = stringPreferencesKey("delivery_type")

    val includeDelivery: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[includeDeliveryKey] ?: false
    }

    val deliveryType: Flow<DeliveryType> = context.dataStore.data.map { prefs ->
        DeliveryType.valueOf(prefs[deliveryTypeKey] ?: DeliveryType.FIXED.name)
    }

    suspend fun setIncludeDelivery(include: Boolean) {
        context.dataStore.edit { it[includeDeliveryKey] = include }
    }

    suspend fun setDeliveryType(type: DeliveryType) {
        context.dataStore.edit { it[deliveryTypeKey] = type.name }
    }
}
