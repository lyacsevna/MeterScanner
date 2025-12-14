package com.vstu.metterscanner.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.vstu.metterscanner.MeterViewModel
import com.vstu.metterscanner.data.Meter
import com.vstu.metterscanner.data.MeterType
import kotlin.math.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    var hoveredPieIndex by remember { mutableStateOf<Int?>(null) }

    val statisticsData = remember(meters, dateRange, selectedType) {
        calculateStatistics(meters, dateRange, selectedType)
    }

    // Создаем данные для графика только если выбран конкретный тип
    val chartData by remember(meters, dateRange, selectedType) {
        derivedStateOf {
            if (selectedType != null) {
                prepareChartData(meters, dateRange, selectedType)
            } else {
                ChartData(emptyList(), dateRange, 0.0, 0.0)
            }
        }
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
                    hoveredIndex = null
                    hoveredPieIndex = null
                }
            )

            KpiCards(statisticsData.kpi, selectedType)

            // Показываем круговую диаграмму когда выбраны все типы
            if (selectedType == null && statisticsData.distribution.isNotEmpty()) {
                InteractivePieChart(
                    distribution = statisticsData.distribution,
                    totalConsumption = statisticsData.kpi.consumption,
                    onHover = { index -> hoveredPieIndex = index }
                )

                hoveredPieIndex?.let { index ->
                    if (index < statisticsData.distribution.size) {
                        val types = statisticsData.distribution.keys.toList()
                        val type = types[index]
                        val value = statisticsData.distribution[type] ?: 0.0
                        PieChartTooltip(
                            type = type,
                            value = value,
                            total = statisticsData.kpi.consumption
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Показываем график ТОЛЬКО когда выбран конкретный тип
            if (selectedType != null) {
                val currentSelectedType = selectedType!! // Можно использовать, так как мы проверили на null

                InteractiveChart(
                    chartData = chartData,
                    period = selectedPeriod,
                    selectedType = currentSelectedType,
                    onHover = { index -> hoveredIndex = index }
                )

                hoveredIndex?.let { index ->
                    if (index < chartData.dataPoints.size) {
                        val point = chartData.dataPoints[index]
                        ChartTooltip(
                            date = point.date,
                            value = point.value,
                            type = currentSelectedType,
                            consumption = point.consumption
                        )
                    }
                }
            } else {
                // Показываем сообщение, если не выбран тип
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Выберите тип счетчика",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Для отображения графика динамики выберите конкретный тип счетчика",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            StatisticsDetails(statisticsData, selectedType)

            RecentReadings(
                meters = if (selectedType != null) meters.filter { it.type == selectedType }.take(5)
                else meters.take(5)
            )
        }
    }
}
@Composable
fun InteractivePieChart(
    distribution: Map<MeterType, Double>,
    totalConsumption: Double,
    onHover: (Int?) -> Unit
) {
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    val surfaceColor = MaterialTheme.colorScheme.surface

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
                    text = "Распределение по типам",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Всего: ${String.format("%.1f", totalConsumption)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Круговая диаграмма
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .pointerInput(distribution) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val position = event.changes.first().position
                                    val index = calculatePieHoveredIndex(
                                        position = position,
                                        size = size,
                                        distribution = distribution
                                    )
                                    hoveredIndex = index
                                    onHover(index)
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (distribution.isEmpty()) return@Canvas

                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = min(size.width, size.height) * 0.4f

                        // Сортируем типы по значению для стабильного отображения
                        val sortedEntries = distribution.entries
                            .sortedByDescending { it.value }
                            .filter { it.value > 0 }

                        if (sortedEntries.isEmpty()) return@Canvas

                        var startAngle = 0f

                        sortedEntries.forEachIndexed { index, (type, value) ->
                            val percentage = value / totalConsumption
                            val sweepAngle = (percentage * 360).toFloat()

                            // Определяем цвет и толщину в зависимости от наведения
                            val isHovered = index == hoveredIndex
                            val color = getColorForType(type)
                            val strokeWidth = if (isHovered) 8f else 0f
                            val arcRadius = if (isHovered) radius + 4f else radius

                            // Рисуем сектор
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true,
                                topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                                size = Size(arcRadius * 2, arcRadius * 2),
                                style = if (strokeWidth > 0) Stroke(strokeWidth) else Fill
                            )

                            // Добавляем обводку для выделенного сектора
                            if (isHovered) {
                                drawArc(
                                    color = Color.White,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = true,
                                    topLeft = Offset(center.x - radius, center.y - radius),
                                    size = Size(radius * 2, radius * 2),
                                    style = Stroke(2f)
                                )
                            }

                            startAngle += sweepAngle
                        }

                        // Рисуем центральный круг для эффекта пончика
                        drawCircle(
                            color = surfaceColor,
                            center = center,
                            radius = radius * 0.4f
                        )

                        // Отображаем общий процент в центре
                        drawContext.canvas.nativeCanvas.apply {
                            drawText(
                                "100%",
                                center.x,
                                center.y + 8,
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.GRAY
                                    textSize = 14f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }
                            )
                        }
                    }
                }

                // Легенда
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    distribution.entries
                        .sortedByDescending { it.value }
                        .filter { it.value > 0 }
                        .forEachIndexed { index, (type, value) ->
                            val percentage = (value / totalConsumption * 100).toInt()
                            val isHovered = index == hoveredIndex

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        hoveredIndex = if (hoveredIndex == index) null else index
                                        onHover(if (hoveredIndex == index) null else index)
                                    }
                                    .padding(vertical = 4.dp)
                                    .background(
                                        if (isHovered) MaterialTheme.colorScheme.surfaceVariant
                                        else Color.Transparent,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(8.dp),
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
                                            .background(
                                                getColorForType(type),
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                    Text(
                                        text = type.getDisplayName(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isHovered) FontWeight.Bold else FontWeight.Normal
                                    )
                                }

                                Text(
                                    text = "$percentage%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isHovered) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                }
            }
        }
    }
}

