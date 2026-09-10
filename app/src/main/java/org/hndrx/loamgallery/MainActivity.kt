package org.hndrx.loamgallery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.hndrx.loamgallery.ui.LoamApp
import org.hndrx.loamgallery.model.MediaAccess
import org.hndrx.loamgallery.model.resolveMediaAccess

class MainActivity : ComponentActivity() {
    private val vm: GalleryViewModel by viewModels()
    private val mediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) { vm.onMediaChanged() }
    }
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.updateAccess(mediaAccess())
    }
    private val mediaActionLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        vm.operationResult(it.resultCode == RESULT_OK)
    }
    private val writePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.operationResult(it)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.consent.collect { consent ->
                    if (consent != null) {
                        vm.consentLaunched()
                        try {
                            if (consent.sender != null) mediaActionLauncher.launch(IntentSenderRequest.Builder(consent.sender).build())
                            else writePermissionLauncher.launch(requireNotNull(consent.permission))
                        } catch (_: Exception) { vm.operationLaunchFailed() }
                    }
                }
            }
        }
        // URI grants from ACTION_VIEW work independently of broad library permission.
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            LoamApp(vm, requestAccess = { permissionLauncher.launch(mediaPermissions()) }, openSettings = {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()))
            })
        }
    }

    override fun onResume() {
        super.onResume()
        vm.updateAccess(mediaAccess())
    }

    override fun onStart() {
        super.onStart()
        contentResolver.registerContentObserver("content://${MediaStore.AUTHORITY}".toUri(), true, mediaObserver)
    }

    override fun onStop() {
        contentResolver.unregisterContentObserver(mediaObserver)
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.takeIf { it.scheme == "content" || it.scheme == "file" }?.let(vm::openExternal)
        }
    }

    private fun granted(permission: String) = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun mediaAccess(): MediaAccess = resolveMediaAccess(
        api = Build.VERSION.SDK_INT,
        images = Build.VERSION.SDK_INT >= 33 && granted(Manifest.permission.READ_MEDIA_IMAGES),
        videos = Build.VERSION.SDK_INT >= 33 && granted(Manifest.permission.READ_MEDIA_VIDEO),
        selected = Build.VERSION.SDK_INT >= 34 && granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED),
        storage = Build.VERSION.SDK_INT < 33 && granted(Manifest.permission.READ_EXTERNAL_STORAGE),
    )

    private fun mediaPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}
