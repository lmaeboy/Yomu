package com.example.yomu.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.yomu.data.ThemePreferences
import com.example.yomu.theme.AppTheme

@Composable
fun SettingsDialog(
    themePreferences: ThemePreferences,
    onDismissRequest: () -> Unit
) {
    val currentTheme by themePreferences.currentTheme.collectAsState()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("App Settings") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(
                    text = "Appearance Theme",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                val options = listOf(
                    AppTheme.LIGHT to "☀️ Light Mode (Default)",
                    AppTheme.DARK to "🌙 Dark Mode",
                    AppTheme.SYSTEM to "📱 Follow System"
                )

                options.forEach { (theme, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { themePreferences.setTheme(theme) }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = (currentTheme == theme),
                            onClick = { themePreferences.setTheme(theme) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        }
    )
}
