package com.resgrid.plugins.resgrid.data

import android.app.Activity
import android.content.SharedPreferences
import android.content.Context;

class Preferences internal constructor(context: Context, configuration: PreferencesConfiguration) {
    private val preferences: SharedPreferences

    private interface PreferencesOperation {
        fun execute(editor: SharedPreferences.Editor?)
    }

    init {
        preferences = context.getSharedPreferences(configuration.group, Activity.MODE_PRIVATE)
    }

    operator fun get(key: String?): String? {
        return preferences.getString(key, null)
    }

    operator fun set(key: String?, value: String?) {
        executeOperation(object : PreferencesOperation {
            override fun execute(editor: SharedPreferences.Editor?) {
                editor?.putString(
                    key,
                    value
                )
            }
        })
    }

    fun remove(key: String?) {
        executeOperation(object : PreferencesOperation {
            override fun execute(editor: SharedPreferences.Editor?) {
                editor?.remove(
                    key
                )
            }
        })
    }

    fun keys(): MutableSet<String> {
        return preferences.all.keys
    }

    fun clear() {
        executeOperation(object : PreferencesOperation {
            override fun execute(editor: SharedPreferences.Editor?) {
                preferences.edit().clear()
            }
        })
    }

    private fun executeOperation(op: PreferencesOperation) {
        val editor = preferences.edit()
        op.execute(editor)
        editor.apply()
    }
}