package com.animate.companion.ui.chat

import android.content.Intent
import android.net.Uri

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.animate.companion.AppContainer
import com.animate.companion.data.CharacterEntity
import com.animate.companion.data.MessageEntity
import com.animate.companion.llm.SafetyPolicy
import com.animate.companion.model.Emotion
import com.animate.companion.model.PersonaPresets
import com.animate.companion.ui.avatar.AvatarView
import com.animate.companion.ui.avatar.IllustrationEditor
import com.animate.companion.model.Gender
import com.animate.companion.ui.avatar.IllustrationGallery
import androidx.compose.runtime.saveable.rememberSaveable
import com.animate.companion.ui.components.AvatarFrame
import com.animate.companion.ui.components.GlassCard
import com.animate.companion.ui.components.MangaStage
import com.animate.companion.ui.components.SakuraBackground
import com.animate.companion.ui.create.fieldColors
import com.animate.companion.ui.theme.Palette

@Composable
fun ChatScreen(container: AppContainer, characterId: Long, greet: Boolean, onBack: () -> Unit, onSettings: () -> Unit) {
    val vm: ChatViewModel = viewModel(
        key = "chat-$characterId",
        factory = viewModelFactory { initializer { ChatViewModel(container, characterId, greet) } },
    )
    val character by vm.character.collectAsStateWithLifecycle()
    val messages by vm.messages.collectAsStateWithLifecycle()
    val speakingId by vm.speakingId.collectAsStateWithLifecycle()
    val c = character ?: run {
        SakuraBackground { }
        return
    }
    var heroOpen by remember { mutableStateOf(true) }
    var menu by remember { mutableStateOf(false) }
    var editArt by rememberSaveable { mutableStateOf(false) }
    var artDialog by remember { mutableStateOf(false) }
    var memoryDialog by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val emotion = vm.emotion ?: Emotion.fromTag(c.lastEmotion)

    SakuraBackground(petals = 8) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            // Header
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Palette.Text) }
                AvatarFrame(Modifier.size(44.dp).clickable { heroOpen = !heroOpen }) {
                    AvatarView(c.appearance, c.genderEnum, Modifier.size(44.dp), emotion = emotion, talking = vm.talking, headOnly = true)
                }
                Column(Modifier.weight(1f).padding(start = 10.dp).clickable { heroOpen = !heroOpen }) {
                    Text(c.name, style = MaterialTheme.typography.titleMedium, color = Palette.Text, maxLines = 1)
                    Text(
                        if (vm.busy) "печатает…" else "${emotion.emoji} ${emotion.label}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (vm.busy) Palette.Amber else Palette.TextDim,
                    )
                }
                Icon(Icons.Rounded.Favorite, null, tint = Palette.Amber, modifier = Modifier.size(16.dp))
                Text(" ${c.affection}", color = Palette.Amber, style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = { heroOpen = !heroOpen }) {
                    Icon(if (heroOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, "Показать персонажа", tint = Palette.Text)
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Меню", tint = Palette.Text) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (c.appearance.illustrationId != null) DropdownMenuItem(
                            text = { Text("Открыть студию образа") }, onClick = { menu = false; editArt = true })
                        DropdownMenuItem(text = { Text("Сменить образ") }, onClick = { menu = false; artDialog = true })
                        DropdownMenuItem(text = { Text("Записи и память") }, onClick = { menu = false; memoryDialog = true })
                        DropdownMenuItem(text = { Text("Начать заново") }, onClick = { menu = false; confirmClear = true })
                        DropdownMenuItem(text = { Text("Настройки мира") }, onClick = { menu = false; onSettings() })
                        DropdownMenuItem(text = { Text("Удалить персонажа", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; confirmDelete = true })
                    }
                }
            }

            // Character scene
            AnimatedVisibility(
                heroOpen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                GlassCard(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Сцена персонажа",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Palette.Amber,
                                )
                                Text(
                                    "образ, эмоция и настроение меняются в реальном времени",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Palette.TextDim,
                                )
                            }
                            Text(
                                emotion.emoji,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }
                        Hero(c, emotion, vm.talking, vm.bubble, onPoke = vm::poke)
                    }
                }
            }

            // Messages
            val listState = rememberLazyListState()
            val total = messages.size + (if (vm.busy) 1 else 0) + (if (vm.error != null) 1 else 0) + (if (vm.concern != null) 1 else 0)
            LaunchedEffect(total) { if (total > 0) listState.animateScrollToItem(total - 1) }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(messages, key = { it.id }) { m ->
                    MessageBubble(
                        m,
                        isLastAssistant = !m.isUser && m.id == messages.lastOrNull()?.id,
                        onDelete = { vm.deleteMessage(m.id) },
                        onRegenerate = vm::regenerate,
                        speaking = speakingId == "msg-${m.id}",
                        onSpeak = { vm.toggleSpeech(m) },
                    )
                }
                vm.concern?.let { concern ->
                    item(key = "help") { HelpCard(concern, onDismiss = vm::dismissConcern) }
                }
                if (vm.busy) item(key = "typing") { TypingBubble() }
                vm.error?.let { err ->
                    item(key = "error") { ErrorCard(err, onRetry = vm::retry, onSettings = onSettings, onDismiss = vm::dismissError) }
                }
            }

            // Input
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.Bottom) {
                TextField(
                    value = vm.input,
                    onValueChange = { vm.input = it },
                    placeholder = { Text("Написать ${c.name.substringBefore(' ')}…", color = Palette.TextDim) },
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(18.dp)),
                    maxLines = 5,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0x2EFFFFFF),
                        unfocusedContainerColor = Palette.Glass,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Palette.Amber,
                        focusedTextColor = Palette.Text,
                        unfocusedTextColor = Palette.Text,
                    ),
                )
                Spacer(Modifier.size(8.dp))
                val canSend = vm.input.isNotBlank() && !vm.busy
                Box(
                    Modifier.size(54.dp).clip(RoundedCornerShape(18.dp))
                        .background(if (canSend) Palette.accent else Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x22FFFFFF))))
                        .clickable(enabled = canSend, onClick = vm::send),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, "Отправить", tint = if (canSend) Palette.Ink else Palette.TextDim)
                }
            }
        }
    }

    if (editArt) IllustrationEditor(c.appearance,
        runCatching { Gender.valueOf(c.gender) }.getOrDefault(Gender.NEUTRAL),
        onDismiss = { editArt = false }, onSave = { vm.saveIllustrationStyle(it) })
    if (artDialog) AlertDialog(
        onDismissRequest = { artDialog = false },
        title = { Text("Образ персонажа") },
        text = { IllustrationGallery(c.appearance.illustrationId, onSelect = { id ->
            vm.setIllustration(id); artDialog = false
        }, modifier = Modifier.fillMaxWidth().height(440.dp)) },
        confirmButton = { TextButton(onClick = { artDialog = false }) { Text("Закрыть") } },
    )
    if (memoryDialog) MemoryDialog(c.memory, onDismiss = { memoryDialog = false }) { vm.saveMemory(it); memoryDialog = false }
    if (confirmClear) ConfirmDialog(
        "Начать заново?",
        "История и память ${c.name} будут стёрты. Персонаж поприветствует тебя снова.",
        "Стереть",
        onDismiss = { confirmClear = false },
    ) { confirmClear = false; vm.clearHistory() }
    if (confirmDelete) ConfirmDialog(
        "Удалить ${c.name}?",
        "Персонаж и вся переписка будут удалены навсегда.",
        "Удалить",
        onDismiss = { confirmDelete = false },
    ) { confirmDelete = false; vm.deleteCharacter(onBack) }
}

