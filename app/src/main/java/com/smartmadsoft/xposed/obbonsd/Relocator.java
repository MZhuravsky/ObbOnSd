package com.smartmadsoft.xposed.obbonsd;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Relocator implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    public static final String PACKAGE_NAME = Relocator.class.getPackage().getName();
    private static XSharedPreferences prefs;

    public static final boolean DEBUG = false;
    public static final String TAG = "ObbOnSd";

    String namespace;
    String realInternal;
    String realExternal;

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        prefs = new XSharedPreferences(PACKAGE_NAME);
        prefs.makeWorldReadable();
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        log("Module is started", true);

        if (prefs.getBoolean("enable_playstorehooks", false))
            if (lpparam.packageName.equals("com.android.providers.downloads.ui") || lpparam.packageName.equals("com.android.vending")) {

                setPaths();

                if (realExternal == null)
                    return;

                XposedHelpers.findAndHookConstructor("java.io.File", lpparam.classLoader, String.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args[0].toString().startsWith(realExternal))
                            return;
                        if (param.args[0].toString().endsWith(".obb"))
                            if (isObbOnSd(getPkgFromFullPath(param.args[0].toString())))
                                param.args[0] = param.args[0].toString().replaceFirst("^" + realInternal, realExternal);
                    }
                });

                XposedHelpers.findAndHookConstructor("java.io.File", lpparam.classLoader, String.class, String.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args[0].toString().startsWith(realExternal))
                            return;
                        if (param.args[1].toString().endsWith(".obb"))
                            if (isObbOnSd(getPkgFromPath(param.args[0].toString())))
                                param.args[0] = param.args[0].toString().replaceFirst("^" + realInternal, realExternal);
                    }
                });

                XposedBridge.hookAllMethods(XposedHelpers.findClass("java.io.File", lpparam.classLoader), "renameTo", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args[0].toString().startsWith(realExternal))
                            return;
                        if (param.args[0].toString().endsWith(".obb"))
                            if (isObbOnSd(getPkgFromFullPath(param.args[0].toString())))
                                param.args[0] = new File(param.args[0].toString().replaceFirst("^" + realInternal, realExternal));
                    }
                });
                return;
            }

        if (isExcludedPackage(lpparam.packageName))
            return;

        setPaths();

        //log(realInternal + " & " + realExternal);

        if (realExternal == null)
            return;

        namespace = lpparam.packageName;

        if (!isObbOnSd(namespace) && !isDataOnSd(namespace))
            return;

        log(namespace + " hooking");

        XposedBridge.hookAllMethods(XposedHelpers.findClass("android.os.Environment", lpparam.classLoader), "getExternalStorageDirectory", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(new File(realExternal));
            }
        });

        XposedBridge.hookAllMethods(XposedHelpers.findClass("android.app.ContextImpl", lpparam.classLoader), "getObbDir", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                File dir = (File) param.getResult();
                String path = dir.getPath().replaceFirst("^" + realInternal, realExternal);
                param.setResult(new File(path));
            }
        });

        XposedBridge.hookAllMethods(XposedHelpers.findClass("android.app.ContextImpl", lpparam.classLoader), "getExternalFilesDir", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                File file = (File) param.getResult();
                String path = file.getPath().replaceFirst("^" + realInternal, realExternal);
                param.setResult(new File(path));
            }
        });

        log(namespace + " hooked");
    }

    void log(String text) {
        log(text, false);
    }

    void log(String text, boolean force) {
        if (DEBUG || force) {
            XposedBridge.log("[" + TAG + "] " + text);
            Log.d(TAG, text);
        }
    }

    boolean containsFile(File directory) {
        for (File file : directory.listFiles()) {
            if (file.isFile())
                return true;
            else if (containsFile(file))
                return true;
        }
        return false;
    }

    boolean isExcludedPackage(String packageName) {

        if (packageName.equals("android") ||
                packageName.startsWith("com.android.") ||
                (packageName.startsWith("com.google.") && !(packageName.equals("com.google.android.apps.translate"))))
            return true;

        if (packageName.startsWith("com.samsung.") ||
                packageName.startsWith("com.sec.") ||
                packageName.startsWith("com.smartmadsoft."))
            return true;

        ArrayList<String> excluded = new ArrayList<String>();
        // these apps natively support extSdCard
        excluded.add("cz.seznam.mapy");
        excluded.add("com.skobbler.forevermapng");
        excluded.add("com.mapswithme.maps.pro");

        if (excluded.contains(packageName))
            return true;

        return false;
    }

    boolean isObbOnSd(String packageName) {
        File obbDir = new File(realExternal + "/Android/obb/" + packageName + "/");
        if (obbDir.isDirectory()) {
            File files[] = obbDir.listFiles();
            for (File file : files) {
                if (file.getName().endsWith(packageName + ".obb")) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean isDataOnSd(String packageName) {
        if (!prefs.getBoolean("enable_dataonsd", false))
            return false;

        File dataDir = new File(realExternal + "/Android/data/" + packageName + "/");
        if (dataDir.isDirectory()) {
            if (containsFile(new File(dataDir.getPath())))
                return true;
        }
        return false;
    }

    String getPkgFromPath(String path) {
        int start = path.lastIndexOf(File.separator, path.length()-1);
        return path.substring(start).replace(File.separator, "");
    }

    String getPkgFromFullPath(String path) {
        int end = path.lastIndexOf(File.separator);
        int start = path.lastIndexOf(File.separator, end-1);
        return path.substring(start + 1, end);
    }

    void setPaths() {
        realInternal = prefs.getString("path_internal", Environment.getExternalStorageDirectory().getPath());
        if (prefs.getBoolean("enable_alternative", false)) {
            realExternal = prefs.getString("path", null);
        } else {
            String env = System.getenv("SECONDARY_STORAGE");
            if (env == null)
                realExternal = null;
            else
                realExternal = env.split(":")[0];
        }
    }
}
