package com.notesapp.tasks

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= 31 ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme(primary = Color(0xFF4DB6AC))
        else -> lightColorScheme(primary = Color(0xFF00796B))
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

private val lightCards = listOf(
    null, Color(0xFFFFF59D), Color(0xFFA5D6A7), Color(0xFF90CAF9), Color(0xFFF48FB1), Color(0xFFCE93D8)
)
private val darkCards = listOf(
    null, Color(0xFF6B5E00), Color(0xFF2E5E31), Color(0xFF1F4E79), Color(0xFF7A2A48), Color(0xFF5A2A6B)
)

const val COLOR_COUNT = 6

fun cardColor(i: Int, dark: Boolean): Color? =
    (if (dark) darkCards else lightCards).getOrNull(i)
