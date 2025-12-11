package com.vstu.metterscanner.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.vstu.metterscanner.MeterViewModel
import com.vstu.metterscanner.data.Meter
import com.vstu.metterscanner.data.MeterType
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

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
    var showSuccessSnackbar by remember { mutableStateOf(false) }
    var showErrorSnackbar by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showCamera by remember { mutableStateOf(false) }

    val isFormValid by remember {
        derivedStateOf {
            value.isNotBlank() && value.toDoubleOrNull() != null
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Получаем последнее показание при изменении типа
    LaunchedEffect(selectedType) {
        lastMeter = viewModel.getLastMeter(selectedType)
    }

    // Показ Snackbar при успехе
    LaunchedEffect(showSuccessSnackbar) {
        if (showSuccessSnackbar) {
            snackbarHostState.showSnackbar(
                message = "Показание успешно сохранено!",
                duration = SnackbarDuration.Short
            )
            showSuccessSnackbar = false
        }
    }

    // Показ Snackbar при ошибке
    LaunchedEffect(showErrorSnackbar) {
        if (showErrorSnackbar) {
            snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = SnackbarDuration.Long,
                withDismissAction = true
            )
            showErrorSnackbar = false
        }
    }

    if (showCamera) {
        CameraScanScreen(
            onResult = { scannedValue ->
                value = scannedValue
                showCamera = false
            },
            onCancel = {
                showCamera = false
            }
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
                            enabled = !showCamera
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
                // Карточка с информацией о последнем показании
                if (lastMeter != null) {
                    LastMeterCard(lastMeter!!)
                    Spacer(modifier = Modifier.height(16.dp))
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

                    // Выпадающий список для выбора типа счетчика
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
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
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
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
                                        // Обновляем значение из последнего показания
                                        if (lastMeter != null) {
                                            value = lastMeter!!.value.toString()
                                            note = lastMeter!!.note
                                        } else {
                                            value = ""
                                            note = ""
                                        }
                                    }
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
                            modifier = Modifier.height(36.dp)
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
                            if (newValue.matches(Regex("^\\d*\\.?\\d*$")) && newValue.length <= 10) {
                                value = newValue
                            }
                        },
                        label = { Text("Например: 1234.56") },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = "Показания")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        isError = value.isNotBlank() && value.toDoubleOrNull() == null
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
                        onValueChange = { note = it },
                        label = { Text("Необязательно") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Кнопки действий
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            if (isFormValid) {
                                focusManager.clearFocus()
                                val meterValue = value.toDouble()

                                // Проверка: новое показание должно быть больше предыдущего
                                val lastValue = lastMeter?.value ?: 0.0
                                if (meterValue < lastValue) {
                                    errorMessage = "Новое показание должно быть больше предыдущего ($lastValue)"
                                    showErrorSnackbar = true
                                    return@Button
                                }

                                val meter = Meter(
                                    type = selectedType,
                                    value = meterValue,
                                    note = note
                                )

                                coroutineScope.launch {
                                    try {
                                        viewModel.addMeter(meter)
                                        showSuccessSnackbar = true
                                        // Задержка перед возвратом на главный экран
                                        kotlinx.coroutines.delay(1500)
                                        navController.popBackStack()
                                    } catch (e: Exception) {
                                        errorMessage = "Ошибка сохранения: ${e.message}"
                                        showErrorSnackbar = true
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = isFormValid,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Сохранить показания",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            navController.popBackStack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Отмена")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScanScreen(
    onResult: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var recognizedText by remember { mutableStateOf("") }
    var manualInput by remember { mutableStateOf("") }

    // Ланчер для запроса разрешения камеры
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("CameraScanScreen", "Разрешение на камеру получено")
        } else {
            Log.d("CameraScanScreen", "Разрешение на камеру отклонено")
        }
    }

    // Проверяем разрешение при запуске
    val hasCameraPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Запрашиваем разрешение если его нет
    LaunchedEffect(Unit) {
        if (!hasCameraPermission.value) {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сканирование показаний") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            if (recognizedText.isNotEmpty() || manualInput.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        val result = if (manualInput.isNotEmpty()) {
                            manualInput
                        } else {
                            extractNumberFromText(recognizedText) ?: ""
                        }
                        if (result.isNotBlank()) {
                            onResult(result)
                        }
                    }
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Использовать")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (hasCameraPermission.value) {
                // Камера
                SimpleCameraView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                // Область для результатов сканирования и ручного ввода
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Распознанный текст",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (recognizedText.isNotEmpty()) recognizedText else "Наведите камеру на показания счетчика...",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Divider()

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Или введите вручную",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = manualInput,
                            onValueChange = { newValue ->
                                if (newValue.matches(Regex("^\\d*\\.?\\d*$")) && newValue.length <= 10) {
                                    manualInput = newValue
                                }
                            },
                            label = { Text("Введите показания") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Демо-кнопка для тестирования (временная)
                        Button(
                            onClick = {
                                recognizedText = "Текущие показания: 1234.56 кВт⋅ч"
                                manualInput = "1234.56"
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Демо: считать 1234.56")
                        }
                    }
                }
            } else {
                // Если нет разрешения
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Camera,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Требуется доступ к камере",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Для сканирования показаний счетчика необходимо разрешение на использование камеры",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = {
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        ) {
                            Text("Запросить разрешение")
                        }
                        OutlinedButton(
                            onClick = onCancel
                        ) {
                            Text("Вернуться к ручному вводу")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleCameraView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE

                // Инициализируем камеру
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder()
                            .build()
                            .also {
                                it.setSurfaceProvider(surfaceProvider)
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        // Останавливаем предыдущие use cases и запускаем новые
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview
                        )

                    } catch (e: Exception) {
                        Log.e("CameraScreen", "Ошибка инициализации камеры: ${e.message}", e)
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        },
        modifier = modifier
    )
}

// Функция для извлечения чисел из текста
private fun extractNumberFromText(text: String): String? {
    val regex = """\d+[,.]?\d*""".toRegex()
    val match = regex.find(text)
    return match?.value?.replace(',', '.')
}

@Composable
fun LastMeterCard(lastMeter: Meter) {
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
                    text = "Последнее показание",
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
                    text = "${lastMeter.value}",
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