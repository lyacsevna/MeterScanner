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

    // Вычисляем данные для статистики
    val statisticsData = remember(meters, dateRange, selectedType) {
        calculateStatistics(meters, dateRange, selectedType)
    }

    // Сортируем метры по дате для графиков
    val sortedMeters = remember(meters) {
        meters.sortedBy { it.date }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Статистика потребления") },
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
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Выбор периода и фильтров
            PeriodAndFilterSection(
                selectedPeriod = selectedPeriod,
                onPeriodChange = { period ->
                    selectedPeriod = period
                    dateRange = getDefaultDateRange(period)
                },
                selectedType = selectedType,
                onTypeChange = { type ->
                    selectedType = if (selectedType == type) null else type
                },
                dateRange = dateRange,
                onDateRangeChange = { range ->
                    dateRange = range
                    selectedPeriod = Period.CUSTOM
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Карточки с ключевыми показателями (KPI)
            KpiCards(statisticsData.kpi)

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Основной график потребления
            ConsumptionChartCard(
                meters = if (selectedType != null) {
                    sortedMeters.filter { it.type == selectedType }
                } else {
                    sortedMeters
                },
                period = selectedPeriod,
                dateRange = dateRange,
                selectedType = selectedType
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Сравнение с предыдущим периодом
            ComparisonCard(statisticsData.comparison)

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Распределение по типам счетчиков
            if (selectedType == null) { // Показываем только если не выбран конкретный тип
                DistributionCard(statisticsData.distribution)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 6. Детальная таблица (последние 10 записей)
            RecentReadingsCard(
                meters = if (selectedType != null) {
                    sortedMeters.filter { it.type == selectedType }.take(10)
                } else {
                    sortedMeters.take(10)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ================== ПЕРИОДЫ И ФИЛЬТРЫ ==================

@Composable
fun PeriodAndFilterSection(
    selectedPeriod: Period,
    onPeriodChange: (Period) -> Unit,
    selectedType: MeterType?,
    onTypeChange: (MeterType) -> Unit,
    dateRange: DateRange,
    onDateRangeChange: (DateRange) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Период анализа",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Выбор периода
            PeriodTabs(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = onPeriodChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Фильтр по типу счетчика
            Text(
                text = "Тип счетчика",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            TypeFilterChips(
                selectedType = selectedType,
                onTypeSelected = onTypeChange
            )

            // Показываем выбранный диапазон дат
            if (selectedPeriod == Period.CUSTOM) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Выбранный период: ${dateRange.start.formatDate()} - ${dateRange.end.formatDate()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PeriodTabs(
    selectedPeriod: Period,
    onPeriodSelected: (Period) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Period.values().filter { it != Period.CUSTOM }.forEach { period ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
                label = { Text(period.displayName) },
                modifier = Modifier.weight(1f),
                enabled = true // ← ДОБАВЬТЕ ЭТУ СТРОКУ
            )
        }

        // Кнопка для произвольного периода
        AssistChip(
            onClick = { expanded = true },
            label = { Text("Другой") },
            enabled = true
        )
    }
}
@Composable
fun TypeFilterChips(
    selectedType: MeterType?,
    onTypeSelected: (MeterType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Кнопка "Все типы"
        FilterChip(
            selected = selectedType == null,
            onClick = { onTypeSelected(MeterType.ELECTRICITY) }, // Будет сброшено
            label = { Text("Все типы") },
            modifier = Modifier.weight(1f),
            enabled = true
        )

        // Кнопки для каждого типа
        MeterType.values().forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = type.getIcon(),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(type.getDisplayName())
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = true // ← ДОБАВЬТЕ ЭТУ СТРОКУ
            )
        }
    }
}

// ================== КАРТОЧКИ KPI ==================

@Composable
fun KpiCards(kpi: KpiData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Общее потребление
        KpiCard(
            title = "Всего",
            value = String.format("%.1f", kpi.totalConsumption),
            unit = if (kpi.unit != null) kpi.unit else "ед.",
            icon = Icons.Default.Timeline,
            color = MaterialTheme.colorScheme.primary
        )

        // Среднее в день
        KpiCard(
            title = "В среднем",
            value = String.format("%.1f", kpi.averagePerDay),
            unit = if (kpi.unit != null) "$" + kpi.unit + "/день" else "ед./день",
            icon = Icons.Default.TrendingUp,
            color = MaterialTheme.colorScheme.secondary
        )

        // Максимальное
        KpiCard(
            title = "Макс",
            value = String.format("%.1f", kpi.maxConsumption),
            unit = if (kpi.unit != null) kpi.unit else "ед.",
            icon = Icons.Default.ArrowUpward,
            color = Color(0xFF4CAF50)
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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = color
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ================== ОСНОВНОЙ ГРАФИК ==================

@Composable
fun ConsumptionChartCard(
    meters: List<Meter>,
    period: Period,
    dateRange: DateRange,
    selectedType: MeterType?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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

                Text(
                    text = when (period) {
                        Period.DAILY -> "По дням"
                        Period.WEEKLY -> "По неделям"
                        Period.MONTHLY -> "По месяцам"
                        Period.YEARLY -> "По годам"
                        Period.CUSTOM -> "Выбранный период"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (meters.isEmpty()) {
                EmptyChartState()
            } else {
                // Простой линейный график на Canvas
                SimpleLineChart(
                    meters = meters,
                    period = period,
                    dateRange = dateRange,
                    selectedType = selectedType
                )
            }
        }
    }
}

@Composable
fun SimpleLineChart(
    meters: List<Meter>,
    period: Period,
    dateRange: DateRange,
    selectedType: MeterType?
) {
    // Группируем данные по периоду
    val groupedData = remember(meters, period) {
        groupMetersByPeriod(meters, period)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (groupedData.isEmpty()) return@Canvas

            val padding = 40f
            val chartWidth = size.width - padding * 2
            val chartHeight = size.height - padding * 2

            // Находим максимальное значение для масштабирования
            val maxValue = groupedData.maxOfOrNull { it.value } ?: 1.0
            val minValue = groupedData.minOfOrNull { it.value } ?: 0.0
            val valueRange = maxValue - minValue

            // Рисуем ось Y
            drawLine(
                color = Color.Gray.copy(alpha = 0.5f),
                start = Offset(padding, padding),
                end = Offset(padding, size.height - padding),
                strokeWidth = 1f
            )

            // Рисуем ось X
            drawLine(
                color = Color.Gray.copy(alpha = 0.5f),
                start = Offset(padding, size.height - padding),
                end = Offset(size.width - padding, size.height - padding),
                strokeWidth = 1f
            )

            // Рисуем линию графика
            val path = Path()
            val pointSize = 6f


            groupedData.forEachIndexed { index, dataPoint ->
                val x = padding + (chartWidth * index / (groupedData.size - 1).coerceAtLeast(1)).toFloat()
                val y = if (valueRange > 0) {
                    size.height - padding - (chartHeight * (dataPoint.value - minValue) / valueRange).toFloat()
                } else {
                    size.height - padding - chartHeight / 2
                }

                val point = Offset(x, y)

                if (index == 0) {
                    path.moveTo(point.x, point.y)
                } else {
                    path.lineTo(point.x, point.y)
                }

                // Рисуем точку
                drawCircle(
                    color = getColorForType(selectedType ?: MeterType.ELECTRICITY),
                    center = point,
                    radius = pointSize
                )
            }

            // Рисуем линию
            drawPath(
                path = path,
                color = getColorForType(selectedType ?: MeterType.ELECTRICITY),
                style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Подписи по оси X
            if (groupedData.size <= 8) { // Не больше 8 подписей
                groupedData.forEachIndexed { index, dataPoint ->
                    val x = padding + (chartWidth * index / (groupedData.size - 1).coerceAtLeast(1))
                    drawContext.canvas.nativeCanvas.drawText(
                        dataPoint.label,
                        x,
                        size.height - padding + 20,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyChartState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Assessment,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "Нет данных для графика",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ================== СРАВНЕНИЕ С ПРЕДЫДУЩИМ ПЕРИОДОМ ==================

@Composable
fun ComparisonCard(comparison: ComparisonData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Сравнение с предыдущим периодом",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (comparison.previousConsumption > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Текущий период",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = String.format("%.1f", comparison.currentConsumption),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (comparison.changePercentage >= 0)
                                    Icons.Default.ArrowUpward
                                else
                                    Icons.Default.ArrowDownward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (comparison.changePercentage >= 0)
                                    Color(0xFFF44336) // Красный - больше
                                else
                                    Color(0xFF4CAF50) // Зеленый - меньше
                            )
                            Text(
                                text = String.format("%.1f", comparison.changePercentage) + "%",
                                color = if (comparison.changePercentage >= 0)
                                    Color(0xFFF44336)
                                else
                                    Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Предыдущий: ${String.format("%.1f", comparison.previousConsumption)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Простая столбчатая диаграмма
                Spacer(modifier = Modifier.height(16.dp))
                ComparisonBarChart(comparison)
            } else {
                Text(
                    text = "Нет данных для сравнения",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ComparisonBarChart(comparison: ComparisonData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxValue = max(comparison.currentConsumption, comparison.previousConsumption)
            val barWidth = size.width * 0.4f
            val spacing = size.width * 0.1f

            val currentBarHeight = if (maxValue > 0) {
                (size.height * (comparison.currentConsumption / maxValue)).toFloat()
            } else {
                size.height * 0.5f
            }

            val previousBarHeight = if (maxValue > 0) {
                (size.height * (comparison.previousConsumption / maxValue)).toFloat()
            } else {
                size.height * 0.5f
            }

            // Рисуем столбец для предыдущего периода
            drawRoundRect(
                color = Color.Gray.copy(alpha = 0.5f),
                topLeft = Offset(0f, size.height - previousBarHeight),
                size = Size(barWidth, previousBarHeight),
                cornerRadius = CornerRadius(4f)
            )

            // Рисуем столбец для текущего периода
            val currentColor = if (comparison.changePercentage >= 0)
                Color(0xFFF44336)
            else
                Color(0xFF4CAF50)

            drawRoundRect(
                color = currentColor,
                topLeft = Offset(barWidth + spacing, size.height - currentBarHeight),
                size = Size(barWidth, currentBarHeight),
                cornerRadius = CornerRadius(4f)
            )

            // Подписи
            drawContext.canvas.nativeCanvas.drawText(
                "Пред.",
                0f + barWidth / 2,
                size.height + 20,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )

            drawContext.canvas.nativeCanvas.drawText(
                "Тек.",
                barWidth + spacing + barWidth / 2,
                size.height + 20,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }
    }
}

// ================== РАСПРЕДЕЛЕНИЕ ПО ТИПАМ ==================

@Composable
fun DistributionCard(distribution: Map<MeterType, Double>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Распределение по типам счетчиков",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (distribution.isNotEmpty()) {
                val total = distribution.values.sum()
                SimplePieChart(distribution, total)

                Spacer(modifier = Modifier.height(12.dp))

                // Легенда
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    distribution.forEach { (type, value) ->
                        val percentage = if (total > 0) (value / total * 100).toInt() else 0
                        DistributionItem(type, value, percentage)
                    }
                }
            } else {
                Text(
                    text = "Нет данных для распределения",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SimplePieChart(distribution: Map<MeterType, Double>, total: Double) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(120.dp)) {
            if (total <= 0) return@Canvas

            var startAngle = -90f // Начинаем с верха

            distribution.forEach { (type, value) ->
                val sweepAngle = (value / total * 360).toFloat()

                drawArc(
                    color = getColorForType(type),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true
                )

                startAngle += sweepAngle
            }
        }
    }
}

@Composable
fun DistributionItem(type: MeterType, value: Double, percentage: Int) {
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
                    .size(12.dp)
                    .background(getColorForType(type), RoundedCornerShape(3.dp))
            )
            Text(
                text = type.getDisplayName(),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Text(
            text = "${String.format("%.1f", value)} ($percentage%)",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// ================== ПОСЛЕДНИЕ ПОКАЗАНИЯ ==================

@Composable
fun RecentReadingsCard(meters: List<Meter>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Последние показания",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (meters.isNotEmpty()) {
                meters.forEach { meter ->
                    RecentReadingItem(meter)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                Text(
                    text = "Нет показаний",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RecentReadingItem(meter: Meter) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = meter.type.getIcon(),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = getColorForType(meter.type)
                )
                Text(
                    text = meter.type.getDisplayName(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = meter.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = String.format("%.1f ${meter.type.getUnit()}", meter.value),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// ================== МОДЕЛИ ДАННЫХ И ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ==================

enum class Period(val displayName: String) {
    DAILY("День"),
    WEEKLY("Неделя"),
    MONTHLY("Месяц"),
    YEARLY("Год"),
    CUSTOM("Произвольный")
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

data class ChartDataPoint(
    val label: String,
    val value: Double,
    val date: LocalDate
)

// Вспомогательные функции
fun getDefaultDateRange(period: Period): DateRange {
    val today = LocalDate.now()
    return when (period) {
        Period.DAILY -> DateRange(today, today)
        Period.WEEKLY -> DateRange(today.minusDays(7), today)
        Period.MONTHLY -> DateRange(today.minusMonths(1), today)
        Period.YEARLY -> DateRange(today.minusYears(1), today)
        Period.CUSTOM -> DateRange(today.minusMonths(1), today)
    }
}

fun calculateStatistics(
    meters: List<Meter>,
    dateRange: DateRange,
    selectedType: MeterType?
): StatisticsData {
    val filteredMeters = if (selectedType != null) {
        meters.filter { it.type == selectedType }
    } else {
        meters
    }

    // Фильтруем по дате
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val periodMeters = filteredMeters.filter { meter ->
        val meterDate = LocalDateTime.parse(meter.date, dateFormatter).toLocalDate()
        !meterDate.isBefore(dateRange.start) && !meterDate.isAfter(dateRange.end)
    }

    if (periodMeters.isEmpty()) {
        return StatisticsData(
            kpi = KpiData(0.0, 0.0, 0.0, null),
            comparison = ComparisonData(0.0, 0.0, 0.0),
            distribution = emptyMap()
        )
    }

    // Рассчитываем потребление
    val groupedByType = periodMeters.groupBy { it.type }
    val consumptions = mutableMapOf<MeterType, Double>()

    groupedByType.forEach { (type, typeMeters) ->
        val sorted = typeMeters.sortedBy { it.date }
        if (sorted.size >= 2) {
            val first = sorted.first().value
            val last = sorted.last().value
            consumptions[type] = last - first
        } else {
            consumptions[type] = 0.0
        }
    }

    // KPI
    val totalConsumption = consumptions.values.sum()
    val days = ChronoUnit.DAYS.between(dateRange.start, dateRange.end).coerceAtLeast(1).toDouble()
    val averagePerDay = totalConsumption / days
    val maxConsumption = consumptions.values.maxOrNull() ?: 0.0
    val unit = if (selectedType != null) selectedType.getUnit() else null

    // Распределение
    val distribution = consumptions

    // Сравнение с предыдущим периодом
    val previousRange = DateRange(
        dateRange.start.minusDays(days.toLong()),
        dateRange.start.minusDays(1)
    )

    val previousMeters = filteredMeters.filter { meter ->
        val meterDate = LocalDateTime.parse(meter.date, dateFormatter).toLocalDate()
        !meterDate.isBefore(previousRange.start) && !meterDate.isAfter(previousRange.end)
    }

    val previousConsumption = if (previousMeters.size >= 2) {
        val sorted = previousMeters.sortedBy { it.date }
        sorted.last().value - sorted.first().value
    } else {
        0.0
    }

    val changePercentage = if (previousConsumption > 0) {
        ((totalConsumption - previousConsumption) / previousConsumption) * 100
    } else {
        0.0
    }

    return StatisticsData(
        kpi = KpiData(totalConsumption, averagePerDay, maxConsumption, unit),
        comparison = ComparisonData(totalConsumption, previousConsumption, changePercentage),
        distribution = distribution
    )
}

fun groupMetersByPeriod(meters: List<Meter>, period: Period): List<ChartDataPoint> {
    if (meters.isEmpty()) return emptyList()

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val sortedMeters = meters.sortedBy { it.date }

    return when (period) {
        Period.DAILY -> {
            val grouped = sortedMeters.groupBy {
                LocalDateTime.parse(it.date, dateFormatter).toLocalDate()
            }
            grouped.map { (date, dayMeters) ->
                val values = dayMeters.map { it.value }
                ChartDataPoint(
                    label = date.format(DateTimeFormatter.ofPattern("dd.MM")),
                    value = values.maxOrNull() ?: 0.0,
                    date = date
                )
            }.sortedBy { it.date }
        }
        else -> {
            // Для упрощения возвращаем последние 10 показаний
            sortedMeters.takeLast(10).map { meter ->
                val date = LocalDateTime.parse(meter.date, dateFormatter).toLocalDate()
                ChartDataPoint(
                    label = date.format(DateTimeFormatter.ofPattern("dd.MM")),
                    value = meter.value,
                    date = date
                )
            }
        }
    }
}

// Расширения для MeterType
fun MeterType.getDisplayName(): String = when (this) {
    MeterType.ELECTRICITY -> "Электричество"
    MeterType.COLD_WATER -> "Холодная вода"
    MeterType.HOT_WATER -> "Горячая вода"
}

fun MeterType.getIcon(): androidx.compose.ui.graphics.vector.ImageVector = when (this) {
    MeterType.ELECTRICITY -> Icons.Default.FlashOn
    MeterType.COLD_WATER -> Icons.Default.WaterDrop
    MeterType.HOT_WATER -> Icons.Default.Whatshot
}

fun MeterType.getUnit(): String = when (this) {
    MeterType.ELECTRICITY -> "кВт·ч"
    MeterType.COLD_WATER -> "м³"
    MeterType.HOT_WATER -> "м³"
}

fun getColorForType(type: MeterType): Color = when (type) {
    MeterType.ELECTRICITY -> Color(0xFF2196F3) // Синий
    MeterType.COLD_WATER -> Color(0xFF03A9F4) // Голубой
    MeterType.HOT_WATER -> Color(0xFFFF9800) // Оранжевый
}

fun LocalDate.formatDate(): String {
    return this.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
}