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
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MeterViewModel,
    navController: NavController
) {
    val meters by viewModel.allMeters.collectAsState()
    var selectedPeriod by remember { mutableStateOf(PeriodFilter.ALL) }
    var selectedTypeFilter by remember { mutableStateOf<MeterType?>(null) }
    var expandedPeriod by remember { mutableStateOf(false) }
    var expandedType by remember { mutableStateOf(false) }

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
    val filteredMeters = remember(filteredByPeriod, selectedTypeFilter) {
        filteredByPeriod.filter { meter ->
            selectedTypeFilter?.let { meter.type == it } ?: true
        }.sortedByDescending { it.date }
    }

    // Группировка по дате
    val groupedByDate = remember(filteredMeters) {
        filteredMeters.groupBy { it.date.substringBefore(" ") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История показаний") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
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
            // Фильтры
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Фильтры",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Фильтр по периоду
                        ExposedDropdownMenuBox(
                            expanded = expandedPeriod,
                            onExpandedChange = { expandedPeriod = !expandedPeriod },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                readOnly = true,
                                value = selectedPeriod.title,
                                onValueChange = {},
                                label = { Text("Период") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPeriod)
                                }
                            )

                            ExposedDropdownMenu(
                                expanded = expandedPeriod,
                                onDismissRequest = { expandedPeriod = false }
                            ) {
                                PeriodFilter.values().forEach { period ->
                                    DropdownMenuItem(
                                        text = { Text(period.title) },
                                        onClick = {
                                            selectedPeriod = period
                                            expandedPeriod = false
                                        }
                                    )
                                }
                            }
                        }

                        // Фильтр по типу
                        ExposedDropdownMenuBox(
                            expanded = expandedType,
                            onExpandedChange = { expandedType = !expandedType },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                readOnly = true,
                                value = selectedTypeFilter?.let {
                                    when (it) {
                                        MeterType.ELECTRICITY -> "⚡ Электричество"
                                        MeterType.COLD_WATER -> "💧 Холодная вода"
                                        MeterType.HOT_WATER -> "🔥 Горячая вода"
                                    }
                                } ?: "Все типы",
                                onValueChange = {},
                                label = { Text("Тип счетчика") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType)
                                }
                            )

                            ExposedDropdownMenu(
                                expanded = expandedType,
                                onDismissRequest = { expandedType = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Все типы") },
                                    onClick = {
                                        selectedTypeFilter = null
                                        expandedType = false
                                    }
                                )
                                MeterType.values().forEach { type ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                when (type) {
                                                    MeterType.ELECTRICITY -> "⚡ Электричество"
                                                    MeterType.COLD_WATER -> "💧 Холодная вода"
                                                    MeterType.HOT_WATER -> "🔥 Горячая вода"
                                                }
                                            )
                                        },
                                        onClick = {
                                            selectedTypeFilter = type
                                            expandedType = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Кнопка сброса фильтров
                    if (selectedPeriod != PeriodFilter.ALL || selectedTypeFilter != null) {
                        OutlinedButton(
                            onClick = {
                                selectedPeriod = PeriodFilter.ALL
                                selectedTypeFilter = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Сбросить фильтры",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Сбросить фильтры")
                        }
                    }
                }
            }

            // Статистика по фильтру
            if (filteredMeters.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Найдено показаний:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = filteredMeters.size.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Период:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedPeriod.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Список показаний
            if (filteredMeters.isEmpty()) {
                EmptyHistoryView(
                    selectedPeriod = selectedPeriod,
                    selectedTypeFilter = selectedTypeFilter
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedByDate.forEach { (date, dateMeters) ->
                        item {
                            DateHeader(date)
                        }

                        items(dateMeters) { meter ->
                            MeterCard(
                                meter = meter,
                                onClick = {
                                    // Здесь можно открыть детали
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateHeader(date: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun EmptyHistoryView(
    selectedPeriod: PeriodFilter,
    selectedTypeFilter: MeterType?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = "История пуста",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Нет показаний за выбранный период",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = buildString {
                append("Попробуйте изменить ")
                if (selectedPeriod != PeriodFilter.ALL) append("период")
                if (selectedTypeFilter != null && selectedPeriod != PeriodFilter.ALL) append(" или ")
                if (selectedTypeFilter != null) append("тип счетчика")
                append(".")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

enum class PeriodFilter(val title: String) {
    TODAY("Сегодня"),
    WEEK("За неделю"),
    MONTH("За месяц"),
    YEAR("За год"),
    ALL("За все время")
}

fun filterMetersByPeriod(meters: List<Meter>, period: PeriodFilter): List<Meter> {
    val now = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    return meters.filter { meter ->
        try {
            val meterDate = LocalDate.parse(meter.date.substringBefore(" "), formatter)
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