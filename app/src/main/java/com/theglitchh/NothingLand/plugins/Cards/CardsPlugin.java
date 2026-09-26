package com.theglitchh.NothingLand.plugins.Cards;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.theglitchh.NothingLand.R;
import com.theglitchh.NothingLand.plugins.BasePlugin;
import com.theglitchh.NothingLand.services.OverlayService;
import com.theglitchh.NothingLand.utils.CallBack;
import com.theglitchh.NothingLand.utils.QuickActions;
import com.theglitchh.NothingLand.utils.SettingStruct;

import java.util.ArrayList;

/**
 * "Quick Cards": panels the user opens with a gesture on the idle island.
 * - sliders: screen brightness, media volume, flashlight brightness
 * - apps: grid of favorite apps
 * The views are built in code so no extra layout files are needed.
 */
public class CardsPlugin extends BasePlugin {

    public static final String CARD_SLIDERS = QuickActions.SLIDERS;
    public static final String CARD_APPS = QuickActions.APPS;
    private static final long ANIMATION_MS = 850;
    private static final long AUTO_CLOSE_MS = 10_000;

    private OverlayService ctx;
    private Handler handler;
    private String card;
    private LinearLayout mView;
    private LinearLayout panel;
    private boolean expanded = false;
    private boolean queued = false;

    @Override
    public String getID() {
        return "CardsPlugin";
    }

    @Override
    public String getName() {
        return "Quick Cards";
    }

    @Override
    public void onCreate(OverlayService context) {
        ctx = context;
        handler = new Handler(context.getMainLooper());
    }

    @Override
    public void onDestroy() {
        if (handler != null) handler.removeCallbacksAndMessages(null);
        card = null;
        queued = false;
        expanded = false;
        mView = null;
    }

    /** Called by the overlay service when a gesture asks for a card. */
    public void show(String which) {
        if (ctx == null) return;
        if (queued && which.equals(card)) {
            // Same card again: toggle it closed.
            close();
            return;
        }
        card = which;
        if (queued) {
            rebuildPanel();
            resetAutoClose();
            return;
        }
        queued = true;
        ctx.enqueue(this);
    }

    private void resetAutoClose() {
        handler.removeCallbacks(closer);
        handler.postDelayed(closer, AUTO_CLOSE_MS);
    }

    private final Runnable closer = this::close;

    private void close() {
        handler.removeCallbacks(closer);
        if (!queued) return;
        if (expanded) {
            collapse();
            handler.postDelayed(this::dequeueNow, ANIMATION_MS);
        } else {
            dequeueNow();
        }
    }

    private void dequeueNow() {
        if (!queued) return;
        queued = false;
        card = null;
        ctx.dequeue(this);
    }

    // ---------------------------------------------------------------- views

    @Override
    public View onBind() {
        mView = new LinearLayout(ctx);
        mView.setId(R.id.binded);
        mView.setOrientation(LinearLayout.VERTICAL);
        // Empty compact row so the island keeps its normal size before it expands.
        View row = new View(ctx);
        row.setMinimumHeight(ctx.minHeight);
        row.setMinimumWidth(ctx.dpToInt(83));
        mView.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ctx.minHeight));
        panel = new LinearLayout(ctx);
        panel.setOrientation(LinearLayout.VERTICAL);
        int pad = ctx.dpToInt(18);
        panel.setPadding(pad, ctx.dpToInt(8), pad, ctx.dpToInt(14));
        panel.setVisibility(View.GONE);
        mView.addView(panel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rebuildPanel();
        return mView;
    }

    @Override
    public void onBindComplete() {
        onExpand();
        resetAutoClose();
    }

    @Override
    public void onUnbind() {
        expanded = false;
        mView = null;
        panel = null;
    }

    private void rebuildPanel() {
        if (panel == null) return;
        panel.removeAllViews();
        if (CARD_APPS.equals(card)) buildAppsCard();
        else buildSlidersCard();
    }

    private TextView label(String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextColor(ctx.textColor);
        tv.setTextSize(13);
        tv.setPadding(0, ctx.dpToInt(6), 0, 0);
        return tv;
    }

    private SeekBar slider(int max, int value, SeekBar.OnSeekBarChangeListener listener) {
        SeekBar sb = new SeekBar(ctx);
        sb.setMax(max);
        sb.setProgress(value);
        sb.setProgressTintList(ColorStateList.valueOf(ctx.textColor));
        sb.setThumbTintList(ColorStateList.valueOf(ctx.textColor));
        sb.setOnSeekBarChangeListener(listener);
        panel.addView(sb, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return sb;
    }

    private abstract static class OnChange implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }

    private void buildSlidersCard() {
        // Brightness
        panel.addView(label("☀ Brightness"));
        int brightness = 128;
        try {
            brightness = Settings.System.getInt(ctx.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Exception ignored) {
        }
        slider(255, brightness, new OnChange() {
            @Override
            public void onProgressChanged(SeekBar sb, int value, boolean fromUser) {
                if (!fromUser) return;
                resetAutoClose();
                if (!Settings.System.canWrite(ctx)) {
                    Toast.makeText(ctx, "Allow OmniLand to modify system settings for brightness", Toast.LENGTH_SHORT).show();
                    try {
                        Intent i = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                        i.setData(android.net.Uri.parse("package:" + ctx.getPackageName()));
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        ctx.startActivity(i);
                    } catch (Exception ignored) {
                    }
                    close();
                    return;
                }
                try {
                    Settings.System.putInt(ctx.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                    Settings.System.putInt(ctx.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, Math.max(1, value));
                } catch (Exception e) {
                    Log.w("CardsPlugin", "Brightness failed", e);
                }
            }
        });

        // Media volume
        final AudioManager audio = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
