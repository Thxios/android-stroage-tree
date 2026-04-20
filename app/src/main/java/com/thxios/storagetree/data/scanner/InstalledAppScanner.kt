package com.thxios.storagetree.data.scanner

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
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
        const val VIRTUAL_OBB_SUFFIX = "/obb"
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

    fun buildVirtualAppsNode(context: Context): FileNode {
        val pm = context.packageManager
        val hasUsageStats = hasUsageStatsPermission(context)
        val storageStatsManager = if (hasUsageStats)
            context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
        else null

        val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
        val appNodes = packages
            .filter { it.applicationInfo != null }
            .mapNotNull { pkg ->
                val appInfo: ApplicationInfo = pkg.applicationInfo ?: return@mapNotNull null
                val appLabel = pm.getApplicationLabel(appInfo).toString()
                val appPath = "$VIRTUAL_APPS_PATH/${pkg.packageName}"

                // APK: base + splits
                val apkSize = try {
                    val base = File(appInfo.sourceDir).length()
                    val splits = appInfo.splitSourceDirs?.sumOf { File(it).length() } ?: 0L
                    base + splits
                } catch (e: Exception) { 0L }

                // Internal data + cache via StorageStatsManager (UUID_DEFAULT = internal storage)
                var internalDataSize = 0L
                var cacheSize = 0L
                if (storageStatsManager != null) {
                    try {
                        val stats = storageStatsManager.queryStatsForPackage(
                            StorageManager.UUID_DEFAULT, pkg.packageName, Process.myUserHandle()
                        )
                        internalDataSize = stats.dataBytes
                        cacheSize = stats.cacheBytes
                    } catch (e: Exception) { /* skip */ }
                }

                // OBB directory (external, accessible with MANAGE_EXTERNAL_STORAGE)
                val obbSize = try {
                    val obbDir = File(
                        Environment.getExternalStorageDirectory(),
                        "Android/obb/${pkg.packageName}"
                    )
                    if (obbDir.exists() && obbDir.isDirectory)
                        obbDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                    else 0L
                } catch (e: Exception) { 0L }

                // External data directory (best-effort; Android 11+ may deny for other apps)
                val externalDataSize = try {
                    val extDataDir = File(
                        Environment.getExternalStorageDirectory(),
                        "Android/data/${pkg.packageName}"
                    )
                    if (extDataDir.exists() && extDataDir.isDirectory)
                        extDataDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                    else 0L
                } catch (e: SecurityException) { 0L }
                  catch (e: Exception) { 0L }

                val totalSize = apkSize + internalDataSize + cacheSize + obbSize + externalDataSize

                val children = buildList {
                    add(FileNode(name = "APK", path = "$appPath$VIRTUAL_APK_SUFFIX",
                        sizeBytes = apkSize, isDirectory = false))
                    val totalData = internalDataSize + externalDataSize
                    if (totalData > 0)
                        add(FileNode(name = "데이터", path = "$appPath$VIRTUAL_DATA_SUFFIX",
                            sizeBytes = totalData, isDirectory = false))
                    if (cacheSize > 0)
                        add(FileNode(name = "캐시", path = "$appPath$VIRTUAL_CACHE_SUFFIX",
                            sizeBytes = cacheSize, isDirectory = false))
                    if (obbSize > 0)
                        add(FileNode(name = "OBB", path = "$appPath$VIRTUAL_OBB_SUFFIX",
                            sizeBytes = obbSize, isDirectory = false))
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