@Composable
internal fun Hero(c: CharacterEntity, emotion: Emotion, talking: Boolean, bubble: String?, onPoke: () -> Unit) {
    var fullBody by rememberSaveable(c.id) { mutableStateOf(false) }
    val arch = PersonaPresets.archetype(c.archetypeId)
    val prof = PersonaPresets.profession(c.professionId)
    val dir = PersonaPresets.direction(c.directionId)
    Box(Modifier.fillMaxWidth().height(230.dp)) {
        Box(
            Modifier.fillMaxHeight().aspectRatio(1f).align(Alignment.Center)
                .clickable(remember { MutableInteractionSource() }, null, onClick = onPoke),
        ) {
            MangaStage(emotion, Modifier.fillMaxSize())
            AvatarView(c.appearance, c.genderEnum, Modifier.fillMaxSize(), emotion = emotion, talking = talking, fullBody = fullBody)
            androidx.compose.animation.AnimatedVisibility(
                visible = bubble != null,
                enter = scaleIn() + fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 24.dp, y = 16.dp),
            ) {
                Text(
                    bubble.orEmpty(),
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp, 24.dp, 24.dp, 7.dp))
                        .background(Palette.Paper)
                        .border(1.dp, Palette.Amber.copy(alpha = 0.58f), RoundedCornerShape(24.dp, 24.dp, 24.dp, 7.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    color = Palette.Ink,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (c.appearance.illustrationId != null) TextButton(
            onClick = { fullBody = !fullBody }, modifier = Modifier.align(Alignment.BottomEnd),
        ) { Text(if (fullBody) "Портрет" else "В полный рост") }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TagChip("${arch.emoji} ${arch.label}")
            TagChip("${prof.emoji} ${prof.label(c.genderEnum)}")
            TagChip("${dir.emoji} ${dir.label}")
        }
    }
}

@Composable
private fun TagChip(text: String) {
    Text(
        text,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0x6610211C)).padding(horizontal = 10.dp, vertical = 4.dp),
        color = Palette.Text,
        style = MaterialTheme.typography.labelSmall,
    )
}

private val actionRegex = Regex("""\*([^*\n]+)\*""")

