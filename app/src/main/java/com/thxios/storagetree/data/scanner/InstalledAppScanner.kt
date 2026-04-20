package com.thxios.storagetree.data.scanner

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
import com.thxios.storagetree.domain.model.FileNode
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstalledAppScanner @Inject constructor() {

    companion object {
        const val VIRTUAL_APPS_PATH = "virtual://installed-apps"
        const val VIRTUAL_APK_SUFFIX = "/apk"
        const val VIRTUAL_DATA_SUFFIX = "/data"
        const val VIRTUAL_CACHE_SUFFIX = "/cache"
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun getAllVolumeStats(
        storageStatsManager: StorageStatsManager,
        storageManager: StorageManager,
        packageName: String
    ): Pair<Long, Long> {
        var totalData = 0L
        var totalCache = 0L
        storageManager.storageVolumes.forEach { volume ->
            val dir = volume.directory ?: return@forEach
            try {
                val uuid = storageManager.getUuidForPath(dir)
                val stats = storageStatsManager.queryStatsForPackage(uuid, packageName, Process.myUserHandle())
                totalData += stats.dataBytes
                totalCache += stats.cacheBytes
            } catch (_: Exception) { }
        }
        return Pair(totalData, totalCache)
    }

    fun buildVirtualAppsNode(context: Context): FileNode {
        val pm = context.packageManager
        val hasUsageStats = hasUsageStatsPermission(context)
        val storageStatsManager = if (hasUsageStats)
            context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
        else null
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

        val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
        val appNodes = packages
            .filter { it.applicationInfo != null }
            .mapNotNull { pkg ->
                val appInfo: ApplicationInfo = pkg.applicationInfo ?: return@mapNotNull null
                val appLabel = pm.getApplicationLabel(appInfo).toString()
                val appPath = "$VIRTUAL_APPS_PATH/${pkg.packageName}"

                val apkSize = try {
                    val base = File(appInfo.sourceDir).length()
                    val splits = appInfo.splitSourceDirs?.sumOf { File(it).length() } ?: 0L
                    base + splits
                } catch (e: Exception) { 0L }

                var totalDataSize = 0L
                var totalCacheSize = 0L
                if (storageStatsManager != null) {
                    val (dataBytes, cacheBytes) = getAllVolumeStats(storageStatsManager, storageManager, pkg.packageName)
                    totalDataSize = dataBytes
                    totalCacheSize = cacheBytes
                }

                val totalSize = apkSize + totalDataSize + totalCacheSize

                val children = buildList {
                    add(FileNode(name = "APK", path = "$appPath$VIRTUAL_APK_SUFFIX",
                        sizeBytes = apkSize, isDirectory = false))
                    if (totalDataSize > 0)
                        add(FileNode(name = "데이터", path = "$appPath$VIRTUAL_DATA_SUFFIX",
                            sizeBytes = totalDataSize, isDirectory = false))
                    if (totalCacheSize > 0)
                        add(FileNode(name = "캐시", path = "$appPath$VIRTUAL_CACHE_SUFFIX",
                            sizeBytes = totalCacheSize, isDirectory = false))
                }

                FileNode(
                    name = appLabel,
                    path = appPath,
                    sizeBytes = totalSize,
                    isDirectory = true,
                    children = children
                )
            }
            .sortedByDescending { it.sizeBytes }

        return FileNode(
            name = "설치된 앱",
            path = VIRTUAL_APPS_PATH,
            sizeBytes = appNodes.sumOf { it.sizeBytes },
            isDirectory = true,
            children = appNodes
        )
    }
}
