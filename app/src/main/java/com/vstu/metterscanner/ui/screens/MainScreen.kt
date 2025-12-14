package com.vstu.metterscanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.vstu.metterscanner.MeterViewModel
import com.vstu.metterscanner.data.Meter
import com.vstu.metterscanner.data.MeterType
import com.vstu.metterscanner.Routes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MeterViewModel,
    navController: NavController
) {
    val meters by viewModel.allMeters.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var selectedMeter by remember { mutableStateOf<Meter?>(null) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }


    val recentMeters = remember(meters) {
        meters.sortedByDescending { it.date }.take(10)
    }

    val today = LocalDate.now()
    val todayCount = remember(meters) {
        meters.count { meter ->
            try {
                val meterDateStr = meter.date.split(" ")[0]
                val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val meterDate = LocalDate.parse(meterDateStr, dateFormatter)
                meterDate == today
            } catch (e: Exception) {
                false
            }
        }
    }
    val totalCount = meters.size

    if (showDetailsDialog && selectedMeter != null) {
        MeterDetailsDialog(
            meter = selectedMeter!!,
            viewModel = viewModel,
            onDismiss = {
                showDetailsDialog = false
                selectedMeter = null
            },
            snackbarHostState = snackbarHostState
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                navController = navController,
                drawerState = drawerState,
                coroutineScope = coroutineScope,
                totalCount = totalCount
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Мои счётчики", fontWeight = FontWeight.Medium)
                            Text(
                                text = "$totalCount показаний • $todayCount сегодня",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.open() } }
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { navController.navigate(Routes.HISTORY_SCREEN) }
                        ) {
                            Icon(Icons.Default.History, contentDescription = "История")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navController.navigate("add") },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить", tint = Color.White)

                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Быстрая статистика (карточка)
                QuickStatsCard(
                    totalCount = totalCount,
                    todayCount = todayCount,
                    recentMeters = recentMeters,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                if (recentMeters.isEmpty()) {
                    EmptyMainView(
                        onAddNew = { navController.navigate("add") },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(recentMeters) { meter ->
                            SimpleMeterCard(
                                meter = meter,
                                onClick = {
                                    selectedMeter = meter
                                    showDetailsDialog = true
                                }
                            )
                        }

                        item {
                            if (meters.size > 10) {
                                OutlinedButton(
                                    onClick = { navController.navigate(Routes.HISTORY_SCREEN) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp)
                                ) {
                                    Text("Вся история (${meters.size})")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.ArrowForward, null)
                                }
                            }

                            Spacer(modifier = Modifier.height(80.dp)) // Для FAB
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickStatsCard(
    totalCount: Int,
    todayCount: Int,
    recentMeters: List<Meter>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Всего показаний",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = totalCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$todayCount сегодня",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }


            val lastMeter = recentMeters.firstOrNull()
            if (lastMeter != null) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Последнее",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = String.format("%.1f", lastMeter.value),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = when (lastMeter.type) {
                                MeterType.ELECTRICITY -> MaterialTheme.colorScheme.primary
                                MeterType.COLD_WATER -> Color(0xFF2196F3)
                                MeterType.HOT_WATER -> Color(0xFFF44336)
                            }
                        )
                        Text(
                            text = getUnitForType(lastMeter.type),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    Text(
                        text = lastMeter.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleMeterCard(
    meter: Meter,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = when (meter.type) {
                        MeterType.ELECTRICITY -> Icons.Default.FlashOn
                        MeterType.COLD_WATER -> Icons.Default.WaterDrop
                        MeterType.HOT_WATER -> Icons.Default.Whatshot
                    },
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = when (meter.type) {
                        MeterType.ELECTRICITY -> Color(0xFFFFFF00)
                        MeterType.COLD_WATER -> Color(0xFF2196F3)
                        MeterType.HOT_WATER -> Color(0xFFF44336)
                    }
                )

                Column {
                    Text(
                        text = when (meter.type) {
                            MeterType.ELECTRICITY -> "Электричество"
                            MeterType.COLD_WATER -> "Холодная вода"
                            MeterType.HOT_WATER -> "Горячая вода"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = meter.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = String.format("%.1f", meter.value),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (meter.type) {
                        MeterType.ELECTRICITY -> MaterialTheme.colorScheme.primary
                        MeterType.COLD_WATER -> MaterialTheme.colorScheme.primary
                        MeterType.HOT_WATER -> MaterialTheme.colorScheme.primary
                    }
                )
                Text(
                    text = getUnitForType(meter.type),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyMainView(
    onAddNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AddCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Нет показаний",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Добавьте первое показание счётчика",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 48.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAddNew,
            modifier = Modifier.width(200.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Добавить")
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterDetailsDialog(
    meter: Meter,
    viewModel: MeterViewModel,
    onDismiss: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Детали показания") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Информация
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Тип
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Тип:", fontWeight = FontWeight.Medium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (meter.type) {
                                        MeterType.ELECTRICITY -> Icons.Default.FlashOn
                                        MeterType.COLD_WATER -> Icons.Default.WaterDrop
                                        MeterType.HOT_WATER -> Icons.Default.Whatshot
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(when (meter.type) {
                                    MeterType.ELECTRICITY -> "Электричество"
                                    MeterType.COLD_WATER -> "Холодная вода"
                                    MeterType.HOT_WATER -> "Горячая вода"
                                })
                            }
                        }

                        // Значение
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Показание:", fontWeight = FontWeight.Medium)
                            Text(
                                "${meter.value} ${getUnitForType(meter.type)}",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Дата
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Дата:", fontWeight = FontWeight.Medium)
                            Text(meter.date)
                        }

                        // Заметка
                        if (meter.note.isNotBlank()) {
                            Column {
                                Text("Заметка:", fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(meter.note)
                            }
                        }
                    }
                }

                // Фото
                if (meter.photoPath != null) {
                    val bitmap = remember(meter.photoPath) {
                        viewModel.loadBitmapFromFile(context, meter.photoPath)
                    }
                    if (bitmap != null) {
                        Card {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Фото:", fontWeight = FontWeight.Medium)
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Фото счетчика",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Закрыть")
                }
                Button(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Изменить")
                }
            }
        }
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить?") },
            text = { Text("Показание будет удалено безвозвратно") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.deleteMeter(meter)
                            snackbarHostState.showSnackbar("Показание удалено")
                            showDeleteDialog = false
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showEditDialog) {
        EditMeterDialog(
            meter = meter,
            onDismiss = { showEditDialog = false },
            onSave = { updatedMeter ->
                coroutineScope.launch {
                    viewModel.updateMeter(updatedMeter)
                    snackbarHostState.showSnackbar("Показание обновлено")
                    showEditDialog = false
                    onDismiss()
                }
            }
        )
    }
}

@Composable
fun EditMeterDialog(
    meter: Meter,
    onDismiss: () -> Unit,
    onSave: (Meter) -> Unit
) {
    var value by remember { mutableStateOf(meter.value.toString()) }
    var note by remember { mutableStateOf(meter.note) }
    val isValid by remember(value) {
        derivedStateOf { value.isNotBlank() && value.toDoubleOrNull() != null }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Изменить показание") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) value = it },
                    label = { Text("Значение (${getUnitForType(meter.type)})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    isError = value.isNotBlank() && !isValid
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Заметка") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        val updatedMeter = meter.copy(
                            value = value.toDouble(),
                            note = note
                        )
                        onSave(updatedMeter)
                    }
                },
                enabled = isValid
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationDrawerContent(
    navController: NavController,
    drawerState: DrawerState,
    coroutineScope: CoroutineScope,
    totalCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Заголовок
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(24.dp)
        ) {
            Icon(
                Icons.Default.Speed,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Счётчики",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Учёт коммунальных услуг",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f)
            )
        }

        // Меню
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp)
        ) {
            DrawerItem(
                icon = Icons.Default.Dashboard,
                label = "Главная",
                badge = { if (totalCount > 0) Badge { Text(totalCount.toString()) } },
                onClick = {
                    navController.navigate(Routes.MAIN_SCREEN) {
                        popUpTo(Routes.MAIN_SCREEN) { inclusive = true }
                    }
                    coroutineScope.launch { drawerState.close() }
                },
                selected = true
            )

            DrawerItem(
                icon = Icons.Default.Add,
                label = "Добавить показания",
                onClick = {
                    navController.navigate(Routes.ADD_METER_SCREEN)
                    coroutineScope.launch { drawerState.close() }
                }
            )

            DrawerItem(
                icon = Icons.Default.History,
                label = "История и поиск",
                onClick = {
                    navController.navigate(Routes.HISTORY_SCREEN)
                    coroutineScope.launch { drawerState.close() }
                }
            )

            DrawerItem(
                icon = Icons.Default.BarChart,
                label = "Аналитика",
                onClick = {
                    navController.navigate(Routes.STATISTICS_SCREEN)
                    coroutineScope.launch { drawerState.close() }
                }
            )

            Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            DrawerItem(
                icon = Icons.Default.Settings,
                label = "Настройки",
                onClick = {
                    navController.navigate(Routes.SETTINGS_SCREEN)
                    coroutineScope.launch { drawerState.close() }
                }
            )

            DrawerItem(
                icon = Icons.Default.Help,
                label = "Помощь",
                onClick = {
                    navController.navigate(Routes.HELP_SCREEN)
                    coroutineScope.launch { drawerState.close() }
                }
            )

            DrawerItem(
                icon = Icons.Default.Info,
                label = "О приложении",
                onClick = {
                    navController.navigate(Routes.ABOUT_SCREEN)
                    coroutineScope.launch { drawerState.close() }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    badge: @Composable (() -> Unit)? = null
) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = {
            Box {
                Icon(icon, contentDescription = null)
                badge?.invoke()
            }
        },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    selected: Boolean = false
) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, contentDescription = null) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
    )
}

// Функции для получения единиц измерения и опций сортировки
fun getUnitForType(type: MeterType): String = when (type) {
    MeterType.ELECTRICITY -> "кВт·ч"
    MeterType.COLD_WATER -> "м³"
    MeterType.HOT_WATER -> "м³"

}

