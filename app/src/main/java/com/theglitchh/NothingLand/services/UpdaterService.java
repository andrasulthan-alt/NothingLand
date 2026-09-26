package com.theglitchh.NothingLand.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import com.theglitchh.NothingLand.BuildConfig;
import com.theglitchh.NothingLand.R;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class UpdaterService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(getPackageName() + ".START_UPDATE")) {
                if (download_url != null) {
                    startDownload(download_url);
                } else {
                    sendNotification("Cannot update app, please report the problem to developer.", NotificationManager.IMPORTANCE_HIGH, false);
                }
            }
        }
    };
    private String download_url;

    @Override
    public void onCreate() {
        super.onCreate();
        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        RequestQueue queue = Volley.newRequestQueue(this);
        sendNotification("Checking for updates", NotificationManager.IMPORTANCE_MIN, true);
        registerReceiver(broadcastReceiver, new IntentFilter(getPackageName() + ".START_UPDATE"));
        int VERSION_CODE = BuildConfig.VERSION_CODE;
        String baseUrl = "https://api.github.com/";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, baseUrl + "repos/" + BuildConfig.UPDATE_REPO + "/releases",
                null, response -> {
            if (response.length() > 0) {
                try {
                    JSONObject object = (JSONObject) response.get(0);
                    try {
                        if (Integer.parseInt(object.getString("tag_name")) > VERSION_CODE) {
                            JSONArray o = object.getJSONArray("assets");
                            download_url = null;
                            for (int i = 0; i < o.length(); i++) {
                                JSONObject asset = (JSONObject) o.get(i);
                                if (asset.optString("name", "").toLowerCase().endsWith(".apk")) {
                                    download_url = asset.getString("browser_download_url");
                                    break;
                                }
                            }
                            if (download_url == null) {
                                // No .apk asset on the release - nothing we can install.
                                stopSelf();
                                return;
                            }
                            Intent intent = new Intent(getPackageName() + ".UPDATE_AVAIL");
                            intent.putExtra("version", object.getString("name"));
                            sendBroadcast(new Intent(intent));
                            sendNotification("Update available", NotificationManager.IMPORTANCE_MAX, false);
                        } else {
                            stopSelf();
                        }
                    } catch (Exception e) {
                        // do nothing lol
                        stopSelf();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    stopSelf();
                }

            } else {
                stopSelf();
            }
        }, error ->

        {
        });
        queue.add(jsonArrayRequest);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        manager.cancel(1001);
    }

    NotificationManager manager;


    private void sendNotification(String text, int priority, boolean ongoing) {
        final String NOTIFICATION_CHANNEL_ID = getPackageName() + ".updater_channel";
        String channelName = "Updater Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_MIN);
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(ongoing)
                .setContentTitle("OmniLand")
                .setContentText(text)
                .setSmallIcon(R.drawable.launcher_foreground)
                .setPriority(priority)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        manager.notify(1001, notification);
    }
    // Replaces the old AsyncTask-based downloader (AsyncTask is deprecated since API 30).
    private void startDownload(String fileUrl) {
        sendNotification("Starting download", NotificationManager.IMPORTANCE_MIN, true);
        android.os.Handler mainHandler = new android.os.Handler(getMainLooper());
        new Thread(() -> {
            try {
                URL url = new URL(fileUrl);
                URLConnection connection = url.openConnection();
                connection.connect();

                int lengthOfFile = connection.getContentLength();
                InputStream input = new BufferedInputStream(url.openStream(), 8192);
                OutputStream output = new FileOutputStream(getExternalFilesDir(null).getAbsolutePath() + "/output.apk");

                byte[] data = new byte[1024];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    final int progress = lengthOfFile > 0 ? (int) ((total * 100) / lengthOfFile) : -1;
                    mainHandler.post(() -> onDownloadProgress(progress));
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();

                mainHandler.post(this::onDownloadComplete);
            } catch (Exception e) {
                Log.e("UpdaterService", "Download failed", e);
                mainHandler.post(() -> sendNotification("Update download failed", NotificationManager.IMPORTANCE_HIGH, false));
            }
        }).start();
    }

    private void onDownloadProgress(int progress) {
        final String NOTIFICATION_CHANNEL_ID = getPackageName() + ".updater_channel";
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.launcher_foreground)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setContentTitle("OmniLand")
                .setContentText("Downloading update")
                .setProgress(100, Math.max(progress, 0), progress < 0);

        manager.notify(1001, notificationBuilder.build());
    }

    //  Source for below codes : https://medium.com/@vishtech36/installing-apps-programmatically-in-android-10-7e39cfe22b86
    private void onDownloadComplete() {
        String PATH = getExternalFilesDir(null).getAbsolutePath() + "/output.apk";
        File file = new File(PATH);
        if (file.exists()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uriFromFile(getApplicationContext(), file), "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                getApplicationContext().startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e("UpdaterService", "Error opening the downloaded APK", e);
            }
        } else {
            Toast.makeText(getApplicationContext(), "installing", Toast.LENGTH_LONG).show();
        }
        stopSelf();
    }

    private Uri uriFromFile(Context context, File file) {
        return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
    }
}
