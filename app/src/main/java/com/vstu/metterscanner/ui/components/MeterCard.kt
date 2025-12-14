package com.vstu.metterscanner.ui.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vstu.metterscanner.data.Meter
import com.vstu.metterscanner.data.MeterType
import com.vstu.metterscanner.ui.screens.ImageUtils

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
                            MeterType.ELECTRICITY -> Color(0xFFFFFF00)
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
                        overflow = TextOverflow.Ellipsis
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
                            MeterType.COLD_WATER -> MaterialTheme.colorScheme.primary
                            MeterType.HOT_WATER -> MaterialTheme.colorScheme.primary
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

@Composable
fun MeterCardWithPhoto(
    meter: Meter,
    onClick: () -> Unit,
    showUnit: Boolean = true
) {
    val context = LocalContext.current

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Верхняя часть с основной информацией
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Тип счетчика с иконкой
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = when (meter.type) {
                            MeterType.ELECTRICITY -> Icons.Default.FlashOn
                            MeterType.COLD_WATER -> Icons.Default.WaterDrop
                            MeterType.HOT_WATER -> Icons.Default.Whatshot
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = when (meter.type) {
                            MeterType.ELECTRICITY -> Color(0xFFFFD700)
                            MeterType.COLD_WATER -> Color(0xFF2196F3)
                            MeterType.HOT_WATER -> Color(0xFFF44336)
                        }
                    )

                    Column {
                        Text(
                            text = when (meter.type) {
                                MeterType.ELECTRICITY -> "Электричество"
                                MeterType.COLD_WATER -> "Холодная вода"
                                MeterType.HOT_WATER -> "Горячая вода"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = meter.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Значение
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = String.format("%.1f", meter.value),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (showUnit) {
                        Text(
                            text = getUnitForType(meter.type),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Заметка (если есть)
            if (meter.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = meter.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Фото (если есть)
            if (meter.photoPath != null) {
                Spacer(modifier = Modifier.height(12.dp))
                PhotoPreviewInCard(
                    photoPath = meter.photoPath,
                    context = context
                )
            }
        }
    }
}

@Composable
fun PhotoPreviewInCard(
    photoPath: String,
    context: Context
) {
    val bitmap by remember(photoPath) {
        mutableStateOf(ImageUtils.loadBitmapFromFile(context, photoPath))
    }

    if (bitmap != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Photo,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Фото счетчика",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Фото счетчика",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
        }
    } else {
        // Показываем плейсхолдер если фото не загрузилось
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.BrokenImage,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Фото не загружено",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun getUnitForType(type: MeterType): String = when (type) {
    MeterType.ELECTRICITY -> "кВт·ч"
    MeterType.COLD_WATER -> "м³"
    MeterType.HOT_WATER -> "м³"
}