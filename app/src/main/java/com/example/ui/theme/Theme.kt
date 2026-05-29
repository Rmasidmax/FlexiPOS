package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.model.AppTheme

private val ClassicNavyScheme = lightColorScheme(
    primary = Color(0xFF1B365D),
    secondary = Color(0xFF2563EB),
    tertiary = Color(0xFF60A5FA),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

private val WarmAmberScheme = lightColorScheme(
    primary = Color(0xFFB45309),
    secondary = Color(0xFFD97706),
    tertiary = Color(0xFFF59E0B),
    background = Color(0xFFFFFBEB),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF451A03),
    onSurface = Color(0xFF451A03)
)

private val EmeraldGreenScheme = lightColorScheme(
    primary = Color(0xFF047857),
    secondary = Color(0xFF059669),
    tertiary = Color(0xFF10B981),
    background = Color(0xFFF0FDF4),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF062F1E),
    onSurface = Color(0xFF062F1E)
)

private val MinimalistDarkScheme = darkColorScheme(
    primary = Color(0xFFF8FAFC),
    secondary = Color(0xFF475569),
    tertiary = Color(0xFF64748B),
    background = Color(0xFF090D16),
    surface = Color(0xFF151D30),
    onPrimary = Color(0xFF090D16),
    onSecondary = Color.White,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC)
)

@Composable
fun MyApplicationTheme(
    theme: AppTheme = AppTheme.CLASSIC_NAVY,
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        AppTheme.CLASSIC_NAVY -> ClassicNavyScheme
        AppTheme.WARM_AMBER -> WarmAmberScheme
        AppTheme.EMERALD_GREEN -> EmeraldGreenScheme
        AppTheme.MINIMALIST_DARK -> MinimalistDarkScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
