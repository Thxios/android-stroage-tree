package com.thxios.storagetree.ui.permission

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thxios.storagetree.ui.theme.StorageTreeTheme

@Composable
fun PermissionScreen(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onNavigateToExplorer: () -> Unit
) {
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            onNavigateToExplorer()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "StorageTree",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "저장소 분석을 위해 파일 접근 권한이 필요합니다.\n모든 파일 관리 권한을 허용해 주세요.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRequestPermission) {
            Text("권한 허용")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionScreenPreview() {
    StorageTreeTheme {
        PermissionScreen(
            hasPermission = false,
            onRequestPermission = {},
            onNavigateToExplorer = {}
        )
    }
}
