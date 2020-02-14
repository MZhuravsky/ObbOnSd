package com.smartmadsoft.xposed.obbonsd.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.smartmadsoft.xposed.obbonsd.Relocator;

public class Prefs {

    @Nullable
    private static Context getModuleContext(@NonNull Context hookContext) {
        try {
            return hookContext.createPackageContext(Relocator.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
        } catch (Exception e) {
            Relocator.log("Cannot load module context");
            Relocator.log(e.getMessage());
        }
        return null;
    }

    public static boolean getHookedBool(@NonNull Context hookedContext, @NonNull String key, boolean def) {
        try {
            Context context = getModuleContext(hookedContext);
            return context != null
                    ? getBool(context, key, def)
                    : def;
        } catch (Throwable t) {
            Relocator.log("Cannot load preferences");
            Relocator.log(t.getMessage());
            return false;
        }
    }

    public static long getHookedLong(@NonNull Context hookedContext, @NonNull String key, long def) {
        try {
            Context context = getModuleContext(hookedContext);
            return context != null
                    ? getLong(context, key, def)
                    : def;
        } catch (Throwable t) {
            Relocator.log("Cannot load preferences");
            Relocator.log(t.getMessage());
            return def;
        }
    }

    @Nullable
    public static String getHookedString(@NonNull Context hookedContext, @NonNull String key, @Nullable String def) {
        try {
            Context context = getModuleContext(hookedContext);
            return context != null
                    ? getString(context, key, def)
                    : def;
        } catch (Throwable t) {
            Relocator.log("Cannot load preferences");
            Relocator.log(t.getMessage());
            return def;
        }
    }

    private static MultiprocessPreferences.MultiprocessSharedPreferences getDef(@NonNull Context context) {
        return MultiprocessPreferences.getDefaultSharedPreferences(context);
    }

    public static boolean getBool(@NonNull Context context, @NonNull String key, boolean def) {
        return getDef(context).getBoolean(key, def);
    }

    public static void setBool(@NonNull Context context, @NonNull String key, boolean value) {
        getDef(context).edit().putBoolean(key, value).apply();
    }

    public static long getLong(@NonNull Context context, @NonNull String key, long def) {
        return getDef(context).getLong(key, def);
    }

    public static void setLong(@NonNull Context context, @NonNull String key, long value) {
        getDef(context).edit().putLong(key, value).apply();
    }

    public static void setString(@NonNull Context context, @NonNull String key, @Nullable String value) {
        getDef(context).edit().putString(key, value).apply();
    }

    @Nullable
    public static String getString(@NonNull Context context, @NonNull String key, @Nullable String def) {
        return getDef(context).getString(key, def);
    }

}
