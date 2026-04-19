package com.thxios.storagetree.ui.components

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.thxios.storagetree.ui.theme.StorageTreeTheme

@Composable
fun SizeProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0f, 1f) },
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Preview(showBackground = true)
@Composable
private fun SizeProgressBarPreview() {
    StorageTreeTheme {
        SizeProgressBar(fraction = 0.6f)
    }
}
