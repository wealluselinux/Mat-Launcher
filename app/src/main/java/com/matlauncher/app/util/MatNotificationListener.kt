package com.matlauncher.app.util

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Drawable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat

class NotifItem(
    val key: String,
    val pkg: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val time: Long,
    val icon: Drawable?,
    val intent: PendingIntent?,
    val clearable: Boolean,
)

/** Shared list the lock screen reads. Filled by [MatNotificationListener] once the user grants access. */
object NotificationStore {
    var items by mutableStateOf<List<NotifItem>>(emptyList())
    var listener: MatNotificationListener? = null

    fun hasAccess(ctx: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

    fun dismiss(item: NotifItem) {
        try {
            listener?.cancelNotification(item.key)
        } catch (e: Exception) {
            // ignore
        }
    }
}

class MatNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        NotificationStore.listener = this
        refresh()
    }

    override fun onListenerDisconnected() {
        NotificationStore.listener = null
        NotificationStore.items = emptyList()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = refresh()

    override fun onNotificationRemoved(sbn: StatusBarNotification) = refresh()

    private fun refresh() {
        val pm = packageManager
        val active = try {
            activeNotifications ?: emptyArray()
        } catch (e: Exception) {
            emptyArray<StatusBarNotification>()
        }
        NotificationStore.items = active
            .filter { sbn ->
                val n = sbn.notification
                sbn.packageName != packageName &&
                    !sbn.isOngoing &&
                    (n.flags and Notification.FLAG_GROUP_SUMMARY) == 0
            }
            .sortedByDescending { it.postTime }
            .map { sbn ->
                val n = sbn.notification
                val extras = n.extras
                val label = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
                } catch (e: Exception) {
                    sbn.packageName
                }
                val icon = try {
                    pm.getApplicationIcon(sbn.packageName)
                } catch (e: Exception) {
                    null
                }
                NotifItem(
                    key = sbn.key,
                    pkg = sbn.packageName,
                    appLabel = label,
                    title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "",
                    text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                        ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString() ?: "",
                    time = sbn.postTime,
                    icon = icon,
                    intent = n.contentIntent,
                    clearable = sbn.isClearable,
                )
            }
    }
}
