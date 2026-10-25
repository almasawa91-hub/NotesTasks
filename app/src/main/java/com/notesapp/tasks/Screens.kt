@file:OptIn(ExperimentalMaterial3Api::class)

package com.notesapp.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Gold = Color(0xFFFFB300)

fun fmt(t: Long): String = SimpleDateFormat("EEE d MMM، h:mm a", Locale("ar")).format(Date(t))

fun pickDateTime(ctx: Context, current: Long?, onPick: (Long) -> Unit) {
    val c = Calendar.getInstance().apply {
        timeInMillis = current ?: (System.currentTimeMillis() + 3_600_000L)
    }
    DatePickerDialog(ctx, { _, y, m, d ->
        TimePickerDialog(ctx, { _, h, min ->
            val r = Calendar.getInstance().apply {
                set(y, m, d, h, min, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onPick(r.timeInMillis)
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
}

@Composable
fun ListScreen(vm: MainVM, onOpen: (Item) -> Unit, onNew: (Boolean) -> Unit) {
    val items by vm.items.collectAsStateWithLifecycle()
    val q by vm.query.collectAsStateWithLifecycle()
    val f by vm.filter.collectAsStateWithLifecycle()
    var fabOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("ملاحظاتي ومهامي") })
                OutlinedTextField(
                    value = q,
                    onValueChange = { vm.query.value = it },
                    placeholder = { Text("بحث...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                )
                TabRow(selectedTabIndex = f) {
                    listOf("الكل", "ملاحظات", "مهام").forEachIndexed { i, t ->
                        Tab(selected = f == i, onClick = { vm.filter.value = i }, text = { Text(t) })
                    }
                }
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (fabOpen) {
                    ExtendedFloatingActionButton(onClick = { fabOpen = false; onNew(true) }) { Text("مهمة جديدة") }
                    ExtendedFloatingActionButton(onClick = { fabOpen = false; onNew(false) }) { Text("ملاحظة جديدة") }
                }
                FloatingActionButton(onClick = { fabOpen = !fabOpen }) {
                    Icon(if (fabOpen) Icons.Default.Close else Icons.Default.Add, null)
                }
            }
        }
    ) { pad ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("لا توجد عناصر بعد", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    ItemCard(item, onClick = { onOpen(item) }, onToggle = { vm.toggleDone(item) })
                }
            }
        }
    }
}

@Composable
fun ItemCard(item: Item, onClick: () -> Unit, onToggle: () -> Unit) {
    val dark = isSystemInDarkTheme()
    val bg = cardColor(item.color, dark) ?: MaterialTheme.colorScheme.surfaceVariant
    val fg = when {
        item.color == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        dark -> Color.White
        else -> Color(0xFF212121)
    }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = bg, contentColor = fg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (item.isTask) {
                Checkbox(checked = item.done, onCheckedChange = { onToggle() })
            }
            Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                val deco = if (item.done) TextDecoration.LineThrough else null
                if (item.title.isNotBlank()) {
                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 17.sp, textDecoration = deco)
                }
                if (item.content.isNotBlank()) {
                    Text(item.content, maxLines = 3, overflow = TextOverflow.Ellipsis, textDecoration = deco)
                }
                item.reminderAt?.let { t ->
                    val over = t < System.currentTimeMillis() && !item.done
                    val c = if (over) MaterialTheme.colorScheme.error else fg
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, null, Modifier.size(14.dp), tint = c)
                        Spacer(Modifier.width(4.dp))
                        Text(fmt(t), fontSize = 12.sp, color = c)
                    }
                }
            }
            if (item.pinned) Icon(Icons.Default.Star, null, tint = Gold)
        }
    }
}

@Composable
fun Editor(initial: Item, onSave: (Item) -> Unit, onDelete: (Item) -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val dark = isSystemInDarkTheme()
    var title by remember { mutableStateOf(initial.title) }
    var content by remember { mutableStateOf(initial.content) }
    var isTask by remember { mutableStateOf(initial.isTask) }
    var pinned by remember { mutableStateOf(initial.pinned) }
    var color by remember { mutableStateOf(initial.color) }
    var reminder by remember { mutableStateOf(initial.reminderAt) }
    var repeat by remember { mutableStateOf(initial.repeat) }

    fun finish() {
        onSave(
            initial.copy(
                title = title.trim(), content = content.trim(), isTask = isTask, pinned = pinned,
                color = color, reminderAt = reminder, repeat = if (reminder == null) 0 else repeat
            )
        )
        onClose()
    }
    BackHandler { finish() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initial.id == 0L) "جديد" else "تعديل") },
                navigationIcon = {
                    IconButton(onClick = { finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { pinned = !pinned }) {
                        Icon(
                            Icons.Default.Star, "تثبيت",
                            tint = if (pinned) Gold else LocalContentColor.current.copy(alpha = 0.4f)
                        )
                    }
                    if (initial.id != 0L) {
                        IconButton(onClick = { onDelete(initial); onClose() }) {
                            Icon(Icons.Default.Delete, "حذف")
                        }
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !isTask, onClick = { isTask = false }, label = { Text("ملاحظة") })
                FilterChip(selected = isTask, onClick = { isTask = true }, label = { Text("مهمة") })
            }
            OutlinedTextField(
                value = title, onValueChange = { title = it }, label = { Text("العنوان") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = content, onValueChange = { content = it }, label = { Text("التفاصيل") },
                minLines = 6, modifier = Modifier.fillMaxWidth()
            )
            Text("اللون", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (i in 0 until COLOR_COUNT) {
                    val sel = color == i
                    Box(
                        Modifier.size(36.dp).clip(CircleShape)
                            .background(cardColor(i, dark) ?: MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                if (sel) 3.dp else 1.dp,
                                if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                CircleShape
                            )
                            .clickable { color = i }
                    )
                }
            }
            Text("التذكير", fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { pickDateTime(ctx, reminder) { reminder = it } }) {
                    Icon(Icons.Default.Notifications, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(reminder?.let { fmt(it) } ?: "إضافة تذكير")
                }
                if (reminder != null) {
                    TextButton(onClick = { reminder = null }) { Text("إزالة") }
                }
            }
            if (reminder != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("مرة واحدة", "يومياً", "أسبوعياً").forEachIndexed { i, t ->
                        FilterChip(selected = repeat == i, onClick = { repeat = i }, label = { Text(t) })
                    }
                }
            }
        }
    }
}
