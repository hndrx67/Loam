package org.hndrx.loamgallery.data

import android.content.SharedPreferences
import androidx.core.content.edit
import org.hndrx.loamgallery.model.SavedAlbum
import org.json.JSONArray
import org.json.JSONObject

class AlbumStore(private val prefs: SharedPreferences) {
    fun read(): List<SavedAlbum> = try {
        val array = JSONArray(prefs.getString("albums", "[]"))
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val uris = item.getJSONArray("uris")
            SavedAlbum(item.getString("id"), item.getString("name"),
                (0 until uris.length()).map { uris.getString(it) }.toSet())
        }
    } catch (_: Exception) { emptyList() }

    fun write(albums: List<SavedAlbum>) {
        val array = JSONArray()
        albums.forEach { album -> array.put(JSONObject().put("id", album.id).put("name", album.name)
            .put("uris", JSONArray(album.uris.toList()))) }
        prefs.edit { putString("albums", array.toString()) }
    }
}
