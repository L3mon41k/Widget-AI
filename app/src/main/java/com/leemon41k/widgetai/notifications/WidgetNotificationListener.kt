package com.leemon41k.widgetai.notifications

import android.service.notification.NotificationListenerService

/**
 * Optional notification access. Notification content is not uploaded or persisted in this MVP.
 * User must explicitly enable Notification Access in Android settings.
 */
class WidgetNotificationListener : NotificationListenerService()
