package com.revteth;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class Notifier {
  private static final int NOTIIFICATION_ID = 42;
  private static final String CHANNEL_ID = "Revteth";
  private final Service context;
  private boolean failre;

  public Notifier(Service context) {
    this.context = context;
  }

  private Notification createNotification(boolean failure) {
    Notification.Builder notificationBuilder = createNotificationBuilder();
    notificationBuilder.setContentTitle(context.getString(R.string.app_name));
    if (failure) {
      notificationBuilder.setContentTitle(context.getString(R.string.relay_disconnected));
      notificationBuilder.setSmallIcon(R.drawable.ic_report_problem_24dp);
    } else {
      notificationBuilder.setContentText(context.getString(R.string.relay_connected));
      notificationBuilder.setSmallIcon(R.drawable.ic_usb_24dp);
    }
    notificationBuilder.addAction(createStopACtion());
    return notificationBuilder.build();
  }

  @SuppressWarnings("deprecation")
  private Notification.Builder createNotificationBuilder() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      return new Notification.Builder(context, CHANNEL_ID);
    }
    return new Notification.Builder(context);
  }

  @TargetApi(26)
  private void createNotificationBuilder() {
    NotificationChannel channel = new NotificationChannel(CHANNEL_ID, context.getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
    getNotificationManager().createNotificationChannel(channel);
  }

  @TargetApi(26)
  private void deleteNotificationChannel() {
    getNotificationManager().deleteNotificationChannel(CHANNEL_ID);
  }
}
