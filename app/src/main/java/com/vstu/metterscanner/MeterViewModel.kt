package com.vstu.metterscanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vstu.metterscanner.data.Meter
import com.vstu.metterscanner.data.MeterRepository
import com.vstu.metterscanner.data.MeterType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MeterViewModel(
    private val repository: MeterRepository
) : ViewModel() {
    private val _allMeters = MutableStateFlow<List<Meter>>(emptyList())
    val allMeters: StateFlow<List<Meter>> = _allMeters.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadAllMeters()
    }

    private fun loadAllMeters() {
        viewModelScope.launch {
            repository.allMeters.collect { meters ->
                _allMeters.value = meters
            }
        }
    }

    suspend fun getLastMeter(type: MeterType): Meter? {
        return repository.getLastMeter(type)
    }

    suspend fun getMeterById(id: Long): Meter? {
        return repository.getMeterById(id)
    }

    fun addMeter(meter: Meter) {
        viewModelScope.launch {
            try {
                repository.addMeter(meter)
                _successMessage.value = "Показание успешно сохранено"
                clearMessageAfterDelay(3000, _successMessage)
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка: ${e.message}"
                clearMessageAfterDelay(5000, _errorMessage)
            }
        }
    }

    fun updateMeter(meter: Meter) {
        viewModelScope.launch {
            try {
                repository.updateMeter(meter)
                _successMessage.value = "Показание обновлено"
                clearMessageAfterDelay(3000, _successMessage)
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка обновления: ${e.message}"
                clearMessageAfterDelay(5000, _errorMessage)
            }
        }
    }

    fun deleteMeter(meter: Meter) {
        viewModelScope.launch {
            try {
                repository.deleteMeter(meter)
                _successMessage.value = "Показание удалено"
                clearMessageAfterDelay(3000, _successMessage)
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка удаления: ${e.message}"
                clearMessageAfterDelay(5000, _errorMessage)
            }
        }
    }

    private fun clearMessageAfterDelay(delay: Long, messageFlow: MutableStateFlow<String?>) {
        viewModelScope.launch {
            kotlinx.coroutines.delay(delay)
            messageFlow.value = null
        }
    }

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
            null
        }
    }
}