/** Renders *actions* in italic accent colour, like roleplay chats do. */
fun styledText(text: String, actionColor: Color): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in actionRegex.findAll(text)) {
        append(text.substring(last, m.range.first))
        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = actionColor)) { append(m.groupValues[1]) }
        last = m.range.last + 1
    }
    append(text.substring(last))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MessageBubble(
    m: MessageEntity,
    isLastAssistant: Boolean,
    onDelete: () -> Unit,
    onRegenerate: () -> Unit,
    speaking: Boolean = false,
    onSpeak: (() -> Unit)? = null,
) {
    var menu by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val shape = if (m.isUser) RoundedCornerShape(24.dp, 24.dp, 7.dp, 24.dp) else RoundedCornerShape(24.dp, 24.dp, 24.dp, 7.dp)
    Box(Modifier.fillMaxWidth(), contentAlignment = if (m.isUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Row(verticalAlignment = Alignment.Bottom) {
        Box {
            Text(
                styledText(m.text, if (m.isUser) Palette.Ink else Palette.Teal),
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(shape)
                    .background(
                        if (m.isUser) Palette.accent
                        else Brush.linearGradient(listOf(Color(0x30243A33), Color(0x203ED0B4))),
                    )
                    .border(
                        1.dp,
                        if (m.isUser) Palette.Amber.copy(alpha = 0.45f) else Palette.Teal.copy(alpha = 0.32f),
                        shape,
                    )
                    .combinedClickable(onClick = {}, onLongClick = { menu = true })
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .animateContentSize(),
                color = if (m.isUser) Palette.Ink else Palette.Text,
                style = MaterialTheme.typography.bodyLarge,
            )
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Копировать") }, onClick = { clipboard.setText(AnnotatedString(m.text)); menu = false })
                if (isLastAssistant) DropdownMenuItem(text = { Text("Другой вариант") }, onClick = { menu = false; onRegenerate() })
                DropdownMenuItem(text = { Text("Удалить") }, onClick = { menu = false; onDelete() })
            }
        }
        if (!m.isUser && onSpeak != null) {
            IconButton(onClick = onSpeak, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (speaking) Icons.Rounded.StopCircle else Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = if (speaking) "Остановить" else "Прочитать вслух",
                    tint = if (speaking) Palette.Amber else Palette.TextDim,
                )
            }
        }
        }
    }
}

@Composable
internal fun TypingBubble() {
    val t = rememberInfiniteTransition(label = "typing")
    Row(
        Modifier.clip(RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp)).background(Color(0x22FFFFFF)).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(3) { i ->
            val y by t.animateFloat(
                0f, -6f,
                infiniteRepeatable(tween(380, delayMillis = i * 120), RepeatMode.Reverse),
                label = "dot$i",
            )
            Box(Modifier.offset(y = y.dp).size(8.dp).clip(CircleShape).background(Palette.Amber))
        }
    }
}

@Composable
internal fun ErrorCard(text: String, onRetry: () -> Unit, onSettings: () -> Unit, onDismiss: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text("Связь с персонажем прервалась", color = Palette.Text, style = MaterialTheme.typography.titleSmall)
            Text(text, color = Palette.TextDim, style = MaterialTheme.typography.bodySmall, maxLines = 6)
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("Скрыть") }
                TextButton(onClick = onSettings) { Text("Настройки") }
                TextButton(onClick = onRetry) { Text("Повторить") }
            }
        }
    }
}

/** Safety card with help lines; shown by the app itself, independent of what the model replies. */
@Composable
internal fun HelpCard(concern: SafetyPolicy.Concern, onDismiss: () -> Unit) {
    val context = LocalContext.current
    fun dial(number: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number.filter { it.isDigit() })))
        }
    }
    GlassCard(Modifier.fillMaxWidth(), selected = true) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("💗 ${concern.title}", color = Palette.Text, style = MaterialTheme.typography.titleSmall)
            Text(concern.advice, color = Palette.Text, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Детский телефон доверия: ${SafetyPolicy.CHILD_HELPLINE} — бесплатно, анонимно, круглосуточно.\n" +
                    "Если есть опасность прямо сейчас: ${SafetyPolicy.EMERGENCY}.",
                color = Palette.TextDim,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { dial(SafetyPolicy.CHILD_HELPLINE) }) { Text("📞 Телефон доверия", maxLines = 1) }
                TextButton(onClick = { dial(SafetyPolicy.EMERGENCY) }) { Text("📞 ${SafetyPolicy.EMERGENCY}", maxLines = 1) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Понятно", maxLines = 1) }
            }
        }
    }
}

@Composable
private fun MemoryDialog(memory: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(memory) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Записи и память") },
        text = {
            Column {
                Text(
                    "Здесь хранится сжатая история важных разговоров. Можно поправить записи вручную — персонаж будет опираться на них дальше.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextDim,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(2000) },
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    placeholder = { Text("Пока пусто — память появится после долгого разговора.") },
                    colors = fieldColors(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun ConfirmDialog(title: String, text: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
