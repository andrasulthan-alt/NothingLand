package com.theglitchh.NothingLand.plugins;

import com.theglitchh.NothingLand.plugins.BatteryPlugin.BatteryPlugin;
import com.theglitchh.NothingLand.plugins.Cards.CardsPlugin;
import com.theglitchh.NothingLand.plugins.SystemEvents.SystemEventsPlugin;
import com.theglitchh.NothingLand.plugins.LiveActivity.LiveActivityPlugin;
import com.theglitchh.NothingLand.plugins.MediaSession.MediaSessionPlugin;
import com.theglitchh.NothingLand.plugins.Notification.NotificationPlugin;

import java.util.ArrayList;

public class ExportedPlugins {
    /** Order = priority: a plugin higher in this list takes over the island from one below it. */
    public static ArrayList<BasePlugin> getPlugins() {
        ArrayList<BasePlugin> plugins = new ArrayList<>();
        plugins.add(new LiveActivityPlugin());
        plugins.add(new CardsPlugin());
        plugins.add(new SystemEventsPlugin());
        plugins.add(new MediaSessionPlugin());
        plugins.add(new NotificationPlugin());
        plugins.add(new BatteryPlugin());
        return plugins;
    }
}
