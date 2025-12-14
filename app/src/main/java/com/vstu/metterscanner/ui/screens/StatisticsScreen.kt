package com.vstu.metterscanner.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.vstu.metterscanner.MeterViewModel
import com.vstu.metterscanner.data.Meter
import com.vstu.metterscanner.data.MeterType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: MeterViewModel,
    navController: NavController
) {
    val meters by viewModel.allMeters.collectAsState()
    var selectedPeriod by remember { mutableStateOf(Period.MONTHLY) }
    var selectedType by remember { mutableStateOf<MeterType?>(null) }
    var dateRange by remember { mutableStateOf(getDefaultDateRange(selectedPeriod)) }

    val statisticsData = remember(meters, dateRange, selectedType) {
        calculateStatistics(meters, dateRange, selectedType)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Статистика") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    Row {
                        IconButton(
                            onClick = { selectedType = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Dashboard,
                                contentDescription = "Все типы",
                                tint = if (selectedType == null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        MeterType.values().forEach { type ->
                            IconButton(
                                onClick = {
                                    selectedType = if (selectedType == type) null else type
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    type.getIcon(),
                                    contentDescription = type.getDisplayName(),
                                    tint = if (selectedType == type) getColorForType(type)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                .verticalScroll(rememberScrollState())
        ) {
            PeriodFilterSection(
                selectedPeriod = selectedPeriod,
                onPeriodChange = { period ->
                    selectedPeriod = period
                    dateRange = getDefaultDateRange(period)
                }
            )

            KpiCards(statisticsData.kpi, selectedType)

            ConsumptionChart(
                meters = if (selectedType != null) meters.filter { it.type == selectedType } else meters,
                period = selectedPeriod,
                selectedType = selectedType
            )

            StatisticsDetails(statisticsData, selectedType)

            RecentReadings(
                meters = if (selectedType != null) meters.filter { it.type == selectedType }.take(5)
                else meters.take(5)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodFilterSection(
    selectedPeriod: Period,
    onPeriodChange: (Period) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedPeriod.displayName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                label = { Text("Период анализа") }
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                Period.values().forEach { period ->
                    DropdownMenuItem(
                        text = { Text(period.displayName) },
                        onClick = {
                            onPeriodChange(period)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KpiCards(kpi: KpiData, selectedType: MeterType?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        KpiCard(
            title = "Расход",
            value = String.format("%.1f", kpi.consumption),
            unit = kpi.unit ?: "ед.",
            icon = Icons.Default.ShowChart,
            color = MaterialTheme.colorScheme.primary
        )

        KpiCard(
            title = "В день",
            value = String.format("%.1f", kpi.averagePerDay),
            unit = kpi.unit?.let { "$it/день" } ?: "ед./день",
            icon = Icons.Default.TrendingUp,
            color = MaterialTheme.colorScheme.secondary
        )

        KpiCard(
            title = "Экономия",
            value = String.format("%.1f", kpi.savings),
            unit = kpi.unit ?: "ед.",
            icon = Icons.Default.Star,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = color
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ConsumptionChart(
    meters: List<Meter>,
    period: Period,
    selectedType: MeterType?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Динамика показаний",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (selectedType != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            selectedType.getIcon(),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = getColorForType(selectedType)
                        )
                        Text(
                            text = selectedType.getDisplayName(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val filteredMeters = if (selectedType != null) meters.filter { it.type == selectedType } else meters
            if (filteredMeters.size >= 2) {
                ReadingsChart(filteredMeters, selectedType ?: MeterType.ELECTRICITY)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Недостаточно данных для графика",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ReadingsChart(meters: List<Meter>, type: MeterType) {
    val sorted = meters.sortedBy { it.date }

    // Вычисляем разницы между последовательными показаниями
    val differences = if (sorted.size >= 2) {
        sorted.windowed(2) { pair ->
            val (prev, curr) = pair
            curr.value - prev.value
        }
    } else {
        emptyList()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (differences.isEmpty()) return@Canvas

            val padding = 40f
            val chartWidth = size.width - padding * 2
            val chartHeight = size.height - padding * 2

            val maxVal = differences.maxOrNull() ?: 1.0
            val minVal = differences.minOrNull() ?: 0.0
            val range = (maxVal - minVal).coerceAtLeast(1.0)

            val points = differences.mapIndexed { index, value ->
                val x = padding + (chartWidth * index / (differences.size - 1))
                val y = size.height - padding - (chartHeight * (value - minVal) / range).toFloat()
                Offset(x.toFloat(), y)
            }

            // Линия графика
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }

            drawPath(
                path = path,
                color = getColorForType(type),
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )

            // Точки
            points.forEach { point ->
                drawCircle(
                    color = getColorForType(type),
                    center = point,
                    radius = 4f
                )
            }

            // Ось X (время)
            drawLine(
                color = Color.Gray,
                start = Offset(padding, size.height - padding),
                end = Offset(size.width - padding, size.height - padding),
                strokeWidth = 1f
            )

            // Ось Y (значения)
            drawLine(
                color = Color.Gray,
                start = Offset(padding, padding),
                end = Offset(padding, size.height - padding),
                strokeWidth = 1f
            )
        }
    }
}

@Composable
fun StatisticsDetails(data: StatisticsData, selectedType: MeterType?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Детали статистики",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Сравнение с предыдущим периодом
            ComparisonItem(
                current = data.comparison.currentConsumption,
                previous = data.comparison.previousConsumption,
                unit = data.kpi.unit ?: "ед."
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Распределение (если не выбран тип)
            if (selectedType == null && data.distribution.isNotEmpty()) {
                Text(
                    text = "Расход по типам",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                data.distribution.forEach { (type, value) ->
                    DistributionRow(type, value, data.kpi.consumption)
                }
            }
        }
    }
}

@Composable
fun ComparisonItem(current: Double, previous: Double, unit: String) {
    val change = if (previous > 0) ((current - previous) / previous * 100) else 0.0
    val isIncrease = change >= 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Сравнение с предыдущим периодом", style = MaterialTheme.typography.bodySmall)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = String.format("%.1f", current),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    if (isIncrease) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isIncrease) Color(0xFFF44336) else Color(0xFF4CAF50)
                )
                Text(
                    text = String.format("%.1f%%", abs(change)),
                    color = if (isIncrease) Color(0xFFF44336) else Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "было: ${String.format("%.1f", previous)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DistributionRow(type: MeterType, value: Double, total: Double) {
    val percentage = if (total > 0) (value / total * 100).toInt() else 0

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
                    .size(10.dp)
                    .background(getColorForType(type), RoundedCornerShape(2.dp))
            )
            Text(type.getDisplayName(), style = MaterialTheme.typography.bodyMedium)
        }

        Text(
            text = "${String.format("%.1f", value)} ($percentage%)",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun RecentReadings(meters: List<Meter>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Последние показания",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (meters.isNotEmpty()) {
                meters.forEach { meter ->
                    ReadingRow(meter)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            } else {
                Text(
                    text = "Нет данных",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ReadingRow(meter: Meter) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                meter.type.getIcon(),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = getColorForType(meter.type)
            )
            Column {
                Text(meter.type.getDisplayName(), style = MaterialTheme.typography.bodyMedium)
                Text(meter.date, style = MaterialTheme.typography.labelSmall)
            }
        }

        Text(
            text = "${String.format("%.1f", meter.value)} ${meter.type.getUnit()}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// Модели данных
enum class Period(val displayName: String) {
    DAILY("День"),
    WEEKLY("Неделя"),
    MONTHLY("Месяц"),
    YEARLY("Год")
}

data class DateRange(val start: LocalDate, val end: LocalDate)

data class StatisticsData(
    val kpi: KpiData,
    val comparison: ComparisonData,
    val distribution: Map<MeterType, Double>
)

data class KpiData(
    val consumption: Double,           // Общий расход за период
    val averagePerDay: Double,         // Средний расход в день
    val savings: Double,               // Экономия/перерасход по сравнению с предыдущим периодом
    val unit: String?                  // Единица измерения
)

data class ComparisonData(
    val currentConsumption: Double,    // Расход в текущем периоде
    val previousConsumption: Double,   // Расход в предыдущем периоде
    val changePercentage: Double       // Процент изменения
)

// Вспомогательные функции
fun getDefaultDateRange(period: Period): DateRange {
    val today = LocalDate.now()
    return when (period) {
        Period.DAILY -> DateRange(today.minusDays(1), today)
        Period.WEEKLY -> DateRange(today.minusWeeks(1), today)
        Period.MONTHLY -> DateRange(today.minusMonths(1), today)
        Period.YEARLY -> DateRange(today.minusYears(1), today)
    }
}

fun calculateStatistics(
    meters: List<Meter>,
    dateRange: DateRange,
    selectedType: MeterType?
): StatisticsData {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    // Фильтруем по дате и типу
    val filtered = meters.filter { meter ->
        val meterDate = LocalDate.parse(meter.date.substringBefore(" "), DateTimeFormatter.ISO_LOCAL_DATE)
        (meterDate.isAfter(dateRange.start.minusDays(1)) && meterDate.isBefore(dateRange.end.plusDays(1))) &&
                (selectedType == null || meter.type == selectedType)
    }

    if (filtered.isEmpty()) {
        return StatisticsData(
            kpi = KpiData(0.0, 0.0, 0.0, selectedType?.getUnit()),
            comparison = ComparisonData(0.0, 0.0, 0.0),
            distribution = emptyMap()
        )
    }

    // Группируем по типу и вычисляем расход для каждого типа
    val consumptionByType = mutableMapOf<MeterType, Double>()
    val distribution = mutableMapOf<MeterType, Double>()

    val types = if (selectedType != null) listOf(selectedType) else MeterType.values().toList()

    types.forEach { type ->
        val typeMeters = filtered.filter { it.type == type }.sortedBy { it.date }

        if (typeMeters.size >= 2) {
            // Вычисляем разницу между первым и последним показанием за период
            val firstReading = typeMeters.first()
            val lastReading = typeMeters.last()
            val consumption = lastReading.value - firstReading.value

            consumptionByType[type] = max(0.0, consumption)
            distribution[type] = consumption
        } else {
            consumptionByType[type] = 0.0
            distribution[type] = 0.0
        }
    }

    // Общий расход
    val totalConsumption = consumptionByType.values.sum()

    // Длительность периода в днях
    val days = ChronoUnit.DAYS.between(dateRange.start, dateRange.end).coerceAtLeast(1).toDouble()

    // Вычисляем предыдущий период для сравнения
    val previousDateRange = getPreviousDateRange(dateRange)
    val previousConsumption = calculateConsumptionForPeriod(meters, previousDateRange, selectedType)

    val savings = previousConsumption - totalConsumption

    return StatisticsData(
        kpi = KpiData(
            consumption = totalConsumption,
            averagePerDay = totalConsumption / days,
            savings = savings,
            unit = selectedType?.getUnit()
        ),
        comparison = ComparisonData(
            currentConsumption = totalConsumption,
            previousConsumption = previousConsumption,
            changePercentage = if (previousConsumption > 0)
                ((totalConsumption - previousConsumption) / previousConsumption * 100)
            else 0.0
        ),
        distribution = distribution.filter { it.value > 0 }
    )
}

// Вычисляет расход для заданного периода
fun calculateConsumptionForPeriod(
    meters: List<Meter>,
    dateRange: DateRange,
    selectedType: MeterType?
): Double {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    val filtered = meters.filter { meter ->
        val meterDate = LocalDate.parse(meter.date.substringBefore(" "), DateTimeFormatter.ISO_LOCAL_DATE)
        (meterDate.isAfter(dateRange.start.minusDays(1)) && meterDate.isBefore(dateRange.end.plusDays(1))) &&
                (selectedType == null || meter.type == selectedType)
    }

    if (filtered.isEmpty()) return 0.0

    val types = if (selectedType != null) listOf(selectedType) else MeterType.values().toList()

    return types.sumOf { type ->
        val typeMeters = filtered.filter { it.type == type }.sortedBy { it.date }
        if (typeMeters.size >= 2) {
            val firstReading = typeMeters.first()
            val lastReading = typeMeters.last()
            max(0.0, lastReading.value - firstReading.value)
        } else {
            0.0
        }
    }
}

// Получает предыдущий период той же длительности
fun getPreviousDateRange(currentRange: DateRange): DateRange {
    val duration = ChronoUnit.DAYS.between(currentRange.start, currentRange.end)
    return DateRange(
        start = currentRange.start.minusDays(duration),
        end = currentRange.start.minusDays(1)
    )
}

// Расширения для MeterType
fun MeterType.getDisplayName(): String = when (this) {
    MeterType.ELECTRICITY -> "Электричество"
    MeterType.COLD_WATER -> "Холодная вода"
    MeterType.HOT_WATER -> "Горячая вода"
}

fun MeterType.getIcon() = when (this) {
    MeterType.ELECTRICITY -> Icons.Default.FlashOn
    MeterType.COLD_WATER -> Icons.Default.WaterDrop
    MeterType.HOT_WATER -> Icons.Default.Whatshot
}

fun MeterType.getUnit() = when (this) {
    MeterType.ELECTRICITY -> "кВт·ч"
    MeterType.COLD_WATER -> "м³"
    MeterType.HOT_WATER -> "м³"
}

fun getColorForType(type: MeterType) = when (type) {
    MeterType.ELECTRICITY -> Color(0xFF2196F3)
    MeterType.COLD_WATER -> Color(0xFF03A9F4)
    MeterType.HOT_WATER -> Color(0xFFFF9800)
}