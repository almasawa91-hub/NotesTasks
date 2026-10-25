package com.notesapp.tasks

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

class MainActivity : ComponentActivity() {
    private val vm: MainVM by viewModels()

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            AppTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    var editing by remember { mutableStateOf<Item?>(null) }
                    val e = editing
                    if (e == null) {
                        ListScreen(
                            vm,
                            onOpen = { editing = it },
                            onNew = { task -> editing = Item(isTask = task) }
                        )
                    } else {
                        Editor(
                            initial = e,
                            onSave = { save(it) },
                            onDelete = { vm.delete(it) },
                            onClose = { editing = null }
                        )
                    }
                }
            }
        }
    }

    private fun save(item: Item) {
        vm.save(item)
        if (item.reminderAt != null && Build.VERSION.SDK_INT >= 31) {
            val am = getSystemService(AlarmManager::class.java)
            val prefs = getSharedPreferences("p", MODE_PRIVATE)
            if (!am.canScheduleExactAlarms() && !prefs.getBoolean("asked", false)) {
                prefs.edit().putBoolean("asked", true).apply()
                try {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
                    )
                } catch (_: Exception) {
                }
            }
        }
    }
}
