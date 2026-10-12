package com.fixnow.app.data.local

import android.content.Context
import com.fixnow.app.domain.model.SolicitudServicio
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PendingSolicitudQueue(context: Context) {
    private val prefs = context.getSharedPreferences("pending_solicitudes", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun enqueue(solicitud: SolicitudServicio) {
        val items = load().toMutableList()
        items.add(solicitud)
        prefs.edit().putString(KEY, gson.toJson(items)).apply()
    }

    fun load(): List<SolicitudServicio> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<SolicitudServicio>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun remove(ids: Collection<String>) {
        if (ids.isEmpty()) return
        val remaining = load().filterNot { it.id in ids }
        prefs.edit().putString(KEY, gson.toJson(remaining)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    companion object {
        private const val KEY = "pending_solicitudes_queue"
    }
}
