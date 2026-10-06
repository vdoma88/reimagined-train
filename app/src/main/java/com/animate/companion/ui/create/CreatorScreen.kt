package com.animate.companion.ui.create

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.Job
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animate.companion.AppContainer
import com.animate.companion.audio.SfxType
import com.animate.companion.data.CharacterEntity
import com.animate.companion.model.Appearance
import com.animate.companion.model.AppearancePresets
import com.animate.companion.model.Emotion
import com.animate.companion.model.Gender
import com.animate.companion.model.NameGenerator
import com.animate.companion.model.PersonaPresets
import com.animate.companion.model.Swatch
import com.animate.companion.model.StudioLooks
import com.animate.companion.model.IllustratedCharacters
import com.animate.companion.ui.avatar.IllustrationEditor
import com.animate.companion.ui.avatar.IllustrationGallery
import androidx.compose.material3.TextButton
import androidx.compose.runtime.saveable.rememberSaveable
import com.animate.companion.ui.components.MangaStage
import com.animate.companion.ui.avatar.AvatarView
import com.animate.companion.ui.components.GlassCard
import com.animate.companion.ui.components.GradientButton
import com.animate.companion.ui.components.SakuraBackground
import com.animate.companion.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class CreatorViewModel : ViewModel() {
    var gender by mutableStateOf(Gender.FEMALE)
    var appearance by mutableStateOf(Appearance.random(Gender.FEMALE).copy(illustrationId = com.animate.companion.model.CartoonLook.STYLE_ID))
    var name by mutableStateOf(NameGenerator.generate(Gender.FEMALE))
    var note by mutableStateOf("")
    var archetype by mutableStateOf(PersonaPresets.archetypes.first().id)
    var profession by mutableStateOf(PersonaPresets.professions.random().id)
    var direction by mutableStateOf(PersonaPresets.directions.first().id)
    var step by mutableIntStateOf(0)
    var category by mutableIntStateOf(Cat.ILLUSTRATIONS.ordinal)
    val voiceSeed = Random.nextInt()

    fun draft() = CharacterEntity(
        name = name.trim().ifBlank { NameGenerator.generate(gender) },
        gender = gender.name,
        appearanceJson = appearance.toJson(),
        archetypeId = archetype,
        professionId = profession,
        directionId = direction,
        extraNote = note.trim(),
        voiceSeed = voiceSeed,
    )
}

private enum class Cat(val label: String, val headOnly: Boolean = true) {
    HAIR("✦ Причёска", false), BANGS("Чёлка"), HAIR_COLOR("Цвет волос"), EYES("♡ Глаза"), EYE_COLOR("Цвет глаз"),
    MOUTH("Рот"), FACE("Лицо"), SKIN("Кожа"), EARS("ᓚᘏᗢ Ушки"), ACCESSORY("🎀 Аксессуар"),
    OUTFIT("✧ Одежда", false), OUTFIT_COLOR("Цвет одежды", false), EXTRAS("✨ Детали"), LOOKS("♡ Готовые образы", false), ILLUSTRATIONS("Иллюстрации", false),
}

private val steps = listOf("Образ", "История", "Характер")

