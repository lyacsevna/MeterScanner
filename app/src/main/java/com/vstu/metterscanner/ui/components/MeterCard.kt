package com.vstu.metterscanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vstu.metterscanner.data.Meter
import com.vstu.metterscanner.data.MeterType

@Composable
fun MeterCard(
    meter: Meter,
    onClick: () -> Unit,
    showUnit: Boolean = true
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (meter.type) {
                            MeterType.ELECTRICITY -> Icons.Default.FlashOn
                            MeterType.COLD_WATER -> Icons.Default.WaterDrop
                            MeterType.HOT_WATER -> Icons.Default.Whatshot
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = when (meter.type) {
                            MeterType.ELECTRICITY -> MaterialTheme.colorScheme.primary
                            MeterType.COLD_WATER -> Color(0xFF2196F3)
                            MeterType.HOT_WATER -> Color(0xFFF44336)
                        }
                    )
                    Text(
                        text = when (meter.type) {
                            MeterType.ELECTRICITY -> "Электричество"
                            MeterType.COLD_WATER -> "Холодная вода"
                            MeterType.HOT_WATER -> "Горячая вода"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = meter.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (meter.note.isNotBlank()) {
                    Text(
                        text = meter.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = String.format("%.1f", meter.value),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = when (meter.type) {
                            MeterType.ELECTRICITY -> MaterialTheme.colorScheme.primary
                            MeterType.COLD_WATER -> Color(0xFF2196F3)
                            MeterType.HOT_WATER -> Color(0xFFF44336)
                        }
                    )
                    if (showUnit) {
                        Text(
                            text = getUnitForType(meter.type),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                if (meter.photoPath != null) {
                    Icon(
                        Icons.Default.Photo,
                        contentDescription = "Есть фото",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

fun getUnitForType(type: MeterType): String = when (type) {
    MeterType.ELECTRICITY -> "кВт·ч"
    MeterType.COLD_WATER -> "м³"
    MeterType.HOT_WATER -> "м³"
}