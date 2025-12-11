package com.vstu.metterscanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vstu.metterscanner.data.Meter
import com.vstu.metterscanner.data.MeterRepository
import com.vstu.metterscanner.data.MeterType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MeterViewModel(
    private val repository: MeterRepository
) : ViewModel() {
    private val _allMeters = MutableStateFlow<List<Meter>>(emptyList())
    val allMeters: StateFlow<List<Meter>> = _allMeters.asStateFlow()

    // Для отслеживания ошибок
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Для отслеживания успеха
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

    fun addMeter(meter: Meter) {
        viewModelScope.launch {
            try {
                repository.addMeter(meter)
                _successMessage.value = "Показание успешно сохранено"
                // Очищаем сообщение через 3 секунды
                viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    _successMessage.value = null
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка: ${e.message}"
                // Очищаем сообщение через 5 секунд
                viewModelScope.launch {
                    kotlinx.coroutines.delay(5000)
                    _errorMessage.value = null
                }
                throw e
            }
        }
    }
}