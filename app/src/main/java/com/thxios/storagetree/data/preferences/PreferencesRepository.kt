package com.thxios.storagetree.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.thxios.storagetree.domain.model.SortOrder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

data class AppSettings(
    val showInstalledApps: Boolean = false,
    val sortOrder: SortOrder = SortOrder.SIZE_DESC
)

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val showInstalledAppsKey = booleanPreferencesKey("show_installed_apps")
    private val sortOrderKey = stringPreferencesKey("sort_order")

    val appSettings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            showInstalledApps = prefs[showInstalledAppsKey] ?: false,
            sortOrder = prefs[sortOrderKey]?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() }
                ?: SortOrder.SIZE_DESC
        )
    }

    suspend fun setShowInstalledApps(value: Boolean) {
        context.dataStore.edit { it[showInstalledAppsKey] = value }
    }

    suspend fun setSortOrder(sortOrder: SortOrder) {
        context.dataStore.edit { it[sortOrderKey] = sortOrder.name }
    }
}
