/*
 * Created by wangzhuozhou on 2015/08/01.
 * Copyright 2015－2021 Sensors Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sensorsdata.analytics.android.sdk.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;

import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.ScreenAutoTracker;
import com.sensorsdata.analytics.android.sdk.SensorsDataAutoTrackAppViewScreenUrl;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SensorsDataUtils {

    private static final String marshmallowMacAddress = "02:00:00:00:00:00";

    private static final String SHARED_PREF_EDITS_FILE = "sensorsdata";
    private static final String SHARED_PREF_USER_AGENT_KEY = "sensorsdata.user.agent";
    private static final String SHARED_PREF_APP_VERSION = "sensorsdata.app.version";
    private static final String SHARED_PREF_DEVICE_ID_KEY = "sensorsdata.device.id";

    public static final String COMMAND_HARMONYOS_VERSION = "getprop hw_sc.build.platform.version";

    private static final Set<String> mPermissionGrantedSet = new HashSet<>();
    private static final Map<String, String> deviceUniqueIdentifiersMap = new HashMap<>();

    private static boolean isAndroidIDEnabled = true;
    private static boolean isOAIDEnabled = true;
    private static boolean isUniApp = false;

    private static final List<String> mInvalidAndroidId = new ArrayList<String>() {
        {
            add("9774d56d682e549c");
            add("0123456789abcdef");
        }
    };
    private static final String TAG = "SA.SensorsDataUtils";

    /**
     * 获取 Activity 的 title
     *
     * @param activity Activity
     * @return Activity 的 title
     */
    public static String getActivityTitle(Activity activity) {
        try {
            if (activity != null) {
                try {
                    String activityTitle = null;

                    if (Build.VERSION.SDK_INT >= 11) {
                        String toolbarTitle = SensorsDataUtils.getToolbarTitle(activity);
                        if (!TextUtils.isEmpty(toolbarTitle)) {
                            activityTitle = toolbarTitle;
                        }
                    }

                    if (TextUtils.isEmpty(activityTitle)) {
                        activityTitle = activity.getTitle().toString();
                    }

                    if (TextUtils.isEmpty(activityTitle)) {
                        PackageManager packageManager = activity.getPackageManager();
                        if (packageManager != null) {
                            ActivityInfo activityInfo = packageManager.getActivityInfo(activity.getComponentName(), 0);
                            if (!TextUtils.isEmpty(activityInfo.loadLabel(packageManager))) {
                                activityTitle = activityInfo.loadLabel(packageManager).toString();
                            }
                        }
                    }

                    return activityTitle;
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        } catch (Exception e) {
            SALog.printStackTrace(e);
            return null;
        }
    }



    private static String getCarrierFromJsonObject(JSONObject jsonObject, String mccMnc) {
        if (jsonObject == null || TextUtils.isEmpty(mccMnc)) {
            return null;
        }
        return jsonObject.optString(mccMnc);

    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return SASpUtils.getSharedPreferences(context, SHARED_PREF_EDITS_FILE, Context.MODE_PRIVATE);
    }

    static String getToolbarTitle(Activity activity) {
        try {
            if ("com.tencent.connect.common.AssistActivity".equals(activity.getClass().getCanonicalName())) {
                if (!TextUtils.isEmpty(activity.getTitle())) {
                    return activity.getTitle().toString();
                }
                return null;
            }
            ActionBar actionBar = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                actionBar = activity.getActionBar();
            }
            if (actionBar != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    if (!TextUtils.isEmpty(actionBar.getTitle())) {
                        return actionBar.getTitle().toString();
                    }
                }
            } else {
                try {
                    Class<?> appCompatActivityClass = compatActivity();
                    if (appCompatActivityClass != null && appCompatActivityClass.isInstance(activity)) {
                        Method method = activity.getClass().getMethod("getSupportActionBar");
                        Object supportActionBar = method.invoke(activity);
                        if (supportActionBar != null) {
                            method = supportActionBar.getClass().getMethod("getTitle");
                            CharSequence charSequence = (CharSequence) method.invoke(supportActionBar);
                            if (charSequence != null) {
                                return charSequence.toString();
                            }
                        }
                    }
                } catch (Exception e) {
                    //ignored
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    private static Class<?> compatActivity() {
        Class<?> appCompatActivityClass = null;
        try {
            appCompatActivityClass = Class.forName("android.support.v7.app.AppCompatActivity");
        } catch (Exception e) {
            //ignored
        }
        if (appCompatActivityClass == null) {
            try {
                appCompatActivityClass = Class.forName("androidx.appcompat.app.AppCompatActivity");
            } catch (Exception e) {
                //ignored
            }
        }
        return appCompatActivityClass;
    }

    /**
     * 尝试读取页面 title
     *
     * @param properties JSONObject
     * @param activity Activity
     */
    public static void getScreenNameAndTitleFromActivity(JSONObject properties, Activity activity) {
        if (activity == null || properties == null) {
            return;
        }

        try {
            properties.put("$screen_name", activity.getClass().getCanonicalName());

            String activityTitle = null;
            if (!TextUtils.isEmpty(activity.getTitle())) {
                activityTitle = activity.getTitle().toString();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                String toolbarTitle = getToolbarTitle(activity);
                if (!TextUtils.isEmpty(toolbarTitle)) {
                    activityTitle = toolbarTitle;
                }
            }

            if (TextUtils.isEmpty(activityTitle)) {
                PackageManager packageManager = activity.getPackageManager();
                if (packageManager != null) {
                    ActivityInfo activityInfo = packageManager.getActivityInfo(activity.getComponentName(), 0);
                    activityTitle = activityInfo.loadLabel(packageManager).toString();
                }
            }
            if (!TextUtils.isEmpty(activityTitle)) {
                properties.put("$title", activityTitle);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public static void mergeJSONObject(final JSONObject source, JSONObject dest) {
        try {
            Iterator<String> superPropertiesIterator = source.keys();

            while (superPropertiesIterator.hasNext()) {
                String key = superPropertiesIterator.next();
                Object value = source.get(key);
                if (value instanceof Date && !"$time".equals(key)) {
                    dest.put(key, TimeUtils.formatDate((Date) value, Locale.CHINA));
                } else {
                    dest.put(key, value);
                }
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    /**
     * 合并、去重公共属性
     *
     * @param source 新加入或者优先级高的属性
     * @param dest 本地缓存或者优先级低的属性，如果有重复会删除该属性
     * @return 合并后的属性
     */
    public static JSONObject mergeSuperJSONObject(JSONObject source, JSONObject dest) {
        if (source == null) {
            source = new JSONObject();
        }
        if (dest == null) {
            return source;
        }

        try {
            Iterator<String> sourceIterator = source.keys();
            while (sourceIterator.hasNext()) {
                String key = sourceIterator.next();
                Iterator<String> destIterator = dest.keys();
                while (destIterator.hasNext()) {
                    String destKey = destIterator.next();
                    if (!TextUtils.isEmpty(key) && key.equalsIgnoreCase(destKey)) {
                        destIterator.remove();
                    }
                }
            }
            //重新遍历赋值，如果在同一次遍历中赋值会导致同一个 json 中大小写不一样的 key 被删除
            mergeJSONObject(source, dest);
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return dest;
    }

    /**
     * 检测权限
     *
     * @param context Context
     * @param permission 权限名称
     * @return true:已允许该权限; false:没有允许该权限
     */
    public static boolean checkHasPermission(Context context, String permission) {
        try {
            if (mPermissionGrantedSet.contains(permission)) {
                return true;
            }
            Class<?> contextCompat = null;
            try {
                contextCompat = Class.forName("android.support.v4.content.ContextCompat");
            } catch (Exception e) {
                //ignored
            }

            if (contextCompat == null) {
                try {
                    contextCompat = Class.forName("androidx.core.content.ContextCompat");
                } catch (Exception e) {
                    //ignored
                }
            }

            if (contextCompat == null) {
                mPermissionGrantedSet.add(permission);
                return true;
            }

            Method checkSelfPermissionMethod = contextCompat.getMethod("checkSelfPermission", Context.class, String.class);
            int result = (int) checkSelfPermissionMethod.invoke(null, new Object[]{context, permission});
            if (result != PackageManager.PERMISSION_GRANTED) {
                SALog.i(TAG, "You can fix this by adding the following to your AndroidManifest.xml file:\n"
                        + "<uses-permission android:name=\"" + permission + "\" />");
                return false;
            }
            mPermissionGrantedSet.add(permission);
            return true;
        } catch (Exception e) {
            SALog.i(TAG, e.toString());
            return true;
        }
    }

    /**
     * 此方法谨慎修改
     * 插件配置 disableAndroidID 会修改此方法
     * 获取 Android ID
     *
     * @param context Context
     * @return androidID
     */
    @SuppressLint("HardwareIds")
    public static String getAndroidID(Context context) {
        String androidID = "";
        try {
            // 新增判断逻辑，是否开启 AndroidId 采集
            if (!isAndroidIDEnabled) {
                SALog.i(TAG, "SensorsData getAndroidID is disabled");
                return "";
            }
            androidID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return androidID;
    }

    public static boolean isValidAndroidId(String androidId) {
        if (TextUtils.isEmpty(androidId)) {
            return false;
        }

        return !mInvalidAndroidId.contains(androidId.toLowerCase(Locale.getDefault()));
    }

    /**
     * 检查版本是否经过升级
     *
     * @param context context
     * @param currVersion 当前 SDK 版本
     * @return true，老版本升级到新版本。false，当前已是最新版本
     */
    public static boolean checkVersionIsNew(Context context, String currVersion) {
        try {
            SharedPreferences appVersionPref = getSharedPreferences(context);
            String localVersion = appVersionPref.getString(SHARED_PREF_APP_VERSION, "");

            if (!TextUtils.isEmpty(currVersion) && !currVersion.equals(localVersion)) {
                appVersionPref.edit().putString(SHARED_PREF_APP_VERSION, currVersion).apply();
                return true;
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
            return true;
        }
        return false;
    }

    /**
     * 是否是连续点击
     *
     * @param view view
     * @return Boolean
     */
    public static boolean isDoubleClick(View view) {
        if (view == null) {
            return false;
        }
        try {
            long currentOnClickTimestamp = SystemClock.elapsedRealtime();
            String tag = (String) view.getTag(R.id.sensors_analytics_tag_view_onclick_timestamp);
            if (!TextUtils.isEmpty(tag)) {
                long lastOnClickTimestamp = Long.parseLong(tag);
                if ((currentOnClickTimestamp - lastOnClickTimestamp) < 500) {
                    return true;
                }
            }
            view.setTag(R.id.sensors_analytics_tag_view_onclick_timestamp, String.valueOf(currentOnClickTimestamp));
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return false;
    }

    /**
     * 获取 ScreenUrl
     *
     * @param object activity/fragment
     * @return screenUrl
     */
    public static String getScreenUrl(Object object) {
        if (object == null) {
            return null;
        }
        String screenUrl = null;
        try {
            if (object instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) object;
                screenUrl = screenAutoTracker.getScreenUrl();
            } else {
                SensorsDataAutoTrackAppViewScreenUrl autoTrackAppViewScreenUrl = object.getClass().getAnnotation(SensorsDataAutoTrackAppViewScreenUrl.class);
                if (autoTrackAppViewScreenUrl != null) {
                    screenUrl = autoTrackAppViewScreenUrl.url();
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        if (screenUrl == null) {
            screenUrl = object.getClass().getCanonicalName();
        }
        return screenUrl;
    }

    /**
     * 解析 Activity 的 Intent 中是否包含 DebugMode、点击图、可视化全埋点的 uri 信息并显示 Dialog。
     * 此方法用来辅助完善 Dialog 的展示，通常用在配置了神策 scheme 的 Activity 页面中的 onNewIntent 方法中，
     * 并且此 Activity 的 launchMode 为 singleTop 或者 singleTask 或者为 singleInstance。
     *
     * @param activity activity
     * @param intent intent
     */
    public static void handleSchemeUrl(Activity activity, Intent intent) {
        SASchemeHelper.handleSchemeUrl(activity, intent);
    }

    public static void initUniAppStatus() {
        try {
            Class.forName("io.dcloud.application.DCloudApplication");
            isUniApp = true;
        } catch (ClassNotFoundException e) {
            // ignore
        }
    }

    public static boolean isUniApp() {
        return isUniApp;
    }

    /**
     * 是否开启 AndroidID 采集
     *
     * @param enabled true 开启，false 关闭
     */
    public static void enableAndroidId(boolean enabled) {
        isAndroidIDEnabled = enabled;
    }
    /**
     * 是否开启 OAID 采集
     *
     * @param enabled true 开启，false 关闭
     */
    public static void enableOAID(boolean enabled) {
        isOAIDEnabled = enabled;
    }

    public static boolean isOAIDEnabled() {
        return isOAIDEnabled;
    }
}
