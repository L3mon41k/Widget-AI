package com.leemon41k.widgetai.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.RemoteViews
import com.leemon41k.widgetai.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_TIME_TICK ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, javaClass))
                .forEach { update(context, manager, it) }
        }
    }

    private fun update(context: Context, manager: AppWidgetManager, id: Int) {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra("level", -1) ?: -1
        val scale = battery?.getIntExtra("scale", -1) ?: -1
        val percent = if (level >= 0 && scale > 0) level * 100 / scale else -1
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val views = RemoteViews(context.packageName, R.layout.widget_basic).apply {
            setTextViewText(R.id.widget_time, time)
            setTextViewText(R.id.widget_battery, if (percent >= 0) "Battery: $percent%" else "Battery: --")
            setTextViewText(R.id.widget_status, "WidgetAI · device")
        }
        manager.updateAppWidget(id, views)
    }
}
