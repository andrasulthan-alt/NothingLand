package com.theglitchh.NothingLand.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import com.theglitchh.NothingLand.R;

/**
 * Demo mode: posts fake notifications from OmniLand itself so every island
 * feature (calls, timer, download, navigation) can be tested without waiting
 * for the real thing. The notifications go through the normal notification
 * listener, exactly like real ones.
 */
public final class DemoNotifications {
    private static final String CHANNEL = "demo";
    public static final int ID_CALL = 9001;
    public static final int ID_TIMER = 9002;
    public static final int ID_DOWNLOAD = 9003;
    public static final int ID_NAV = 9004;

    private static final String ACTION_ANSWER = "demo.answer";
    private static final String ACTION_END = "demo.end";

    private DemoNotifications() {
    }

    private static NotificationManager manager(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Demo mode", NotificationManager.IMPORTANCE_LOW));
        return nm;
    }

    private static PendingIntent broadcast(Context c, String action, int id) {
        Intent i = new Intent(c, DemoReceiver.class).setAction(action).putExtra("id", id);
        return PendingIntent.getBroadcast(c, action.hashCode() + id, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static NotificationCompat.Builder base(Context c) {
        return new NotificationCompat.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.launcher_foreground)
                .setOnlyAlertOnce(true)
                .setSilent(true);
    }

    /** Fake incoming call with Answer / Decline. Answering turns it into an ongoing call. */
    public static void incomingCall(Context c) {
        Bundle extras = new Bundle();
        extras.putInt("android.callType", 1);
        extras.putParcelable("android.answerIntent", broadcast(c, ACTION_ANSWER, ID_CALL));
        extras.putParcelable("android.declineIntent", broadcast(c, ACTION_END, ID_CALL));
        Notification n = base(c)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setContentTitle("Demo Caller")
                .setContentText("Incoming call (demo)")
                .setOngoing(true)
                .addExtras(extras)
                .addAction(0, "Decline", broadcast(c, ACTION_END, ID_CALL))
                .addAction(0, "Answer", broadcast(c, ACTION_ANSWER, ID_CALL))
                .build();
        manager(c).notify(ID_CALL, n);
    }

    public static void ongoingCall(Context c) {
        Bundle extras = new Bundle();
        extras.putInt("android.callType", 2);
        extras.putParcelable("android.hangUpIntent", broadcast(c, ACTION_END, ID_CALL));
        Notification n = base(c)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setContentTitle("Demo Caller")
                .setContentText("Ongoing call (demo)")
                .setOngoing(true)
                .setWhen(System.currentTimeMillis())
                .setUsesChronometer(true)
                .addExtras(extras)
                .addAction(0, "Hang up", broadcast(c, ACTION_END, ID_CALL))
                .build();
        manager(c).notify(ID_CALL, n);
    }

    /** Fake 3-minute countdown timer. */
    public static void timer(Context c) {
        Notification n = base(c)
                .setContentTitle("Timer (demo)")
                .setContentText("3 minutes")
                .setOngoing(true)
                .setWhen(System.currentTimeMillis() + 3 * 60 * 1000)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setContentIntent(broadcast(c, ACTION_END, ID_TIMER))
                .build();
        manager(c).notify(ID_TIMER, n);
    }

    /** Fake download that goes from 0 to 100% in about 12 seconds. */
    public static void download(Context c) {
        final Context app = c.getApplicationContext();
        final Handler h = new Handler(Looper.getMainLooper());
        final int[] progress = {0};
        Runnable step = new Runnable() {
            @Override
            public void run() {
                progress[0] += 5;
                if (progress[0] >= 100) {
                    manager(app).notify(ID_DOWNLOAD, base(app)
                            .setContentTitle("demo-file.zip")
                            .setContentText("Download complete")
                            .setOngoing(false)
                            .setAutoCancel(true)
                            .build());
                    return;
                }
                manager(app).notify(ID_DOWNLOAD, base(app)
                        .setContentTitle("demo-file.zip")
                        .setContentText("Downloading… " + progress[0] + "%")
                        .setOngoing(true)
                        .setProgress(100, progress[0], false)
                        .build());
                h.postDelayed(this, 600);
            }
        };
        h.post(step);
    }

    /** Fake Google Maps style turn-by-turn instruction. */
    public static void navigation(Context c) {
        Notification n = base(c)
                .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
                .setContentTitle("In 200 m, turn right")
                .setContentText("Jl. Jenderal Sudirman")
                .setSubText("12 min · 5.4 km · 14:32")
                .setLargeIcon(arrow(c))
                .setOngoing(true)
                .setContentIntent(broadcast(c, ACTION_END, ID_NAV))
                .build();
        manager(c).notify(ID_NAV, n);
    }

    public static void clearAll(Context c) {
        NotificationManager nm = manager(c);
        nm.cancel(ID_CALL);
        nm.cancel(ID_TIMER);
        nm.cancel(ID_DOWNLOAD);
        nm.cancel(ID_NAV);
    }

    /** A simple white "turn right" arrow, drawn in code. */
    private static Bitmap arrow(Context c) {
        int s = 128;
        Bitmap bm = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bm);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.WHITE);
        p.setStrokeWidth(14);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        Path path = new Path();
        path.moveTo(40, 112);
        path.lineTo(40, 48);
        path.lineTo(96, 48);
        cv.drawPath(path, p);
        p.setStyle(Paint.Style.FILL);
        Path head = new Path();
        head.moveTo(84, 24);
        head.lineTo(116, 48);
        head.lineTo(84, 72);
        head.close();
        cv.drawPath(head, p);
        return bm;
    }

    /** Handles the buttons of the demo notifications. Registered in the manifest. */
    public static class DemoReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int id = intent.getIntExtra("id", 0);
            if (ACTION_ANSWER.equals(intent.getAction()) && id == ID_CALL) {
                ongoingCall(context);
            } else if (id != 0) {
                manager(context).cancel(id);
            }
        }
    }
}
