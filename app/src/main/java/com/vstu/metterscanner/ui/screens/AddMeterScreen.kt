package com.vstu.metterscanner.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.vstu.metterscanner.MeterViewModel
import com.vstu.metterscanner.data.Meter
import com.vstu.metterscanner.data.MeterType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMeterScreen(
    viewModel: MeterViewModel,
    navController: NavController
) {
    var selectedType by remember { mutableStateOf(MeterType.ELECTRICITY) }
    var value by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var lastMeter by remember { mutableStateOf<Meter?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var capturedPhotoPath by remember { mutableStateOf<String?>(null) }
    var showPhotoPreview by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val isFormValid by remember {
        derivedStateOf {
            value.isNotBlank() && value.toDoubleOrNull() != null
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Получаем последнее показание при изменении типа только для отображения в карточке
    LaunchedEffect(selectedType) {
        lastMeter = viewModel.getLastMeter(selectedType)
    }

    if (showCamera) {
        CameraScanScreen(
            onResult = { scannedValue, photoPath ->
                // АВТОМАТИЧЕСКОЕ ОБНОВЛЕНИЕ ПОЛЯ ПРИ ПОЛУЧЕНИИ РЕЗУЛЬТАТА
                value = scannedValue
                capturedPhotoPath = photoPath
                showCamera = false

                // Показать уведомление, что значение обновлено
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Значение распознано и установлено: $scannedValue",
                        duration = SnackbarDuration.Short
                    )
                }
            },
            onCancel = {
                showCamera = false
            }
        )
    } else if (showPhotoPreview && capturedPhotoPath != null) {
        PhotoPreviewScreen(
            photoPath = capturedPhotoPath!!,
            onConfirm = {
                showPhotoPreview = false
            },
            onRetake = {
                showPhotoPreview = false
                showCamera = true
            },
            recognizedValue = value,
            viewModel = viewModel
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Добавить показания") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showCamera = true },
                            enabled = !showCamera && !isSaving
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Сканировать")
                        }
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Карточка с информацией о последнем показании (ТОЛЬКО ДЛЯ СПРАВКИ)
                if (lastMeter != null) {
                    // ИСПРАВЛЕНО: используем простую карточку без фото
                    LastMeterCardSimple(lastMeter!!)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Превью фотографии, если она есть (только для текущего добавления)
                capturedPhotoPath?.let { photoPath ->
                    val bitmap = ImageUtils.loadBitmapFromFile(context, photoPath)
                    bitmap?.let {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
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
                                    Text(
                                        text = "Сделанное фото",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = {
                                            capturedPhotoPath = null
                                            showPhotoPreview = false
                                        },
                                        modifier = Modifier.size(24.dp),
                                        enabled = !isSaving
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Удалить фото",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "Фото счетчика",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )
                            }
                        }
                    }
                }

                // Выбор типа счетчика
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = "Тип счетчика",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded && !isSaving }
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            readOnly = true,
                            value = when (selectedType) {
                                MeterType.ELECTRICITY -> "Электричество"
                                MeterType.COLD_WATER -> "Холодная вода"
                                MeterType.HOT_WATER -> "Горячая вода"
                            },
                            onValueChange = {},
                            label = { Text("Выберите тип счетчика") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            enabled = !isSaving
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            MeterType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (type) {
                                                MeterType.ELECTRICITY -> "Электричество"
                                                MeterType.COLD_WATER -> "Холодная вода"
                                                MeterType.HOT_WATER -> "Горячая вода"
                                            }
                                        )
                                    },
                                    onClick = {
                                        selectedType = type
                                        expanded = false
                                        // Только загружаем последнее показание для отображения в карточке
                                        coroutineScope.launch {
                                            lastMeter = viewModel.getLastMeter(type)
                                        }
                                    },
                                    enabled = !isSaving
                                )
                            }
                        }
                    }
                }

                // Показания счетчика
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "Показания",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )

                        // Кнопка сканирования
                        OutlinedButton(
                            onClick = { showCamera = true },
                            modifier = Modifier.height(36.dp),
                            enabled = !isSaving
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Сканировать",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Сканировать")
                        }
                    }

                    OutlinedTextField(
                        value = value,
                        onValueChange = { newValue ->
                            if (!isSaving && newValue.matches(Regex("^\\d*\\.?\\d*$")) && newValue.length <= 10) {
                                value = newValue
                            }
                        },
                        label = {
                            val unit = when (selectedType) {
                                MeterType.ELECTRICITY -> "кВт·ч"
                                MeterType.COLD_WATER -> "м³"
                                MeterType.HOT_WATER -> "м³"
                            }
                            Text("Например: 1234.56 ($unit)")
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = "Показания")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        isError = value.isNotBlank() && value.toDoubleOrNull() == null,
                        enabled = !isSaving
                    )

                    if (value.isNotBlank() && value.toDoubleOrNull() == null) {
                        Text(
                            text = "Введите корректное число",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                        )
                    }
                }

                // Заметка
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Заметка",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = { if (!isSaving) note = it },
                        label = { Text("Необязательно") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSaving
                    )
                }

                // Кнопки действий
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            if (isFormValid && !isSaving) {
                                isSaving = true
                                focusManager.clearFocus()
                                val meterValue = value.toDouble()

                                // Проверка, что новое значение больше предыдущего
                                val lastValue = lastMeter?.value ?: 0.0
                                if (meterValue < lastValue) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Новое показание должно быть больше предыдущего ($lastValue)",
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                    isSaving = false
                                    return@Button
                                }

                                val meter = Meter(
                                    type = selectedType,
                                    value = meterValue,
                                    note = note,
                                    photoPath = capturedPhotoPath
                                )

                                coroutineScope.launch {
                                    try {
                                        viewModel.addMeter(meter)
                                        snackbarHostState.showSnackbar(
                                            message = "Показание успешно сохранено!",
                                            duration = SnackbarDuration.Short
                                        )

                                        // Короткая задержка для показа snackbar, затем автоматическое закрытие
                                        kotlinx.coroutines.delay(800)

                                        // Автоматически закрываем экран и возвращаемся на MainScreen
                                        navController.popBackStack()
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar(
                                            message = "Ошибка сохранения: ${e.message}",
                                            duration = SnackbarDuration.Long
                                        )
                                        isSaving = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = isFormValid && !isSaving,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Сохранение...")
                            }
                        } else {
                            Text(
                                text = "Сохранить показания",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            if (!isSaving) {
                                navController.popBackStack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isSaving
                    ) {
                        Text("Отмена")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun LastMeterCardSimple(lastMeter: Meter) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Предыдущее показание",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = lastMeter.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Тип: ${when (lastMeter.type) {
                            MeterType.ELECTRICITY -> "Электричество"
                            MeterType.COLD_WATER -> "Холодная вода"
                            MeterType.HOT_WATER -> "Горячая вода"
                        }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (lastMeter.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Заметка: ${lastMeter.note}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "${lastMeter.value} ${getUnitForType(lastMeter.type)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Введите значение больше ${lastMeter.value}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
            )
        }
    }
}

