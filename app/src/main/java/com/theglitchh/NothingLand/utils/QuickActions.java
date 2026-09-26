package com.theglitchh.NothingLand.utils;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

/**
 * Actions that can be attached to gestures on the idle island (tap, double tap,
 * long press, swipes). Stored in settings as short ids; "app:<package>" opens an app.
 */
public final class QuickActions {
    public static final String NONE = "none";
    public static final String FLASHLIGHT = "flashlight";
    public static final String CAMERA = "camera";
    public static final String SLIDERS = "sliders";
    public static final String APPS = "apps";
    public static final String HOME = "home";
    public static final String BACK = "back";
    public static final String RECENTS = "recents";
    public static final String NOTIFICATIONS = "notifications";
    public static final String QUICK_SETTINGS = "quick_settings";
    public static final String SCREENSHOT = "screenshot";
    public static final String LOCK = "lock";
    public static final String OPEN_APP = "app";
    public static final String APP_PREFIX = "app:";

    /** Ids and labels shown in the gesture settings, in the same order. */
    public static final String[] IDS = {NONE, FLASHLIGHT, CAMERA, SLIDERS, APPS, HOME, BACK, RECENTS,
            NOTIFICATIONS, QUICK_SETTINGS, SCREENSHOT, LOCK, OPEN_APP};
    public static final String[] LABELS = {"Nothing", "Flashlight on/off", "Open camera",
            "Sliders card (brightness, volume, flashlight)", "Favorite apps card", "Go home", "Back",
            "Recent apps", "Open notifications", "Open quick settings", "Take screenshot", "Lock screen",
            "Open an app…"};

    private QuickActions() {
    }

    /** Human readable label for a stored action id. */
    public static String label(Context context, String action) {
        if (action == null) return LABELS[0];
        if (action.startsWith(APP_PREFIX)) {
            String pkg = action.substring(APP_PREFIX.length());
            try {
                ApplicationInfo info = context.getPackageManager().getApplicationInfo(pkg, 0);
                return "Open " + context.getPackageManager().getApplicationLabel(info);
            } catch (Exception e) {
                return "Open " + pkg;
            }
        }
        for (int i = 0; i < IDS.length; i++) {
            if (IDS[i].equals(action)) return LABELS[i];
        }
        return LABELS[0];
    }

