package com.vstu.metterscanner.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream

object TextRecognitionHelper {

    suspend fun recognizeTextFromUri(
        context: Context,
        imageUri: Uri,
        cropToScanArea: Boolean = true
    ): String {
        return try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            // Загружаем изображение
            val fullBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, imageUri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            }

            if (fullBitmap == null) {
                Log.e("TextRecognition", "Не удалось загрузить изображение")
                return ""
            }

            Log.d("TextRecognition", "Размер изображения: ${fullBitmap.width}x${fullBitmap.height}")

            val bitmapToProcess = if (cropToScanArea) {
                // Обрезаем до области сканирования
                cropToScanArea(fullBitmap)
            } else {
                fullBitmap
            }

            if (bitmapToProcess == null) {
                Log.e("TextRecognition", "Не удалось обрезать изображение")
                fullBitmap.recycle()
                return ""
            }

            // Сохраняем обрезанное изображение для отладки
            saveDebugImage(context, bitmapToProcess, "cropped_${System.currentTimeMillis()}.jpg")

            // Создаем InputImage
            val image = InputImage.fromBitmap(bitmapToProcess, 0)

            // Выполняем распознавание с await()
            val result = recognizer.process(image).await()

            val text = result.text
            Log.d("TextRecognition", "Распознанный текст: $text")

            // Освобождаем ресурсы
            try {
                bitmapToProcess.recycle()
                fullBitmap.recycle()
            } catch (e: Exception) {
                Log.e("TextRecognition", "Ошибка при переработке битмапов", e)
            }

            text
        } catch (e: Exception) {
            Log.e("TextRecognition", "Ошибка распознавания: ${e.message}", e)
            ""
        }
    }

    private fun cropToScanArea(fullBitmap: Bitmap): Bitmap? {
        return try {
            val fullWidth = fullBitmap.width
            val fullHeight = fullBitmap.height

            // Параметры области сканирования (80% ширины, 30% от этой ширины высота)
            // ДОЛЖНО СОВПАДАТЬ С UI В CameraScanScreen!
            val scanAreaWidth = (fullWidth * 0.8).toInt()
            val scanAreaHeight = (scanAreaWidth * 0.3).toInt()

            // Центрируем область
            val left = (fullWidth - scanAreaWidth) / 2
            val top = (fullHeight - scanAreaHeight) / 2

            // Проверяем границы
            if (left < 0 || top < 0 ||
                left + scanAreaWidth > fullWidth ||
                top + scanAreaHeight > fullHeight) {
                Log.w("TextRecognition", "Область сканирования выходит за границы изображения")
                return fullBitmap // Возвращаем оригинал
            }

            Log.d("TextRecognition", "Область обрезки: left=$left, top=$top, width=$scanAreaWidth, height=$scanAreaHeight")

            Bitmap.createBitmap(
                fullBitmap,
                left,
                top,
                scanAreaWidth,
                scanAreaHeight
            )
        } catch (e: Exception) {
            Log.e("TextRecognition", "Ошибка при обрезке изображения", e)
            null
        }
    }

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

    // Упрощенная функция для извлечения чисел
    fun extractNumbersFromText(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        val results = mutableListOf<String>()

        // Простая регулярка для поиска чисел
        val pattern = "\\d+[\\.,]?\\d*".toRegex()
        val matches = pattern.findAll(text)

        matches.forEach { match ->
            val number = match.value.replace(',', '.')
            // Проверяем, что это действительно число
            if (number.replace(".", "").toDoubleOrNull() != null) {
                results.add(removeLeadingZeros(number))
            }
        }

        Log.d("extractNumbersFromText", "Из текста '$text' извлечены числа: $results")
        return results.distinct()
    }

    fun filterMeterReadings(numbers: List<String>): List<String> {
        return numbers.filter { number ->
            val cleanNumber = removeLeadingZeros(number)
            val digitCount = cleanNumber.replace(".", "").length
            // Принимаем числа от 3 до 8 цифр (типичные показания счетчиков)
            digitCount in 3..8 && cleanNumber.replace(".", "").toDoubleOrNull() != null
        }.distinct().sortedBy { it.length }
    }

    private fun removeLeadingZeros(number: String): String {
        if (number.isBlank()) return ""

        val parts = number.split('.')
        var integerPart = parts[0]

        // Убираем ведущие нули, но оставляем хотя бы одну цифру
        integerPart = integerPart.replaceFirst("^0+".toRegex(), "")
        if (integerPart.isEmpty()) {
            integerPart = "0"
        }

        return if (parts.size > 1) {
            "$integerPart.${parts[1]}"
        } else {
            integerPart
        }
    }
}