@Composable
fun CreatorScreen(container: AppContainer, onBack: () -> Unit, onCreated: (Long) -> Unit) {
    val vm: CreatorViewModel = viewModel()
    val scope = rememberCoroutineScope()
    var editArt by rememberSaveable { mutableStateOf(false) }
    var fullBody by rememberSaveable { mutableStateOf(false) }
    var bubble by remember { mutableStateOf<String?>(null) }
    var previewEmotion by remember { mutableStateOf(Emotion.HAPPY) }
    var talking by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var voiceJob by remember { mutableStateOf<Job?>(null) }

    fun voicePreview(emotion: Emotion = Emotion.HAPPY) {
        voiceJob?.cancel()
        talking = false
        bubble = null
        previewEmotion = emotion
        voiceJob = scope.launch {
            val text = container.sound.voice(vm.draft(), emotion, force = true)
            if (text != null) {
                bubble = text
                talking = true
                delay(600)
                talking = false
                delay(900)
                bubble = null
            }
        }
    }

    SakuraBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            // Top bar
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (vm.step > 0) vm.step-- else onBack() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Palette.Text)
                }
                Column(Modifier.weight(1f)) {
                    Text("Character Studio", style = MaterialTheme.typography.titleLarge, color = Palette.Text)
                    Text("создай своего аниме-героя", style = MaterialTheme.typography.labelSmall, color = Palette.TextDim)
                }
                IconButton(onClick = {
                    container.sound.sfx(SfxType.DICE)
                    when (vm.step) {
                        0 -> {
                            if (vm.appearance.illustrationId == com.animate.companion.model.CartoonLook.STYLE_ID) {
                                vm.appearance = vm.appearance.copy(cartoonLook = com.animate.companion.model.CartoonLook.random())
                            } else if (vm.appearance.illustrationId != null) {
                                val art = IllustratedCharacters.all.random()
                                vm.appearance = vm.appearance.selectIllustration(art.id)
                                if (vm.gender != art.gender) vm.name = NameGenerator.generate(art.gender)
                                vm.gender = art.gender
                            } else vm.appearance = Appearance.random(vm.gender)
                        }
                        1 -> vm.name = NameGenerator.generate(vm.gender)
                        else -> {
                            vm.archetype = PersonaPresets.archetypes.random().id
                            vm.profession = PersonaPresets.professions.random().id
                            vm.direction = PersonaPresets.directions.random().id
                        }
                    }
                }) { Icon(Icons.Rounded.Casino, "Случайно", tint = Palette.Amber) }
            }

            // Live preview
            Box(Modifier.fillMaxWidth().weight(0.42f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.fillMaxHeight().aspectRatio(1f)
                        .clip(RoundedCornerShape(36.dp))
                        .background(Color.White.copy(alpha = 0.035f))
                        .border(1.dp, Palette.Amber.copy(alpha = 0.22f), RoundedCornerShape(36.dp))
                        .clickable(remember { MutableInteractionSource() }, null) { voicePreview(listOf(Emotion.HAPPY, Emotion.SHY, Emotion.SURPRISED, Emotion.LOVE, Emotion.SMUG).random()) },
                ) {
                    MangaStage(previewEmotion, Modifier.fillMaxSize())
                    AvatarView(vm.appearance, vm.gender, Modifier.fillMaxSize(), emotion = previewEmotion, talking = talking, fullBody = fullBody)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = bubble != null,
                        enter = scaleIn() + fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 4.dp),
                    ) {
                        Text(
                            bubble.orEmpty(),
                            modifier = Modifier.clip(RoundedCornerShape(22.dp, 22.dp, 22.dp, 8.dp))
                                .background(Color(0xFFFFF6FB))
                                .border(1.dp, Palette.Amber.copy(alpha = 0.55f), RoundedCornerShape(22.dp, 22.dp, 22.dp, 8.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            color = Palette.Ink,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    if (vm.appearance.illustrationId != null) "Нажми, чтобы услышать персонажа" else "♡ нажми на героя — он оживёт",
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.TextDim,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            if (vm.appearance.illustrationId != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = { fullBody = false }) { Text(if (!fullBody) "✓ Портрет" else "Портрет") }
                    TextButton(onClick = { fullBody = true }) { Text(if (fullBody) "✓ В полный рост" else "В полный рост") }
                }
                TextButton(onClick = { editArt = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Редактировать образ") }
            } else EmotionStrip(previewEmotion) { voicePreview(it) }
            StepTabs(vm.step) { container.sound.sfx(SfxType.TAP); vm.step = it }

            Box(Modifier.weight(0.58f).fillMaxWidth()) {
                AnimatedContent(vm.step, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "step") { s ->
                    when (s) {
                        0 -> AppearanceStep(vm) { container.sound.sfx(SfxType.TAP) }
                        1 -> IdentityStep(vm, onDice = { container.sound.sfx(SfxType.DICE) }, onTap = { container.sound.sfx(SfxType.TAP) })
                        else -> PersonaStep(vm) { container.sound.sfx(SfxType.TAP) }
                    }
                }
            }

            GradientButton(
                text = if (vm.step < 2) "Дальше ✦" else "Оживить героя ♡",
                icon = if (vm.step < 2) Icons.AutoMirrored.Rounded.ArrowForward else Icons.Rounded.Favorite,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                onClick = {
                    if (vm.step < 2) {
                        container.sound.sfx(SfxType.TAP)
                        vm.step++
                    } else {
                        saving = true
                        container.sound.sfx(SfxType.SPARKLE)
                        scope.launch {
                            val id = container.chat.createCharacter(vm.draft())
                            onCreated(id)
                        }
                    }
                },
            )
        }
    }
    if (editArt) IllustrationEditor(vm.appearance, vm.gender,
        onDismiss = { editArt = false }, onSave = { vm.appearance = it; true })

}

@Composable
internal fun StepTabs(step: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(50)).background(Palette.Glass).padding(4.dp),
    ) {
        steps.forEachIndexed { i, label ->
            val sel = i == step
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(50))
                    .background(if (sel) Palette.accent else androidx.compose.ui.graphics.SolidColor(Color.Transparent))
                    .clickable { onSelect(i) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${i + 1}. $label",
                    color = if (sel) Palette.Ink else Palette.TextDim,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

// ------------------------------------------------------------------ step 1: appearance

@Composable
internal fun AppearanceStep(vm: CreatorViewModel, onTap: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        com.animate.companion.ui.avatar.CartoonLookControls(vm.appearance, enabled = true) {
            onTap()
            vm.appearance = it
        }
    }
}

@Composable
internal fun EmotionStrip(selected: Emotion, onSelect: (Emotion) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(Emotion.entries) { _, emotion ->
            Text(
                "${emotion.emoji} ${emotion.label}",
                color = if (selected == emotion) Palette.Ink else Palette.Text,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .background(if (selected == emotion) Palette.Amber else Palette.Glass)
                    .selectable(selected == emotion, role = Role.RadioButton) { onSelect(emotion) }
                    .padding(horizontal = 12.dp, vertical = 14.dp),
            )
        }
    }
}

