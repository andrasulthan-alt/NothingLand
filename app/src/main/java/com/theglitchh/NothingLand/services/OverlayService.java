package com.theglitchh.NothingLand.services;

import android.accessibilityservice.AccessibilityService;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.HapticFeedbackConstants;
import android.view.animation.DecelerateInterpolator;

import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;

import com.theglitchh.NothingLand.plugins.BasePlugin;
import com.theglitchh.NothingLand.plugins.ExportedPlugins;
import com.theglitchh.NothingLand.utils.CallBack;
import com.theglitchh.NothingLand.utils.CutoutPosition;
import com.theglitchh.NothingLand.R;
import com.google.android.material.color.DynamicColors;
import android.graphics.RenderEffect;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import android.graphics.BlurMaskFilter;
import android.os.Build;

public class OverlayService extends AccessibilityService {
    private boolean isOverlayHiddenByScreenshot = false;
    private final ArrayList<BasePlugin> plugins = ExportedPlugins.getPlugins();
    public int minHeight;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        // boolean setblurback;
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
                if (sharedPreferences.getBoolean("enable_on_lockscreen", false)) return;
                if (mView != null) {
                    mView.setVisibility(View.VISIBLE);
                    updateBubble();
                }
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                if (sharedPreferences.getBoolean("enable_on_lockscreen", false)) return;
                mView.setVisibility(View.INVISIBLE);
                hideBubble();
            } else if (intent.getAction().equals(getPackageName() + ".OVERLAY_LAYOUT_CHANGE")) {
                Bundle settings = Objects.requireNonNull(intent.getExtras()).getBundle("settings");
                assert settings != null;
                for (String s : settings.keySet()) {
                    if (settings.get(s) instanceof Float) {
                        sharedPreferences.putFloat(s, settings.getFloat(s));
                    }
                }
                if (mView != null && mWindowManager != null) {
                    WindowManager.LayoutParams mParams = (WindowManager.LayoutParams) mView.getLayoutParams();

                    minWidth = dpToInt((int) sharedPreferences.getFloat("overlay_w", 83));
                    minHeight = dpToInt((int) sharedPreferences.getFloat("overlay_h", 40));
                    gap = dpToInt((int) sharedPreferences.getFloat("overlay_gap", 50));
                    y = (int) (sharedPreferences.getFloat("overlay_y", defaultYPercent) * 0.01 * metrics.heightPixels);
                    x = (int) (sharedPreferences.getFloat("overlay_x", defaultXPercent) * 0.01 * metrics.widthPixels);
                    mParams.y = y;
                    mParams.x = x;
                    mParams.height = minHeight;
                    if (mView.findViewById(R.id.blank_space) != null) {
                        mView.findViewById(R.id.blank_space).setMinimumWidth(gap);
                    }
                    mWindowManager.updateViewLayout(mView, mParams);
                }
            } else if (Objects.equals(intent.getAction(), getPackageName() + ".COLOR_CHANGED")) {
                String uriString = intent.getStringExtra("background_image_uri");
                View mainView = mView != null ? mView.findViewById(R.id.main) : null;
                if (uriString != null && mView != null) {
                    try {
                        mView.findViewById(R.id.main).setBackgroundTintList(null);
                        Uri imageUri = Uri.parse(uriString);
                        Drawable background = Drawable.createFromStream(
                                getContentResolver().openInputStream(imageUri), uriString);

                        if (mainView != null) {
                            mainView.setBackground(background);
                            applyBlurToMainView();
                            imageBackground = true;
                        }
                    } catch (Exception e) {
                        Log.e("IMAGE_CHANGED", "Failed to load image URI", e);
                    }
                }else {
                    imageBackground = false;
                    int targetColor = 0x80000000;
                    color = Objects.requireNonNull(intent.getExtras()).getInt("color", Color.RED);

                    if (color == targetColor) {
                        textColor = isColorDark(color) ? getColor(R.color.white) : getColor(R.color.black);
                        mView.findViewById(R.id.main).setBackgroundTintList(ColorStateList.valueOf(color));


                    }  else {
                        textColor = isColorDark(color) ? getColor(R.color.white) : getColor(R.color.black);
                        if (mView != null) {
                            mView.findViewById(R.id.main).setBackgroundTintList(ColorStateList.valueOf(color));
                            if (binded_plugin != null) {
                                binded_plugin.onTextColorChange();
                            }
                        }

                    }
                    if (colorOverride != null) applyIslandColor();
                }

            } else {
                Bundle settings = Objects.requireNonNull(intent.getExtras()).getBundle("settings");
                assert settings != null;
                for (String s : settings.keySet()) {
                    if (settings.get(s) instanceof Boolean) {
                        sharedPreferences.putBoolean(s, settings.getBoolean(s));
                    }
                }
                plugins.forEach(p -> {
                    try {
                        p.onDestroy();
                    } catch (Throwable ignored) {
                    }
                });
                queued.clear();
                removeBubble();
                if (mView != null && mWindowManager != null) {
                    mWindowManager.removeViewImmediate(mView);
                }
                init();
            }
        }
    };


    private void hideOverlayOnScreenshot() {
        if (mView != null && mView.getVisibility() == View.VISIBLE) {
            Log.d("OverlayService", "Hiding the overlay for screenshot");
            isOverlayHiddenByScreenshot = true;
            mView.setVisibility(View.INVISIBLE);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isOverlayHiddenByScreenshot && mView != null) {
                    mView.setVisibility(View.VISIBLE);
                    isOverlayHiddenByScreenshot = false;
                }
            }, 2500); // Reappear after 2.5 seconds
        }
    }

    public int x, y;
    private int color;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        plugins.forEach(x -> x.onEvent(accessibilityEvent));
    }

    @Override
    public void onInterrupt() {

    }


    private void expandOverlay() {
        haptic(HapticFeedbackConstants.LONG_PRESS);
        if (binded_plugin != null) {
            if (sharedPreferences.getBoolean("invert_click", false)) {
                binded_plugin.onClick();
            } else
                binded_plugin.onExpand();

        } else {
            animateOverlay(minHeight + dpToInt(20), minWidth + dpToInt(20), false, new CallBack(), new CallBack() {
                @Override
                public void onFinish() {
                    animateOverlay(minHeight, minWidth, false, new CallBack(), new CallBack(), false);
                }
            }, false);
        }
    }


    private void shrinkOverlay() {
        if (binded_plugin != null) binded_plugin.onCollapse();
        //removeBlurFromParent(mView);

    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //ConstraintLayout constraintLayout = mView.findViewById(R.id.main);
        ConstraintLayout mainLayout;
        mainLayout = mView.findViewById(R.id.main);

        return START_STICKY;

    }

    public Bundle sharedPreferences = new Bundle();

    private WindowManager.LayoutParams getParams(int width, int height, int extFlags) {
        return new WindowManager.LayoutParams(
                width, height,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                extFlags,
                PixelFormat.TRANSLUCENT);
    }

    private float y1, y2, x1, x2;
    static final int MIN_DISTANCE = 50;
    private final AtomicLong press_start = new AtomicLong();
    public DisplayMetrics metrics = new DisplayMetrics();

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e("OverlayService", "Uncaught exception on " + thread.getName(), throwable);
            copyCrashLog(throwable);
            // A background thread dying doesn't take the island down, so only
            // exit if the main thread itself is gone (the guard below normally
            // catches main-thread errors before they get here).
            if (thread == Looper.getMainLooper().getThread()) {
                Runtime.getRuntime().exit(0);
            }
        });
        installMainThreadGuard();
        IntentFilter filter = new IntentFilter(getPackageName() + ".SETTINGS_CHANGED");
        filter.addAction(getPackageName() + ".OVERLAY_LAYOUT_CHANGE");
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(getPackageName() + ".COLOR_CHANGED");
        registerReceiver(broadcastReceiver, filter);

        SharedPreferences sharedPreferences2 = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        sharedPreferences2.getAll().forEach((key, value) -> {
            if (value instanceof Boolean)
                sharedPreferences.putBoolean(key, (boolean) value);
            else if (value instanceof Float) {
                sharedPreferences.putFloat(key, (float) value);
            } else if (value instanceof String) {
                sharedPreferences.putString(key, (String) value);
            } else if (value instanceof Integer) {
                sharedPreferences.putInt(key, (int) value);
            }
        });
        mWindowManager = (WindowManager) this.getSystemService(WINDOW_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Rect bounds = mWindowManager.getCurrentWindowMetrics().getBounds();
            metrics.widthPixels = bounds.width();
            metrics.heightPixels = bounds.height();
        } else {
            //noinspection deprecation
            mWindowManager.getDefaultDisplay().getMetrics(metrics);
        }
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        float[] cutoutDefault = CutoutPosition.compute(this, sharedPreferences.getFloat("overlay_h", 40));
        if (cutoutDefault != null) {
            defaultXPercent = cutoutDefault[0];
            defaultYPercent = cutoutDefault[1];
        }
        init();
    }

    // Default island position (percent of screen) used until the user sets their own.
    // Centred on the front camera when the phone reports a cutout.
    private float defaultXPercent = CutoutPosition.LEGACY_X;
    private float defaultYPercent = CutoutPosition.LEGACY_Y;

    private final ArrayList<Long> recentCrashes = new ArrayList<>();
    private long lastCrashToast = 0;

    /**
     * Keeps the island alive when a plugin throws on the main thread. Instead of
     * the whole service process dying (and the island disappearing until the user
     * re-enables accessibility), the error is logged and the island is rebuilt.
     * If it keeps failing (3 errors within 30 s) we stop and let it exit.
     */
    private void installMainThreadGuard() {
        new Handler(Looper.getMainLooper()).post(() -> {
            while (true) {
                try {
                    Looper.loop();
                } catch (Throwable t) {
                    Log.e("OverlayService", "Recovering from error", t);
                    long now = System.currentTimeMillis();
                    recentCrashes.add(now);
                    recentCrashes.removeIf(time -> now - time > 30_000);
                    copyCrashLog(t);
                    if (recentCrashes.size() >= 3) {
                        Runtime.getRuntime().exit(0);
                    }
                    rebuildOverlay();
                }
            }
        });
    }

    private void rebuildOverlay() {
        try {
            plugins.forEach(p -> {
                try {
                    p.onDestroy();
                } catch (Throwable ignored) {
                }
            });
            queued.clear();
            removeBubble();
            if (mView != null && mWindowManager != null && mView.getParent() != null) {
                mWindowManager.removeViewImmediate(mView);
            }
            init();
        } catch (Throwable t) {
            Log.e("OverlayService", "Could not rebuild the island", t);
        }
    }

    private void copyCrashLog(Throwable throwable) {
        try {
            if (!sharedPreferences.getBoolean("clip_copy_enabled", true)) return;
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("OmniLand error log", throwable + " : " + Arrays.toString(throwable.getStackTrace()));
            clipboard.setPrimaryClip(clip);
            long now = System.currentTimeMillis();
            if (now - lastCrashToast > 10_000 && Looper.myLooper() == Looper.getMainLooper()) {
                lastCrashToast = now;
                Toast.makeText(this, "OmniLand hit an error, log copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        } catch (Throwable ignored) {
        }
    }

    public int gap;
    private Context ctx;

    public int getAttr(int attr) {
        final TypedValue value = new TypedValue();
        ctx.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, value, true);
        return value.data;
    }

    public int statusBarHeight = 0;

    private boolean isColorDark(int color) {
        // Source : https://stackoverflow.com/a/24261119
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        // It's a dark color
        return !(darkness < 0.5); // It's a light color
    }

    public int textColor;

    @SuppressLint("ClickableViewAccessibility")
    private void init() {

        binded_plugin = null;
        colorOverride = null;
        int flags = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;


        if (minWidth == 0) {
            minWidth = dpToInt((int) sharedPreferences.getFloat("overlay_w", 83));
        }
        if (minHeight == 0) {
            minHeight = dpToInt((int) sharedPreferences.getFloat("overlay_h", 40));
        }
        if (gap == 0) {
            gap = dpToInt((int) sharedPreferences.getFloat("overlay_gap", 50));
        }
        color = sharedPreferences.getInt("color", getColor(R.color.black));
        textColor = isColorDark(color) ? getColor(R.color.white) : getColor(R.color.black);
        last_min_size = minWidth;
        WindowManager.LayoutParams mParams = getParams(minWidth, minHeight, flags);
        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        getBaseContext().setTheme(R.style.Theme_TheGlitchh);
        mView = layoutInflater.inflate(R.layout.overlay_layout, null);


        ctx = DynamicColors.wrapContextIfAvailable(getBaseContext(), com.google.android.material.R.style.ThemeOverlay_Material3_DynamicColors_DayNight);
        mParams.gravity = Gravity.TOP | Gravity.CENTER;
        if (y == 0) {
            y = (int) (sharedPreferences.getFloat("overlay_y", defaultYPercent) * 0.01f * metrics.heightPixels);
        }
        mParams.y = y;
        if (x == 0) {
            x = (int) (sharedPreferences.getFloat("overlay_x", defaultXPercent) * 0.01f * metrics.widthPixels);
        }
        mView.setBackgroundTintList(ColorStateList.valueOf(color));

        mParams.x = x;
        Runnable mLongPressed = this::expandOverlay;
        try {

            if (mView.getWindowToken() == null) {
                if (mView.getParent() == null) {
                    mWindowManager.addView(mView, mParams);
                }
            }
        } catch (Exception e) {
            Log.d("Error1", e.toString());
        }
        applyBlurToMainView();

        mView.setOnTouchListener((view, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                if (event.getPointerCount() == 3) {
                   // hideOverlayOnScreenshot();

                }
            }

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mHandler.postDelayed(mLongPressed, ViewConfiguration.getLongPressTimeout());
                press_start.set(Instant.now().toEpochMilli());
                y1 = event.getY();
                x1 = event.getX();
            }

            if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                shrinkOverlay();
            }
            if ((event.getAction() == MotionEvent.ACTION_UP)) {
                mHandler.removeCallbacks(mLongPressed);
                y2 = event.getY();
                x2 = event.getX();
                float deltaY = y2 - y1;
                float deltaX = x2 - x1;

                if (Math.abs(deltaX) > MIN_DISTANCE) {
                    if (binded_plugin != null) {
                        if (deltaX < 0) {
                            binded_plugin.onLeftSwipe();
                        } else {
                            binded_plugin.onRightSwipe();
                        }
                    }
                }
                if (-deltaY > MIN_DISTANCE) {
                    if (binded_plugin != null) {
                        binded_plugin.onSwipeUp();
                    }
                } else if (deltaY > MIN_DISTANCE) {
                    if (binded_plugin != null) {
                        binded_plugin.onSwipeDown();
                    }
                }
                if (Math.abs(deltaX) < MIN_DISTANCE && Math.abs(deltaY) < MIN_DISTANCE) {
                    if (press_start.get() + ViewConfiguration.getLongPressTimeout() > Instant.now().toEpochMilli()) {
                        if (binded_plugin != null) {
                            haptic(HapticFeedbackConstants.VIRTUAL_KEY);
                            if (sharedPreferences.getBoolean("invert_click", false)) {
                                binded_plugin.onExpand();

                            } else {
                                binded_plugin.onClick(); // Normal click action
                            }
                        }
                    }
                }


                if (-deltaY > MIN_DISTANCE) {
                    shrinkOverlay();
                    return false;
                }
            }

            return false;
        });
        plugins.forEach(x -> {
            if (sharedPreferences.getBoolean(x.getID() + "_enabled", true)) x.onCreate(this);
        });
        binded_plugin = null;
        bindPlugin();
    }

    ArrayList<String> queued = new ArrayList<>();
    private BasePlugin binded_plugin;
    public void enqueue(BasePlugin plugin) {
        if (!queued.contains(plugin.getID())) {
            if (binded_plugin != null && plugins.indexOf(plugin) < plugins.indexOf(binded_plugin)) {
                queued.add(0, plugin.getID());
            } else queued.add(plugin.getID());
        }
        bindPlugin();
    }

    public void dequeue(BasePlugin plugin) {
        if (!queued.contains(plugin.getID())) return;
        else queued.remove(plugin.getID());
        if (binded_plugin != null && binded_plugin.getID().equals(plugin.getID()))
            binded_plugin = null;
        bindPlugin();
    }

    private int last_min_size = 200;

    public void animateOverlay(int h, int w, boolean expanded, CallBack callBackStart, CallBack callBackEnd, boolean expandedPrev) {
        int init_w = w;
        if (!expanded && w == ViewGroup.LayoutParams.WRAP_CONTENT) {
            w = last_min_size;
            if (w < minWidth) w = minWidth;
        }
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) mView.getLayoutParams();
        if (expanded) {
            if (!expandedPrev) last_min_size = mView.getMeasuredWidth();
        }
        if (sharedPreferences.getBoolean("smooth_animation", true)) {
            animateOverlaySmooth(h, w, init_w, expanded, callBackStart, callBackEnd, null);
            return;
        }
        ValueAnimator height_anim = ValueAnimator.ofInt(params.height, h);
        height_anim.setDuration(800);
        height_anim.addUpdateListener(valueAnimator -> {
            params.height = (int) valueAnimator.getAnimatedValue();
            mWindowManager.updateViewLayout(mView, params);
        });
        ValueAnimator width_anim = ValueAnimator.ofInt(mView.getMeasuredWidth(), w);
        width_anim.setDuration(800);
        width_anim.addUpdateListener(v2 -> {
            params.width = Math.abs((int) v2.getAnimatedValue());
            mWindowManager.updateViewLayout(mView, params);
        });
        width_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                callBackStart.onFinish();
                if (expanded && params.x != 0) {
                    ValueAnimator v = ValueAnimator.ofInt(params.x, 0).setDuration(500);
                    v.addUpdateListener(valueAnimator -> {
                        params.x = (int) valueAnimator.getAnimatedValue();
                    });
                    v.start();
                }
                if (!expanded && x != params.x) {
                    ValueAnimator v = ValueAnimator.ofInt(params.x, x).setDuration(500);
                    v.addUpdateListener(valueAnimator -> {
                        params.x = (int) valueAnimator.getAnimatedValue();
                    });
                    v.start();
                }

            }

            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                callBackEnd.onFinish();
                if (init_w == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                }
                mWindowManager.updateViewLayout(mView, params);
            }
        });
        if (w != 0) {
            width_anim.setInterpolator(new OvershootInterpolator(1f));
            height_anim.setInterpolator(new OvershootInterpolator(1f));
        }

        width_anim.start();
        height_anim.start();
    }

    public void animateOverlay(int h, int w, boolean expanded, CallBack callBackStart, CallBack callBackEnd, CallBack onChange, boolean expandedPrev) {
        int init_w = w;
        if (!expanded && w == ViewGroup.LayoutParams.WRAP_CONTENT) {
            w = last_min_size;
            if (w < minWidth) w = minWidth;
        }

        WindowManager.LayoutParams params = (WindowManager.LayoutParams) mView.getLayoutParams();
        if (expanded) {
            if (!expandedPrev) last_min_size = mView.getMeasuredWidth();
        }
        if (sharedPreferences.getBoolean("smooth_animation", true)) {
            animateOverlaySmooth(h, w, init_w, expanded, callBackStart, callBackEnd, onChange);
            return;
        }
        ValueAnimator height_anim = ValueAnimator.ofInt(params.height, h);
        height_anim.setDuration(800);
        height_anim.addUpdateListener(valueAnimator -> {
            params.height = (int) valueAnimator.getAnimatedValue();
            mWindowManager.updateViewLayout(mView, params);
        });

        ValueAnimator width_anim = ValueAnimator.ofInt(mView.getMeasuredWidth(), w);
        width_anim.setDuration(800);
        width_anim.addUpdateListener(v2 -> {
            onChange.onChange(v2.getAnimatedFraction());
            params.width = Math.abs((int) v2.getAnimatedValue());
            mWindowManager.updateViewLayout(mView, params);
        });
        width_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                callBackStart.onFinish();
                if (expanded && params.x != 0) {
                    ValueAnimator v = ValueAnimator.ofInt(params.x, 0).setDuration(500);
                    v.addUpdateListener(valueAnimator -> {
                        params.x = (int) valueAnimator.getAnimatedValue();
                    });
                    v.start();
                }
                if (!expanded && x != params.x) {
                    ValueAnimator v = ValueAnimator.ofInt(params.x, x).setDuration(500);
                    v.addUpdateListener(valueAnimator -> {
                        params.x = (int) valueAnimator.getAnimatedValue();
                    });
                    v.start();
                }
            }

            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                callBackEnd.onFinish();

                if (init_w == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                }
                mWindowManager.updateViewLayout(mView, params);
            }
        });
        if (w != 0) {
            width_anim.setInterpolator(new OvershootInterpolator(1f));
            height_anim.setInterpolator(new OvershootInterpolator(1f));
        }

        width_anim.start();
        height_anim.start();
    }

    private int minWidth;

    private void closeOverlay() {
        animateOverlay(minHeight, minWidth, false, new CallBack(), new CallBack() {
            @Override
            public void onFinish() {
                super.onFinish();
                View replace = mView.findViewById(R.id.binded);
                if (replace == null) return;
                ((ViewGroup) mView).removeView(replace);
                mView.getLayoutParams().width = minWidth;
                mView.setLayoutParams(mView.getLayoutParams());
            }
        }, false);

    }

    private void bindPlugin() {

        ConstraintLayout mainLayout;
        mainLayout = mView.findViewById(R.id.main);
        if (queued.size() <= 0) {
            if (binded_plugin != null) binded_plugin.onUnbind();
            closeOverlay();
            hideBubble();
            return;
        }//pplyBlurToMainView();
        if (binded_plugin != null && Objects.equals(queued.get(0), binded_plugin.getID())) {
            return;
        }
        if (binded_plugin != null) binded_plugin.onUnbind();
        Optional<BasePlugin> optionalBasePlugin = plugins.stream().filter(x -> x.getID().equals(queued.get(0))).findFirst();
        if (!optionalBasePlugin.isPresent()) return;
        binded_plugin = optionalBasePlugin.get();
        View view = binded_plugin.onBind();
        View replace = mView.findViewById(R.id.binded);
        ViewGroup.LayoutParams params = mView.getLayoutParams();
        if (replace == null) {
            ((ViewGroup) mView).addView(view);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(mainLayout);
            constraintSet.connect(view.getId(), ConstraintSet.TOP, mView.getId(), ConstraintSet.TOP, 0);
            constraintSet.connect(view.getId(), ConstraintSet.BOTTOM, mView.getId(), ConstraintSet.BOTTOM, 0);
            constraintSet.applyTo(mainLayout);
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            View vGap = mView.findViewById(R.id.blank_space);
            if (vGap != null) {
                ViewGroup.LayoutParams params1 = vGap.getLayoutParams();
                vGap.setMinimumWidth(gap);
                vGap.setLayoutParams(params1);
            }
            mWindowManager.updateViewLayout(mView, params);
            if (binded_plugin != null) binded_plugin.onBindComplete();
            refreshBubble();
            return;
        }
        ViewGroup parent = (ViewGroup) replace.getParent();
        parent.removeView(replace);
        parent.addView(view);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(mainLayout);
        constraintSet.connect(view.getId(), ConstraintSet.TOP, mView.getId(), ConstraintSet.TOP, 0);
        constraintSet.connect(view.getId(), ConstraintSet.BOTTOM, mView.getId(), ConstraintSet.BOTTOM, 0);
        constraintSet.applyTo(mainLayout);
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        View vGap = mView.findViewById(R.id.blank_space);
        if (vGap != null) {
            ViewGroup.LayoutParams params1 = vGap.getLayoutParams();
            vGap.setMinimumWidth(gap);
            vGap.setLayoutParams(params1);
        }
        mWindowManager.updateViewLayout(mView, params);
        if (binded_plugin != null) binded_plugin.onBindComplete();
        refreshBubble();
    }
    private void applyBlurToMainView() {
        View mainView = mView.findViewById(R.id.main);


        mainView.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {

                float radius = getResources().getDimension(R.dimen.corner_radius); // 30dp
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });


        mainView.setClipToOutline(true);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
           RenderEffect blurEffect = RenderEffect.createBlurEffect(85f, 85f, Shader.TileMode.CLAMP);
           mainView.setRenderEffect(blurEffect);
        }
    }

    private void applyBlurLite() {
        View mainView = mView.findViewById(R.id.main);


        mainView.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {

                float radius = getResources().getDimension(R.dimen.corner_radius); // 30dp
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });


        mainView.setClipToOutline(true);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            RenderEffect blurEffect = RenderEffect.createBlurEffect(85f, 85f, Shader.TileMode.CLAMP);
             mainView.setRenderEffect(blurEffect);
        }
    }



    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mView.setVisibility(View.INVISIBLE);
        } else mView.setVisibility(View.VISIBLE);
        refreshBubble();
    }

    // ------------------------------------------------------------ smooth animation

    /**
     * Resizes the island with a single animator that updates the window once per
     * frame (the classic path runs separate height, width and x animators, each
     * forcing its own window relayout). A hardware layer is used while it runs.
     * Switch off with the "Smooth animations" setting to get the classic path.
     */
    private void animateOverlaySmooth(int h, int w, int initW, boolean expanded, CallBack callBackStart,
                                      CallBack callBackEnd, CallBack onChange) {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) mView.getLayoutParams();
        final int startH = params.height;
        final int startW = mView.getMeasuredWidth();
        final int startX = params.x;
        final int targetX = expanded ? 0 : x;
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(expanded ? 450 : 380);
        animator.setInterpolator(w != 0 ? new OvershootInterpolator(0.8f) : new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            float linear = a.getAnimatedFraction();
            params.height = Math.max(1, (int) (startH + (h - startH) * f));
            params.width = Math.max(1, Math.abs((int) (startW + (w - startW) * f)));
            params.x = (int) (startX + (targetX - startX) * linear);
            if (onChange != null) onChange.onChange(linear);
            if (mView.getParent() != null) mWindowManager.updateViewLayout(mView, params);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                hideBubble();
                mView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                callBackStart.onFinish();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mView.setLayerType(View.LAYER_TYPE_NONE, null);
                callBackEnd.onFinish();
                if (initW == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                }
                params.x = targetX;
                if (mView.getParent() != null) mWindowManager.updateViewLayout(mView, params);
                refreshBubble();
            }
        });
        animator.start();
    }

    // ------------------------------------------------------------ haptics

    private void haptic(int type) {
        if (mView == null || !sharedPreferences.getBoolean("haptics_enabled", true)) return;
        try {
            mView.performHapticFeedback(type, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------ island colour from plugins

    private Integer colorOverride = null;
    private boolean imageBackground = false;

    /** Temporarily colours the island (e.g. from the album art). Cleared on unbind. */
    public void setIslandColorOverride(int c) {
        colorOverride = c;
        applyIslandColor();
    }

    public void clearIslandColorOverride() {
        if (colorOverride == null) return;
        colorOverride = null;
        applyIslandColor();
    }

    private int currentIslandColor() {
        return colorOverride != null ? colorOverride : color;
    }

    private void applyIslandColor() {
        if (mView == null || imageBackground) return;
        int c = currentIslandColor();
        textColor = isColorDark(c) ? getColor(R.color.white) : getColor(R.color.black);
        mView.setBackgroundTintList(ColorStateList.valueOf(c));
        View main = mView.findViewById(R.id.main);
        if (main != null) main.setBackgroundTintList(ColorStateList.valueOf(c));
        if (bubbleView != null) bubbleView.setBackgroundTintList(ColorStateList.valueOf(c));
        if (binded_plugin != null) {
            try {
                binded_plugin.onTextColorChange();
            } catch (Throwable ignored) {
            }
        }
    }

    // ------------------------------------------------------------ second activity bubble

    private FrameLayout bubbleView;
    private ImageView bubbleIcon;
    private WindowManager.LayoutParams bubbleParams;
    private String bubblePluginId;

    /** Puts a plugin in front of the others (bubble tap, or an incoming call). */
    public void promote(BasePlugin plugin) {
        queued.remove(plugin.getID());
        queued.add(0, plugin.getID());
        bindPlugin();
    }

    /** Plugins call this when the icon they'd show in the bubble changes. */
    public void refreshBubble() {
        mHandler.removeCallbacks(bubbleUpdater);
        mHandler.postDelayed(bubbleUpdater, 60);
    }

    private final Runnable bubbleUpdater = this::updateBubble;

    private boolean islandExpanded() {
        if (mView == null) return false;
        return mView.getWidth() > metrics.widthPixels * 0.6f || mView.getHeight() > minHeight * 1.5f;
    }

    private void updateBubble() {
        try {
            if (mView == null || mView.getVisibility() != View.VISIBLE || islandExpanded()
                    || !sharedPreferences.getBoolean("split_bubble", true) || queued.size() < 2) {
                hideBubble();
                return;
            }
            Drawable icon = null;
            String id = null;
            for (int i = 1; i < queued.size() && icon == null; i++) {
                final String qid = queued.get(i);
                Optional<BasePlugin> p = plugins.stream().filter(pl -> pl.getID().equals(qid)).findFirst();
                if (p.isPresent()) {
                    icon = p.get().getMiniIcon();
                    id = qid;
                }
            }
            if (icon == null) {
                hideBubble();
                return;
            }
            ensureBubble();
            bubbleIcon.setImageDrawable(icon);
            bubblePluginId = id;
            positionBubble();
            bubbleView.setVisibility(View.VISIBLE);
        } catch (Throwable t) {
            Log.w("OverlayService", "Bubble update failed", t);
            hideBubble();
        }
    }

    private void ensureBubble() {
        if (bubbleView != null) return;
        bubbleView = new FrameLayout(this);
        bubbleView.setBackgroundResource(R.drawable.rounded_corner);
        bubbleView.setBackgroundTintList(ColorStateList.valueOf(currentIslandColor()));
        bubbleIcon = new ImageView(this);
        bubbleIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bubbleIcon.setClipToOutline(true);
        bubbleIcon.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        int pad = dpToInt(6);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.setMargins(pad, pad, pad, pad);
        bubbleView.addView(bubbleIcon, lp);
        bubbleView.setOnClickListener(v -> {
            haptic(HapticFeedbackConstants.VIRTUAL_KEY);
            final String id = bubblePluginId;
            if (id == null) return;
            plugins.stream().filter(pl -> pl.getID().equals(id)).findFirst().ifPresent(this::promote);
        });
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        bubbleParams = getParams(minHeight, minHeight, flags);
        bubbleParams.gravity = Gravity.TOP | Gravity.CENTER;
        bubbleParams.y = y;
        bubbleView.setVisibility(View.GONE);
        mWindowManager.addView(bubbleView, bubbleParams);
    }

    private void positionBubble() {
        WindowManager.LayoutParams p = (WindowManager.LayoutParams) mView.getLayoutParams();
        int islandCenter = p.x;
        int half = mView.getWidth() / 2;
        int size = minHeight;
        int offset = half + dpToInt(6) + size / 2;
        int right = islandCenter + offset;
        // Put it on the left if it would run off the right edge.
        bubbleParams.x = (right + size / 2 > metrics.widthPixels / 2) ? islandCenter - offset : right;
        bubbleParams.y = p.y + (mView.getHeight() - size) / 2;
        bubbleParams.width = size;
        bubbleParams.height = size;
        mWindowManager.updateViewLayout(bubbleView, bubbleParams);
    }

    private void hideBubble() {
        if (bubbleView != null) bubbleView.setVisibility(View.GONE);
    }

    private void removeBubble() {
        try {
            if (bubbleView != null && bubbleView.getParent() != null) mWindowManager.removeViewImmediate(bubbleView);
        } catch (Throwable ignored) {
        }
        bubbleView = null;
        bubbleIcon = null;
    }

    public int dpToInt(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        removeBubble();
        mWindowManager.removeView(mView);
        plugins.forEach(BasePlugin::onDestroy);
        Runtime.getRuntime().exit(0);
    }

    public final Handler mHandler = new Handler();


    private View mView;

    public WindowManager mWindowManager;


}
