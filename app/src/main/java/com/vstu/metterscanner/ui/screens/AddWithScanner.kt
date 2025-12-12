package com.vstu.metterscanner.ui.screens

import android.R.color.transparent
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
import androidx.compose.foundation.background
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
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.vstu.metterscanner.MeterViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
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
    val coroutineScope = rememberCoroutineScope()
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Анимация сканирования - вынесена за пределы Canvas
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

                        // ВАЖНО: Всегда обрезаем до области сканирования
                        recognizeTextFromImage(context, uri, cropToScanArea = true) { text ->
                            recognizedText = text
                            val numbers = extractNumbersFromText(text)

                            // Строгая фильтрация: только числа из области сканирования
                            val filteredNumbers = filterMeterReadings(numbers)

                            if (filteredNumbers.isNotEmpty()) {
                                // Берем наиболее вероятное показание (самое длинное)
                                val selectedNumber = filteredNumbers.maxByOrNull { it.replace(".", "").length }
                                manualInput = selectedNumber ?: ""
                            } else {
                                manualInput = ""
                            }
                            isScanning = false
                        }
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

                // Кнопка подтверждения
                if (recognizedText.isNotEmpty() || manualInput.isNotEmpty() || capturedImageUri != null) {
                    FloatingActionButton(
                        onClick = {
                            val result = if (manualInput.isNotEmpty()) {
                                manualInput
                            } else {
                                val numbers = extractNumbersFromText(recognizedText)
                                if (numbers.isNotEmpty()) {
                                    val selectedNumber = numbers.maxByOrNull { it.replace(".", "").length }
                                        ?: numbers.first()
                                    removeLeadingZeros(selectedNumber)
                                } else {
                                    ""
                                }
                            }

                            val photoPath = capturedImageUri?.path
                            onResult(result, photoPath)
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

                    // Overlay с рамкой сканирования - ПРАВИЛЬНАЯ ЛОГИКА!
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

                        if (recognizedText.isNotEmpty()) {
                            val foundNumbers = extractNumbersFromText(recognizedText)
                            val filteredNumbers = filterMeterReadings(foundNumbers)

                            if (filteredNumbers.isNotEmpty()) {
                                // Убираем ведущие нули для отображения
                                val cleanNumbers = filteredNumbers.distinct()
                                Text(
                                    text = "Найдены цифры в области сканирования: ${cleanNumbers.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                // Выбираем наиболее вероятное показание
                                val selectedNumber = if (manualInput.isNotEmpty()) {
                                    manualInput
                                } else {
                                    // Выбираем число с наибольшим количеством цифр
                                    val longestNumber = cleanNumbers.maxByOrNull { it.replace(".", "").length }
                                    longestNumber ?: cleanNumbers.first()
                                }

                                Text(
                                    text = "Выбрано: $selectedNumber",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )

                                // Предлагаем выбрать другое число
                                if (cleanNumbers.size > 1) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Выберите другое значение:",
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
                                                onClick = { manualInput = number },
                                                label = { Text(number) }
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "Значимые цифры не найдены. Убедитесь, что счетчик в области сканирования",
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
                            Text(
                                text = "✓ Значение сохранено",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
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

// Улучшенная функция для распознавания текста ТОЛЬКО из области сканирования
private fun recognizeTextFromImage(
    context: Context,
    imageUri: Uri,
    cropToScanArea: Boolean = true,
    onResult: (String) -> Unit
) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    try {
        // 1. Загружаем полное изображение
        val fullBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, imageUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        }

        if (fullBitmap == null) {
            Log.e("TextRecognition", "Не удалось загрузить изображение")
            onResult("")
            return
        }

        // 2. Логирование для отладки
        Log.d("TextRecognition", "Начало распознавания, cropToScanArea: $cropToScanArea")
        Log.d("TextRecognition", "Размер изображения: ${fullBitmap.width}x${fullBitmap.height}")

        if (!cropToScanArea) {
            // Если не обрезаем - распознаем все изображение
            val image = com.google.mlkit.vision.common.InputImage.fromBitmap(fullBitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    onResult(visionText.text)
                }
                .addOnFailureListener { e ->
                    Log.e("TextRecognition", "Ошибка распознавания: ${e.message}", e)
                    onResult("")
                }
            fullBitmap.recycle()
            return
        }

        // 3. ТОЧНЫЕ параметры обрезки (должны совпадать с UI!)
        val fullWidth = fullBitmap.width
        val fullHeight = fullBitmap.height

        // ТАКИЕ ЖЕ пропорции, как на экране:
        // Область сканирования: 80% ширины, высота = 30% от этой ширины
        val scanAreaWidth = (fullWidth * 0.8).toInt()
        val scanAreaHeight = (scanAreaWidth * 0.3).toInt() // 0.3 = 30%

        // Центрируем область
        val left = (fullWidth - scanAreaWidth) / 2
        val top = (fullHeight - scanAreaHeight) / 2

        // 4. Проверяем границы
        if (left < 0 || top < 0 ||
            left + scanAreaWidth > fullWidth ||
            top + scanAreaHeight > fullHeight) {
            Log.e("TextRecognition", "Область сканирования выходит за границы изображения")
            Log.e("TextRecognition", "left=$left, top=$top, width=$scanAreaWidth, height=$scanAreaHeight")
            onResult("")
            fullBitmap.recycle()
            return
        }

        Log.d("TextRecognition", "Область сканирования: $left,$top ${scanAreaWidth}x$scanAreaHeight")

        // 5. ВАЖНО: Обрезаем ТОЛЬКО область сканирования
        val croppedBitmap = Bitmap.createBitmap(
            fullBitmap,
            left,
            top,
            scanAreaWidth,
            scanAreaHeight
        )

        // 6. Сохраняем для отладки (опционально)
        saveDebugImage(context, croppedBitmap, "scan_area_${System.currentTimeMillis()}.jpg")

        // 7. Распознаем ТОЛЬКО из обрезанной области
        val image = com.google.mlkit.vision.common.InputImage.fromBitmap(croppedBitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val resultText = visionText.text
                Log.d("TextRecognition", "Распознанный текст ИЗ ОБЛАСТИ СКАНИРОВАНИЯ: $resultText")
                onResult(resultText)
            }
            .addOnFailureListener { e ->
                Log.e("TextRecognition", "Ошибка распознавания: ${e.message}", e)
                onResult("")
            }

        // 8. Освобождаем ресурсы
        croppedBitmap.recycle()
        fullBitmap.recycle()

    } catch (e: Exception) {
        Log.e("TextRecognition", "Ошибка обработки изображения: ${e.message}", e)
        onResult("")
    }
}

// Функция для сохранения debug изображения
private fun saveDebugImage(context: Context, bitmap: Bitmap, fileName: String) {
    try {
        val debugDir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "debug")
        if (!debugDir.exists()) {
            debugDir.mkdirs()
        }

        val file = File(debugDir, fileName)
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        stream.flush()
        stream.close()
        Log.d("Debug", "Сохранено debug изображение: ${file.absolutePath}")
    } catch (e: Exception) {
        Log.e("Debug", "Ошибка сохранения debug изображения", e)
    }
}

// Улучшенная функция для извлечения чисел из текста
private fun extractNumbersFromText(text: String): List<String> {
    if (text.isBlank()) return emptyList()

    // Ищем числа в форматах: 1234, 1234.56, 1,234.56
    val patterns = listOf(
        """\b\d{4,8}\b""",                     // 1234567
        """\b\d{1,3}(?:,\d{3})*(?:\.\d+)?\b""", // 1,234.56
        """\b\d+\.\d{1,3}\b""",                 // 1234.56
        """\b\d{4,8}[\.,]\d{1,3}\b"""           // 1234,56 или 1234.56
    )

    val results = mutableListOf<String>()

    patterns.forEach { pattern ->
        val regex = pattern.toRegex()
        val matches = regex.findAll(text)
        matches.forEach { match ->
            val number = match.value.replace(',', '.')
            // Проверяем, что это действительно число
            if (number.replace(".", "").toDoubleOrNull() != null) {
                results.add(removeLeadingZeros(number))
            }
        }
    }

    return results.distinct()
}

// Функция для фильтрации показаний счетчика
private fun filterMeterReadings(numbers: List<String>): List<String> {
    return numbers.filter { number ->
        val cleanNumber = removeLeadingZeros(number)
        val digitCount = cleanNumber.replace(".", "").length

        // Типичные показания счетчика: 4-8 цифр, может быть точка/запятая
        digitCount in 4..8 && cleanNumber.replace(".", "").toDoubleOrNull() != null
    }.distinct()
}

// Функция для удаления ведущих нулей
private fun removeLeadingZeros(number: String): String {
    if (number.isBlank()) return ""

    // Разделяем на целую и дробную части
    val parts = number.split('.')
    var integerPart = parts[0]

    // Убираем ведущие нули из целой части, но оставляем один ноль если все цифры нули
    integerPart = integerPart.replaceFirst("^0+".toRegex(), "")
    if (integerPart.isEmpty()) {
        integerPart = "0"
    }

    // Возвращаем число с дробной частью если она была
    return if (parts.size > 1) {
        "$integerPart.${parts[1]}"
    } else {
        integerPart
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