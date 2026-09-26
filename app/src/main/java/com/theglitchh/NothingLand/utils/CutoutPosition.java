package com.theglitchh.NothingLand.utils;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.TypedValue;
import android.view.DisplayCutout;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;

/**
 * Works out a default pill position centred on the front-camera cutout, so the
 * island lines up with the camera on any phone without manual adjustment.
 * Only used while the user has not set a position themselves.
 */
public final class CutoutPosition {
    /** Old defaults, tuned for Nothing phones. Used when no cutout is found. */
    public static final float LEGACY_X = 0f;
    public static final float LEGACY_Y = 0.67f;

    private CutoutPosition() {
    }

    /**
     * @param pillHeightDp height of the collapsed pill, in dp
     * @return {xPercent, yPercent} in the same units as the overlay_x / overlay_y
     * settings, or null when no top cutout can be detected (Android 10 and older,
     * phones without a punch hole, or landscape).
     */
    public static float[] compute(Context context, float pillHeightDp) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null;
        try {
            WindowManager wm = context.getSystemService(WindowManager.class);
            if (wm == null) return null;
            WindowMetrics metrics = wm.getCurrentWindowMetrics();
            WindowInsets insets = metrics.getWindowInsets();
            DisplayCutout cutout = insets.getDisplayCutout();
            if (cutout == null) return null;
            Rect camera = cutout.getBoundingRectTop();
            if (camera == null || camera.isEmpty()) return null;
            Rect screen = metrics.getBounds();
            if (screen.width() <= 0 || screen.height() <= 0) return null;

            float pillHeightPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    pillHeightDp, context.getResources().getDisplayMetrics());
            // The pill is centred horizontally (Gravity.CENTER), so x is an offset from the middle.
            float xPx = camera.exactCenterX() - screen.width() / 2f;
            // Put the pill's vertical centre on the camera's centre.
            float yPx = Math.max(0f, camera.exactCenterY() - pillHeightPx / 2f);

            float xPercent = clamp(xPx / screen.width() * 100f, -50f, 50f);
            float yPercent = clamp(yPx / screen.height() * 100f, 0f, 100f);
            return new float[]{xPercent, yPercent};
        } catch (Throwable t) {
            return null;
        }
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
