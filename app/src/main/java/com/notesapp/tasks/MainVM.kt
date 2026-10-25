package com.notesapp.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainVM(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).dao()
    private val ctx = app

    val query = MutableStateFlow("")
    val filter = MutableStateFlow(0) // 0 الكل، 1 ملاحظات، 2 مهام

    val items = combine(dao.observeAll(), query, filter) { list, q, f ->
        list.filter {
            (f == 0 || (f == 1 && !it.isTask) || (f == 2 && it.isTask)) &&
                (q.isBlank() || it.title.contains(q, true) || it.content.contains(q, true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleDone(item: Item) = viewModelScope.launch {
        val n = item.copy(done = !item.done)
        dao.upsert(n)
        if (n.done) Reminder.cancel(ctx, n.id) else Reminder.schedule(ctx, n)
    }

    fun save(item: Item) {
        if (item.title.isBlank() && item.content.isBlank()) {
            if (item.id != 0L) delete(item)
            return
        }
        viewModelScope.launch {
            val id = dao.upsert(item)
            val saved = if (item.id == 0L) item.copy(id = id) else item
            Reminder.cancel(ctx, saved.id)
            Reminder.schedule(ctx, saved)
        }
    }

    fun delete(item: Item) = viewModelScope.launch {
        Reminder.cancel(ctx, item.id)
        dao.delete(item)
    }
}
