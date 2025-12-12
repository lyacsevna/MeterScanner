package com.vstu.metterscanner.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.vstu.metterscanner.MeterViewModel
import com.vstu.metterscanner.data.Meter
import com.vstu.metterscanner.data.MeterType
import com.vstu.metterscanner.ui.components.MeterCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.vstu.metterscanner.Routes
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
    var selectedSortOption by remember { mutableStateOf(SortOption.DATE_DESC) }
    var selectedFilterType by remember { mutableStateOf<MeterType?>(MeterType.ELECTRICITY) } // Изначально фильтр по электричеству
    var selectedMeter by remember { mutableStateOf<Meter?>(null) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Сегодняшняя дата для фильтрации
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    val todayCount = remember(meters) {
        meters.count { meter ->
            val meterDate = meter.date.split(" ")[0]
            meterDate == today
        }
    }
    val totalCount = meters.size

    val sortedFilteredMeters = remember(meters, selectedSortOption, selectedFilterType) {
        val filtered = if (selectedFilterType != null) {
            meters.filter { meter -> meter.type == selectedFilterType }
        } else {
            meters // Если фильтр не выбран, показываем все
        }
        when (selectedSortOption) {
            SortOption.DATE_DESC -> filtered.sortedByDescending { it.date }
            SortOption.DATE_ASC -> filtered.sortedBy { it.date }
            SortOption.VALUE_DESC -> filtered.sortedByDescending { it.value }
            SortOption.VALUE_ASC -> filtered.sortedBy { it.value }
            SortOption.TYPE -> filtered.sortedBy { it.type.ordinal }
        }
    }

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
                onFilterTypeSelected = { type -> selectedFilterType = type },
                onClearFilter = { selectedFilterType = null },
                coroutineScope = coroutineScope
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
                        // Сортировка
                        var expandedSort by remember { mutableStateOf(false) }
                        IconButton(onClick = { expandedSort = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Сортировка")
                        }
                        DropdownMenu(
                            expanded = expandedSort,
                            onDismissRequest = { expandedSort = false }
                        ) {
                            SortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = if (selectedSortOption == option) MaterialTheme.colorScheme.primary else Color.Transparent
                                            )
                                            Text(option.title)
                                        }
                                    },
                                    onClick = {
                                        selectedSortOption = option
                                        expandedSort = false
                                    }
                                )
                            }
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
                // Dropdown фильтр по типу счетчиков
                FilterDropdown(
                    selectedFilterType = selectedFilterType,
                    onFilterSelected = { selectedFilterType = it },
                    onClearFilter = { selectedFilterType = null },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (sortedFilteredMeters.isEmpty()) {
                    EmptyStateView(
                        isFiltered = selectedFilterType != null,
                        onResetFilter = { selectedFilterType = null },
                        onAddNew = { navController.navigate("add") },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Статистика (просто количество)
                    FilterStatsCard(
                        meters = sortedFilteredMeters,
                        selectedFilterType = selectedFilterType,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    // Список (показывает только отфильтрованные показания)
                    // При выборе "Все типы" показываем сгруппированные по типам
                    MetersList(
                        meters = sortedFilteredMeters,
                        selectedFilterType = selectedFilterType,
                        selectedSortOption = selectedSortOption,
                        onMeterClick = { meter ->
                            selectedMeter = meter
                            showDetailsDialog = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(
    selectedFilterType: MeterType?,
    onFilterSelected: (MeterType) -> Unit,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Тип счётчика:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (selectedFilterType != null) {
                AssistChip(
                    onClick = onClearFilter,
                    label = { Text("Все типы") },
                    leadingIcon = {
                        Icon(Icons.Default.AllInclusive, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // Dropdown для выбора типа счётчика
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = selectedFilterType?.let {
                    when (it) {
                        MeterType.ELECTRICITY -> "Электричество"
                        MeterType.COLD_WATER -> "Холодная вода"
                        MeterType.HOT_WATER -> "Горячая вода"
                    }
                } ?: "Все типы",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                placeholder = { Text("Выберите тип счётчика") },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // Опция "Все типы"
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.AllInclusive,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text("Все типы")
                        }
                    },
                    onClick = {
                        onClearFilter()
                        expanded = false
                    },
                    trailingIcon = {
                        if (selectedFilterType == null) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    }
                )

                Divider()

                // Опции по типам счётчиков
                MeterType.values().forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = when (type) {
                                        MeterType.ELECTRICITY -> Icons.Default.FlashOn
                                        MeterType.COLD_WATER -> Icons.Default.WaterDrop
                                        MeterType.HOT_WATER -> Icons.Default.Whatshot
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = when (type) {
                                        MeterType.ELECTRICITY -> MaterialTheme.colorScheme.primary
                                        MeterType.COLD_WATER -> Color(0xFF2196F3)
                                        MeterType.HOT_WATER -> Color(0xFFF44336)
                                    }
                                )
                                Text(
                                    text = when (type) {
                                        MeterType.ELECTRICITY -> "Электричество"
                                        MeterType.COLD_WATER -> "Холодная вода"
                                        MeterType.HOT_WATER -> "Горячая вода"
                                    }
                                )
                            }
                        },
                        onClick = {
                            onFilterSelected(type)
                            expanded = false
                        },
                        trailingIcon = {
                            if (selectedFilterType == type) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FilterStatsCard(
    meters: List<Meter>,
    selectedFilterType: MeterType?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Информация о фильтре
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (selectedFilterType) {
                            MeterType.ELECTRICITY -> Icons.Default.FlashOn
                            MeterType.COLD_WATER -> Icons.Default.WaterDrop
                            MeterType.HOT_WATER -> Icons.Default.Whatshot
                            null -> Icons.Default.AllInclusive
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = when (selectedFilterType) {
                            MeterType.ELECTRICITY -> MaterialTheme.colorScheme.primary
                            MeterType.COLD_WATER -> Color(0xFF2196F3)
                            MeterType.HOT_WATER -> Color(0xFFF44336)
                            null -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Column {
                        Text(
                            text = when (selectedFilterType) {
                                MeterType.ELECTRICITY -> "Электричество"
                                MeterType.COLD_WATER -> "Холодная вода"
                                MeterType.HOT_WATER -> "Горячая вода"
                                null -> "Все типы счётчиков"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${meters.size} показаний",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Последнее показание - выделенный блок
            val lastMeter = meters.firstOrNull()
            if (lastMeter != null && selectedFilterType != null) {
                LastReadingBadge(
                    value = lastMeter.value,
                    unit = getUnitForType(lastMeter.type),
                    type = lastMeter.type
                )
            }
        }
    }
}

@Composable
fun LastReadingBadge(
    value: Double,
    unit: String,
    type: MeterType
) {
    Surface(
        modifier = Modifier,
        shape = MaterialTheme.shapes.large,
        color = when (type) {
            MeterType.ELECTRICITY -> MaterialTheme.colorScheme.primaryContainer
            MeterType.COLD_WATER -> Color(0xFF2196F3).copy(alpha = 0.1f)
            MeterType.HOT_WATER -> Color(0xFFF44336).copy(alpha = 0.1f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = when (type) {
                MeterType.ELECTRICITY -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                MeterType.COLD_WATER -> Color(0xFF2196F3).copy(alpha = 0.3f)
                MeterType.HOT_WATER -> Color(0xFFF44336).copy(alpha = 0.3f)
            }
        ),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Иконка и подпись
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timelapse,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = when (type) {
                        MeterType.ELECTRICITY -> MaterialTheme.colorScheme.primary
                        MeterType.COLD_WATER -> Color(0xFF2196F3)
                        MeterType.HOT_WATER -> Color(0xFFF44336)
                    }
                )
                Text(
                    text = "Последнее",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            // Значение и единицы измерения
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = String.format("%.1f", value),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = when (type) {
                        MeterType.ELECTRICITY -> MaterialTheme.colorScheme.primary
                        MeterType.COLD_WATER -> Color(0xFF2196F3)
                        MeterType.HOT_WATER -> Color(0xFFF44336)
                    }
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
fun MetersList(
    meters: List<Meter>,
    selectedFilterType: MeterType?,
    selectedSortOption: SortOption,
    onMeterClick: (Meter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (selectedFilterType == null) {
            // При выборе "Все типы" - группируем по типам
            val groupedMeters = meters.groupBy { it.type }

            MeterType.values().forEach { type ->
                val typeMeters = groupedMeters[type]
                if (!typeMeters.isNullOrEmpty()) {
                    item {
                        TypeHeader(
                            type = type,
                            count = typeMeters.size,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(typeMeters) { meter ->
                        MeterCardWithUnit(
                            meter = meter,
                            onClick = { onMeterClick(meter) }
                        )
                    }
                }
            }
        } else {
            // При выборе конкретного типа - просто показываем список
            items(meters) { meter ->
                MeterCardWithUnit(
                    meter = meter,
                    onClick = { onMeterClick(meter) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp)) // Для FAB
        }
    }
}

@Composable
fun TypeHeader(
    type: MeterType,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = when (type) {
                MeterType.ELECTRICITY -> Icons.Default.FlashOn
                MeterType.COLD_WATER -> Icons.Default.WaterDrop
                MeterType.HOT_WATER -> Icons.Default.Whatshot
            },
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = when (type) {
                MeterType.ELECTRICITY -> MaterialTheme.colorScheme.primary
                MeterType.COLD_WATER -> Color(0xFF2196F3)
                MeterType.HOT_WATER -> Color(0xFFF44336)
            }
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = when (type) {
                    MeterType.ELECTRICITY -> "Электричество"
                    MeterType.COLD_WATER -> "Холодная вода"
                    MeterType.HOT_WATER -> "Горячая вода"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$count показаний • ${getUnitForType(type)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MeterCardWithUnit(
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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (meter.type) {
                            MeterType.ELECTRICITY -> Icons.Default.FlashOn
                            MeterType.COLD_WATER -> Icons.Default.WaterDrop
                            MeterType.HOT_WATER -> Icons.Default.Whatshot
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = when (meter.type) {
                            MeterType.ELECTRICITY -> MaterialTheme.colorScheme.primary
                            MeterType.COLD_WATER -> Color(0xFF2196F3)
                            MeterType.HOT_WATER -> Color(0xFFF44336)
                        }
                    )
                    Text(
                        text = when (meter.type) {
                            MeterType.ELECTRICITY -> "Электричество"
                            MeterType.COLD_WATER -> "Холодная вода"
                            MeterType.HOT_WATER -> "Горячая вода"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = meter.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (meter.note.isNotBlank()) {
                    Text(
                        text = meter.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = String.format("%.1f", meter.value),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = when (meter.type) {
                            MeterType.ELECTRICITY -> MaterialTheme.colorScheme.primary
                            MeterType.COLD_WATER -> Color(0xFF2196F3)
                            MeterType.HOT_WATER -> Color(0xFFF44336)
                        }
                    )
                    Text(
                        text = getUnitForType(meter.type),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                if (meter.photoPath != null) {
                    Icon(
                        Icons.Default.Photo,
                        contentDescription = "Есть фото",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    isFiltered: Boolean,
    onResetFilter: () -> Unit,
    onAddNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Assessment,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isFiltered) "Нет показаний" else "Нет данных",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isFiltered)
                "Для выбранного типа счётчика показаний не найдено"
            else
                "Добавьте первое показание счётчика",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 48.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onAddNew,
                modifier = Modifier.width(200.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Добавить")
            }

            if (isFiltered) {
                OutlinedButton(
                    onClick = onResetFilter,
                    modifier = Modifier.width(200.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Все типы")
                }
            }
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
    onFilterTypeSelected: (MeterType) -> Unit,
    onClearFilter: () -> Unit,
    coroutineScope: CoroutineScope
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
            // В функции NavigationDrawerContent обновите навигацию:
            DrawerItem(
                icon = Icons.Default.Home,
                label = "Главная",
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
                label = "Добавить",
                onClick = {
                    navController.navigate(Routes.ADD_METER_SCREEN)
                    coroutineScope.launch { drawerState.close() }
                }
            )

            DrawerItem(
                icon = Icons.Default.BarChart,
                label = "Статистика",
                onClick = {
                    navController.navigate(Routes.STATISTICS_SCREEN)
                    coroutineScope.launch { drawerState.close() }
                }
            )

            DrawerItem(
                icon = Icons.Default.History,
                label = "История",
                onClick = {
                    navController.navigate(Routes.HISTORY_SCREEN)
                    coroutineScope.launch { drawerState.close() }
                }
            )

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

enum class SortOption(val title: String) {
    DATE_DESC("Сначала новые"),
    DATE_ASC("Сначала старые"),
    VALUE_DESC("По убыванию"),
    VALUE_ASC("По возрастанию"),
    TYPE("По типу")
}