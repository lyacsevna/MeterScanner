package com.vstu.metterscanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vstu.metterscanner.data.Meter
import com.vstu.metterscanner.data.MeterType

// Локальные иконки или альтернативы
val ElectricityIcon: ImageVector = Icons.Default.Bolt
val ColdWaterIcon: ImageVector = Icons.Default.Waves
val HotWaterIcon: ImageVector = Icons.Default.LocalFireDepartment

@Composable
fun MeterCard(
    meter: Meter,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Иконка типа с использованием доступных иконок
                    Icon(
                        imageVector = when(meter.type) {
                            MeterType.ELECTRICITY -> ElectricityIcon
                            MeterType.COLD_WATER -> ColdWaterIcon
                            MeterType.HOT_WATER -> HotWaterIcon
                        },
                        contentDescription = when(meter.type) {
                            MeterType.ELECTRICITY -> "Электричество"
                            MeterType.COLD_WATER -> "Холодная вода"
                            MeterType.HOT_WATER -> "Горячая вода"
                        },
                        tint = when(meter.type) {
                            MeterType.ELECTRICITY -> MaterialTheme.colorScheme.primary
                            MeterType.COLD_WATER -> Color(0xFF2196F3) // Синий для холодной воды
                            MeterType.HOT_WATER -> Color(0xFFF44336) // Красный для горячей воды
                        },
                        modifier = Modifier.size(24.dp)
                    )

                    Column {
                        Text(
                            text = when(meter.type) {
                                MeterType.ELECTRICITY -> "Электричество"
                                MeterType.COLD_WATER -> "Холодная вода"
                                MeterType.HOT_WATER -> "Горячая вода"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = meter.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "${meter.value}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (meter.note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = meter.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}