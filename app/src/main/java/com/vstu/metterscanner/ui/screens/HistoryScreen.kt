@file:OptIn(ExperimentalMaterial3Api::class)

package com.vstu.metterscanner.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.vstu.metterscanner.MeterViewModel
import com.vstu.metterscanner.data.Meter
import com.vstu.metterscanner.data.MeterType
import com.vstu.metterscanner.ui.components.MeterCard
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import java.io.File
import java.io.FileOutputStream
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.activity.compose.BackHandler
import com.vstu.metterscanner.ui.components.MeterCardWithPhoto
import com.vstu.metterscanner.ui.screens.ImageUtils
import com.vstu.metterscanner.ui.screens.ImageUtils.loadBitmapFromFile


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MeterViewModel,
    navController: NavController
) {
    val meters by viewModel.allMeters.collectAsStateWithLifecycle()

    // Фильтрация
    var selectedPeriod by remember { mutableStateOf(PeriodFilter.ALL) }
    var selectedTypeFilter by remember { mutableStateOf<MeterType?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Сортировка и группировка
    var selectedSortOption by remember { mutableStateOf(SortOption.DATE_DESC) }
    var groupingMode by remember { mutableStateOf(GroupingMode.NONE) }

    // UI состояния
    var showFilters by remember { mutableStateOf(false) }

    // Диалоги редактирования/удаления
    var selectedMeter by remember { mutableStateOf<Meter?>(null) }
    var showMeterMenuDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Сброс всех диалогов при уходе с экрана
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                showMeterMenuDialog = false
                showDeleteDialog = false
                showEditDialog = false
                selectedMeter = null
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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

    // Диалог меню для показаний
    if (showMeterMenuDialog && selectedMeter != null) {
        BackHandler {
            showMeterMenuDialog = false
            selectedMeter = null
        }
        MeterMenuDialog(
            meter = selectedMeter!!,
            onDismiss = {
                showMeterMenuDialog = false
                selectedMeter = null
            },
            onEdit = {
                showMeterMenuDialog = false
                showEditDialog = true
            },
            onDelete = {
                showMeterMenuDialog = false
                showDeleteDialog = true
            },
            snackbarHostState = snackbarHostState
        )
    }

    // Диалог удаления
    if (showDeleteDialog && selectedMeter != null) {
        BackHandler {
            showDeleteDialog = false
            selectedMeter = null
        }
        DeleteMeterDialog(
            meter = selectedMeter!!,
            onDismiss = {
                showDeleteDialog = false
                selectedMeter = null
            },
            onConfirm = {
                coroutineScope.launch {
                    try {
                        viewModel.deleteMeter(selectedMeter!!)
                        snackbarHostState.showSnackbar("Показание удалено")
                        showDeleteDialog = false
                        selectedMeter = null
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Ошибка при удалении")
                    }
                }
            }
        )
    }

    // Диалог редактирования
    if (showEditDialog && selectedMeter != null) {
        BackHandler {
            showEditDialog = false
            selectedMeter = null
        }
        EditMeterDialog(
            meter = selectedMeter!!,
            viewModel = viewModel,
            onDismiss = {
                showEditDialog = false
                selectedMeter = null
            },
            snackbarHostState = snackbarHostState
        )
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
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
                            MeterCardWithPhoto(
                                meter = meter,
                                onClick = {
                                    // Открываем меню для редактирования/удаления
                                    selectedMeter = meter
                                    showMeterMenuDialog = true
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
fun MeterMenuDialog(
    meter: Meter,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Действия с показанием") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Информация о показании
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Тип:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                when (meter.type) {
                                    MeterType.ELECTRICITY -> "Электричество"
                                    MeterType.COLD_WATER -> "Холодная вода"
                                    MeterType.HOT_WATER -> "Горячая вода"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Значение:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${meter.value} ${getUnitForType(meter.type)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Дата:", style = MaterialTheme.typography.bodySmall)
                            Text(meter.date, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Информация о фото
                if (meter.photoPath != null) {
                    Text(
                        text = "✓ С фото",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Кнопка удаления
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Удалить")
                }

                // Кнопка редактирования
                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Изменить")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Отмена")
            }
        }
    )
}

// ДИАЛОГ УДАЛЕНИЯ
@Composable
fun DeleteMeterDialog(
    meter: Meter,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удалить показание?") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Вы уверены, что хотите удалить это показание?")

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "${meter.value} ${getUnitForType(meter.type)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            when (meter.type) {
                                MeterType.ELECTRICITY -> "Электричество"
                                MeterType.COLD_WATER -> "Холодная вода"
                                MeterType.HOT_WATER -> "Горячая вода"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            meter.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Это действие нельзя отменить.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Удалить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

// Обновите функцию EditMeterDialog:
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMeterDialog(
    meter: Meter,
    viewModel: MeterViewModel,
    onDismiss: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var value by remember { mutableStateOf(meter.value.toString()) }
    var note by remember { mutableStateOf(meter.note) }
    var capturedPhotoPath by remember { mutableStateOf(meter.photoPath) }
    var showCropScreen by remember { mutableStateOf(false) }
    var imageToCropUri by remember { mutableStateOf<Uri?>(null) }

    val isValid by remember(value) {
        derivedStateOf { value.isNotBlank() && value.toDoubleOrNull() != null }
    }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Лаунчер для выбора фото из галереи
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val copiedPath = copyImageToAppStorage(context, uri)
            if (copiedPath != null) {
                imageToCropUri = Uri.fromFile(File(copiedPath))
                showCropScreen = true
            }
        }
    }

    // Экран обрезки фото
    if (showCropScreen && imageToCropUri != null) {
        ImageCropScreen(
            imageUri = imageToCropUri!!,
            onCropComplete = { croppedPath ->
                capturedPhotoPath = croppedPath
                showCropScreen = false
                imageToCropUri = null
            },
            onCancel = {
                showCropScreen = false
                imageToCropUri = null
            },
            context = context
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Изменить показание") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Информация о типе и дате
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Тип:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                when (meter.type) {
                                    MeterType.ELECTRICITY -> "Электричество"
                                    MeterType.COLD_WATER -> "Холодная вода"
                                    MeterType.HOT_WATER -> "Горячая вода"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Дата:", style = MaterialTheme.typography.bodySmall)
                            Text(meter.date, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Поле для значения
                OutlinedTextField(
                    value = value,
                    onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) value = it },
                    label = { Text("Значение (${getUnitForType(meter.type)})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    isError = value.isNotBlank() && !isValid,
                    leadingIcon = {
                        Icon(
                            when (meter.type) {
                                MeterType.ELECTRICITY -> Icons.Default.FlashOn
                                MeterType.COLD_WATER -> Icons.Default.WaterDrop
                                MeterType.HOT_WATER -> Icons.Default.Whatshot
                            },
                            contentDescription = null
                        )
                    }
                )

                // Поле для заметки
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Заметка") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    maxLines = 4,
                    leadingIcon = {
                        Icon(Icons.Default.Notes, contentDescription = null)
                    }
                )

                // Секция с фото
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Фото счетчика",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Кнопка добавить фото
                            OutlinedButton(
                                onClick = {
                                    // Открываем выбор фото из галереи
                                    imagePickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Добавить фото",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Добавить")
                            }

                            // Кнопка удалить фото
                            if (capturedPhotoPath != null) {
                                IconButton(
                                    onClick = {
                                        capturedPhotoPath = null
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Удалить фото",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }


                    if (capturedPhotoPath != null) {
                        val currentBitmap = loadBitmapFromFile(context, capturedPhotoPath!!)
                        if (currentBitmap != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                )
                            ) {
                                Column {
                                    Image(
                                        bitmap = currentBitmap.asImageBitmap(),
                                        contentDescription = "Текущее фото",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Текущее фото",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Нажмите чтобы обрезать",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        } else {
                            // Если фото не загружается
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.BrokenImage,
                                        contentDescription = "Фото не загружено",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Файл фото недоступен",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    } else {
                        // Если фото нет
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Photo,
                                    contentDescription = "Нет фото",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "Нет прикрепленного фото",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Подсказка
                    Text(
                        text = "Вы можете добавить фото счетчика для лучшей идентификации",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
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
                    Text("Отмена")
                }
                Button(
                    onClick = {
                        if (isValid) {
                            val updatedMeter = meter.copy(
                                value = value.toDouble(),
                                note = note,
                                photoPath = capturedPhotoPath
                            )
                            coroutineScope.launch {
                                try {
                                    viewModel.updateMeter(updatedMeter)
                                    snackbarHostState.showSnackbar("Показание обновлено")
                                    onDismiss()
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Ошибка при обновлении: ${e.message}")
                                }
                            }
                        }
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Сохранить")
                }
            }
        }
    )
}

// ЭКРАН ОБРЕЗКИ ФОТО
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCropScreen(
    imageUri: Uri,
    onCropComplete: (String) -> Unit,
    onCancel: () -> Unit,
    context: Context
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var cropRect by remember { mutableStateOf(android.graphics.Rect()) }

    val transformableState = rememberTransformableState { zoomChange, panChange, rotationChange ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        offset += panChange * scale
    }

    val bitmap = remember(imageUri) {
        loadBitmapFromUri(context, imageUri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Обрезка фото") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // Выполняем обрезку
                            if (bitmap != null && cropRect.width() > 0 && cropRect.height() > 0) {
                                val croppedBitmap = android.graphics.Bitmap.createBitmap(
                                    bitmap!!,
                                    cropRect.left,
                                    cropRect.top,
                                    cropRect.width(),
                                    cropRect.height()
                                )

                                // Сохраняем обрезанное фото
                                val croppedPath = saveCroppedBitmap(context, croppedBitmap)
                                onCropComplete(croppedPath)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Готово")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (bitmap != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Инструкция
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Обрежьте фото счетчика",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Перемещайте и масштабируйте изображение, чтобы счетчик был в рамке",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Область для обрезки
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.1f))
                ) {
                    // Изображение
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Фото для обрезки",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .transformable(transformableState)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                                    offset += pan * scale
                                }
                            }
                    )

                    // Рамка для обрезки
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        // Размеры рамки (такие же как при сканировании)
                        val frameWidth = canvasWidth * 0.8f
                        val frameHeight = frameWidth * 0.3f

                        val frameLeft = (canvasWidth - frameWidth) / 2
                        val frameTop = (canvasHeight - frameHeight) / 2
                        val frameRight = frameLeft + frameWidth
                        val frameBottom = frameTop + frameHeight

                        // Сохраняем координаты для обрезки
                        cropRect = android.graphics.Rect(
                            frameLeft.toInt(),
                            frameTop.toInt(),
                            frameRight.toInt(),
                            frameBottom.toInt()
                        )

                        // Оверлей вокруг рамки
                        drawRect(
                            color = Color.Black.copy(alpha = 0.6f),
                            topLeft = Offset(0f, 0f),
                            size = size.copy(height = frameTop)
                        )
                        drawRect(
                            color = Color.Black.copy(alpha = 0.6f),
                            topLeft = Offset(0f, frameBottom),
                            size = size.copy(height = canvasHeight - frameBottom)
                        )
                        drawRect(
                            color = Color.Black.copy(alpha = 0.6f),
                            topLeft = Offset(0f, frameTop),
                            size = size.copy(width = frameLeft, height = frameHeight)
                        )
                        drawRect(
                            color = Color.Black.copy(alpha = 0.6f),
                            topLeft = Offset(frameRight, frameTop),
                            size = size.copy(width = canvasWidth - frameRight, height = frameHeight)
                        )

                        // Зеленая рамка
                        drawRect(
                            color = Color.Green,
                            topLeft = Offset(frameLeft, frameTop),
                            size = Size(frameWidth, frameHeight),
                            style = Stroke(width = 3f)
                        )

                        // Уголки рамки
                        val cornerLength = 30f
                        val cornerWidth = 4f

                        // Левый верхний угол
                        drawLine(
                            color = Color.Green,
                            start = Offset(frameLeft, frameTop),
                            end = Offset(frameLeft + cornerLength, frameTop),
                            strokeWidth = cornerWidth
                        )
                        drawLine(
                            color = Color.Green,
                            start = Offset(frameLeft, frameTop),
                            end = Offset(frameLeft, frameTop + cornerLength),
                            strokeWidth = cornerWidth
                        )

                        // Правый верхний угол
                        drawLine(
                            color = Color.Green,
                            start = Offset(frameRight, frameTop),
                            end = Offset(frameRight - cornerLength, frameTop),
                            strokeWidth = cornerWidth
                        )
                        drawLine(
                            color = Color.Green,
                            start = Offset(frameRight, frameTop),
                            end = Offset(frameRight, frameTop + cornerLength),
                            strokeWidth = cornerWidth
                        )

                        // Левый нижний угол
                        drawLine(
                            color = Color.Green,
                            start = Offset(frameLeft, frameBottom),
                            end = Offset(frameLeft + cornerLength, frameBottom),
                            strokeWidth = cornerWidth
                        )
                        drawLine(
                            color = Color.Green,
                            start = Offset(frameLeft, frameBottom),
                            end = Offset(frameLeft, frameBottom - cornerLength),
                            strokeWidth = cornerWidth
                        )

                        // Правый нижний угол
                        drawLine(
                            color = Color.Green,
                            start = Offset(frameRight, frameBottom),
                            end = Offset(frameRight - cornerLength, frameBottom),
                            strokeWidth = cornerWidth
                        )
                        drawLine(
                            color = Color.Green,
                            start = Offset(frameRight, frameBottom),
                            end = Offset(frameRight, frameBottom - cornerLength),
                            strokeWidth = cornerWidth
                        )
                    }
                }

                // Подсказки по управлению
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ZoomIn,
                                contentDescription = "Масштабирование",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Два пальца\nдля масштаба",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.PanTool,
                                contentDescription = "Перемещение",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Перемещайте\nдля позиции",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            // Если не удалось загрузить изображение
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = "Ошибка",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Не удалось загрузить изображение",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onCancel) {
                    Text("Вернуться назад")
                }
            }
        }
    }
}

// ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ
fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun saveCroppedBitmap(context: Context, bitmap: Bitmap): String {
    val timeStamp = System.currentTimeMillis()
    val fileName = "meter_cropped_${timeStamp}.jpg"
    val file = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), fileName)

    try {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return file.absolutePath
}



// Компоненты ниже остаются без изменений
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

            androidx.compose.material3.TextField(
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

fun copyImageToAppStorage(context: Context, uri: Uri): String? {
    return try {
        val timeStamp = System.currentTimeMillis()
        val fileName = "meter_edit_${timeStamp}.jpg"
        val file = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), fileName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }

        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
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