@Composable
fun PieChartTooltip(
    type: MeterType,
    value: Double,
    total: Double
) {
    val percentage = if (total > 0) (value / total * 100).toInt() else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    type.getIcon(),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = getColorForType(type)
                )
                Text(
                    text = type.getDisplayName(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format("%.1f", value)} ${type.getUnit()}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$percentage% от общего расхода",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun calculatePieHoveredIndex(
    position: Offset,
    size: IntSize,
    distribution: Map<MeterType, Double>
): Int? {
    if (distribution.isEmpty()) return null

    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = min(size.width, size.height) * 0.4f

    // Проверяем, находится ли точка внутри круга
    val distanceFromCenter = sqrt(
        (position.x - center.x).pow(2) + (position.y - center.y).pow(2)
    )

    if (distanceFromCenter > radius) return null

    // Вычисляем угол точки
    val dx = position.x - center.x
    val dy = center.y - position.y // Инвертируем Y для правильного угла

    var angle = atan2(dy, dx) * 180 / PI.toFloat()
    if (angle < 0) angle += 360f

    // Сортируем записи по значению
    val sortedEntries = distribution.entries
        .sortedByDescending { it.value }
        .filter { it.value > 0 }

    if (sortedEntries.isEmpty()) return null

    val totalConsumption = distribution.values.sum()
    var currentAngle = 0f

    sortedEntries.forEachIndexed { index, (_, value) ->
        val percentage = value / totalConsumption
        val sweepAngle = (percentage * 360).toFloat()

        if (angle >= currentAngle && angle < currentAngle + sweepAngle) {
            return index
        }

        currentAngle += sweepAngle
    }

    return null
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
            color = MaterialTheme.colorScheme.tertiary,
            isPositive = kpi.savings >= 0
        )
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isPositive: Boolean = true
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
                fontWeight = FontWeight.Bold,
                color = if (title == "Экономия") {
                    if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
                } else MaterialTheme.colorScheme.onSurface
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
fun InteractiveChart(
    chartData: ChartData,
    period: Period,
    selectedType: MeterType?,
    onHover: (Int?) -> Unit
) {
    var hoveredIndex by remember { mutableStateOf<Int?>(null) }

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

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = getPeriodLabel(period, chartData.dateRange),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (chartData.dataPoints.size >= 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val x = event.changes.first().position.x
                                        val y = event.changes.first().position.y

                                        val index = calculateHoveredIndex(
                                            x = x,
                                            y = y,
                                            chartData = chartData,
                                            canvasSize = size
                                        )

                                        hoveredIndex = index
                                        onHover(index)
                                    }
                                }
                            }
                    ) {
                        val dataPoints = chartData.dataPoints
                        if (dataPoints.size < 2) return@Canvas

                        val padding = 48f
                        val chartWidth = size.width - padding * 2
                        val chartHeight = size.height - padding * 2

                        val minValue = dataPoints.minOf { it.value }
                        val maxValue = dataPoints.maxOf { it.value }
                        val valueRange = (maxValue - minValue).coerceAtLeast(1.0)

                        // Создаем точки для графика
                        val points = dataPoints.mapIndexed { index, point ->
                            val x = padding + (chartWidth * index / (dataPoints.size - 1))
                            val y = size.height - padding - (chartHeight * (point.value - minValue) / valueRange).toFloat()
                            PointData(x, y, point)
                        }

                        // Рисуем оси
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.5f),
                            start = Offset(padding, size.height - padding),
                            end = Offset(size.width - padding, size.height - padding),
                            strokeWidth = 2f
                        )

                        drawLine(
                            color = Color.Gray.copy(alpha = 0.5f),
                            start = Offset(padding, padding),
                            end = Offset(padding, size.height - padding),
                            strokeWidth = 2f
                        )

                        // Рисуем сетку
                        for (i in 0..5) {
                            val x = padding + (chartWidth * i / 5)
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.2f),
                                start = Offset(x, padding),
                                end = Offset(x, size.height - padding),
                                strokeWidth = 1f
                            )
                        }

                        for (i in 0..5) {
                            val y = size.height - padding - (chartHeight * i / 5)
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.2f),
                                start = Offset(padding, y),
                                end = Offset(size.width - padding, y),
                                strokeWidth = 1f
                            )
                        }

                        // Рисуем линию графика
                        val path = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            points.drop(1).forEach { point ->
                                lineTo(point.x, point.y)
                            }
                        }

                        drawPath(
                            path = path,
                            color = getColorForType(selectedType ?: MeterType.ELECTRICITY),
                            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // Рисуем точки
                        points.forEachIndexed { index, point ->
                            val radius = if (index == hoveredIndex) 8f else 4f
                            val color = getColorForType(selectedType ?: MeterType.ELECTRICITY)
                                .copy(alpha = if (index == hoveredIndex) 1f else 0.7f)

                            drawCircle(
                                color = color,
                                center = Offset(point.x, point.y),
                                radius = radius
                            )
                        }

                        // Подписи оси X
                        val step = max(1, dataPoints.size / 5)
                        dataPoints.forEachIndexed { index, point ->
                            if (index % step == 0 || index == dataPoints.size - 1) {
                                val x = padding + (chartWidth * index / (dataPoints.size - 1))

                                drawContext.canvas.nativeCanvas.apply {
                                    drawText(
                                        point.dateShort,
                                        x,
                                        size.height - padding + 20,
                                        android.graphics.Paint().apply {
                                            color = android.graphics.Color.GRAY
                                            textSize = 10f
                                            textAlign = android.graphics.Paint.Align.CENTER
                                        }
                                    )
                                }
                            }
                        }

                        // Подписи оси Y
                        for (i in 0..5) {
                            val y = size.height - padding - (chartHeight * i / 5)
                            val value = minValue + (maxValue - minValue) * i / 5

                            drawContext.canvas.nativeCanvas.apply {
                                drawText(
                                    String.format("%.0f", value),
                                    padding - 25,
                                    y + 4,
                                    android.graphics.Paint().apply {
                                        color = android.graphics.Color.GRAY
                                        textSize = 10f
                                        textAlign = android.graphics.Paint.Align.RIGHT
                                    }
                                )
                            }
                        }

                        // Подсветка наведенной точки
                        hoveredIndex?.let { index ->
                            if (index < points.size) {
                                val point = points[index]

                                // Вертикальная линия
                                drawLine(
                                    color = Color.Gray.copy(alpha = 0.3f),
                                    start = Offset(point.x, 0f),
                                    end = Offset(point.x, point.y),
                                    strokeWidth = 1f
                                )

                                // Горизонтальная линия
                                drawLine(
                                    color = Color.Gray.copy(alpha = 0.3f),
                                    start = Offset(0f, point.y),
                                    end = Offset(point.x, point.y),
                                    strokeWidth = 1f
                                )

                                // Подсветка точки
                                drawCircle(
                                    color = getColorForType(selectedType ?: MeterType.ELECTRICITY),
                                    center = Offset(point.x, point.y),
                                    radius = 12f,
                                    style = Stroke(width = 2f)
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
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
fun ChartTooltip(
    date: String,
    value: Double,
    type: MeterType,
    consumption: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Показание: ${String.format("%.1f", value)} ${type.getUnit()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Расход: ${String.format("%.1f", consumption)} ${type.getUnit()}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = type.getDisplayName(),
                    style = MaterialTheme.typography.labelSmall,
                    color = getColorForType(type)
                )
            }
        }
    }
}

private fun calculateHoveredIndex(
    x: Float,
    y: Float,
    chartData: ChartData,
    canvasSize: IntSize
): Int? {
    val padding = 48f
    val chartWidth = canvasSize.width - padding * 2

    if (x < padding || x > canvasSize.width - padding ||
        y < padding || y > canvasSize.height - padding) {
        return null
    }

    val relativeX = (x - padding) / chartWidth
    val index = (relativeX * (chartData.dataPoints.size - 1)).toInt()

    return index.coerceIn(0, chartData.dataPoints.size - 1)
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

            // Всегда показываем сравнение
            ComparisonItem(
                current = data.comparison.currentConsumption,
                previous = data.comparison.previousConsumption,
                unit = data.kpi.unit ?: "ед."
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Показываем распределение только если выбраны все типы
            if (selectedType == null && data.distribution.isNotEmpty()) {
                Text(
                    text = "Распределение по типам",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                data.distribution.forEach { (type, value) ->
                    DistributionRow(type, value, data.kpi.consumption)
                    Spacer(modifier = Modifier.height(4.dp))
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
    val consumption: Double,
    val averagePerDay: Double,
    val savings: Double,
    val unit: String?
)

data class ComparisonData(
    val currentConsumption: Double,
    val previousConsumption: Double,
    val changePercentage: Double
)

// Новые структуры данных для графика
data class ChartPoint(
    val date: String,
    val dateShort: String,
    val value: Double,
    val consumption: Double,
    val index: Int
)

data class ChartData(
    val dataPoints: List<ChartPoint>,
    val dateRange: DateRange,
    val minValue: Double,
    val maxValue: Double
)

private data class PointData(
    val x: Float,
    val y: Float,
    val chartPoint: ChartPoint
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
    val allMetersSorted = meters.sortedBy { parseMeterDateTime(it.date) }

    // Получаем все показания за период для выбранных типов
    val periodMeters = meters.filter { meter ->
        val meterDateTime = parseMeterDateTime(meter.date)
        meterDateTime.isAfter(dateRange.start.atStartOfDay().minusSeconds(1)) &&
                meterDateTime.isBefore(dateRange.end.plusDays(1).atStartOfDay()) &&
                (selectedType == null || meter.type == selectedType)
    }

    if (periodMeters.isEmpty()) {
        return StatisticsData(
            kpi = KpiData(0.0, 0.0, 0.0, selectedType?.getUnit()),
            comparison = ComparisonData(0.0, 0.0, 0.0),
            distribution = emptyMap()
        )
    }

    val types = if (selectedType != null) listOf(selectedType) else MeterType.values().toList()
    val consumptionByType = mutableMapOf<MeterType, Double>()

    types.forEach { type ->
        // Получаем все показания этого типа
        val allTypeMeters = allMetersSorted.filter { it.type == type }

        if (allTypeMeters.isEmpty()) {
            consumptionByType[type] = 0.0
            return@forEach
        }

        // Получаем показания этого типа за период
        val typePeriodMeters = periodMeters.filter { it.type == type }.sortedBy { parseMeterDateTime(it.date) }

        if (typePeriodMeters.isEmpty()) {
            consumptionByType[type] = 0.0
            return@forEach
        }

        // Находим первое показание перед началом периода
        val firstReadingBeforePeriod = allTypeMeters
            .filter { parseMeterDateTime(it.date).isBefore(dateRange.start.atStartOfDay()) }
            .lastOrNull()

        // Первое показание в периоде
        val firstReadingInPeriod = typePeriodMeters.first()
        val lastReadingInPeriod = typePeriodMeters.last()

        // Определяем начальное значение для расчета расхода
        val startValue = if (firstReadingBeforePeriod != null &&
            parseMeterDateTime(firstReadingBeforePeriod.date).isBefore(parseMeterDateTime(firstReadingInPeriod.date))) {
            firstReadingBeforePeriod.value
        } else {
            // Если нет показаний до периода, начинаем с первого в периоде
            firstReadingInPeriod.value
        }

        val endValue = lastReadingInPeriod.value
        val consumption = max(0.0, endValue - startValue)

        consumptionByType[type] = consumption
    }

    val totalConsumption = consumptionByType.values.sum()

    // Рассчитываем количество дней в периоде
    val days = ChronoUnit.DAYS.between(dateRange.start, dateRange.end).coerceAtLeast(1).toDouble()

    // Получаем предыдущий период
    val previousDateRange = getPreviousDateRange(dateRange)

    // Рассчитываем потребление за предыдущий период
    val previousConsumption = if (previousDateRange.start.isBefore(previousDateRange.end)) {
        calculateConsumptionForPeriod(meters, previousDateRange, selectedType)
    } else {
        0.0
    }

    // Рассчитываем экономию (положительная - экономия, отрицательная - перерасход)
    val savings = if (previousConsumption > 0) {
        previousConsumption - totalConsumption
    } else {
        0.0
    }

    // Определяем единицу измерения для KPI
    val unit = if (selectedType != null) {
        selectedType.getUnit()
    } else {
        // При выборе всех типов показываем общий расход без единиц
        "ед."
    }

    return StatisticsData(
        kpi = KpiData(
            consumption = totalConsumption,
            averagePerDay = if (days > 0) totalConsumption / days else 0.0,
            savings = savings,
            unit = unit
        ),
        comparison = ComparisonData(
            currentConsumption = totalConsumption,
            previousConsumption = previousConsumption,
            changePercentage = if (previousConsumption > 0) {
                ((totalConsumption - previousConsumption) / previousConsumption * 100)
            } else {
                0.0
            }
        ),
        distribution = consumptionByType.filter { it.value > 0 }
    )
}

fun prepareChartData(
    meters: List<Meter>,
    dateRange: DateRange,
    selectedType: MeterType?
): ChartData {
    // Получаем все метры отсортированные по дате
    val allMetersSorted = meters.sortedBy { parseMeterDateTime(it.date) }

    // Фильтруем метры за период
    val filtered = meters.filter { meter ->
        val meterDateTime = parseMeterDateTime(meter.date)
        meterDateTime.isAfter(dateRange.start.atStartOfDay().minusSeconds(1)) &&
                meterDateTime.isBefore(dateRange.end.plusDays(1).atStartOfDay()) &&
                (selectedType == null || meter.type == selectedType)
    }

    if (filtered.isEmpty()) {
        return ChartData(emptyList(), dateRange, 0.0, 0.0)
    }

    // Группируем по типу
    val types = if (selectedType != null) listOf(selectedType) else MeterType.values().toList()

    val dataPoints = mutableListOf<ChartPoint>()

    types.forEach { type ->
        // Получаем все метры этого типа
        val allTypeMeters = allMetersSorted.filter { it.type == type }

        if (allTypeMeters.isEmpty()) return@forEach

        // Фильтруем метры этого типа за период
        val periodTypeMeters = filtered.filter { it.type == type }.sortedBy { parseMeterDateTime(it.date) }

        if (periodTypeMeters.isEmpty()) return@forEach

        // Находим первое показание перед периодом
        val firstReadingBeforePeriod = allTypeMeters
            .filter { parseMeterDateTime(it.date).isBefore(dateRange.start.atStartOfDay()) }
            .lastOrNull()

        var previousValue = if (firstReadingBeforePeriod != null &&
            parseMeterDateTime(firstReadingBeforePeriod.date).isBefore(parseMeterDateTime(periodTypeMeters.first().date))) {
            firstReadingBeforePeriod.value
        } else {
            periodTypeMeters.first().value
        }

        // Создаем точки для каждого показания в периоде
        periodTypeMeters.forEachIndexed { index, meter ->
            val currentValue = meter.value
            val consumption = max(0.0, currentValue - previousValue)

            dataPoints.add(
                ChartPoint(
                    date = meter.date,
                    dateShort = formatDateShort(meter.date, dateRange),
                    value = currentValue,
                    consumption = consumption,
                    index = dataPoints.size
                )
            )

            previousValue = currentValue
        }
    }

    // Сортируем все точки по дате
    val sortedDataPoints = dataPoints.sortedBy { parseMeterDateTime(it.date) }

    if (sortedDataPoints.isEmpty()) {
        return ChartData(emptyList(), dateRange, 0.0, 0.0)
    }

    val values = sortedDataPoints.map { it.value }
    return ChartData(
        dataPoints = sortedDataPoints,
        dateRange = dateRange,
        minValue = values.minOrNull() ?: 0.0,
        maxValue = values.maxOrNull() ?: 0.0
    )
}

fun parseMeterDateTime(dateString: String): LocalDateTime {
    val formats = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"),
        DateTimeFormatter.ofPattern("dd.MM.yyyy")
    )

    for (formatter in formats) {
        try {
            return LocalDateTime.parse(dateString, formatter)
        } catch (e: Exception) {
            try {
                return LocalDate.parse(dateString, formatter).atStartOfDay()
            } catch (e2: Exception) {
                continue
            }
        }
    }


    return try {
        LocalDateTime.parse(dateString)
    } catch (e: Exception) {
        LocalDateTime.now()
    }
}
fun calculateConsumptionForPeriod(
    meters: List<Meter>,
    dateRange: DateRange,
    selectedType: MeterType?
): Double {
    val allMetersSorted = meters.sortedBy { parseMeterDateTime(it.date) }

    // Получаем метры за период
    val periodMeters = meters.filter { meter ->
        val meterDateTime = parseMeterDateTime(meter.date)
        meterDateTime.isAfter(dateRange.start.atStartOfDay().minusSeconds(1)) &&
                meterDateTime.isBefore(dateRange.end.plusDays(1).atStartOfDay()) &&
                (selectedType == null || meter.type == selectedType)
    }

    if (periodMeters.isEmpty()) return 0.0

    val types = if (selectedType != null) listOf(selectedType) else MeterType.values().toList()
    var totalConsumption = 0.0

    types.forEach { type ->
        val allTypeMeters = allMetersSorted.filter { it.type == type }

        if (allTypeMeters.isEmpty()) return@forEach

        val typePeriodMeters = periodMeters.filter { it.type == type }.sortedBy { parseMeterDateTime(it.date) }

        if (typePeriodMeters.isEmpty()) return@forEach

        // Находим первое показание перед периодом
        val firstReadingBeforePeriod = allTypeMeters
            .filter { parseMeterDateTime(it.date).isBefore(dateRange.start.atStartOfDay()) }
            .lastOrNull()

        val firstReadingInPeriod = typePeriodMeters.first()
        val lastReadingInPeriod = typePeriodMeters.last()

        val startValue = if (firstReadingBeforePeriod != null &&
            parseMeterDateTime(firstReadingBeforePeriod.date).isBefore(parseMeterDateTime(firstReadingInPeriod.date))) {
            firstReadingBeforePeriod.value
        } else {
            firstReadingInPeriod.value
        }

        val endValue = lastReadingInPeriod.value

        totalConsumption += max(0.0, endValue - startValue)
    }

    return totalConsumption
}
fun getPreviousDateRange(currentRange: DateRange): DateRange {
    val daysBetween = ChronoUnit.DAYS.between(currentRange.start, currentRange.end).coerceAtLeast(1)
    return DateRange(
        start = currentRange.start.minusDays(daysBetween),
        end = currentRange.start.minusDays(1)
    )
}

fun parseMeterDate(dateString: String): LocalDateTime {
    return try {
        LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    } catch (e: Exception) {
        LocalDateTime.parse(dateString.substringBefore(" ") + " 00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    }
}

fun formatDateShort(dateString: String, dateRange: DateRange): String {
    val formatter = when {
        ChronoUnit.DAYS.between(dateRange.start, dateRange.end) <= 7 ->
            DateTimeFormatter.ofPattern("dd.MM")
        ChronoUnit.MONTHS.between(dateRange.start, dateRange.end) <= 3 ->
            DateTimeFormatter.ofPattern("dd.MM")
        else ->
            DateTimeFormatter.ofPattern("MM.yy")
    }

    return try {
        LocalDate.parse(dateString.substringBefore(" ")).format(formatter)
    } catch (e: Exception) {
        dateString.substringBefore(" ")
    }
}

fun getPeriodLabel(period: Period, dateRange: DateRange): String {
    val startStr = dateRange.start.format(DateTimeFormatter.ofPattern("dd.MM.yy"))
    val endStr = dateRange.end.format(DateTimeFormatter.ofPattern("dd.MM.yy"))
    return "$startStr - $endStr"
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
    MeterType.ELECTRICITY -> Color(0xFFFFFF00)
    MeterType.COLD_WATER -> Color(0xFF2196F3)
    MeterType.HOT_WATER -> Color(0xFFF44336)
}