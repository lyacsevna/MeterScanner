package com.vstu.metterscanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.vstu.metterscanner.data.Meter
import com.vstu.metterscanner.data.MeterRepository
import com.vstu.metterscanner.data.MeterType

@Composable
fun AddMeterScreen(
    repository: MeterRepository,
    navController: NavController
) {
    var selectedType by remember { mutableStateOf(MeterType.ELECTRICITY) }
    var value by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Добавить показания",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Выбор типа счетчика
        Text(
            text = "Тип счетчика",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            MeterType.values().forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = {
                        Text(when(type) {
                            MeterType.ELECTRICITY -> "⚡"
                            MeterType.COLD_WATER -> "💧"
                            MeterType.HOT_WATER -> "🔥"
                        })
                    }
                )
            }
        }

        // Ввод значения
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text("Показания") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Заметка
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Заметка (необязательно)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        // Кнопка сохранения
        Button(
            onClick = {
                if (value.isNotEmpty()) {
                    val meter = Meter(
                        type = selectedType,
                        value = value.toDoubleOrNull() ?: 0.0,
                        note = note
                    )
                    repository.addMeter(meter)
                    navController.popBackStack()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = value.isNotEmpty()
        ) {
            Text("Сохранить")
        }
    }
}