@Composable
private fun StudioLookGrid(vm: CreatorViewModel, onTap: () -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(140.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text("Выбери настроение образа, затем меняй любые детали. Тон кожи и форма лица сохранятся.",
                color = Palette.TextDim, style = MaterialTheme.typography.bodySmall)
        }
        itemsIndexed(StudioLooks.all) { _, look ->
            val appearance = look.applyTo(vm.appearance, vm.gender)
            GlassCard(selected = vm.appearance == appearance, onClick = { onTap(); vm.appearance = appearance }) {
                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                    Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                        MangaStage(Emotion.HAPPY, Modifier.fillMaxSize())
                        AvatarView(appearance, vm.gender, Modifier.fillMaxSize(), animated = false, emotion = Emotion.HAPPY)
                    }
                    Text(look.label, color = Palette.Text, style = MaterialTheme.typography.titleSmall)
                    Text(look.description, color = Palette.TextDim, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun OptionGrid(
    labels: List<String>,
    current: Int,
    gender: Gender,
    headOnly: Boolean,
    preview: (Int) -> Appearance,
    onPick: (Appearance) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(96.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(labels) { i, label ->
            GlassCard(selected = i == current, onClick = { onPick(preview(i)) }, shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AvatarView(preview(i), gender, Modifier.fillMaxWidth().aspectRatio(1f), animated = false, headOnly = headOnly)
                    Text(label, style = MaterialTheme.typography.labelMedium, color = Palette.Text, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwatchGrid(swatches: List<Swatch>, current: Int, onPick: (Int) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        swatches.forEachIndexed { i, s ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp).clickable { onPick(i) }) {
                Box(
                    Modifier.size(52.dp).clip(CircleShape)
                        .border(if (i == current) 3.dp else 1.dp, if (i == current) Palette.Amber else Palette.GlassBorder, CircleShape)
                        .padding(5.dp).clip(CircleShape)
                        .background(Color(s.argb)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (i == current) Icon(Icons.Rounded.Check, null, tint = if (s.argb.lum() > 0.6f) Palette.Ink else Color.White)
                }
                Text(s.label, style = MaterialTheme.typography.labelSmall, color = Palette.TextDim, maxLines = 1)
            }
        }
    }
}

private fun Long.lum(): Float {
    val r = ((this shr 16) and 0xFF) / 255f
    val g = ((this shr 8) and 0xFF) / 255f
    val b = (this and 0xFF) / 255f
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExtrasGrid(a: Appearance, onChange: (Appearance) -> Unit) {
    val items = listOf(
        Triple("Ахогэ", a.ahoge) { v: Boolean -> a.copy(ahoge = v) },
        Triple("Румянец", a.blush) { v: Boolean -> a.copy(blush = v) },
        Triple("Клычок", a.fang) { v: Boolean -> a.copy(fang = v) },
        Triple("Родинка", a.beautyMark) { v: Boolean -> a.copy(beautyMark = v) },
        Triple("Гетерохромия", a.heterochromia) { v: Boolean -> a.copy(heterochromia = v) },
        Triple("Пластырь", a.bandaid) { v: Boolean -> a.copy(bandaid = v) },
    )
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items.forEach { (label, on, set) ->
            GlassCard(selected = on, onClick = { onChange(set(!on)) }, shape = RoundedCornerShape(50)) {
                Text(
                    (if (on) "✓ " else "") + label,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = Palette.Text,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

// ------------------------------------------------------------------ step 2: identity

@Composable
internal fun IdentityStep(vm: CreatorViewModel, onDice: () -> Unit, onTap: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        SectionTitle("Пол")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Gender.entries.forEach { g ->
                GlassCard(Modifier.weight(1f), selected = vm.gender == g, onClick = {
                    onTap()
                    if (vm.gender != g) {
                        vm.gender = g
                        vm.name = NameGenerator.generate(g)
                        val style = AppearancePresets.hairStyles[vm.appearance.hairStyle]
                        if (g !in style.genders) {
                            vm.appearance = vm.appearance.copy(hairStyle = AppearancePresets.hairStyles.indexOfFirst { g in it.genders })
                        }
                    }
                }) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(g.emoji, style = MaterialTheme.typography.headlineMedium, color = Palette.Amber)
                        Text(g.label, color = Palette.Text, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        SectionTitle("Имя")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = vm.name,
                onValueChange = { vm.name = it.take(40) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = fieldColors(),
                textStyle = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(Palette.accent)
                    .clickable { onDice(); vm.name = NameGenerator.generate(vm.gender) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Casino, "Случайное имя", tint = Palette.Ink) }
        }
        Text(
            "Будут звать «${vm.name.substringBefore(' ')}${NameGenerator.suffix(vm.gender)}» ✦",
            color = Palette.TextDim,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp),
        )
        SectionTitle("Пара слов о персонаже (необязательно)")
        OutlinedTextField(
            value = vm.note,
            onValueChange = { vm.note = it.take(400) },
            placeholder = { Text("Боится грозы, коллекционирует странные карты, мечтает увидеть море…", color = Palette.TextDim) },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(18.dp),
            colors = fieldColors(),
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Palette.Amber,
    unfocusedBorderColor = Palette.GlassBorder,
    focusedContainerColor = Palette.Glass,
    unfocusedContainerColor = Palette.Glass,
    cursorColor = Palette.Amber,
    focusedTextColor = Palette.Text,
    unfocusedTextColor = Palette.Text,
)

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = Palette.Text, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
}

// ------------------------------------------------------------------ step 3: persona

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PersonaStep(vm: CreatorViewModel, onTap: () -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(2) }) { SectionTitle("Характер") }
        itemsIndexed(PersonaPresets.archetypes) { _, a ->
            ChoiceCard(a.emoji, a.label, a.short, vm.archetype == a.id) { onTap(); vm.archetype = a.id }
        }
        item(span = { GridItemSpan(2) }) { SectionTitle("Роль в истории") }
        item(span = { GridItemSpan(2) }) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PersonaPresets.professions.forEach { p ->
                    GlassCard(selected = vm.profession == p.id, onClick = { onTap(); vm.profession = p.id }, shape = RoundedCornerShape(50)) {
                        Text(
                            "${p.emoji} ${p.label(vm.gender)}",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            color = Palette.Text,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
        item(span = { GridItemSpan(2) }) { SectionTitle("Тон общения") }
        itemsIndexed(PersonaPresets.directions) { _, d ->
            ChoiceCard(d.emoji, d.label, d.short, vm.direction == d.id) { onTap(); vm.direction = d.id }
        }
        item(span = { GridItemSpan(2) }) { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ChoiceCard(emoji: String, title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), selected = selected, onClick = onClick, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Text(title, style = MaterialTheme.typography.titleSmall, color = Palette.Text, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Palette.TextDim, maxLines = 2)
        }
    }
}

