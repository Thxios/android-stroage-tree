package com.thxios.storagetree.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thxios.storagetree.data.preferences.AppSettings
import com.thxios.storagetree.domain.model.SortOrder
import com.thxios.storagetree.ui.theme.StorageTreeTheme

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.appSettings.collectAsStateWithLifecycle(initialValue = AppSettings())
    SettingsContent(
        settings = settings,
        onNavigateBack = onNavigateBack,
        onShowInstalledAppsChanged = { viewModel.setShowInstalledApps(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsContent(
    settings: AppSettings,
    onNavigateBack: () -> Unit,
    onShowInstalledAppsChanged: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Section: Display
            Text(
                text = "표시",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "설치된 앱 표시", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "파일 탐색기에 설치된 앱 크기를 표시합니다",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.showInstalledApps,
                    onCheckedChange = onShowInstalledAppsChanged
                )
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    StorageTreeTheme {
        SettingsContent(
            settings = AppSettings(showInstalledApps = false, sortOrder = SortOrder.SIZE_DESC),
            onNavigateBack = {},
            onShowInstalledAppsChanged = {}
        )
    }
}
