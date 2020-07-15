package io.briones.jdkw

import com.intellij.notification.Notification
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

class Notifier(private val project: Project) {
    companion object {
        private val NOTIFICATION_GROUP = NotificationGroup(
                "JDK Wrapper Auto Import",
                NotificationDisplayType.BALLOON,
                true
        )
    }

    fun error(configure: Notification.() -> Notification = { this }): Notification {
        return notify(NotificationType.ERROR, configure)
    }

    fun warn(configure: Notification.() -> Notification = { this }): Notification {
        return notify(NotificationType.WARNING, configure)
    }

    fun info(configure: Notification.() -> Notification = { this }): Notification {
        return notify(NotificationType.INFORMATION, configure)
    }

    private inline fun notify(type: NotificationType,
                              configure: Notification.() -> Notification): Notification {
        val notification = NOTIFICATION_GROUP.createNotification(type).let(configure)
        notification.notify(project)
        return notification
    }
}
