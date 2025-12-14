package com.vstu.metterscanner.ui.screens

import android.content.Context
import android.graphics.*
import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ImageUtils {

    /**
     * Загружает Bitmap из файла с учетом ориентации EXIF
     */
    fun loadBitmapFromFile(context: Context, filePath: String): Bitmap? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            BitmapFactory.decodeFile(filePath, options)

            // Вычисляем коэффициент масштабирования
            options.inSampleSize = calculateInSampleSize(options, 1080, 1920)
            options.inJustDecodeBounds = false

            val bitmap = BitmapFactory.decodeFile(filePath, options)

            // Исправляем ориентацию
            correctImageOrientation(filePath, bitmap)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Загружает Bitmap из Uri с учетом ориентации EXIF
     */
    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }

                val bytes = stream.readBytes()
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                // Вычисляем коэффициент масштабирования
                options.inSampleSize = calculateInSampleSize(options, 1080, 1920)
                options.inJustDecodeBounds = false

                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                // Исправляем ориентацию (только для файлов, не для контента)
                if (uri.scheme == "file" || uri.scheme == "content") {
                    correctImageOrientation(uri, context, bitmap)
                } else {
                    bitmap
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Обрезает изображение по указанным координатам
     */
    fun cropToScanArea(
        context: Context,
        originalUri: Uri,
        cropRect: android.graphics.Rect
    ): Bitmap? {
        return try {
            val originalBitmap = loadBitmapFromUri(context, originalUri) ?: return null

            // Проверяем, что область обрезки находится в пределах изображения
            val safeCropRect = android.graphics.Rect(
                cropRect.left.coerceAtLeast(0),
                cropRect.top.coerceAtLeast(0),
                cropRect.right.coerceAtMost(originalBitmap.width),
                cropRect.bottom.coerceAtMost(originalBitmap.height)
            )

            if (safeCropRect.width() <= 0 || safeCropRect.height() <= 0) {
                return originalBitmap
            }

            Bitmap.createBitmap(
                originalBitmap,
                safeCropRect.left,
                safeCropRect.top,
                safeCropRect.width(),
                safeCropRect.height()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            originalUri.let { loadBitmapFromUri(context, it) }
        }
    }

    /**
     * Исправляет ориентацию изображения на основе EXIF данных
     */
    private fun correctImageOrientation(filePath: String, bitmap: Bitmap?): Bitmap? {
        if (bitmap == null) return null

        return try {
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            rotateBitmap(bitmap, orientation)
        } catch (e: IOException) {
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Исправляет ориентацию изображения из Uri
     */
    private fun correctImageOrientation(uri: Uri, context: Context, bitmap: Bitmap?): Bitmap? {
        if (bitmap == null) return null

        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val exif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    ExifInterface(stream)
                } else {
                    @Suppress("DEPRECATION")
                    ExifInterface(uri.path ?: return bitmap)
                }

                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

                rotateBitmap(bitmap, orientation)
            } ?: bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Поворачивает Bitmap в соответствии с ориентацией EXIF
     */
    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }

        return try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Вычисляет коэффициент масштабирования для уменьшения размера изображения
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight &&
                halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    fun saveBitmapToFile(bitmap: Bitmap, file: File): Boolean {
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}