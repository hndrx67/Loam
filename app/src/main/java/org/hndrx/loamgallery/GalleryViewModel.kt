package org.hndrx.loamgallery

import android.app.Application
import android.app.RecoverableSecurityException
import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.hndrx.loamgallery.data.MediaRepository
import org.hndrx.loamgallery.data.MediaActions
import org.hndrx.loamgallery.data.SettingsStore
import org.hndrx.loamgallery.data.assetMetadata
import org.hndrx.loamgallery.data.assetFromMetadata
import org.hndrx.loamgallery.model.AppSettings
import org.hndrx.loamgallery.model.MediaAction
import org.hndrx.loamgallery.model.TrashEntry
import java.util.UUID
import org.hndrx.loamgallery.model.MediaAsset
import org.hndrx.loamgallery.model.MediaAccess

data class LibraryState(
    val media: List<MediaAsset> = emptyList(),
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val failed: Boolean = false,
    val access: MediaAccess = MediaAccess.None,
)

data class TrashState(val items: List<TrashEntry> = emptyList(), val loading: Boolean = false, val failed: Boolean = false)
data class OperationConsent(val sender: IntentSender?, val permission: String? = null, val token: String = UUID.randomUUID().toString())

class GalleryViewModel(app: Application, private val savedState: SavedStateHandle) : AndroidViewModel(app) {
    private val repository = MediaRepository(app)
    private val prefs = app.getSharedPreferences("loam", 0)
    private val settingsStore = SettingsStore(prefs)
    private val actions = MediaActions(app, repository)
    private val _library = MutableStateFlow(LibraryState())
    val library = _library.asStateFlow()
    private val _favorites = MutableStateFlow(prefs.getStringSet("favorites", emptySet()).orEmpty().toSet())
    val favorites = _favorites.asStateFlow()
    private val _settings = MutableStateFlow(settingsStore.read())
    val settings = _settings.asStateFlow()
    private val _trash = MutableStateFlow(TrashState())
    val trash = _trash.asStateFlow()
    private val _operationBusy = MutableStateFlow(savedState.contains("operation"))
    val operationBusy = _operationBusy.asStateFlow()
    private val _consent = MutableStateFlow<OperationConsent?>(null)
    val consent = _consent.asStateFlow()
    private val _message = MutableStateFlow<Int?>(null)
    val message = _message.asStateFlow()
    private val _external = MutableStateFlow<MediaAsset?>(null)
    val external = _external.asStateFlow()
    private val _externalError = MutableStateFlow(false)
    val externalError = _externalError.asStateFlow()
    private val _viewerCollection = MutableStateFlow<List<MediaAsset>>(emptyList())
    val viewerCollection = _viewerCollection.asStateFlow()
    private var refreshJob: Job? = null
    private var externalJob: Job? = null
    private var generation = 0
    private var mediaChangeJob: Job? = null
    private var trashJob: Job? = null
    private var trashGeneration = 0
    private var trashOpened = false

    init {
        savedState.get<String>("externalUri")?.let { openExternal(it.toUri()) }
        val pending = savedState.get<Bundle>("operation")
        if (pending != null && !pending.getBoolean("launched")) {
            savedState.remove<Bundle>("operation")
            _operationBusy.value = false
            _message.value = R.string.operation_interrupted
        }
    }

    fun onMediaChanged() {
        if (!_settings.value.watchChanges) return
        mediaChangeJob?.cancel()
        mediaChangeJob = viewModelScope.launch { delay(350); refresh() }
    }

    fun openCollection(items: List<MediaAsset>) { _viewerCollection.value = items }
    fun closeCollection() { _viewerCollection.value = emptyList() }

    fun updateAccess(access: MediaAccess) {
        // Old results may no longer be authorized after a permission change.
        if (access != _library.value.access) _library.value = LibraryState(access = access)
        refresh()
        if (trashOpened) refreshTrash()
    }

