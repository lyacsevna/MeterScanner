package com.vstu.metterscanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.vstu.metterscanner.MeterViewModel
import com.vstu.metterscanner.data.Meter
import com.vstu.metterscanner.data.MeterType
import com.vstu.metterscanner.ui.components.MeterCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MeterViewModel,
    navController: NavController
) {
    val meters by viewModel.allMeters.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var selectedSortOption by remember { mutableStateOf(SortOption.DATE_DESC) }
    var selectedFilterType by remember { mutableStateOf<MeterType?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val sortedFilteredMeters = remember(meters, selectedSortOption, selectedFilterType) {
        val filtered = meters.filter { meter ->
            selectedFilterType?.let { meter.type == it } ?: true
        }

        when (selectedSortOption) {
            SortOption.DATE_DESC -> filtered.sortedByDescending { it.date }
            SortOption.DATE_ASC -> filtered.sortedBy { it.date }
            SortOption.VALUE_DESC -> filtered.sortedByDescending { it.value }
            SortOption.VALUE_ASC -> filtered.sortedBy { it.value }
            SortOption.TYPE -> filtered.sortedWith(
                compareBy<Meter> { it.type.ordinal }
                    .thenByDescending { it.date }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MetterScanner") },
                navigationIcon = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            drawerState.open()
                        }
                    }) {
                        Icon(Icons.Default.Menu, contentDescription = "Меню")
                    }
                },
                actions = {
                    // Меню сортировки
                    var expanded by remember { mutableStateOf(false) }

                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Сортировка")
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            SortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (selectedSortOption == option) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.size(16.dp))
                                            }
                                            Text(option.title)
                                        }
                                    },
                                    onClick = {
                                        selectedSortOption = option
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    selectedFilterType?.let { type ->
                        FilterChip(
                            selected = true,
                            onClick = { selectedFilterType = null },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        when (type) {
                                            MeterType.ELECTRICITY -> "⚡"
                                            MeterType.COLD_WATER -> "💧"
                                            MeterType.HOT_WATER -> "🔥"
                                        }
                                    )
                                    Text(
                                        when (type) {
                                            MeterType.ELECTRICITY -> "Эл."
                                            MeterType.COLD_WATER -> "Х.в."
                                            MeterType.HOT_WATER -> "Г.в."
                                        }
                                    )
                                }
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Очистить фильтр",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add") }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }
    ) { paddingValues ->
        if (sortedFilteredMeters.isEmpty()) {
            EmptyStateView(
                navController = navController,
                paddingValues = paddingValues,
                isFiltered = selectedFilterType != null,
                onResetFilter = { selectedFilterType = null }
            )
        } else {
            MetersListView(
                meters = sortedFilteredMeters,
                paddingValues = paddingValues,
                selectedFilterType = selectedFilterType,
                selectedSortOption = selectedSortOption
            )
        }
    }
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
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = "Профиль",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Личный кабинет",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Пользователь",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        NavigationDrawerItem(
            label = { Text("Главная") },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            selected = true,
            onClick = {
                coroutineScope.launch {
                    drawerState.close()
                }
            }
        )

        NavigationDrawerItem(
            label = { Text("Добавить") },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            selected = false,
            onClick = {
                navController.navigate("add")
                coroutineScope.launch {
                    drawerState.close()
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Фильтр по типу",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        MeterType.values().forEach { type ->
            NavigationDrawerItem(
                label = {
                    Row {
                        Text(
                            when (type) {
                                MeterType.ELECTRICITY -> "⚡ "
                                MeterType.COLD_WATER -> "💧 "
                                MeterType.HOT_WATER -> "🔥 "
                            }
                        )
                        Text(
                            when (type) {
                                MeterType.ELECTRICITY -> "Электричество"
                                MeterType.COLD_WATER -> "Холодная вода"
                                MeterType.HOT_WATER -> "Горячая вода"
                            }
                        )
                    }
                },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                when (type) {
                                    MeterType.ELECTRICITY -> MaterialTheme.colorScheme.primary
                                    MeterType.COLD_WATER -> Color(0xFF2196F3)
                                    MeterType.HOT_WATER -> Color(0xFFF44336)
                                },
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                },
                selected = false,
                onClick = {
                    onFilterTypeSelected(type)
                    coroutineScope.launch {
                        drawerState.close()
                    }
                }
            )
        }

        NavigationDrawerItem(
            label = { Text("Сбросить фильтр") },
            icon = { Icon(Icons.Default.Clear, contentDescription = null) },
            selected = false,
            onClick = {
                onClearFilter()
                coroutineScope.launch {
                    drawerState.close()
                }
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        NavigationDrawerItem(
            label = { Text("Статистика") },
            icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
            selected = false,
            onClick = {
                coroutineScope.launch {
                    drawerState.close()
                }
            }
        )

        NavigationDrawerItem(
            label = { Text("История") },
            icon = { Icon(Icons.Default.History, contentDescription = null) },
            selected = false,
            onClick = {
                coroutineScope.launch {
                    drawerState.close()
                }
            }
        )

        NavigationDrawerItem(
            label = { Text("Настройки") },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            selected = false,
            onClick = {
                coroutineScope.launch {
                    drawerState.close()
                }
            }
        )

        NavigationDrawerItem(
            label = { Text("Помощь") },
            icon = { Icon(Icons.Default.Help, contentDescription = null) },
            selected = false,
            onClick = {
                coroutineScope.launch {
                    drawerState.close()
                }
            }
        )

        NavigationDrawerItem(
            label = { Text("Выйти") },
            icon = { Icon(Icons.Default.Logout, contentDescription = null) },
            selected = false,
            onClick = {
                coroutineScope.launch {
                    drawerState.close()
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun EmptyStateView(
    navController: NavController,
    paddingValues: PaddingValues,
    isFiltered: Boolean,
    onResetFilter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📊",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isFiltered) "Нет показаний для выбранного фильтра" else "Показаний пока нет",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isFiltered)
                "Попробуйте изменить фильтр или добавьте новое показание"
            else
                "Нажмите + чтобы добавить первое показание",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { navController.navigate("add") },
            modifier = Modifier.fillMaxWidth(0.7f),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Добавить показание")
        }

        if (isFiltered) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onResetFilter,
                modifier = Modifier.fillMaxWidth(0.7f),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Сбросить фильтр")
            }
        }
    }
}

@Composable
fun MetersListView(
    meters: List<Meter>,
    paddingValues: PaddingValues,
    selectedFilterType: MeterType?,
    selectedSortOption: SortOption
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column {
                // Заголовок с информацией о сортировке и фильтрах
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Показания: ${meters.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Сортировка: ${selectedSortOption.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (selectedFilterType != null) {
                    Text(
                        text = "Фильтр: ${when(selectedFilterType) {
                            MeterType.ELECTRICITY -> "⚡ Электричество"
                            MeterType.COLD_WATER -> "💧 Холодная вода"
                            MeterType.HOT_WATER -> "🔥 Горячая вода"
                        }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Группировка по типу при сортировке TYPE
                if (selectedSortOption == SortOption.TYPE) {
                    val groupedByType = meters.groupBy { it.type }

                    MeterType.values().forEach { type ->
                        val typeMeters = groupedByType[type]
                        if (!typeMeters.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "${when (type) {
                                    MeterType.ELECTRICITY -> "⚡ Электричество"
                                    MeterType.COLD_WATER -> "💧 Холодная вода"
                                    MeterType.HOT_WATER -> "🔥 Горячая вода"
                                }} (${typeMeters.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            typeMeters.forEach { meter ->
                                MeterCard(meter = meter)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }

        // Если не сортировка по типу, показываем обычный список
        if (selectedSortOption != SortOption.TYPE) {
            items(meters) { meter ->
                MeterCard(meter = meter)
            }
        }

        if (meters.isNotEmpty()) {
            item {
                TotalSummaryCard(meters)
            }
        }
    }
}

@Composable
fun TotalSummaryCard(meters: List<Meter>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Итого показаний: ${meters.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            val groupedByType = meters.groupBy { it.type }
            MeterType.values().forEach { type ->
                val typeMeters = groupedByType[type]
                if (!typeMeters.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when (type) {
                                MeterType.ELECTRICITY -> "⚡ Электричество:"
                                MeterType.COLD_WATER -> "💧 Холодная вода:"
                                MeterType.HOT_WATER -> "🔥 Горячая вода:"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = typeMeters.size.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

enum class SortOption(
    val title: String
) {
    DATE_DESC("Сначала новые"),
    DATE_ASC("Сначала старые"),
    VALUE_DESC("По убыванию значения"),
    VALUE_ASC("По возрастанию значения"),
    TYPE("По типу счетчика")
}