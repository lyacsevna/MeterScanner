//package com.vstu.metterscanner.ui.screens
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavController
//import com.vstu.metterscanner.data.MeterRepository
//import com.vstu.metterscanner.data.MeterType
//import com.vstu.metterscanner.ui.components.MeterCard
//
//@Composable
//fun HistoryScreen(
//    repository: MeterRepository,
//    navController: NavController
//) {
//    var selectedType by remember { mutableStateOf<MeterType?>(null) }
//    val meters by remember(selectedType) {
//        mutableStateOf(
//            selectedType?.let { repository.getMetersByType(it) }
//                ?: repository.getAllMeters()
//        )
//    }
//
//    Column(
//        modifier = Modifier.fillMaxSize()
//    ) {
//        // Фильтр по типам
//        Row(
//            horizontalArrangement = Arrangement.spacedBy(8.dp),
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp)
//        ) {
//            FilterChip(
//                selected = selectedType == null,
//                onClick = { selectedType = null },
//                label = { Text("Все") }
//            )
//
//            MeterType.values().forEach { type ->
//                FilterChip(
//                    selected = selectedType == type,
//                    onClick = { selectedType = type },
//                    label = {
//                        Text(when(type) {
//                            MeterType.ELECTRICITY -> "⚡"
//                            MeterType.COLD_WATER -> "💧"
//                            MeterType.HOT_WATER -> "🔥"
//                        })
//                    }
//                )
//            }
//        }
//
//        // Список показаний
//        if (meters.isEmpty()) {
//            Box(
//                modifier = Modifier.fillMaxSize(),
//                contentAlignment = Alignment.Center
//            ) {
//                Text("Показаний не найдено")
//            }
//        } else {
//            LazyColumn(
//                modifier = Modifier.fillMaxSize(),
//                contentPadding = PaddingValues(horizontal = 16.dp)
//            ) {
//                items(meters) { meter ->
//                    MeterCard(meter = meter)
//                }
//            }
//        }
//    }
//}