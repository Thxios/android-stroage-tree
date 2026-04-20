package com.thxios.storagetree.data.storage

import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import javax.inject.Inject
import javax.inject.Singleton

data class StorageRoot(
    val path: String,
    val label: String,
    val isPrimary: Boolean
)

@Singleton
class StorageVolumeHelper @Inject constructor() {
    fun getAvailableRoots(context: Context): List<StorageRoot> {
        val roots = mutableListOf<StorageRoot>()

        // Primary internal storage — always add first
        val primary = Environment.getExternalStorageDirectory().absolutePath
        roots.add(StorageRoot(path = primary, label = "내부 저장소", isPrimary = true))

        // Detect additional volumes (SD card, USB OTG) via StorageManager
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val volumes: List<StorageVolume> = storageManager.storageVolumes
        for (volume in volumes) {
            if (volume.isPrimary) continue  // already added
            // API 30+: volume.directory gives the File
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val dir = volume.directory ?: continue
                val label = volume.getDescription(context) ?: "외부 저장소"
                roots.add(StorageRoot(path = dir.absolutePath, label = label, isPrimary = false))
            }
        }

        return roots
    }
}