    fun refresh() {
        val request = ++generation
        refreshJob?.cancel()
        if (_library.value.access == MediaAccess.None) {
            _library.value = LibraryState(loaded = true)
            return
        }
        refreshJob = viewModelScope.launch {
            _library.value = _library.value.copy(loading = true, failed = false)
            try {
                val media = repository.load()
                if (request == generation) _library.value = _library.value.copy(media = media, loaded = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SecurityException) {
                if (request == generation) _library.value = _library.value.copy(media = emptyList(), failed = true)
            } catch (_: Exception) {
                if (request == generation) _library.value = _library.value.copy(failed = true)
            } finally {
                if (request == generation) _library.value = _library.value.copy(loading = false, loaded = true)
            }
        }
    }

    fun openExternal(uri: Uri) {
        savedState["externalUri"] = uri.toString()
        externalJob?.cancel()
        externalJob = viewModelScope.launch {
            _external.value = null
            _externalError.value = false
            try {
                _external.value = repository.resolve(uri)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _externalError.value = true
            }
        }
    }

    fun closeExternal() { externalJob?.cancel(); _external.value = null; savedState.remove<String>("externalUri") }
    fun dismissExternalError() { _externalError.value = false; savedState.remove<String>("externalUri") }

    fun toggleFavorite(id: Long) {
        val next = _favorites.value.toMutableSet().apply {
            if (!add(id.toString())) remove(id.toString())
        }.toSet()
        _favorites.value = next
        prefs.edit { putStringSet("favorites", next) }
    }

    fun updateSettings(value: AppSettings) {
        if (!value.watchChanges) mediaChangeJob?.cancel()
        _settings.value = value.sanitized()
        settingsStore.write(_settings.value)
    }

    fun refreshTrash() {
        trashOpened = true
        val request = ++trashGeneration
        trashJob?.cancel()
        trashJob = viewModelScope.launch {
            _trash.value = _trash.value.copy(loading = true, failed = false)
            try {
                val items = actions.trash(includeNative = _library.value.access != MediaAccess.None)
                if (request == trashGeneration) _trash.value = TrashState(items)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (request == trashGeneration) _trash.value = _trash.value.copy(loading = false, failed = true) }
        }
    }

    fun requestOperation(asset: MediaAsset, action: MediaAction, localKey: String? = null) {
        if (_operationBusy.value) return
        savedState["operation"] = Bundle().apply {
            assetMetadata(asset).forEach { (key, value) -> putString(key, value) }
            putString("action", action.name)
            putString("localKey", localKey ?: if (action == MediaAction.Trash && Build.VERSION.SDK_INT < 30) UUID.randomUUID().toString() else null)
        }
        _operationBusy.value = true
        executePending()
    }

    private fun executePending() = viewModelScope.launch {
        val pending = savedState.get<Bundle>("operation") ?: return@launch
        val action = MediaAction.valueOf(pending.getString("action")!!)
        val asset = assetFromMetadata(pending.keySet().filter { it in ASSET_KEYS }.associateWith { pending.getString(it).orEmpty() })
        try {
            if (Build.VERSION.SDK_INT <= 28 && !(action == MediaAction.Delete && pending.getString("localKey") != null) &&
                ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                pending.putString("phase", "permission")
                savedState["operation"] = pending
                _consent.value = OperationConsent(null, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                return@launch
            }
            if (Build.VERSION.SDK_INT == 29 && action == MediaAction.Trash &&
                ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_MEDIA_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                pending.putString("phase", "permission")
                savedState["operation"] = pending
                _consent.value = OperationConsent(null, Manifest.permission.ACCESS_MEDIA_LOCATION)
                return@launch
            }
            val sender = actions.execute(asset, action, pending.getString("localKey"))
            if (sender != null) {
                pending.putString("phase", "native")
                savedState["operation"] = pending
                _consent.value = OperationConsent(sender)
            } else finishOperation(R.string.operation_complete)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            if (Build.VERSION.SDK_INT == 29 && error is RecoverableSecurityException && !pending.getBoolean("retried")) {
                pending.putString("phase", "retry")
                savedState["operation"] = pending
                _consent.value = OperationConsent(error.userAction.actionIntent.intentSender)
            } else finishOperation(R.string.operation_failed)
        }
    }

    fun consentLaunched() {
        savedState.get<Bundle>("operation")?.let { it.putBoolean("launched", true); savedState["operation"] = it }
        _consent.value = null
    }

    fun operationResult(approved: Boolean) {
        val pending = savedState.get<Bundle>("operation") ?: return
        if (!approved) {
            viewModelScope.launch {
                try { if (pending.getString("action") == MediaAction.Trash.name) actions.cancelPrepared(pending.getString("localKey"), pending.getString("uri")!!.toUri()) }
                catch (_: Exception) { /* Retain the recovery copy if cleanup cannot be verified. */ }
                finally { finishOperation(R.string.operation_cancelled) }
            }
        } else if (pending.getString("phase") == "native") finishOperation(R.string.operation_complete)
        else {
            if (pending.getString("phase") == "retry") pending.putBoolean("retried", true)
            pending.putBoolean("launched", false)
            savedState["operation"] = pending
            executePending()
        }
    }

    fun operationLaunchFailed() { finishOperation(R.string.operation_failed) }
    fun dismissMessage() { _message.value = null }
    fun cacheCleared() { _message.value = R.string.cache_cleared }

    private fun finishOperation(message: Int) {
        savedState.remove<Bundle>("operation")
        _operationBusy.value = false
        _consent.value = null
        _message.value = message
        refresh()
        refreshTrash()
    }

    companion object {
        private val ASSET_KEYS = setOf("id", "uri", "name", "mime", "date", "bucketId", "bucketName", "duration", "size", "width", "height")
    }
}
