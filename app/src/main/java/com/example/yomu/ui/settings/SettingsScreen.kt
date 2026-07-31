package com.example.yomu.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.yomu.data.ThemePreferences
import com.example.yomu.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themePreferences: ThemePreferences
) {
    val currentTheme by themePreferences.currentTheme.collectAsState()
    var dropdownExpanded by remember { mutableStateOf(false) }

    val themeOptions = listOf(
        AppTheme.LIGHT to "Light",
        AppTheme.DARK to "Dark",
        AppTheme.SYSTEM to "Follow System"
    )

    val currentThemeLabel = themeOptions.firstOrNull { it.first == currentTheme }?.second ?: "Light"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "App Theme",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Select application visual mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    OutlinedButton(onClick = { dropdownExpanded = true }) {
                        Text(currentThemeLabel)
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        themeOptions.forEach { (theme, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    themePreferences.setTheme(theme)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        }
    }
}
