package com.vstu.metterscanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var autoSaveEnabled by remember { mutableStateOf(true) }
    var darkModeEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Основные настройки",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SettingsCard {
                    Column {
                        SettingsItem(
                            icon = Icons.Default.Notifications,
                            title = "Уведомления",
                            subtitle = "Напоминания о передаче показаний",
                            trailing = {
                                Switch(
                                    checked = notificationsEnabled,
                                    onCheckedChange = { notificationsEnabled = it }
                                )
                            }
                        )

                        SettingsItem(
                            icon = Icons.Default.Save,
                            title = "Автосохранение",
                            subtitle = "Автоматически сохранять фото",
                            trailing = {
                                Switch(
                                    checked = autoSaveEnabled,
                                    onCheckedChange = { autoSaveEnabled = it }
                                )
                            }
                        )

                        SettingsItem(
                            icon = Icons.Default.DarkMode,
                            title = "Темная тема",
                            subtitle = "Использовать темную тему",
                            trailing = {
                                Switch(
                                    checked = true,
                                    onCheckedChange = { darkModeEnabled = it }
                                )
                            }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Данные",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SettingsCard {
                    Column {
                        SettingsItem(
                            icon = Icons.Default.Backup,
                            title = "Резервное копирование",
                            subtitle = "Создать копию данных",
                            onClick = { /* TODO */ }
                        )

                        SettingsItem(
                            icon = Icons.Default.Restore,
                            title = "Восстановление",
                            subtitle = "Восстановить из резервной копии",
                            onClick = { /* TODO */ }
                        )

                        SettingsItem(
                            icon = Icons.Default.Delete,
                            title = "Очистить данные",
                            subtitle = "Удалить все показания",
                            onClick = { /* TODO */ },
                            isWarning = true
                        )
                    }
                }
            }

            item {
                Text(
                    text = "О приложении",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SettingsCard {
                    Column {
                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "Версия",
                            subtitle = "1.0.0",
                            showDivider = false
                        )

                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        content()
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    isWarning: Boolean = false,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = if (isWarning) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isWarning) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            trailing?.invoke()

            if (onClick != null && trailing == null) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showDivider && onClick != null) {
            Divider(
                modifier = Modifier.padding(start = 56.dp),
                thickness = 0.5.dp
            )
        }
    }

}