package com.vstu.metterscanner.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    navController: NavController
) {
    var expandedItem by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Помощь") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Help,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Центр помощи",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Ответы на частые вопросы",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // FAQ
            item {
                Text(
                    text = "Частые вопросы",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            val faqItems = listOf(
                FAQItem(
                    "Как добавить показания?",
                    "Для добавления показаний:\n1. На главном экране нажмите + \n2. Выберите тип счетчика\n3. Введите значение или отсканируйте\n4. При необходимости добавьте заметку и фото\n5. Нажмите 'Сохранить'"
                ),
                FAQItem(
                    "Как работает сканирование?",
                    "1. Нажмите кнопку камеры в форме добавления\n2. Наведите камеру на счетчик так, чтобы цифры были в зеленой рамке\n3. Сделайте фото\n4. Приложение автоматически распознает цифры\n5. Подтвердите или отредактируйте значение"
                ),
                FAQItem(
                    "Как отфильтровать показания?",
                    "Вы можете фильтровать показания по типу:\n1. Откройте боковое меню\n2. Выберите нужный тип счетчика в разделе 'Фильтр по типу'\n3. Для сброса фильтра нажмите 'Сбросить фильтр'"
                ),
                FAQItem(
                    "Как редактировать или удалить показания?",
                    "1. На главном экране нажмите на нужное показание\n2. В открывшемся диалоге выберите 'Редактировать' или 'Удалить'\n3. Подтвердите действие"
                ),
                FAQItem(
                    "Как посмотреть статистику?",
                    "Статистика доступна в боковом меню:\n1. Откройте боковое меню\n2. Выберите 'Статистика'\n3. Выберите период для анализа\n4. Просмотрите графики и сводки"
                )
            )

            faqItems.forEach { faq ->
                item {
                    FAQCard(
                        question = faq.question,
                        answer = faq.answer,
                        isExpanded = expandedItem == faq.question,
                        onToggle = {
                            expandedItem = if (expandedItem == faq.question) null else faq.question
                        }
                    )
                }
            }

            // Контакты поддержки
            item {
                Text(
                    text = "Контакты поддержки",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ContactItem(
                            icon = Icons.Default.Email,
                            title = "Электронная почта",
                            subtitle = "support@meterscanner.ru",
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:support@meterscanner.ru")
                                    putExtra(Intent.EXTRA_SUBJECT, "Обращение в поддержку MeterScanner")
                                    putExtra(Intent.EXTRA_TEXT, "Здравствуйте!\n\n")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {

                                    Toast.makeText(context, "Не удалось открыть почтовое приложение", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        ContactItem(
                            icon = Icons.Default.Phone,
                            title = "Телефон",
                            subtitle = "+7 (900) 200-30-30",
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:+79002003030")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {

                                    Toast.makeText(context, "Не удалось открыть телефон", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            // Инструкции
            item {
                Text(
                    text = "Инструкции (в разработке)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InstructionItem(
                            title = "Руководство пользователя",
                            description = "Полное руководство по использованию приложения",
                            onClick = { /* TODO */ }
                        )

                        InstructionItem(
                            title = "Видео инструкции",
                            description = "Видео-гайд по основным функциям",
                            onClick = { /* TODO */ }
                        )

                        InstructionItem(
                            title = "Советы по сканированию",
                            description = "Как получить лучшие результаты",
                            onClick = { /* TODO */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FAQCard(
    question: String,
    answer: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onToggle
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Скрыть" else "Показать",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = answer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ContactItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun InstructionItem(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Book,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            Icons.Default.Download,
            contentDescription = "Скачать",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

data class FAQItem(
    val question: String,
    val answer: String
)