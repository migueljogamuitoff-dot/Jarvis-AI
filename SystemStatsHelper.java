package com.seuprojeto.nexus;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.SystemClock;

public class SystemStatsHelper {

    public static int batteryPercent(Context context) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent battery = context.registerReceiver(null, filter);
        if (battery == null) return -1;
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level < 0 || scale <= 0) return -1;
        return Math.round(level * 100f / scale);
    }

    public static String ramUsage(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        long usedMb = (mi.totalMem - mi.availMem) / (1024 * 1024);
        long totalMb = mi.totalMem / (1024 * 1024);
        return usedMb + "/" + totalMb + " MB";
    }

    public static String uptime(long startElapsedRealtime) {
        long diff = SystemClock.elapsedRealtime() - startElapsedRealtime;
        long h = diff / 3600000;
        long m = (diff / 60000) % 60;
        long s = (diff / 1000) % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
