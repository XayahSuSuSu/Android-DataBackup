package com.xayah.databackup.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
suspend fun Context.preloadingDataStore() = dataStore.data.first()

private fun Context.readString(key: Preferences.Key<String>, defValue: String) = dataStore.data.map { preferences -> preferences[key] ?: defValue }
private fun Context.readBoolean(key: Preferences.Key<Boolean>, defValue: Boolean) = dataStore.data.map { preferences -> preferences[key] ?: defValue }
private fun Context.readInt(key: Preferences.Key<Int>, defValue: Int) = dataStore.data.map { preferences -> preferences[key] ?: defValue }
private fun Context.readLong(key: Preferences.Key<Long>, defValue: Long) = dataStore.data.map { preferences -> preferences[key] ?: defValue }

fun Context.readString(pair: Pair<Preferences.Key<String>, String>) = dataStore.data.map { preferences -> preferences[pair.first] ?: pair.second }
fun Context.readBoolean(pair: Pair<Preferences.Key<Boolean>, Boolean>) = dataStore.data.map { preferences -> preferences[pair.first] ?: pair.second }
fun Context.readInt(pair: Pair<Preferences.Key<Int>, Int>) = dataStore.data.map { preferences -> preferences[pair.first] ?: pair.second }
fun Context.readLong(pair: Pair<Preferences.Key<Long>, Long>) = dataStore.data.map { preferences -> preferences[pair.first] ?: pair.second }

suspend fun Context.saveString(key: Preferences.Key<String>, value: String) = dataStore.edit { settings -> settings[key] = value }
suspend fun Context.saveBoolean(key: Preferences.Key<Boolean>, value: Boolean) = dataStore.edit { settings -> settings[key] = value }
suspend fun Context.saveInt(key: Preferences.Key<Int>, value: Int) = dataStore.edit { settings -> settings[key] = value }
suspend fun Context.saveLong(key: Preferences.Key<Long>, value: Long) = dataStore.edit { settings -> settings[key] = value }

// Key to defValue
val KeyFirstLaunch = booleanPreferencesKey("first_launch")
const val DefFirstLaunch = true
val FirstLaunch = Pair(KeyFirstLaunch, DefFirstLaunch)

val KeyCustomSuFile = stringPreferencesKey("custom_su_file")
const val DefCustomSuFile = "su"
val CustomSuFile = Pair(KeyCustomSuFile, DefCustomSuFile)