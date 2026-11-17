package com.example.localization

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppLanguage(val code: String, val nativeName: String) {
    ENGLISH("en", "English"),
    URDU("ur", "اردو");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
    }
}

private val Context.zeluPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "zelu_prefs")

class LanguagePreferencesRepository(private val context: Context) {
    private val langKey = stringPreferencesKey("preferred_language")
    private val darkModeKey = booleanPreferencesKey("dark_mode_override")
    private val hasDarkOverrideKey = booleanPreferencesKey("has_dark_override")

    val languageFlow: Flow<AppLanguage> = context.zeluPrefsDataStore.data.map { prefs ->
        AppLanguage.fromCode(prefs[langKey])
    }

    val darkThemeOverrideFlow: Flow<Boolean?> = context.zeluPrefsDataStore.data.map { prefs ->
        if (prefs[hasDarkOverrideKey] == true) prefs[darkModeKey] else null
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.zeluPrefsDataStore.edit { prefs ->
            prefs[langKey] = language.code
        }
    }

    suspend fun setDarkThemeOverride(dark: Boolean?) {
        context.zeluPrefsDataStore.edit { prefs ->
            if (dark == null) {
                prefs[hasDarkOverrideKey] = false
            } else {
                prefs[hasDarkOverrideKey] = true
                prefs[darkModeKey] = dark
            }
        }
    }
}
