// StatisticsScreen.kt - полная исправленная версия

package com.vstu.metterscanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import android.util.Log
import java.time.LocalDateTime


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: MeterViewModel,
    navController: NavController
) {
    val meters by viewModel.allMeters.collectAsState()
    var selectedPeriod by remember { mutableStateOf(StatsPeriod.MONTH) }

    // Отладочный вывод
    LaunchedEffect(meters) {
        Log.d("STATISTICS", "Meters loaded: ${meters.size}")
        meters.forEachIndexed { index, meter ->
            Log.d("STATISTICS", "Meter $index: type=${meter.type}, value=${meter.value}, date=${meter.date}")
        }
    }

    // Рассчитываем расходы для всех счетчиков
    val meterConsumptions = remember(meters) {
        Log.d("STATISTICS", "Calculating consumptions for ${meters.size} meters")
        calculateConsumptions(meters)
    }

    val filteredConsumptions = remember(meterConsumptions, selectedPeriod) {
        Log.d("STATISTICS", "Filtering consumptions: ${meterConsumptions.size} total")
        val filtered = filterConsumptionsForStats(meterConsumptions, selectedPeriod)
        Log.d("STATISTICS", "Filtered to: ${filtered.size}")
        filtered
    }

    // Статистика по типам (теперь по расходам)
    val statsByType = remember(filteredConsumptions) {
        val stats = calculateStatsByType(filteredConsumptions)
        Log.d("STATISTICS", "Stats by type: $stats")
        stats
    }

    // Общая статистика (теперь по расходам)
    val totalStats = remember(filteredConsumptions, selectedPeriod) {
        val stats = calculateTotalStats(filteredConsumptions, selectedPeriod)
        Log.d("STATISTICS", "Total stats: $stats")
        stats
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Статистика расходов") },
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
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Выбор периода
            item {
                PeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { selectedPeriod = it }
                )
            }

            // Общая статистика по расходам
            item {
                TotalStatisticsCard(totalStats, selectedPeriod)
            }

            // Статистика по типам счетчиков
            if (statsByType.isNotEmpty()) {
                statsByType.forEach { (type, stats) ->
                    item {
                        TypeStatisticsCard(type, stats)
                    }
                }
            } else {
                item {
                    EmptyStatisticsView(selectedPeriod)
                }
            }

            // Сравнение с предыдущим периодом
            item {
                ComparisonCard(meterConsumptions, selectedPeriod)
            }
        }
    }
}

