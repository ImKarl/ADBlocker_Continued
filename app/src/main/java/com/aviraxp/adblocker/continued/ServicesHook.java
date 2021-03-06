package com.aviraxp.adblocker.continued;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class ServicesHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private Set<String> patterns;
    private boolean isMIUI = false;

    private void isMIUI() {
        Properties properties = new Properties();
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(new File(Environment.getRootDirectory(), "build.prop"));
            properties.load(fileInputStream);
            if (properties.getProperty("ro.miui.ui.version.name") != null || properties.getProperty("ro.miui.ui.version.code") != null || properties.getProperty("ro.miui.internal.storage") != null) {
                XposedBridge.log("MIUI Detected, Never Block MiPush");
                isMIUI = true;
            }
        } catch (Exception e) {
            XposedBridge.log("Load System Property Failed, Printing StackTrace");
            XposedBridge.log(e);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("android")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                XposedHelpers.findAndHookMethod("com.android.server.am.ActiveServices", lpparam.classLoader, "startServiceLocked", "android.app.IApplicationThread", Intent.class, String.class, int.class, int.class, String.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Intent intent = (Intent) param.args[1];
                        handleServiceStart(param, intent);
                        if (BuildConfig.DEBUG) {
                            XposedBridge.log("Hook Services Flag Success: " + Build.VERSION.SDK_INT);
                        }
                    }
                });
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                try {
                    XposedHelpers.findAndHookMethod("com.android.server.am.ActiveServices", lpparam.classLoader, "startServiceLocked", "android.app.IApplicationThread", Intent.class, String.class, int.class, int.class, int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Intent intent = (Intent) param.args[1];
                            handleServiceStart(param, intent);
                            if (BuildConfig.DEBUG) {
                                XposedBridge.log("Hook Services Flag Success: " + Build.VERSION.SDK_INT);
                            }
                        }
                    });
                } catch (NoSuchMethodError e) {
                    XposedHelpers.findAndHookMethod("com.android.server.am.ActiveServices", lpparam.classLoader, "startServiceLocked", "android.app.IApplicationThread", Intent.class, String.class, int.class, int.class, int.class, Context.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Intent intent = (Intent) param.args[1];
                            handleServiceStart(param, intent);
                            if (BuildConfig.DEBUG) {
                                XposedBridge.log("Hook Services Flag Success: " + Build.VERSION.SDK_INT);
                            }
                        }
                    });
                }
            } else {
                XposedHelpers.findAndHookMethod("com.android.server.am.ActivityManagerService", lpparam.classLoader, "startServiceLocked", "android.app.IApplicationThread", Intent.class, String.class, int.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Intent intent = (Intent) param.args[1];
                        handleServiceStart(param, intent);
                        if (BuildConfig.DEBUG) {
                            XposedBridge.log("Hook Services Flags Success: " + Build.VERSION.SDK_INT);
                        }
                    }
                });
            }
        }
    }

    private void handleServiceStart(XC_MethodHook.MethodHookParam param, Intent serviceIntent) {
        if (serviceIntent != null && serviceIntent.getComponent() != null) {
            String serviceName = serviceIntent.getComponent().flattenToShortString();
            if (serviceName != null) {
                String splitServicesName = serviceName.substring(serviceName.indexOf("/") + 1);
                if ((!isMIUI && patterns.contains(splitServicesName)) || (isMIUI && patterns.contains(splitServicesName) && !splitServicesName.contains("xiaomi"))) {
                    param.setResult(null);
                    if (BuildConfig.DEBUG) {
                        XposedBridge.log("Service Block Success: " + serviceName);
                    }
                }
            }
        }
    }

    public void initZygote(StartupParam startupParam) throws Throwable {
        String MODULE_PATH = startupParam.modulePath;
        Resources res = XModuleResources.createInstance(MODULE_PATH, null);
        byte[] array = XposedHelpers.assetAsByteArray(res, "blocklist/services");
        String decoded = new String(array);
        String[] sUrls = decoded.split("\n");
        patterns = new HashSet<>();
        Collections.addAll(patterns, sUrls);
        isMIUI();
    }
}