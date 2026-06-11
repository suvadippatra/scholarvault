package com.scholarvault.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.scholarvault.data.AppDatabase
import com.scholarvault.scheduler.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.LOCKED_BOOT_COMPLETED") return

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val app = context.applicationContext as com.scholarvault.MainApplication
            val db = app.database
            val now = System.currentTimeMillis()
            val scheduler = AlarmScheduler(context)
            db.reminderDao().getAllRemindersOnce()
                .filter { !it.isDeleted && it.timeInMillis > now }
                .forEach { r -> scheduler.scheduleAlarm(r.id, r.title, "", r.timeInMillis) }
        }
    }
}
