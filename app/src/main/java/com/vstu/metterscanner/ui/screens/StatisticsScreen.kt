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
                    // Быстрый фильтр по типу (иконки)
                    Row {
                        // Кнопка "Все"
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
            // 1. Период и фильтры
            PeriodFilterSection(
                selectedPeriod = selectedPeriod,
                onPeriodChange = { period ->
                    selectedPeriod = period
                    dateRange = getDefaultDateRange(period)
                }
            )

            // 2. KPI карточки
            KpiCards(statisticsData.kpi, selectedType)

            // 3. Основной график
            ConsumptionChart(
                meters = if (selectedType != null) meters.filter { it.type == selectedType } else meters,
                period = selectedPeriod,
                selectedType = selectedType
            )

            // 4. Детальная статистика
            StatisticsDetails(statisticsData, selectedType)

            // 5. Последние показания
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
            title = "Всего",
            value = String.format("%.1f", kpi.totalConsumption),
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
            title = "Макс",
            value = String.format("%.1f", kpi.maxConsumption),
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
                    text = "Динамика потребления",
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

            if (meters.size >= 2) {
                SimpleChart(meters, selectedType ?: MeterType.ELECTRICITY)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Недостаточно данных",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleChart(meters: List<Meter>, type: MeterType) {
    val sorted = meters.sortedBy { it.date }
    val values = sorted.map { it.value }

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
            if (values.size < 2) return@Canvas

            val padding = 40f
            val chartWidth = size.width - padding * 2
            val chartHeight = size.height - padding * 2

            val maxVal = values.maxOrNull() ?: 1.0
            val minVal = values.minOrNull() ?: 0.0
            val range = (maxVal - minVal).coerceAtLeast(1.0)

            val points = values.mapIndexed { index, value ->
                val x = padding + (chartWidth * index / (values.size - 1))
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

            // Сравнение
            ComparisonItem(
                current = data.comparison.currentConsumption,
                previous = data.comparison.previousConsumption,
                unit = data.kpi.unit ?: "ед."
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Распределение (если не выбран тип)
            if (selectedType == null && data.distribution.isNotEmpty()) {
                Text(
                    text = "Распределение по типам",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                data.distribution.forEach { (type, value) ->
                    DistributionRow(type, value, data.kpi.totalConsumption)
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
    val totalConsumption: Double,
    val averagePerDay: Double,
    val maxConsumption: Double,
    val unit: String?
)

data class ComparisonData(
    val currentConsumption: Double,
    val previousConsumption: Double,
    val changePercentage: Double
)

// Вспомогательные функции
fun getDefaultDateRange(period: Period): DateRange {
    val today = LocalDate.now()
    return when (period) {
        Period.DAILY -> DateRange(today, today)
        Period.WEEKLY -> DateRange(today.minusDays(7), today)
        Period.MONTHLY -> DateRange(today.minusMonths(1), today)
        Period.YEARLY -> DateRange(today.minusYears(1), today)
    }
}

fun calculateStatistics(
    meters: List<Meter>,
    dateRange: DateRange,
    selectedType: MeterType?
): StatisticsData {
    val filtered = selectedType?.let { meters.filter { it.type == selectedType } } ?: meters

    if (filtered.isEmpty()) {
        return StatisticsData(
            kpi = KpiData(0.0, 0.0, 0.0, selectedType?.getUnit()),
            comparison = ComparisonData(0.0, 0.0, 0.0),
            distribution = emptyMap()
        )
    }

    // Расчет потребления
    val consumptions = filtered.groupBy { it.type }.mapValues { (_, typeMeters) ->
        val sorted = typeMeters.sortedBy { it.date }
        if (sorted.size >= 2) sorted.last().value - sorted.first().value else 0.0
    }

    val total = consumptions.values.sum()
    val days = ChronoUnit.DAYS.between(dateRange.start, dateRange.end).coerceAtLeast(1).toDouble()

    return StatisticsData(
        kpi = KpiData(
            totalConsumption = total,
            averagePerDay = total / days,
            maxConsumption = consumptions.values.maxOrNull() ?: 0.0,
            unit = selectedType?.getUnit()
        ),
        comparison = ComparisonData(
            currentConsumption = total,
            previousConsumption = total * 0.8, // Для примера
            changePercentage = if (total > 0) 20.0 else 0.0
        ),
        distribution = consumptions
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