@Composable
fun PeriodSelector(
    selectedPeriod: StatsPeriod,
    onPeriodSelected: (StatsPeriod) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Период анализа",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatsPeriod.values().forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { onPeriodSelected(period) },
                        label = { Text(period.title) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun TotalStatisticsCard(stats: TotalStats, period: StatsPeriod) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Общая статистика расходов",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = period.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Общий расход
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Summarize,
                        contentDescription = "Общий расход",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("%.1f", stats.totalConsumption),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Всего",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Средний расход в день
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Средний расход в день",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("%.1f", stats.averagePerDay),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "В день",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Типов счетчиков с данными
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Типов счетчиков",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stats.activeTypes.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Типов",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TypeStatisticsCard(type: MeterType, stats: TypeStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                getTypeColor(type),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                    Column {
                        Text(
                            text = when (type) {
                                MeterType.ELECTRICITY -> "Электричество"
                                MeterType.COLD_WATER -> "Холодная вода"
                                MeterType.HOT_WATER -> "Горячая вода"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${stats.periodCount} периодов",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Статистика расходов
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Всего:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("%.1f", stats.totalConsumption),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Среднее:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("%.1f", stats.averageConsumption),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Максимум:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("%.1f", stats.maxConsumption),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Единица измерения
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = getTypeUnit(type),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun ComparisonCard(allConsumptions: List<ConsumptionData>, currentPeriod: StatsPeriod) {
    val currentConsumptions = filterConsumptionsForStats(allConsumptions, currentPeriod)
    val previousConsumptions = filterConsumptionsForStats(allConsumptions, getPreviousPeriod(currentPeriod))

    if (previousConsumptions.isNotEmpty() && currentConsumptions.isNotEmpty()) {
        val currentTotal = currentConsumptions.sumOf { it.consumption }
        val previousTotal = previousConsumptions.sumOf { it.consumption }

        val changePercent = if (previousTotal > 0) {
            ((currentTotal - previousTotal) / previousTotal * 100)
        } else 0.0

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Сравнение периодов",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ComparisonItem(
                        title = "Текущий",
                        value = currentTotal,
                        unit = "ед.",
                        period = currentPeriod
                    )

                    Icon(
                        Icons.Default.CompareArrows,
                        contentDescription = "Сравнение",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    ComparisonItem(
                        title = "Предыдущий",
                        value = previousTotal,
                        unit = "ед.",
                        period = getPreviousPeriod(currentPeriod)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (changePercent > 0) "↑ Увеличение на ${String.format("%.1f", changePercent)}%"
                    else if (changePercent < 0) "↓ Уменьшение на ${String.format("%.1f", abs(changePercent))}%"
                    else "Без изменений",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (changePercent > 0) Color(0xFF4CAF50)
                    else if (changePercent < 0) Color(0xFFF44336)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ComparisonItem(title: String, value: Double, unit: String, period: StatsPeriod) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = String.format("%.1f", value),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = period.title,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun EmptyStatisticsView(period: StatsPeriod) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Assessment,
                contentDescription = "Нет данных",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "Нет данных о расходах за $period",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// --- Data Classes ---

data class ConsumptionData(
    val type: MeterType,
    val consumption: Double, // Расход между показаниями
    val startDate: LocalDate,
    val endDate: LocalDate,
    val daysBetween: Long // Количество дней между показаниями
)

data class TotalStats(
    val totalConsumption: Double,
    val averagePerDay: Double,
    val activeTypes: Int
)

data class TypeStats(
    val periodCount: Int,
    val totalConsumption: Double,
    val averageConsumption: Double,
    val maxConsumption: Double
)

enum class StatsPeriod(val title: String) {
    WEEK("Неделя"),
    MONTH("Месяц"),
    QUARTER("Квартал"),
    YEAR("Год"),
    ALL("Все время")
}

// --- Utility Functions ---

fun getTypeColor(type: MeterType): Color {
    return when (type) {
        MeterType.ELECTRICITY -> Color(0xFFFF9800)
        MeterType.COLD_WATER -> Color(0xFF2196F3)
        MeterType.HOT_WATER -> Color(0xFFF44336)
    }
}

fun getTypeUnit(type: MeterType): String {
    return when (type) {
        MeterType.ELECTRICITY -> "кВт·ч"
        MeterType.COLD_WATER -> "м³"
        MeterType.HOT_WATER -> "м³"
    }
}

/**
 * Рассчитывает расходы между последовательными показаниями для каждого типа счетчиков
 */
fun calculateConsumptions(meters: List<Meter>): List<ConsumptionData> {
    if (meters.isEmpty()) {
        Log.d("STATISTICS", "calculateConsumptions: meters list is empty")
        return emptyList()
    }

    // Используем формат даты из Meter.kt: "yyyy-MM-dd HH:mm"
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val consumptions = mutableListOf<ConsumptionData>()

    Log.d("STATISTICS", "calculateConsumptions: Total meters: ${meters.size}")

    // Группируем показания по типам счетчиков
    val metersByType = meters.groupBy { it.type }
    Log.d("STATISTICS", "calculateConsumptions: Meters by type: ${metersByType.keys}")

    metersByType.forEach { (type, meterReadings) ->
        Log.d("STATISTICS", "calculateConsumptions: Processing type: $type, readings: ${meterReadings.size}")

        // Сортируем показания по дате
        val sortedReadings = meterReadings.sortedBy { meter ->
            try {
                LocalDateTime.parse(meter.date, formatter) // Изменено с LocalDate на LocalDateTime
            } catch (e: Exception) {
                Log.e("STATISTICS", "Error parsing date: ${meter.date}, error: ${e.message}")
                LocalDateTime.MIN
            }
        }

        if (sortedReadings.size < 2) {
            Log.d("STATISTICS", "calculateConsumptions: Not enough readings for type $type (need at least 2)")
            return@forEach
        }

        Log.d("STATISTICS", "calculateConsumptions: Sorted readings for $type: ${sortedReadings.size}")

        // Рассчитываем расход между последовательными показаниями
        for (i in 1 until sortedReadings.size) {
            val previous = sortedReadings[i - 1]
            val current = sortedReadings[i]

            try {
                val prevDateTime = LocalDateTime.parse(previous.date, formatter)
                val currDateTime = LocalDateTime.parse(current.date, formatter)

                val prevDate = prevDateTime.toLocalDate()
                val currDate = currDateTime.toLocalDate()

                val consumption = current.value - previous.value

                Log.d("STATISTICS", "calculateConsumptions: Comparing ${prevDateTime} (${previous.value}) -> ${currDateTime} (${current.value}) = $consumption")

                if (consumption > 0) {
                    consumptions.add(
                        ConsumptionData(
                            type = type,
                            consumption = consumption,
                            startDate = prevDate,
                            endDate = currDate,
                            daysBetween = ChronoUnit.DAYS.between(prevDate, currDate)
                        )
                    )
                    Log.d("STATISTICS", "calculateConsumptions: Added consumption for $type: $consumption")
                } else {
                    Log.d("STATISTICS", "calculateConsumptions: Skipping non-positive consumption: $consumption")
                }
            } catch (e: Exception) {
                Log.e("STATISTICS", "calculateConsumptions: Error calculating consumption: ${e.message}")
                continue
            }
        }
    }

    Log.d("STATISTICS", "calculateConsumptions: Total consumptions calculated: ${consumptions.size}")
    return consumptions
}
fun filterConsumptionsForStats(consumptions: List<ConsumptionData>, period: StatsPeriod): List<ConsumptionData> {
    if (consumptions.isEmpty()) {
        Log.d("STATISTICS", "filterConsumptionsForStats: No consumptions to filter")
        return emptyList()
    }

    val now = LocalDate.now()
    Log.d("STATISTICS", "filterConsumptionsForStats: Current date: $now, period: $period")
    Log.d("STATISTICS", "filterConsumptionsForStats: Total consumptions before filter: ${consumptions.size}")

    val result = when (period) {
        StatsPeriod.WEEK -> consumptions.filter {
            val daysBetween = ChronoUnit.DAYS.between(it.endDate, now)
            daysBetween <= 7
        }
        StatsPeriod.MONTH -> consumptions.filter {
            val daysBetween = ChronoUnit.DAYS.between(it.endDate, now)
            daysBetween <= 30
        }
        StatsPeriod.QUARTER -> consumptions.filter {
            val daysBetween = ChronoUnit.DAYS.between(it.endDate, now)
            daysBetween <= 90
        }
        StatsPeriod.YEAR -> consumptions.filter {
            it.endDate.year == now.year
        }
        StatsPeriod.ALL -> consumptions
    }

    Log.d("STATISTICS", "filterConsumptionsForStats: Consumptions after filter: ${result.size}")
    return result
}

fun calculateStatsByType(consumptions: List<ConsumptionData>): Map<MeterType, TypeStats> {
    val result = mutableMapOf<MeterType, TypeStats>()

    if (consumptions.isEmpty()) {
        Log.d("STATISTICS", "calculateStatsByType: No consumptions")
        return result
    }

    // Группируем по типам
    val consumptionsByType = consumptions.groupBy { it.type }

    consumptionsByType.forEach { (type, typeConsumptions) ->
        if (typeConsumptions.isNotEmpty()) {
            val consumptionsList = typeConsumptions.map { it.consumption }
            result[type] = TypeStats(
                periodCount = typeConsumptions.size,
                totalConsumption = consumptionsList.sum(),
                averageConsumption = consumptionsList.average(),
                maxConsumption = consumptionsList.maxOrNull() ?: 0.0
            )
            Log.d("STATISTICS", "calculateStatsByType: Stats for $type: ${result[type]}")
        }
    }

    return result
}

fun calculateTotalStats(consumptions: List<ConsumptionData>, period: StatsPeriod): TotalStats {
    if (consumptions.isEmpty()) {
        Log.d("STATISTICS", "calculateTotalStats: No consumptions")
        return TotalStats(
            totalConsumption = 0.0,
            averagePerDay = 0.0,
            activeTypes = 0
        )
    }

    val daysInPeriod = when (period) {
        StatsPeriod.WEEK -> 7
        StatsPeriod.MONTH -> 30
        StatsPeriod.QUARTER -> 90
        StatsPeriod.YEAR -> 365
        StatsPeriod.ALL -> {
            val dates = consumptions.flatMap { listOf(it.startDate, it.endDate) }
            val minDate = dates.minOrNull() ?: LocalDate.now()
            val maxDate = dates.maxOrNull() ?: LocalDate.now()
            ChronoUnit.DAYS.between(minDate, maxDate).toInt().coerceAtLeast(1)
        }
    }

    val totalConsumption = consumptions.sumOf { it.consumption }
    val activeTypes = consumptions.map { it.type }.distinct().size

    Log.d("STATISTICS", "calculateTotalStats: totalConsumption=$totalConsumption, daysInPeriod=$daysInPeriod, activeTypes=$activeTypes")

    return TotalStats(
        totalConsumption = totalConsumption,
        averagePerDay = if (daysInPeriod > 0) totalConsumption / daysInPeriod else 0.0,
        activeTypes = activeTypes
    )
}

fun getPreviousPeriod(current: StatsPeriod): StatsPeriod {
    return when (current) {
        StatsPeriod.WEEK -> StatsPeriod.WEEK
        StatsPeriod.MONTH -> StatsPeriod.WEEK
        StatsPeriod.QUARTER -> StatsPeriod.MONTH
        StatsPeriod.YEAR -> StatsPeriod.QUARTER
        StatsPeriod.ALL -> StatsPeriod.YEAR
    }
}