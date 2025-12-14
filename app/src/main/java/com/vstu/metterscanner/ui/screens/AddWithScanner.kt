package com.vstu.metterscanner.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.vstu.metterscanner.MeterViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScanScreen(
    onResult: (String, String?) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var recognizedText by remember { mutableStateOf("") }
    var manualInput by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var scanResult by remember { mutableStateOf<String?>(null) }
    var photoPath by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Анимация сканирования
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            animatedProgress.animateTo(
                1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            animatedProgress.stop()
            animatedProgress.snapTo(0f)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("CameraScanScreen", "Разрешение на камеру получено")
        } else {
            Log.d("CameraScanScreen", "Разрешение на камеру отклонено")
        }
    }

    val hasCameraPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission.value) {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // Функция для немедленного возврата результата
    fun returnResult(value: String, path: String?) {
        Log.d("CameraScanScreen", "Возвращаем результат: $value, путь: $path")
        onResult(value, path)
    }

    fun capturePhoto() {
        val imageCapture = imageCapture ?: return

        // Создаем временный файл для фото
        val photoFile = File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES),
            "meter_photo_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    coroutineScope.launch {
                        val uri = Uri.fromFile(photoFile)
                        capturedImageUri = uri
                        photoPath = photoFile.absolutePath

                        // Используем TextRecognitionHelper для распознавания
                        val textResult = TextRecognitionHelper.recognizeTextFromUri(
                            context,
                            uri,

                            cropToScanArea = true
                        )

                        recognizedText = textResult
                        Log.d("CameraScanScreen", "Полный распознанный текст: $textResult")

                        val numbers = TextRecognitionHelper.extractNumbersFromText(textResult)
                        Log.d("CameraScanScreen", "Извлечены числа: $numbers")

                        val filteredNumbers = TextRecognitionHelper.filterMeterReadings(numbers)
                        Log.d("CameraScanScreen", "Отфильтрованные числа: $filteredNumbers")

                        if (filteredNumbers.isNotEmpty()) {
                            // Берем наиболее вероятное показание (самое длинное)
                            val selectedNumber = filteredNumbers.maxByOrNull { it.replace(".", "").length }
                            manualInput = selectedNumber ?: ""
                            scanResult = selectedNumber

                            Log.d("CameraScanScreen", "Выбрано число: $selectedNumber")

                            // Немедленно возвращаем результат
                            if (selectedNumber != null && selectedNumber.isNotBlank()) {
                                Log.d("CameraScanScreen", "Автоматически подтверждаем результат")
                                returnResult(selectedNumber, photoFile.absolutePath)
                            }
                        } else {
                            manualInput = ""
                            scanResult = null
                            Log.d("CameraScanScreen", "Не найдено подходящих чисел")
                        }
                        isScanning = false
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraScanScreen", "Ошибка при съемке фото: ${exception.message}", exception)
                    isScanning = false
                }
            }
        )
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
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Кнопка сделать фото
                FloatingActionButton(
                    onClick = {
                        isScanning = true
                        capturePhoto()
                    }
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Camera, contentDescription = "Сделать фото")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Кнопка подтверждения (всегда видима если есть мануальный ввод)
                if (manualInput.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = {
                            returnResult(manualInput, photoPath)
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Использовать")
                    }
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
                // Камера - занимает большую часть экрана
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Полноэкранная камера
                    AdvancedCameraView(
                        modifier = Modifier.fillMaxSize(),
                        onCameraInitialized = { imageCaptureInstance ->
                            imageCapture = imageCaptureInstance
                        }
                    )

                    // Overlay с рамкой сканирования
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Рисуем 4 черных прямоугольника ВОКРУГ области сканирования
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val scanWidth = canvasWidth * 0.8f
                            val scanHeight = scanWidth * 0.3f
                            val scanRect = Rect(
                                left = (canvasWidth - scanWidth) / 2,
                                top = (canvasHeight - scanHeight) / 2,
                                right = (canvasWidth + scanWidth) / 2,
                                bottom = (canvasHeight + scanHeight) / 2
                            )

                            // 1. Верхний черный прямоугольник
                            drawRect(
                                color = Color.Black.copy(alpha = 0.6f),
                                topLeft = Offset(0f, 0f),
                                size = Size(canvasWidth, scanRect.top)
                            )

                            // 2. Нижний черный прямоугольник
                            drawRect(
                                color = Color.Black.copy(alpha = 0.6f),
                                topLeft = Offset(0f, scanRect.bottom),
                                size = Size(canvasWidth, canvasHeight - scanRect.bottom)
                            )

                            // 3. Левый черный прямоугольник (слева от области сканирования)
                            drawRect(
                                color = Color.Black.copy(alpha = 0.6f),
                                topLeft = Offset(0f, scanRect.top),
                                size = Size(scanRect.left, scanRect.height)
                            )

                            // 4. Правый черный прямоугольник (справа от области сканирования)
                            drawRect(
                                color = Color.Black.copy(alpha = 0.6f),
                                topLeft = Offset(scanRect.right, scanRect.top),
                                size = Size(canvasWidth - scanRect.right, scanRect.height)
                            )

                            // Зеленая рамка области сканирования
                            drawRect(
                                color = Color.Green,
                                topLeft = Offset(scanRect.left, scanRect.top),
                                size = Size(scanRect.width, scanRect.height),
                                style = Stroke(width = 3f)
                            )

                            // Уголки рамки
                            val cornerLength = 30f
                            val cornerWidth = 4f

                            // Левый верхний угол
                            drawLine(
                                color = Color.Green,
                                start = Offset(scanRect.left, scanRect.top),
                                end = Offset(scanRect.left + cornerLength, scanRect.top),
                                strokeWidth = cornerWidth
                            )
                            drawLine(
                                color = Color.Green,
                                start = Offset(scanRect.left, scanRect.top),
                                end = Offset(scanRect.left, scanRect.top + cornerLength),
                                strokeWidth = cornerWidth
                            )

                            // Правый верхний угол
                            drawLine(
                                color = Color.Green,
                                start = Offset(scanRect.right, scanRect.top),
                                end = Offset(scanRect.right - cornerLength, scanRect.top),
                                strokeWidth = cornerWidth
                            )
                            drawLine(
                                color = Color.Green,
                                start = Offset(scanRect.right, scanRect.top),
                                end = Offset(scanRect.right, scanRect.top + cornerLength),
                                strokeWidth = cornerWidth
                            )

                            // Левый нижний угол
                            drawLine(
                                color = Color.Green,
                                start = Offset(scanRect.left, scanRect.bottom),
                                end = Offset(scanRect.left + cornerLength, scanRect.bottom),
                                strokeWidth = cornerWidth
                            )
                            drawLine(
                                color = Color.Green,
                                start = Offset(scanRect.left, scanRect.bottom),
                                end = Offset(scanRect.left, scanRect.bottom - cornerLength),
                                strokeWidth = cornerWidth
                            )

                            // Правый нижний угол
                            drawLine(
                                color = Color.Green,
                                start = Offset(scanRect.right, scanRect.bottom),
                                end = Offset(scanRect.right - cornerLength, scanRect.bottom),
                                strokeWidth = cornerWidth
                            )
                            drawLine(
                                color = Color.Green,
                                start = Offset(scanRect.right, scanRect.bottom),
                                end = Offset(scanRect.right, scanRect.bottom - cornerLength),
                                strokeWidth = cornerWidth
                            )

                            // Движущаяся линия сканирования (только если идет сканирование)
                            if (isScanning) {
                                drawLine(
                                    color = Color.Green.copy(alpha = 0.7f),
                                    start = Offset(
                                        scanRect.left,
                                        scanRect.top + scanRect.height * animatedProgress.value
                                    ),
                                    end = Offset(
                                        scanRect.right,
                                        scanRect.top + scanRect.height * animatedProgress.value
                                    ),
                                    strokeWidth = 3f
                                )
                            }
                        }

                        // Подсказка
                        Text(
                            text = "Наведите счетчик в рамку",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 32.dp)
                        )
                    }
                }

                // Индикатор сканирования (поверх камеры)
                if (isScanning) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 4.dp,
                                color = Color.Green
                            )
                            Text(
                                text = "Распознавание...",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Нижняя панель с результатами
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
                            text = "Результаты распознавания",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (scanResult != null) {
                            Text(
                                text = "✓ Значение автоматически распознано: $scanResult",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Экран закроется автоматически через 2 секунды",
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else if (recognizedText.isNotEmpty()) {
                            val numbers = TextRecognitionHelper.extractNumbersFromText(recognizedText)
                            val filteredNumbers = TextRecognitionHelper.filterMeterReadings(numbers)

                            if (filteredNumbers.isNotEmpty()) {
                                val cleanNumbers = filteredNumbers.distinct()
                                Text(
                                    text = "Найдены цифры: ${cleanNumbers.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                // Предлагаем выбрать другое число
                                if (cleanNumbers.size > 1) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Выберите значение:",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        cleanNumbers.forEach { number ->
                                            FilterChip(
                                                selected = manualInput == number,
                                                onClick = {
                                                    manualInput = number
                                                    scanResult = number
                                                },
                                                label = { Text(number) }
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "Значимые цифры не найдены",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            Text(
                                text = "Сделайте фото счетчика в области сканирования...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

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
                                // Разрешаем только цифры и одну точку
                                if (newValue.matches(Regex("^\\d*\\.?\\d*$")) && newValue.length <= 10) {
                                    manualInput = newValue
                                    scanResult = newValue
                                }
                            },
                            label = { Text("Введите показания") },
                            placeholder = { Text("Например: 1234.56") },
                            leadingIcon = {
                                Icon(Icons.Default.Numbers, contentDescription = "Цифры")
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        if (manualInput.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    returnResult(manualInput, photoPath)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Использовать это значение")
                            }
                        }
                    }
                }
            } else {
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
                            Icon(Icons.Default.Camera, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Запросить разрешение")
                        }
                        OutlinedButton(
                            onClick = onCancel
                        ) {
                            Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Вернуться к ручному вводу")
                        }
                    }
                }
            }
        }
    }

    // Автоматически закрываем экран через 2 секунды после успешного распознавания
    LaunchedEffect(scanResult) {
        if (scanResult != null && scanResult!!.isNotBlank()) {
            kotlinx.coroutines.delay(2000)
            returnResult(scanResult!!, photoPath)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

@Composable
fun AdvancedCameraView(
    modifier: Modifier = Modifier,
    onCameraInitialized: (ImageCapture) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE

                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder()
                            .build()
                            .also {
                                it.setSurfaceProvider(surfaceProvider)
                            }

                        imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )

                        imageCapture?.let { onCameraInitialized(it) }

                    } catch (e: Exception) {
                        Log.e("AdvancedCameraView", "Ошибка инициализации камеры: ${e.message}", e)
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPreviewScreen(
    photoPath: String,
    onConfirm: () -> Unit,
    onRetake: () -> Unit,
    recognizedValue: String,
    viewModel: MeterViewModel
) {
    val context = LocalContext.current
    val bitmap = remember(photoPath) {
        loadBitmapFromFile(context, photoPath)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Предпросмотр фото") },
                navigationIcon = {
                    IconButton(onClick = onRetake) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onRetake,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Camera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Переснять")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Использовать")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            bitmap?.let { it ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Фото счетчика",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Поле для подтверждения/исправления значения
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
                        text = "Распознанное значение:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = recognizedValue,
                        onValueChange = { /* Можно добавить редактирование */ },
                        label = { Text("Показания") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Если значение распознано неверно, вы можете отредактировать его на предыдущем экране",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Вспомогательная функция для загрузки Bitmap
fun loadBitmapFromFile(context: Context, filePath: String): Bitmap? {
    return try {
        val file = File(filePath)
        if (!file.exists()) return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, Uri.fromFile(file))
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, Uri.fromFile(file))
        }
    } catch (e: Exception) {
        Log.e("BitmapLoader", "Ошибка загрузки изображения: ${e.message}", e)
        null
    }
}