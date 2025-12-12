@file:OptIn(ExperimentalMaterial3Api::class)

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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MeterViewModel,
    navController: NavController
) {
    val meters by viewModel.allMeters.collectAsState()

    // Фильтрация
    var selectedPeriod by remember { mutableStateOf(PeriodFilter.ALL) }
    var selectedTypeFilter by remember { mutableStateOf<MeterType?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Сортировка и группировка
    var selectedSortOption by remember { mutableStateOf(SortOption.DATE_DESC) }
    var groupingMode by remember { mutableStateOf(GroupingMode.NONE) }

    // UI состояния
    var showFilters by remember { mutableStateOf(false) }

    // Фильтрация по периоду
    val filteredByPeriod = remember(meters, selectedPeriod) {
        when (selectedPeriod) {
            PeriodFilter.TODAY -> filterMetersByPeriod(meters, PeriodFilter.TODAY)
            PeriodFilter.WEEK -> filterMetersByPeriod(meters, PeriodFilter.WEEK)
            PeriodFilter.MONTH -> filterMetersByPeriod(meters, PeriodFilter.MONTH)
            PeriodFilter.YEAR -> filterMetersByPeriod(meters, PeriodFilter.YEAR)
            PeriodFilter.ALL -> meters
        }
    }

    // Фильтрация по типу
    val filteredByType = remember(filteredByPeriod, selectedTypeFilter) {
        filteredByPeriod.filter { meter ->
            selectedTypeFilter?.let { meter.type == it } ?: true
        }
    }

    // Фильтрация по поиску
    val filteredBySearch = remember(filteredByType, searchQuery) {
        if (searchQuery.isBlank()) {
            filteredByType
        } else {
            filteredByType.filter { meter ->
                meter.note.contains(searchQuery, ignoreCase = true) ||
                        meter.value.toString().contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Сортировка
    val sortedMeters = remember(filteredBySearch, selectedSortOption) {
        when (selectedSortOption) {
            SortOption.DATE_DESC -> filteredBySearch.sortedByDescending { it.date }
            SortOption.DATE_ASC -> filteredBySearch.sortedBy { it.date }
            SortOption.VALUE_DESC -> filteredBySearch.sortedByDescending { it.value }
            SortOption.VALUE_ASC -> filteredBySearch.sortedBy { it.value }
            else -> filteredBySearch
        }
    }

    // Группировка
    val groupedMeters = remember(sortedMeters, groupingMode) {
        when (groupingMode) {
            GroupingMode.NONE -> mapOf("Все показания" to sortedMeters)
            GroupingMode.BY_DATE -> sortedMeters.groupBy {
                try {
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    val dateTime = LocalDateTime.parse(it.date, formatter)
                    dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                } catch (e: Exception) {
                    it.date.substringBefore(" ")
                }
            }
            GroupingMode.BY_TYPE -> sortedMeters.groupBy {
                when (it.type) {
                    MeterType.ELECTRICITY -> "⚡ Электричество"
                    MeterType.COLD_WATER -> "💧 Холодная вода"
                    MeterType.HOT_WATER -> "🔥 Горячая вода"
                }
            }
        }
    }

    // Проверяем, применены ли фильтры
    val hasActiveFilters = remember {
        derivedStateOf {
            selectedPeriod != PeriodFilter.ALL ||
                    selectedTypeFilter != null ||
                    searchQuery.isNotBlank()
        }
    }

    // Функция для применения фильтров и скрытия панели
    fun applyFiltersAndHide() {
        showFilters = false
    }

    // Функция для сброса всех фильтров
    fun resetAllFilters() {
        selectedPeriod = PeriodFilter.ALL
        selectedTypeFilter = null
        searchQuery = ""
        selectedSortOption = SortOption.DATE_DESC
        groupingMode = GroupingMode.NONE
        showFilters = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("История показаний")
                        if (sortedMeters.isNotEmpty()) {
                            Text(
                                text = "${sortedMeters.size} показаний",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    // Кнопка фильтров
                    IconButton(
                        onClick = { showFilters = !showFilters }
                    ) {
                        Badge(
                            containerColor = if (hasActiveFilters.value) MaterialTheme.colorScheme.primary else Color.Transparent
                        ) {
                            Icon(
                                if (showFilters) Icons.Default.FilterAlt else Icons.Default.FilterList,
                                contentDescription = "Фильтры"
                            )
                        }
                    }

                    // Кнопка сброса фильтров
                    if (hasActiveFilters.value) {
                        IconButton(
                            onClick = { resetAllFilters() }
                        ) {
                            Icon(Icons.Default.Clear, "Сбросить фильтры")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Поисковая строка (всегда видна)
            CompactSearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = {
                    searchQuery = it
                    // Скрываем панель фильтров при активном поиске
                    showFilters = false
                },
                onClearSearch = { searchQuery = "" },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // Активные фильтры (чипы)
            if (hasActiveFilters.value && !showFilters) {
                ActiveFiltersRow(
                    selectedPeriod = selectedPeriod,
                    selectedTypeFilter = selectedTypeFilter,
                    onClearFilters = { resetAllFilters() },
                    onEditFilters = { showFilters = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Панель фильтров
            if (showFilters) {
                CompactFiltersPanel(
                    selectedPeriod = selectedPeriod,
                    onPeriodChange = {
                        selectedPeriod = it
                        applyFiltersAndHide()
                    },
                    selectedTypeFilter = selectedTypeFilter,
                    onTypeFilterChange = {
                        selectedTypeFilter = it
                        applyFiltersAndHide()
                    },
                    selectedSortOption = selectedSortOption,
                    onSortOptionChange = {
                        selectedSortOption = it
                        applyFiltersAndHide()
                    },
                    groupingMode = groupingMode,
                    onGroupingModeChange = {
                        groupingMode = it
                        applyFiltersAndHide()
                    },
                    onApplyFilters = { applyFiltersAndHide() },
                    onResetFilters = { resetAllFilters() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Информация о текущих настройках
            if (sortedMeters.isNotEmpty() && !showFilters) {
                CurrentSettingsInfo(
                    meterCount = sortedMeters.size,
                    selectedPeriod = selectedPeriod,
                    groupingMode = groupingMode,
                    selectedSortOption = selectedSortOption,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Список показаний (основное пространство)
            if (sortedMeters.isEmpty()) {
                EmptyHistoryView(
                    hasActiveFilters = hasActiveFilters.value,
                    onClearFilters = { resetAllFilters() },
                    onShowFilters = { showFilters = true },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedMeters.forEach { (groupTitle, groupMeters) ->
                        if (groupingMode != GroupingMode.NONE && groupMeters.isNotEmpty()) {
                            item {
                                CompactGroupHeader(
                                    title = groupTitle,
                                    count = groupMeters.size,
                                    groupingMode = groupingMode
                                )
                            }
                        }

                        items(groupMeters) { meter ->
                            MeterCard(
                                meter = meter,
                                onClick = {
                                    // Можно открыть детали или редактирование
                                },
                                showUnit = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Поиск",
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(10.dp))

            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Поиск по заметкам...") },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            if (searchQuery.isNotBlank()) {
                IconButton(
                    onClick = onClearSearch,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Очистить поиск",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveFiltersRow(
    selectedPeriod: PeriodFilter,
    selectedTypeFilter: MeterType?,
    onClearFilters: () -> Unit,
    onEditFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Чипы активных фильтров
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (selectedPeriod != PeriodFilter.ALL) {
                SuggestionChip(
                    onClick = onEditFilters,
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(selectedPeriod.title)
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Изменить",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                )
            }

            selectedTypeFilter?.let { type ->
                SuggestionChip(
                    onClick = onEditFilters,
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                when (type) {
                                    MeterType.ELECTRICITY -> "Электричество"
                                    MeterType.COLD_WATER -> "Холодная вода"
                                    MeterType.HOT_WATER -> "Горячая вода"
                                }
                            )
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Изменить",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                )
            }
        }

        // Кнопка сброса
        TextButton(
            onClick = onClearFilters,
            modifier = Modifier.height(32.dp)
        ) {
            Icon(
                Icons.Default.Clear,
                contentDescription = "Сбросить",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Сбросить")
        }
    }
}

@Composable
fun CompactFiltersPanel(
    selectedPeriod: PeriodFilter,
    onPeriodChange: (PeriodFilter) -> Unit,
    selectedTypeFilter: MeterType?,
    onTypeFilterChange: (MeterType?) -> Unit,
    selectedSortOption: SortOption,
    onSortOptionChange: (SortOption) -> Unit,
    groupingMode: GroupingMode,
    onGroupingModeChange: (GroupingMode) -> Unit,
    onApplyFilters: () -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedPeriod by remember { mutableStateOf(false) }
    var expandedSort by remember { mutableStateOf(false) }
    var expandedGroup by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Тип счетчика с иконками
            Column {
                Text(
                    text = "Тип счетчика",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Кнопка "Все"
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { onTypeFilterChange(null) },
                        label = { Text("Все") },
                        leadingIcon = if (selectedTypeFilter == null) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )

                    // Кнопки для каждого типа с иконками
                    MeterType.values().forEach { type ->
                        FilterChip(
                            selected = selectedTypeFilter == type,
                            onClick = { onTypeFilterChange(type) },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        when (type) {
                                            MeterType.ELECTRICITY -> Icons.Default.FlashOn
                                            MeterType.COLD_WATER -> Icons.Default.WaterDrop
                                            MeterType.HOT_WATER -> Icons.Default.Whatshot
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        when (type) {
                                            MeterType.ELECTRICITY -> "Эл"
                                            MeterType.COLD_WATER -> "ХВ"
                                            MeterType.HOT_WATER -> "ГВ"
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Период в дропдауне
            Column {
                Text(
                    text = "Период",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                ExposedDropdownMenuBox(
                    expanded = expandedPeriod,
                    onExpandedChange = { expandedPeriod = !expandedPeriod }
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        value = selectedPeriod.title,
                        onValueChange = {},
                        leadingIcon = {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPeriod)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = expandedPeriod,
                        onDismissRequest = { expandedPeriod = false }
                    ) {
                        PeriodFilter.values().forEach { period ->
                            DropdownMenuItem(
                                text = { Text(period.title) },
                                onClick = {
                                    onPeriodChange(period)
                                    expandedPeriod = false
                                }
                            )
                        }
                    }
                }
            }

            // Сортировка в дропдауне
            Column {
                Text(
                    text = "Сортировка",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                ExposedDropdownMenuBox(
                    expanded = expandedSort,
                    onExpandedChange = { expandedSort = !expandedSort }
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        value = selectedSortOption.title,
                        onValueChange = {},
                        leadingIcon = {
                            Icon(
                                when (selectedSortOption) {
                                    SortOption.DATE_DESC -> Icons.Default.ArrowDownward
                                    SortOption.DATE_ASC -> Icons.Default.ArrowUpward
                                    SortOption.VALUE_DESC -> Icons.Default.TrendingDown
                                    SortOption.VALUE_ASC -> Icons.Default.TrendingUp
                                },
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSort)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = expandedSort,
                        onDismissRequest = { expandedSort = false }
                    ) {
                        SortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.title) },
                                onClick = {
                                    onSortOptionChange(option)
                                    expandedSort = false
                                }
                            )
                        }
                    }
                }
            }

            // Компактная группировка
            Column {
                Text(
                    text = "Группировка",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GroupingMode.values().forEach { mode ->
                        val icon = when (mode) {
                            GroupingMode.NONE -> Icons.Default.FormatListBulleted
                            GroupingMode.BY_DATE -> Icons.Default.DateRange
                            GroupingMode.BY_TYPE -> Icons.Default.Category
                        }

                        val label = when (mode) {
                            GroupingMode.NONE -> "Нет"
                            GroupingMode.BY_DATE -> "Дата"
                            GroupingMode.BY_TYPE -> "Тип"
                        }

                        FilterChip(
                            selected = groupingMode == mode,
                            onClick = { onGroupingModeChange(mode) },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(label)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Кнопки действий
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onResetFilters,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Сброс")
                }

                Button(
                    onClick = onApplyFilters,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Готово")
                }
            }
        }
    }
}



@Composable
fun CurrentSettingsInfo(
    meterCount: Int,
    selectedPeriod: PeriodFilter,
    groupingMode: GroupingMode,
    selectedSortOption: SortOption,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$meterCount показаний",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Сортировка: ${selectedSortOption.title.lowercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = selectedPeriod.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = groupingMode.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CompactGroupHeader(
    title: String,
    count: Int,
    groupingMode: GroupingMode
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = when (groupingMode) {
                GroupingMode.BY_DATE -> MaterialTheme.colorScheme.primary
                GroupingMode.BY_TYPE -> MaterialTheme.colorScheme.secondary
                GroupingMode.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        Badge(
            containerColor = when (groupingMode) {
                GroupingMode.BY_DATE -> MaterialTheme.colorScheme.primaryContainer
                GroupingMode.BY_TYPE -> MaterialTheme.colorScheme.secondaryContainer
                GroupingMode.NONE -> MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = when (groupingMode) {
                GroupingMode.BY_DATE -> MaterialTheme.colorScheme.onPrimaryContainer
                GroupingMode.BY_TYPE -> MaterialTheme.colorScheme.onSecondaryContainer
                GroupingMode.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun EmptyHistoryView(
    hasActiveFilters: Boolean,
    onClearFilters: () -> Unit,
    onShowFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = "Нет результатов",
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (hasActiveFilters) {
            Text(
                text = "Показания не найдены",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Попробуйте изменить параметры фильтрации",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 48.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onClearFilters) {
                    Text("Сбросить фильтры")
                }

                Button(onClick = onShowFilters) {
                    Text("Изменить фильтры")
                }
            }
        } else {
            Text(
                text = "Нет показаний",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Добавьте первое показание в главном меню",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 48.dp)
            )
        }
    }
}

// ИСПРАВЛЕННАЯ функция фильтрации по периоду
fun filterMetersByPeriod(meters: List<Meter>, period: PeriodFilter): List<Meter> {
    val now = LocalDate.now()

    return meters.filter { meter ->
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val meterDateTime = LocalDateTime.parse(meter.date, formatter)
            val meterDate = meterDateTime.toLocalDate()

            when (period) {
                PeriodFilter.TODAY -> meterDate == now
                PeriodFilter.WEEK -> ChronoUnit.DAYS.between(meterDate, now) <= 7
                PeriodFilter.MONTH -> ChronoUnit.DAYS.between(meterDate, now) <= 30
                PeriodFilter.YEAR -> meterDate.year == now.year
                PeriodFilter.ALL -> true
            }
        } catch (e: Exception) {
            false
        }
    }
}


// Модели данных для HistoryScreen
enum class PeriodFilter(val title: String) {
    TODAY("Сегодня"),
    WEEK("Неделя"),
    MONTH("Месяц"),
    YEAR("Год"),
    ALL("Все время")
}

enum class SortOption(val title: String) {
    DATE_DESC("Сначала новые"),
    DATE_ASC("Сначала старые"),
    VALUE_DESC("По убыванию"),
    VALUE_ASC("По возрастанию")
}

enum class GroupingMode(val title: String) {
    NONE("Без группировки"),
    BY_DATE("По дате"),
    BY_TYPE("По типу")
}