    /**
     * Runs an action. Returns false when nothing was done (action "none" or unknown),
     * so the caller can fall back to its default behaviour.
     */
    public static boolean run(AccessibilityService service, String action, CardOpener cards) {
        if (action == null || NONE.equals(action)) return false;
        try {
            if (action.startsWith(APP_PREFIX)) {
                return launchApp(service, action.substring(APP_PREFIX.length()));
            }
            switch (action) {
                case FLASHLIGHT:
                    toggleTorch(service);
                    return true;
                case CAMERA:
                    Intent camera = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
                    camera.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    service.startActivity(camera);
                    return true;
                case SLIDERS:
                case APPS:
                    if (cards != null) cards.openCard(action);
                    return true;
                case HOME:
                    return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
                case BACK:
                    return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                case RECENTS:
                    return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
                case NOTIFICATIONS:
                    return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
                case QUICK_SETTINGS:
                    return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS);
                case SCREENSHOT:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT);
                    }
                    Toast.makeText(service, "Screenshot needs Android 9 or newer", Toast.LENGTH_SHORT).show();
                    return true;
                case LOCK:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN);
                    }
                    Toast.makeText(service, "Lock screen needs Android 9 or newer", Toast.LENGTH_SHORT).show();
                    return true;
                default:
                    return false;
            }
        } catch (Exception e) {
            Log.w("QuickActions", "Action failed: " + action, e);
            Toast.makeText(service, "Couldn't run that action", Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    /** Something that can open a quick card (implemented by the overlay service). */
    public interface CardOpener {
        void openCard(String card);
    }

    public static boolean launchApp(Context context, String pkg) {
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(pkg);
        if (intent == null) {
            Toast.makeText(context, "App not installed", Toast.LENGTH_SHORT).show();
            return true;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        context.startActivity(intent);
        return true;
    }

    // ---------------------------------------------------------------- flashlight

    private static boolean torchOn = false;
    private static boolean torchCallbackRegistered = false;

    /**
     * The camera whose flash we use as the flashlight. Phones with several cameras can
     * report brightness levels on only one of them, so we check them all and prefer
     * the back camera with the most brightness levels.
     */
    private static String backCameraWithFlash(CameraManager cm) throws Exception {
        String best = null;
        int bestLevels = 0;
        boolean bestIsBack = false;
        for (String id : cm.getCameraIdList()) {
            CameraCharacteristics c = cm.getCameraCharacteristics(id);
            if (!Boolean.TRUE.equals(c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE))) continue;
            Integer facing = c.get(CameraCharacteristics.LENS_FACING);
            boolean isBack = facing != null && facing == CameraCharacteristics.LENS_FACING_BACK;
            int levels = 1;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Integer max = c.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL);
                if (max != null && max > 1) levels = max;
            }
            boolean better = best == null
                    || (isBack && !bestIsBack)
                    || (isBack == bestIsBack && levels > bestLevels);
            if (better) {
                best = id;
                bestLevels = levels;
                bestIsBack = isBack;
            }
        }
        return best;
    }

    /** Current flashlight brightness level (0 when off). */
    public static int torchLevel(Context context) {
        if (!torchOn) return 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                CameraManager cm = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
                String id = backCameraWithFlash(cm);
                if (id != null) return Math.max(1, cm.getTorchStrengthLevel(id));
            } catch (Exception ignored) {
            }
        }
        return torchMaxLevel(context);
    }

    private static void watchTorch(CameraManager cm) {
        if (torchCallbackRegistered) return;
        torchCallbackRegistered = true;
        cm.registerTorchCallback(new CameraManager.TorchCallback() {
            @Override
            public void onTorchModeChanged(String cameraId, boolean enabled) {
                torchOn = enabled;
            }
        }, null);
    }

    public static boolean isTorchOn() {
        return torchOn;
    }

    public static void toggleTorch(Context context) {
        setTorch(context, !torchOn);
    }

    public static void setTorch(Context context, boolean on) {
        try {
            CameraManager cm = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String id = backCameraWithFlash(cm);
            if (id == null) {
                Toast.makeText(context, "No flashlight on this phone", Toast.LENGTH_SHORT).show();
                return;
            }
            watchTorch(cm);
            cm.setTorchMode(id, on);
            torchOn = on;
        } catch (Exception e) {
            Log.w("QuickActions", "Torch failed", e);
            Toast.makeText(context, "Flashlight is busy (camera in use?)", Toast.LENGTH_SHORT).show();
        }
    }

    /** Max flashlight brightness level, or 1 when the phone only supports on/off. */
    public static int torchMaxLevel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return 1;
        try {
            CameraManager cm = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String id = backCameraWithFlash(cm);
            if (id == null) return 1;
            Integer max = cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL);
            return max == null || max < 1 ? 1 : max;
        } catch (Exception e) {
            return 1;
        }
    }

    /** Sets flashlight brightness (level 0 = off). Falls back to on/off on older phones. */
    public static void setTorchLevel(Context context, int level) {
        if (level <= 0) {
            setTorch(context, false);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && torchMaxLevel(context) > 1) {
            try {
                CameraManager cm = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
                String id = backCameraWithFlash(cm);
                if (id != null) {
                    watchTorch(cm);
                    cm.turnOnTorchWithStrengthLevel(id, Math.min(level, torchMaxLevel(context)));
                    torchOn = true;
                    return;
                }
            } catch (Exception e) {
                Log.w("QuickActions", "Torch level failed", e);
            }
        }
        setTorch(context, true);
    }
}
