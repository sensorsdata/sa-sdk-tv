/*
 * Created by dengshiwei on 2020/10/20.
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

package com.sensorsdata.analytics.android.sdk;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.sensorsdata.analytics.android.sdk.aop.push.PushLifecycleCallbacks;
import com.sensorsdata.analytics.android.sdk.autotrack.ActivityLifecycleCallbacks;
import com.sensorsdata.analytics.android.sdk.autotrack.ActivityPageLeaveCallbacks;
import com.sensorsdata.analytics.android.sdk.autotrack.FragmentPageLeaveCallbacks;
import com.sensorsdata.analytics.android.sdk.autotrack.FragmentViewScreenCallbacks;
import com.sensorsdata.analytics.android.sdk.autotrack.aop.FragmentTrackHelper;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbAdapter;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentDistinctId;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstDay;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstStart;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstTrackInstallation;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentFirstTrackInstallationWithCallback;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentLoader;
import com.sensorsdata.analytics.android.sdk.data.persistent.PersistentSuperProperties;
import com.sensorsdata.analytics.android.sdk.deeplink.SensorsDataDeepLinkCallback;
import com.sensorsdata.analytics.android.sdk.encrypt.SensorsDataEncrypt;
import com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException;
import com.sensorsdata.analytics.android.sdk.internal.api.FragmentAPI;
import com.sensorsdata.analytics.android.sdk.internal.api.IFragmentAPI;
import com.sensorsdata.analytics.android.sdk.internal.rpc.SensorsDataContentObserver;
import com.sensorsdata.analytics.android.sdk.listener.SAEventListener;
import com.sensorsdata.analytics.android.sdk.listener.SAFunctionListener;
import com.sensorsdata.analytics.android.sdk.listener.SAJSListener;
import com.sensorsdata.analytics.android.sdk.remote.BaseSensorsDataSDKRemoteManager;
import com.sensorsdata.analytics.android.sdk.remote.SensorsDataRemoteManager;
import com.sensorsdata.analytics.android.sdk.util.AppInfoUtils;
import com.sensorsdata.analytics.android.sdk.advert.utils.ChannelUtils;
import com.sensorsdata.analytics.android.sdk.util.JSONUtils;
import com.sensorsdata.analytics.android.sdk.util.NetworkUtils;
import com.sensorsdata.analytics.android.sdk.advert.utils.OaidHelper;
import com.sensorsdata.analytics.android.sdk.util.SADataHelper;
import com.sensorsdata.analytics.android.sdk.util.SAContextManager;
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;
import com.sensorsdata.analytics.android.sdk.visual.model.ViewNode;
import com.sensorsdata.analytics.android.sdk.visual.property.VisualPropertiesManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.sensorsdata.analytics.android.sdk.util.SADataHelper.assertKey;
import static com.sensorsdata.analytics.android.sdk.util.SADataHelper.assertPropertyTypes;
import static com.sensorsdata.analytics.android.sdk.util.SADataHelper.assertValue;

abstract class AbstractSensorsDataAPI implements ISensorsDataAPI {
    protected static final String TAG = "SA.SensorsDataAPI";
    // SDK版本
    static final String VERSION = BuildConfig.SDK_VERSION;
    // Maps each token to a singleton SensorsDataAPI instance
    protected static final Map<Context, SensorsDataAPI> sInstanceMap = new HashMap<>();
    static boolean mIsMainProcess = false;
    static boolean SHOW_DEBUG_INFO_VIEW = true;
    protected static SensorsDataGPSLocation mGPSLocation;
    /* 远程配置 */
    protected static SAConfigOptions mSAConfigOptions;
    protected SAContextManager mSAContextManager;
    protected final Context mContext;
    protected AnalyticsMessages mMessages;
    protected final PersistentDistinctId mDistinctId;
    protected final PersistentSuperProperties mSuperProperties;
    protected final PersistentFirstStart mFirstStart;
    protected final PersistentFirstDay mFirstDay;
    protected final PersistentFirstTrackInstallation mFirstTrackInstallation;
    protected final PersistentFirstTrackInstallationWithCallback mFirstTrackInstallationWithCallback;
    protected final Object mLoginIdLock = new Object();
    protected List<Class> mIgnoredViewTypeList = new ArrayList<>();
    /* LoginId */
    protected String mLoginId = null;
    /* SensorsAnalytics 地址 */
    protected String mServerUrl;
    protected String mOriginServerUrl;
    /* SDK 配置是否初始化 */
    protected boolean mSDKConfigInit;
    /* Debug 模式选项 */
    protected SensorsDataAPI.DebugMode mDebugMode = SensorsDataAPI.DebugMode.DEBUG_OFF;
    /* SDK 自动采集事件 */
    protected boolean mAutoTrack;
    /* 上个页面的 Url */
    protected String mLastScreenUrl;
    /* 上个页面的 Title */
    protected String mReferrerScreenTitle;
    /* 当前页面的 Title */
    protected String mCurrentScreenTitle;
    protected JSONObject mLastScreenTrackProperties;
    /* 是否请求网络 */
    protected boolean mEnableNetworkRequest = true;
    protected boolean mClearReferrerWhenAppEnd = false;
    protected boolean mDisableDefaultRemoteConfig = false;
    protected boolean mDisableTrackDeviceId = false;
    protected static boolean isChangeEnableNetworkFlag = false;
    // Session 时长
    protected int mSessionTime = 30 * 1000;
    protected List<Integer> mAutoTrackIgnoredActivities;
    protected List<Integer> mHeatMapActivities;
    protected List<Integer> mVisualizedAutoTrackActivities;
    protected String mCookie;
    protected TrackTaskManager mTrackTaskManager;
    protected TrackTaskManagerThread mTrackTaskManagerThread;
    protected SensorsDataScreenOrientationDetector mOrientationDetector;
    protected SensorsDataDynamicSuperProperties mDynamicSuperPropertiesCallBack;
    protected SimpleDateFormat mIsFirstDayDateFormat;
    protected SensorsDataTrackEventCallBack mTrackEventCallBack;
    protected List<SAEventListener> mEventListenerList;
    protected List<SAFunctionListener> mFunctionListenerList;
    private CopyOnWriteArrayList<SAJSListener> mSAJSListeners;
    protected IFragmentAPI mFragmentAPI;
    SensorsDataEncrypt mSensorsDataEncrypt;
    protected SensorsDataDeepLinkCallback mDeepLinkCallback;
    // $AppDeeplinkLaunch 是否携带设备信息
    boolean mEnableDeepLinkInstallSource = false;
    BaseSensorsDataSDKRemoteManager mRemoteManager;
    /**
     * 标记是否已经采集了带有插件版本号的事件
     */
    private boolean isTrackEventWithPluginVersion = false;

    public AbstractSensorsDataAPI(Context context, SAConfigOptions configOptions, SensorsDataAPI.DebugMode debugMode) {
        mContext = context;
        setDebugMode(debugMode);
        final String packageName = context.getApplicationContext().getPackageName();
        mAutoTrackIgnoredActivities = new ArrayList<>();
        mHeatMapActivities = new ArrayList<>();
        mVisualizedAutoTrackActivities = new ArrayList<>();
        PersistentLoader.initLoader(context);
        mDistinctId = (PersistentDistinctId) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.DISTINCT_ID);
        mSuperProperties = (PersistentSuperProperties) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.SUPER_PROPERTIES);
        mFirstStart = (PersistentFirstStart) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.FIRST_START);
        mFirstTrackInstallation = (PersistentFirstTrackInstallation) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.FIRST_INSTALL);
        mFirstTrackInstallationWithCallback = (PersistentFirstTrackInstallationWithCallback) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.FIRST_INSTALL_CALLBACK);
        mFirstDay = (PersistentFirstDay) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.FIRST_DAY);

        mFragmentAPI = new FragmentAPI();
        try {
            mSAConfigOptions = configOptions.clone();
            mTrackTaskManager = TrackTaskManager.getInstance();
            mTrackTaskManagerThread = new TrackTaskManagerThread();
            new Thread(mTrackTaskManagerThread, ThreadNameConstants.THREAD_TASK_QUEUE).start();
            SensorsDataExceptionHandler.init();
            initSAConfig(mSAConfigOptions.mServerUrl, packageName);
            mSAContextManager = new SAContextManager(mContext, mDisableTrackDeviceId);
            mMessages = AnalyticsMessages.getInstance(mContext, (SensorsDataAPI) this);
            mRemoteManager = new SensorsDataRemoteManager((SensorsDataAPI) this);
            //先从缓存中读取 SDKConfig
            mRemoteManager.applySDKConfigFromCache();
            // 可视化自定义属性拉取配置
            if (isVisualizedAutoTrackEnabled()) {
                VisualPropertiesManager.getInstance().requestVisualConfig(mContext, (SensorsDataAPI) this);
            }
            //打开 debug 模式，弹出提示
            if (mDebugMode != SensorsDataAPI.DebugMode.DEBUG_OFF && mIsMainProcess) {
                if (SHOW_DEBUG_INFO_VIEW) {
                    if (!isSDKDisabled()) {
                        showDebugModeWarning();
                    }
                }
            }

            registerLifecycleCallbacks();
            registerObserver();
            if (!mSAConfigOptions.isDisableSDK()) {
                delayInitTask();
            }
            if (SALog.isLogEnabled()) {
                SALog.i(TAG, String.format(Locale.CHINA, "Initialized the instance of Sensors Analytics SDK with server"
                        + " url '%s', flush interval %d ms, debugMode: %s", mServerUrl, mSAConfigOptions.mFlushInterval, debugMode));
            }
            mLoginId = DbAdapter.getInstance().getLoginId();
            SensorsDataUtils.initUniAppStatus();
        } catch (Throwable ex) {
            SALog.d(TAG, ex.getMessage());
        }
        trackTimerEndByCache();
    }

    protected AbstractSensorsDataAPI() {
        mContext = null;
        mMessages = null;
        mDistinctId = null;
        mSuperProperties = null;
        mFirstStart = null;
        mFirstDay = null;
        mFirstTrackInstallation = null;
        mFirstTrackInstallationWithCallback = null;
        mSensorsDataEncrypt = null;
    }

    /**
     * 返回采集控制是否关闭了 SDK
     *
     * @return true：关闭；false：没有关闭
     */
    private static boolean isSDKDisabledByRemote() {
        boolean isSDKDisabled = SensorsDataRemoteManager.isSDKDisabledByRemote();
        if (isSDKDisabled) {
            SALog.i(TAG, "remote config: SDK is disabled");
        }
        return isSDKDisabled;
    }

    /**
     * 返回本地是否关闭了 SDK
     *
     * @return true：关闭；false：没有关闭
     */
    private static boolean isSDKDisableByLocal() {
        if (mSAConfigOptions == null) {
            SALog.i(TAG, "SAConfigOptions is null");
            return true;
        }
        return mSAConfigOptions.isDisableSDK;
    }

    /**
     * 返回是否关闭了 SDK
     *
     * @return true：关闭；false：没有关闭
     */
    public static boolean isSDKDisabled() {
        return isSDKDisableByLocal() || isSDKDisabledByRemote();
    }

    /**
     * SDK 事件回调监听，目前用于弹窗业务
     *
     * @param eventListener 事件监听
     */
    public void addEventListener(SAEventListener eventListener) {
        try {
            if (this.mEventListenerList == null) {
                this.mEventListenerList = new ArrayList<>();
            }
            this.mEventListenerList.add(eventListener);
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    /**
     * 移除 SDK 事件回调监听
     *
     * @param eventListener 事件监听
     */
    public void removeEventListener(SAEventListener eventListener) {
        try {
            if (mEventListenerList != null && mEventListenerList.contains(eventListener)) {
                this.mEventListenerList.remove(eventListener);
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    /**
     * 监听 JS 消息
     *
     * @param listener JS 监听
     */
    public void addSAJSListener(final SAJSListener listener) {
        try {
            if (mSAJSListeners == null) {
                mSAJSListeners = new CopyOnWriteArrayList<>();
            }
            if (!mSAJSListeners.contains(listener)) {
                mSAJSListeners.add(listener);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    public boolean isDataManagerServiceEnable() {
        return mSAConfigOptions.mEnableDataManagerService;
    }

    /**
     * 移除 JS 消息
     *
     * @param listener JS 监听
     */
    public void removeSAJSListener(final SAJSListener listener) {
        try {
            if (mSAJSListeners != null && mSAJSListeners.contains(listener)) {
                this.mSAJSListeners.remove(listener);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    void handleJsMessage(WeakReference<View> view, final String message) {
        if (mSAJSListeners != null && mSAJSListeners.size() > 0) {
            for (final SAJSListener listener : mSAJSListeners) {
                try {
                    if (listener != null) {
                        listener.onReceiveJSMessage(view, message);
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        }
    }

    /**
     * SDK 函数回调监听
     *
     * @param functionListener 事件监听
     */
    public void addFunctionListener(SAFunctionListener functionListener) {
        try {
            if (this.mFunctionListenerList == null) {
                mFunctionListenerList = new ArrayList<>();
            }
            if (functionListener != null && !mFunctionListenerList.contains(functionListener)) {
                mFunctionListenerList.add(functionListener);
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    /**
     * 移除 SDK 事件回调监听
     *
     * @param functionListener 事件监听
     */
    public void removeFunctionListener(SAFunctionListener functionListener) {
        try {
            if (this.mFunctionListenerList != null && functionListener != null) {
                this.mFunctionListenerList.remove(functionListener);
            }
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    public static SAConfigOptions getConfigOptions() {
        return mSAConfigOptions;
    }

    public Context getContext() {
        return mContext;
    }

    boolean isSaveDeepLinkInfo() {
        return mSAConfigOptions.mEnableSaveDeepLinkInfo;
    }

    public SensorsDataDeepLinkCallback getDeepLinkCallback() {
        return mDeepLinkCallback;
    }

    boolean _trackEventFromH5(String eventInfo) {
        try {
            if (TextUtils.isEmpty(eventInfo)) {
                return false;
            }
            JSONObject eventObject = new JSONObject(eventInfo);

            String serverUrl = eventObject.optString("server_url");
            if (!TextUtils.isEmpty(serverUrl)) {
                if (!(new ServerUrl(serverUrl).check(new ServerUrl(mServerUrl)))) {
                    return false;
                }
                trackEventFromH5(eventInfo);
                return true;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return false;
    }

    /**
     * SDK 内部用来调用触发事件
     *
     * @param eventName 事件名称
     * @param properties 事件属性
     */
    public void trackInternal(final String eventName, final JSONObject properties) {
        trackInternal(eventName, properties, null);
    }

    /**
     * SDK 内部用来调用触发事件
     *
     * @param eventName 事件名称
     * @param properties 事件属性
     * @param viewNode ViewTree 中的 View 节点
     */
    public void trackInternal(final String eventName, final JSONObject properties, final ViewNode viewNode) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    if (viewNode != null) {
                        VisualPropertiesManager.getInstance().mergeVisualProperties(VisualPropertiesManager.VisualEventType.APP_CLICK, properties, viewNode);
                    }
                    trackEvent(EventType.TRACK, eventName, properties, null);
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        });
    }

    public SensorsDataAPI.DebugMode getDebugMode() {
        return mDebugMode;
    }

    public void setDebugMode(SensorsDataAPI.DebugMode debugMode) {
        mDebugMode = debugMode;
        if (debugMode == SensorsDataAPI.DebugMode.DEBUG_OFF) {
            enableLog(false);
            SALog.setDebug(false);
            mServerUrl = mOriginServerUrl;
        } else {
            enableLog(true);
            SALog.setDebug(true);
            setServerUrl(mOriginServerUrl);
        }
    }

    void enableAutoTrack(int autoTrackEventType) {
        try {
            if (autoTrackEventType <= 0 || autoTrackEventType > 15) {
                return;
            }
            this.mAutoTrack = true;
            mSAConfigOptions.setAutoTrackEventType(mSAConfigOptions.mAutoTrackEventType | autoTrackEventType);
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    /**
     * 返回是否开启点击图的提示框
     *
     * @return true 代表开启了点击图的提示框， false 代表关闭了点击图的提示框
     */
    public boolean isAppHeatMapConfirmDialogEnabled() {
        return mSAConfigOptions.mHeatMapConfirmDialogEnabled;
    }

    public boolean isVisualizedAutoTrackConfirmDialogEnabled() {
        return mSAConfigOptions.mVisualizedConfirmDialogEnabled;
    }

    public BaseSensorsDataSDKRemoteManager getRemoteManager() {
        return mRemoteManager;
    }

    public void setRemoteManager(BaseSensorsDataSDKRemoteManager remoteManager) {
        this.mRemoteManager = remoteManager;
    }

    public SensorsDataEncrypt getSensorsDataEncrypt() {
        return mSensorsDataEncrypt;
    }

    public boolean isDisableDefaultRemoteConfig() {
        return mDisableDefaultRemoteConfig;
    }

    /**
     * App 从后台恢复，遍历 mTrackTimer
     * startTime = System.currentTimeMillis()
     */
    public void appBecomeActive() {
        try {
            for (EventTimeInfo eventTimeInfo : DbAdapter.getInstance().queryEventTimeInfoSet()) {
                if (eventTimeInfo != null && !eventTimeInfo.isPaused) {
                    eventTimeInfo.startTime = System.currentTimeMillis();
                    DbAdapter.getInstance().updateEventTimer(eventTimeInfo);
                }
            }
        } catch (Exception e) {
            SALog.i(TAG, "appBecomeActive error:" + e.getMessage());
        }
    }

    /**
     * App 进入后台，遍历 mTrackTimer
     * eventAccumulatedDuration =
     * eventAccumulatedDuration + System.currentTimeMillis() - startTime - SessionIntervalTime
     */
    public void appEnterBackground() {
        for (EventTimeInfo eventTimeInfo : DbAdapter.getInstance().queryEventTimeInfoSet()) {
            if (eventTimeInfo != null && !eventTimeInfo.isPaused) {
                eventTimeInfo.eventAccumulatedDuration = eventTimeInfo.eventAccumulatedDuration + System.currentTimeMillis() - eventTimeInfo.startTime - getSessionIntervalTime();
                eventTimeInfo.startTime = System.currentTimeMillis();
                DbAdapter.getInstance().updateEventTimer(eventTimeInfo);
            }
        }
    }

    void trackChannelDebugInstallation() {
        final JSONObject _properties = new JSONObject();
        addTimeProperty(_properties);
        transformTaskQueue(new Runnable() {
            @Override
            public void run() {
                try {
                    _properties.put("$ios_install_source", ChannelUtils.getDeviceInfo(mContext,
                            mSAContextManager.getAndroidId(), OaidHelper.getOAID(mContext)));
                    // 先发送 track
                    trackEvent(EventType.TRACK, "$ChannelDebugInstall", _properties, null);

                    // 再发送 profile_set_once 或者 profile_set
                    JSONObject profileProperties = new JSONObject();
                    SensorsDataUtils.mergeJSONObject(_properties, profileProperties);
                    profileProperties.put("$first_visit_time", new java.util.Date());
                    if (mSAConfigOptions.mEnableMultipleChannelMatch) {
                        trackEvent(EventType.PROFILE_SET, null, profileProperties, null);
                    } else {
                        trackEvent(EventType.PROFILE_SET_ONCE, null, profileProperties, null);
                    }
                    flush();
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        });
    }

    /**
     * SDK 内部调用方法
     *
     * @param eventName 事件名
     * @param properties 事件属性
     */
    public void trackAutoEvent(final String eventName, final JSONObject properties) {
        trackAutoEvent(eventName, properties, null);
    }

    /**
     * SDK 全埋点调用方法，支持可视化自定义属性
     *
     * @param eventName 事件名
     * @param properties 事件属性
     */
    void trackAutoEvent(final String eventName, final JSONObject properties, final ViewNode viewNode) {
        //添加 $lib_method 属性
        JSONObject eventProperties = SADataHelper.appendLibMethodAutoTrack(properties);
        trackInternal(eventName, eventProperties, viewNode);
    }

    public SAContextManager getSAContextManager() {
        return mSAContextManager;
    }

    void registerNetworkListener() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                NetworkUtils.registerNetworkListener(mContext);
            }
        });
    }

    void unregisterNetworkListener() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                NetworkUtils.unregisterNetworkListener(mContext);
            }
        });
    }

    protected void addTimeProperty(JSONObject jsonObject) {
        if (!jsonObject.has("$time")) {
            try {
                jsonObject.put("$time", new Date(System.currentTimeMillis()));
            } catch (JSONException e) {
                SALog.printStackTrace(e);
            }
        }
    }

    protected boolean isFirstDay(long eventTime) {
        String firstDay = mFirstDay.get();
        if (firstDay == null) {
            return true;
        }
        try {
            if (mIsFirstDayDateFormat == null) {
                mIsFirstDayDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            }
            String current = mIsFirstDayDateFormat.format(eventTime);
            return firstDay.equals(current);
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
        return true;
    }

    protected void trackItemEvent(String itemType, String itemId, String eventType, long time, JSONObject properties) {
        try {
            assertKey(itemType);
            assertValue(itemId);
            assertPropertyTypes(properties);

            // 禁用采集事件时，先计算基本信息存储到缓存中
            if (!mSAConfigOptions.isDataCollectEnable) {
                transformItemTaskQueue(itemType, itemId, eventType, time, properties);
                return;
            }

            String eventProject = null;
            if (properties != null && properties.has("$project")) {
                eventProject = (String) properties.get("$project");
                properties.remove("$project");
            }

            JSONObject libProperties = new JSONObject();
            libProperties.put("$lib", "Android");
            libProperties.put("$lib_version", VERSION);
            libProperties.put("$lib_method", "code");
            mSAContextManager.addKeyIfExist(libProperties, "$app_version");

            JSONObject superProperties = mSuperProperties.get();
            if (superProperties != null) {
                if (superProperties.has("$app_version")) {
                    libProperties.put("$app_version", superProperties.get("$app_version"));
                }
            }

            StackTraceElement[] trace = (new Exception()).getStackTrace();
            if (trace.length > 1) {
                StackTraceElement traceElement = trace[0];
                String libDetail = String.format("%s##%s##%s##%s", traceElement
                                .getClassName(), traceElement.getMethodName(), traceElement.getFileName(),
                        traceElement.getLineNumber());
                if (!TextUtils.isEmpty(libDetail)) {
                    libProperties.put("$lib_detail", libDetail);
                }
            }

            JSONObject eventProperties = new JSONObject();
            eventProperties.put("item_type", itemType);
            eventProperties.put("item_id", itemId);
            eventProperties.put("type", eventType);
            eventProperties.put("time", time);
            eventProperties.put("properties", TimeUtils.formatDate(properties));
            eventProperties.put("lib", libProperties);

            if (!TextUtils.isEmpty(eventProject)) {
                eventProperties.put("project", eventProject);
            }
            mMessages.enqueueEventMessage(eventType, eventProperties);
            SALog.i(TAG, "track event:\n" + JSONUtils.formatJson(eventProperties.toString()));
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    protected void trackEvent(EventType eventType, String eventName, JSONObject properties, String originalDistinctId) {
        trackEvent(eventType, eventName, properties, null, getDistinctId(), getLoginId(), originalDistinctId, null);
    }

    protected void trackEvent(final EventType eventType, String eventName, final JSONObject properties, JSONObject dynamicProperty, String
            distinctId, String loginId, String originalDistinctId, EventTimeInfo eventTimeInfo) {
        try {
            if (eventType.isTrack()) {
                assertKey(eventName);
                //如果在线控制禁止了事件，则不触发
                if (mRemoteManager != null && mRemoteManager.ignoreEvent(eventName)) {
                    return;
                }
            }
            assertPropertyTypes(properties);

            try {
                JSONObject sendProperties;

                if (eventType.isTrack()) {
                    Map<String, Object> deviceInfo = mSAContextManager.getDeviceInfo();
                    if (deviceInfo != null) {
                        sendProperties = new JSONObject(deviceInfo);
                    } else {
                        sendProperties = new JSONObject();
                    }
                    //之前可能会因为没有权限无法获取运营商信息，检测再次获取
                    getCarrier(sendProperties);
                    if (!"$AppEnd".equals(eventName) && !"$AppDeeplinkLaunch".equals(eventName)) {
                        //合并 $latest_utm 属性
                        SensorsDataUtils.mergeJSONObject(ChannelUtils.getLatestUtmProperties(), sendProperties);
                    }
                    mergerDynamicAndSuperProperties(sendProperties, dynamicProperty);

                    if (mSAConfigOptions.mEnableReferrerTitle && mReferrerScreenTitle != null) {
                        sendProperties.put("$referrer_title", mReferrerScreenTitle);
                    }

                    // 当前网络状况
                    String networkType = NetworkUtils.networkType(mContext);
                    sendProperties.put("$wifi", "WIFI".equals(networkType));
                    sendProperties.put("$network_type", networkType);

                    // GPS
                    try {
                        if (mGPSLocation != null) {
                            mGPSLocation.toJSON(sendProperties);
                        }
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }

                    // 屏幕方向
                    try {
                        String screenOrientation = getScreenOrientation();
                        if (!TextUtils.isEmpty(screenOrientation)) {
                            sendProperties.put("$screen_orientation", screenOrientation);
                        }
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                } else if (eventType.isProfile()) {
                    sendProperties = new JSONObject();
                } else {
                    return;
                }

                // 禁用采集事件时，先计算基本信息存储到缓存中
                if (!mSAConfigOptions.isDataCollectEnable) {
                    if (SALog.isLogEnabled()) {
                        SALog.i(TAG, "track event, isDataCollectEnable = false, eventName = " + eventName + ",property = " + JSONUtils.formatJson(sendProperties.toString()));
                    }
                    transformEventTaskQueue(eventType, eventName, properties, sendProperties, distinctId, loginId, originalDistinctId, eventTimeInfo);
                    return;
                }
                trackEventInternal(eventType, eventName, properties, sendProperties, distinctId, loginId, originalDistinctId, eventTimeInfo);
            } catch (JSONException e) {
                throw new InvalidDataException("Unexpected property");
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 处理 H5 打通的事件
     *
     * @param eventInfo 事件信息
     */
    protected void trackEventH5(String eventInfo) {
        try {
            if (TextUtils.isEmpty(eventInfo)) {
                return;
            }

            // 禁用采集事件时，先计算基本信息存储到缓存中
            if (!mSAConfigOptions.isDataCollectEnable) {
                transformH5TaskQueue(eventInfo);
                return;
            }

            JSONObject eventObject = new JSONObject(eventInfo);
            eventObject.put("_hybrid_h5", true);
            String type = eventObject.getString("type");
            EventType eventType = EventType.valueOf(type.toUpperCase(Locale.getDefault()));

            String distinctIdKey = "distinct_id";
            if (eventType == EventType.TRACK_SIGNUP) {
                eventObject.put("original_id", getAnonymousId());
            } else if (!TextUtils.isEmpty(getLoginId())) {
                eventObject.put(distinctIdKey, getLoginId());
            } else {
                eventObject.put(distinctIdKey, getAnonymousId());
            }
            eventObject.put("anonymous_id", getAnonymousId());
            long eventTime = System.currentTimeMillis();
            eventObject.put("time", eventTime);

            try {
                SecureRandom secureRandom = new SecureRandom();
                eventObject.put("_track_id", secureRandom.nextInt());
            } catch (Exception e) {
                //ignore
            }

            JSONObject propertiesObject = eventObject.optJSONObject("properties");
            // 校验 H5 属性
            assertPropertyTypes(propertiesObject);
            if (propertiesObject == null) {
                propertiesObject = new JSONObject();
            }

            JSONObject libObject = eventObject.optJSONObject("lib");
            if (libObject != null) {
                mSAContextManager.addKeyIfExist(libObject, "$app_version");
                //update lib $app_version from super properties
                JSONObject superProperties = mSuperProperties.get();
                if (superProperties != null) {
                    if (superProperties.has("$app_version")) {
                        libObject.put("$app_version", superProperties.get("$app_version"));
                    }
                }
            }

            if (eventType.isTrack()) {
                Map<String, Object> deviceInfo = mSAContextManager.getDeviceInfo();
                if (deviceInfo != null) {
                    for (Map.Entry<String, Object> entry : deviceInfo.entrySet()) {
                        String key = entry.getKey();
                        if (!TextUtils.isEmpty(key)) {
                            if ("$lib".equals(key) || "$lib_version".equals(key)) {
                                continue;
                            }
                            propertiesObject.put(entry.getKey(), entry.getValue());
                        }
                    }
                }
                //之前可能会因为没有权限无法获取运营商信息，检测再次获取
                getCarrier(propertiesObject);
                // 当前网络状况
                String networkType = NetworkUtils.networkType(mContext);
                propertiesObject.put("$wifi", "WIFI".equals(networkType));
                propertiesObject.put("$network_type", networkType);

                // SuperProperties
                mergerDynamicAndSuperProperties(propertiesObject, getDynamicProperty());

                //是否首日访问
                if (eventType.isTrack()) {
                    propertiesObject.put("$is_first_day", isFirstDay(eventTime));
                }
                SensorsDataUtils.mergeJSONObject(ChannelUtils.getLatestUtmProperties(), propertiesObject);
            }

            if (eventObject.has("_nocache")) {
                eventObject.remove("_nocache");
            }

            if (eventObject.has("server_url")) {
                eventObject.remove("server_url");
            }

            if (eventObject.has("_flush_time")) {
                eventObject.remove("_flush_time");
            }

            if (propertiesObject.has("$project")) {
                eventObject.put("project", propertiesObject.optString("$project"));
                propertiesObject.remove("$project");
            }

            if (propertiesObject.has("$token")) {
                eventObject.put("token", propertiesObject.optString("$token"));
                propertiesObject.remove("$token");
            }

            if (propertiesObject.has("$time")) {
                try {
                    long time = propertiesObject.getLong("$time");
                    if (TimeUtils.isDateValid(time)) {
                        eventObject.put("time", time);
                    }
                } catch (Exception ex) {
                    SALog.printStackTrace(ex);
                }
                propertiesObject.remove("$time");
            }

            String eventName = eventObject.optString("event");
            if (eventType.isTrack()) {
                // 校验 H5 事件名称
                assertKey(eventName);
                boolean enterDb = isEnterDb(eventName, propertiesObject);
                if (!enterDb) {
                    SALog.d(TAG, eventName + " event can not enter database");
                    return;
                }

                if (!isTrackEventWithPluginVersion && !propertiesObject.has("$lib_plugin_version")) {
                    JSONArray libPluginVersion = getPluginVersion();
                    if (libPluginVersion == null) {
                        isTrackEventWithPluginVersion = true;
                    } else {
                        try {
                            propertiesObject.put("$lib_plugin_version", libPluginVersion);
                            isTrackEventWithPluginVersion = true;
                        } catch (Exception e) {
                            SALog.printStackTrace(e);
                        }
                    }
                }
            }
            eventObject.put("properties", propertiesObject);

            if (eventType == EventType.TRACK_SIGNUP) {
                String loginId = eventObject.getString("distinct_id");
                synchronized (mLoginIdLock) {
                    if (!loginId.equals(DbAdapter.getInstance().getLoginId()) && !loginId.equals(getAnonymousId())) {
                        mLoginId = loginId;
                        DbAdapter.getInstance().commitLoginId(loginId);
                        eventObject.put("login_id", loginId);
                        try {
                            if (mEventListenerList != null) {
                                for (SAEventListener eventListener : mEventListenerList) {
                                    eventListener.login();
                                }
                            }
                        } catch (Exception e) {
                            SALog.printStackTrace(e);
                        }
                        try {
                            if (mFunctionListenerList != null) {
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("distinctId", loginId);
                                for (SAFunctionListener listener : mFunctionListenerList) {
                                    listener.call("login", jsonObject);
                                }
                            }
                        } catch (Exception e) {
                            SALog.printStackTrace(e);
                        }
                        mMessages.enqueueEventMessage(type, eventObject);
                        if (SALog.isLogEnabled()) {
                            SALog.i(TAG, "track event:\n" + JSONUtils.formatJson(eventObject.toString()));
                        }
                    }
                }
            } else {
                if (!TextUtils.isEmpty(getLoginId())) {
                    eventObject.put("login_id", getLoginId());
                }
                try {
                    if (mEventListenerList != null && eventType.isTrack()) {
                        for (SAEventListener eventListener : mEventListenerList) {
                            eventListener.trackEvent(eventObject);
                        }
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
                try {
                    if (mFunctionListenerList != null && eventType.isTrack()) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("eventJSON", eventObject);
                        for (SAFunctionListener listener : mFunctionListenerList) {
                            listener.call("trackEvent", jsonObject);
                        }
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
                mMessages.enqueueEventMessage(type, eventObject);
                if (SALog.isLogEnabled()) {
                    SALog.i(TAG, "track event from H5:\n" + JSONUtils.formatJson(eventObject.toString()));
                }
            }
        } catch (Exception e) {
            //ignore
            SALog.printStackTrace(e);
        }
    }

    /**
     * 在未同意合规时转换队列
     *
     * @param runnable 任务
     */
    public void transformTaskQueue(final Runnable runnable) {
        // 禁用采集事件时，先计算基本信息存储到缓存中
        if (!mSAConfigOptions.isDataCollectEnable) {
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    mTrackTaskManager.transformTaskQueue(runnable);
                }
            });
            return;
        }

        mTrackTaskManager.addTrackEventTask(runnable);
    }

    protected void initSAConfig(String serverURL, String packageName) {
        Bundle configBundle = AppInfoUtils.getAppInfoBundle(mContext);
        if (mSAConfigOptions == null) {
            this.mSDKConfigInit = false;
            mSAConfigOptions = new SAConfigOptions(serverURL);
        } else {
            this.mSDKConfigInit = true;
        }

        if (mSAConfigOptions.mEnableEncrypt) {
            mSensorsDataEncrypt = new SensorsDataEncrypt(mContext, mSAConfigOptions.mPersistentSecretKey, mSAConfigOptions.getEncryptors());
        }

        DbAdapter.getInstance(mContext, packageName, mSensorsDataEncrypt);
        mTrackTaskManager.setDataCollectEnable(mSAConfigOptions.isDataCollectEnable);

        if (mSAConfigOptions.mInvokeLog) {
            enableLog(mSAConfigOptions.mLogEnabled);
        } else {
            enableLog(configBundle.getBoolean("com.sensorsdata.analytics.android.EnableLogging",
                    this.mDebugMode != SensorsDataAPI.DebugMode.DEBUG_OFF));
        }
        SALog.setDisableSDK(mSAConfigOptions.isDisableSDK);

        setServerUrl(serverURL);
        if (mSAConfigOptions.mEnableTrackAppCrash) {
            SensorsDataExceptionHandler.enableAppCrash();
        }

        if (mSAConfigOptions.mFlushInterval == 0) {
            mSAConfigOptions.setFlushInterval(configBundle.getInt("com.sensorsdata.analytics.android.FlushInterval",
                    15000));
        }

        if (mSAConfigOptions.mFlushBulkSize == 0) {
            mSAConfigOptions.setFlushBulkSize(configBundle.getInt("com.sensorsdata.analytics.android.FlushBulkSize",
                    100));
        }

        if (mSAConfigOptions.mMaxCacheSize == 0) {
            mSAConfigOptions.setMaxCacheSize(32 * 1024 * 1024L);
        }

        if (mSAConfigOptions.isSubProcessFlushData && DbAdapter.getInstance().isFirstProcess()) {
            //如果是首个进程
            DbAdapter.getInstance().commitFirstProcessState(false);
            DbAdapter.getInstance().commitSubProcessFlushState(false);
        }

        this.mAutoTrack = configBundle.getBoolean("com.sensorsdata.analytics.android.AutoTrack",
                false);
        if (mSAConfigOptions.mAutoTrackEventType != 0) {
            enableAutoTrack(mSAConfigOptions.mAutoTrackEventType);
            this.mAutoTrack = true;
        }

        if (!mSAConfigOptions.mInvokeHeatMapEnabled) {
            mSAConfigOptions.mHeatMapEnabled = configBundle.getBoolean("com.sensorsdata.analytics.android.HeatMap",
                    false);
        }

        if (!mSAConfigOptions.mInvokeHeatMapConfirmDialog) {
            mSAConfigOptions.mHeatMapConfirmDialogEnabled = configBundle.getBoolean("com.sensorsdata.analytics.android.EnableHeatMapConfirmDialog",
                    false);
        }

        if (!mSAConfigOptions.mInvokeVisualizedEnabled) {
            mSAConfigOptions.mVisualizedEnabled = configBundle.getBoolean("com.sensorsdata.analytics.android.VisualizedAutoTrack",
                    false);
        }

        if (!mSAConfigOptions.mInvokeVisualizedConfirmDialog) {
            mSAConfigOptions.mVisualizedConfirmDialogEnabled = configBundle.getBoolean("com.sensorsdata.analytics.android.EnableVisualizedAutoTrackConfirmDialog",
                    false);
        }

        enableTrackScreenOrientation(mSAConfigOptions.mTrackScreenOrientationEnabled);

        if (!TextUtils.isEmpty(mSAConfigOptions.mAnonymousId)) {
            identify(mSAConfigOptions.mAnonymousId);
        }

        if (mSAConfigOptions.isDisableSDK) {
            mEnableNetworkRequest = false;
            isChangeEnableNetworkFlag = true;
        }

        SHOW_DEBUG_INFO_VIEW = configBundle.getBoolean("com.sensorsdata.analytics.android.ShowDebugInfoView",
                true);

        this.mDisableDefaultRemoteConfig = configBundle.getBoolean("com.sensorsdata.analytics.android.DisableDefaultRemoteConfig",
                false);

        if (mSAConfigOptions.isDataCollectEnable) {
            mIsMainProcess = AppInfoUtils.isMainProcess(mContext, configBundle);
        }

        this.mDisableTrackDeviceId = configBundle.getBoolean("com.sensorsdata.analytics.android.DisableTrackDeviceId",
                false);
    }

    protected void applySAConfigOptions() {
        if (mSAConfigOptions.mEnableTrackAppCrash) {
            SensorsDataExceptionHandler.enableAppCrash();
        }

        if (mSAConfigOptions.mAutoTrackEventType != 0) {
            this.mAutoTrack = true;
        }

        if (mSAConfigOptions.mInvokeLog) {
            enableLog(mSAConfigOptions.mLogEnabled);
        }

        enableTrackScreenOrientation(mSAConfigOptions.mTrackScreenOrientationEnabled);

        if (!TextUtils.isEmpty(mSAConfigOptions.mAnonymousId)) {
            identify(mSAConfigOptions.mAnonymousId);
        }
    }

    /**
     * 触发事件的暂停/恢复
     *
     * @param eventName 事件名称
     * @param isPause 设置是否暂停
     */
    protected void trackTimerState(final String eventName, final boolean isPause) {
        final long startTime = System.currentTimeMillis();
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    EventTimeInfo eventTimeInfo = DbAdapter.getInstance().queryEventTimeInfo(eventName);
                    if (eventTimeInfo != null && isPause != eventTimeInfo.isPaused) {
                        eventTimeInfo.setTimerState(isPause, startTime);
                        eventTimeInfo.distinctId = getDistinctId();
                        eventTimeInfo.loginId = getLoginId();
                        eventTimeInfo.anonymous_id = getAnonymousId();
                        DbAdapter.getInstance().updateEventTimer(eventTimeInfo);
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        });
    }

    protected void trackTimerEndSync(EventTimeInfo eventTimeInfo, final JSONObject properties) {
        JSONObject sendProperties = new JSONObject();
        if (eventTimeInfo != null) {
            JSONObject oldProperties = eventTimeInfo.properties;
            if (oldProperties != null && oldProperties.length() > 0) {
                SensorsDataUtils.mergeJSONObject(oldProperties, sendProperties);
            }
            DbAdapter.getInstance().deleteEventTimeInfo(eventTimeInfo);
        }
        if (properties != null && properties.length() > 0) {
            SensorsDataUtils.mergeJSONObject(properties, sendProperties);
        }
        trackEvent(EventType.TRACK, eventTimeInfo.eventName, sendProperties, null, getDistinctId(), getLoginId(), null, eventTimeInfo);
    }

    /**
     * 补发数据库中存储的计时事件
     */
    private void trackTimerEndByCache() {
        final long endTime = DbAdapter.getInstance().getAppEndTime();
        TrackTaskManager.getInstance().addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                for (final EventTimeInfo eventTimeInfo : DbAdapter.getInstance().queryEventTimeInfoSet()) {
                    if (eventTimeInfo == null) {
                        continue;
                    }
                    if (!eventTimeInfo.isPaused) {
                        if (eventTimeInfo.endTime < eventTimeInfo.startTime) {
                            eventTimeInfo.endTime = endTime + 2000;
                        } else {
                            eventTimeInfo.endTime = endTime;
                        }
                        eventTimeInfo.setTimerState(true, eventTimeInfo.endTime);
                    }
                    trackTimerEndSync(eventTimeInfo, null);
                }
            }
        });
    }

    /**
     * 读取动态公共属性
     *
     * @return 动态公共属性
     */
    protected JSONObject getDynamicProperty() {
        JSONObject dynamicProperty = null;
        try {
            if (mDynamicSuperPropertiesCallBack != null) {
                dynamicProperty = mDynamicSuperPropertiesCallBack.getDynamicSuperProperties();
                assertPropertyTypes(dynamicProperty);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return dynamicProperty;
    }

    /**
     * 合并、去重静态公共属性与动态公共属性
     *
     * @param eventProperty 保存合并后属性的 JSON
     * @param dynamicProperty 动态公共属性
     */
    private void mergerDynamicAndSuperProperties(JSONObject eventProperty, JSONObject dynamicProperty) {
        JSONObject superProperties = getSuperProperties();
        if (dynamicProperty == null) {
            dynamicProperty = getDynamicProperty();
        }
        JSONObject removeDuplicateSuperProperties = SensorsDataUtils.mergeSuperJSONObject(dynamicProperty, superProperties);
        SensorsDataUtils.mergeJSONObject(removeDuplicateSuperProperties, eventProperty);
    }

    private void showDebugModeWarning() {
        try {
            if (mDebugMode == SensorsDataAPI.DebugMode.DEBUG_OFF) {
                return;
            }
            if (TextUtils.isEmpty(mServerUrl)) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    String info = null;
                    if (mDebugMode == SensorsDataAPI.DebugMode.DEBUG_ONLY) {
                        info = "现在您打开了 SensorsData SDK 的 'DEBUG_ONLY' 模式，此模式下只校验数据但不导入数据，数据出错时会以 Toast 的方式提示开发者，请上线前一定使用 DEBUG_OFF 模式。";
                    } else if (mDebugMode == SensorsDataAPI.DebugMode.DEBUG_AND_TRACK) {
                        info = "现在您打开了神策 SensorsData SDK 的 'DEBUG_AND_TRACK' 模式，此模式下校验数据并且导入数据，数据出错时会以 Toast 的方式提示开发者，请上线前一定使用 DEBUG_OFF 模式。";
                    }
                    CharSequence appName = AppInfoUtils.getAppName(mContext);
                    if (!TextUtils.isEmpty(appName)) {
                        info = String.format(Locale.CHINA, "%s：%s", appName, info);
                    }
                    Toast.makeText(mContext, info, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        }
    }

    /**
     * @param eventName 事件名
     * @param eventProperties 事件属性
     * @return 该事件是否入库
     */
    private boolean isEnterDb(String eventName, JSONObject eventProperties) {
        boolean enterDb = true;
        if (mTrackEventCallBack != null) {
            SALog.d(TAG, "SDK have set trackEvent callBack");
            try {
                enterDb = mTrackEventCallBack.onTrackEvent(eventName, eventProperties);
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
            if (enterDb) {
                try {
                    Iterator<String> it = eventProperties.keys();
                    while (it.hasNext()) {
                        String key = it.next();
                        try {
                            assertKey(key);
                        } catch (Exception e) {
                            SALog.printStackTrace(e);
                            return false;
                        }
                        Object value = eventProperties.opt(key);
                        if (!(value instanceof CharSequence || value instanceof Number || value
                                instanceof JSONArray || value instanceof Boolean || value instanceof Date)) {
                            SALog.d(TAG, String.format("The property value must be an instance of " +
                                            "CharSequence/Number/Boolean/JSONArray/Date. [key='%s', value='%s', class='%s']",
                                    key,
                                    value == null ? "" : value.toString(),
                                    value == null ? "" : value.getClass().getCanonicalName()));
                            return false;
                        }

                        if ("app_crashed_reason".equals(key)) {
                            if (value instanceof String && ((String) value).length() > 8191 * 2) {
                                SALog.d(TAG, "The property value is too long. [key='" + key
                                        + "', value='" + value.toString() + "']");
                                value = ((String) value).substring(0, 8191 * 2) + "$";
                            }
                        } else {
                            if (value instanceof String && ((String) value).length() > 8191) {
                                SALog.d(TAG, "The property value is too long. [key='" + key
                                        + "', value='" + value.toString() + "']");
                                value = ((String) value).substring(0, 8191) + "$";
                            }
                        }
                        if (value instanceof Date) {
                            eventProperties.put(key, TimeUtils.formatDate((Date) value, Locale.CHINA));
                        } else {
                            eventProperties.put(key, value);
                        }
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        }
        return enterDb;
    }

    private void trackEventInternal(final EventType eventType, final String eventName, final JSONObject properties, final JSONObject sendProperties,
                                    final String originalDistinctId, String distinctId, String loginId, final EventTimeInfo eventTimeInfo) throws JSONException {
        String libDetail = null;
        String lib_version = VERSION;
        String appEnd_app_version = null;
        long eventTime = System.currentTimeMillis();
        String anonymousId = getAnonymousId();
        if (null != eventTimeInfo) {//重启应用补发事件事件信息
            try {
                double duration = Double.parseDouble(eventTimeInfo.duration());
                if (!TextUtils.isEmpty(eventTimeInfo.distinctId)) {
                    distinctId = eventTimeInfo.distinctId;
                }
                loginId = eventTimeInfo.loginId;
                anonymousId = eventTimeInfo.anonymous_id;
                if (duration > 0) {
                    sendProperties.put("event_duration", duration);
                }
                eventTime = eventTimeInfo.endTime;
            } catch (Exception e) {
                //ignore
                SALog.printStackTrace(e);
            }
        }

        JSONObject libProperties = new JSONObject();
        if (null != properties) {
            try {
                if (properties.has("$lib_detail")) {
                    libDetail = properties.getString("$lib_detail");
                    properties.remove("$lib_detail");
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
            try {
                // 单独处理 $AppStart 和 $AppEnd 的时间戳
                if ("$AppEnd".equals(eventName)) {
                    long appEndTime = properties.optLong("event_time");
                    // 退出时间戳不合法不使用，2000 为打点间隔时间戳
                    if (appEndTime > 2000) {
                        eventTime = appEndTime;
                    }
                    String appEnd_lib_version = properties.optString("$lib_version");
                    appEnd_app_version = properties.optString("$app_version");
                    if (!TextUtils.isEmpty(appEnd_lib_version)) {
                        lib_version = appEnd_lib_version;
                    } else {
                        properties.remove("$lib_version");
                    }

                    if (TextUtils.isEmpty(appEnd_app_version)) {
                        properties.remove("$app_version");
                    }

                    properties.remove("event_time");
                    if (properties.has("$distinctId")) {
                        distinctId = properties.optString("$distinctId");
                        properties.remove("$distinctId");
                    }
                    if (properties.has("$loginId")) {
                        loginId = properties.optString("$loginId");
                        properties.remove("$loginId");
                    }
                    if (properties.has("$anonymous_id")) {
                        anonymousId = properties.optString("$anonymous_id");
                        properties.remove("$anonymous_id");
                    }
                } else if ("$AppStart".equals(eventName)) {
                    long appStartTime = properties.optLong("event_time");
                    if (appStartTime > 0) {
                        eventTime = appStartTime;
                    }
                    properties.remove("event_time");
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
            SensorsDataUtils.mergeJSONObject(properties, sendProperties);
            if (eventType.isTrack()) {
                if ("autoTrack".equals(properties.optString("$lib_method"))) {
                    libProperties.put("$lib_method", "autoTrack");
                } else {
                    libProperties.put("$lib_method", "code");
                    sendProperties.put("$lib_method", "code");
                }
            } else {
                libProperties.put("$lib_method", "code");
            }
        } else {
            libProperties.put("$lib_method", "code");
            if (eventType.isTrack()) {
                sendProperties.put("$lib_method", "code");
            }
        }

        libProperties.put("$lib", "Android");
        libProperties.put("$lib_version", lib_version);
        if (TextUtils.isEmpty(appEnd_app_version)) {
            mSAContextManager.addKeyIfExist(libProperties, "$app_version");
        } else {
            libProperties.put("$app_version", appEnd_app_version);
        }

        //update lib $app_version from super properties
        JSONObject superProperties = mSuperProperties.get();
        if (superProperties != null) {
            if (superProperties.has("$app_version")) {
                libProperties.put("$app_version", superProperties.get("$app_version"));
            }
        }

        final JSONObject dataObj = new JSONObject();

        try {
            SecureRandom random = new SecureRandom();
            dataObj.put("_track_id", random.nextInt());
        } catch (Exception e) {
            // ignore
        }

        dataObj.put("time", eventTime);
        if (!mSAConfigOptions.mEnableEncrypt) {
            dataObj.put("SAElapsedRealtime", SystemClock.elapsedRealtime());
        }

        dataObj.put("type", eventType.getEventType());
        try {
            if (sendProperties.has("$project")) {
                dataObj.put("project", sendProperties.optString("$project"));
                sendProperties.remove("$project");
            }

            if (sendProperties.has("$token")) {
                dataObj.put("token", sendProperties.optString("$token"));
                sendProperties.remove("$token");
            }

            if (sendProperties.has("$time")) {
                try {
                    Object timeDate = sendProperties.opt("$time");
                    if (timeDate instanceof Date) {
                        if (TimeUtils.isDateValid((Date) timeDate)) {
                            dataObj.put("time", ((Date) timeDate).getTime());
                        }
                    }
                } catch (Exception ex) {
                    SALog.printStackTrace(ex);
                }
                sendProperties.remove("$time");
            }

            //针对 SF 弹窗展示事件特殊处理
            if ("$PlanPopupDisplay".equals(eventName)) {
                if (sendProperties.has("$sf_internal_anonymous_id")) {
                    anonymousId = sendProperties.optString("$sf_internal_anonymous_id");
                    sendProperties.remove("$sf_internal_anonymous_id");
                }

                if (sendProperties.has("$sf_internal_login_id")) {
                    loginId = sendProperties.optString("$sf_internal_login_id");
                    sendProperties.remove("$sf_internal_login_id");
                }
                if (!TextUtils.isEmpty(loginId)) {
                    distinctId = loginId;
                } else {
                    distinctId = anonymousId;
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }

        if (TextUtils.isEmpty(distinctId)) {// 如果为空，则说明没有 loginId，所以重新设置当时状态的匿名 Id
            dataObj.put("distinct_id", getAnonymousId());
        } else {
            dataObj.put("distinct_id", distinctId);
        }

        if (!TextUtils.isEmpty(loginId)) {
            dataObj.put("login_id", loginId);
        }
        dataObj.put("anonymous_id", anonymousId);
        dataObj.put("lib", libProperties);

        if (eventType == EventType.TRACK) {
            dataObj.put("event", eventName);
            //是否首日访问
            sendProperties.put("$is_first_day", isFirstDay(eventTime));
        } else if (eventType == EventType.TRACK_SIGNUP) {
            dataObj.put("event", eventName);
            dataObj.put("original_id", originalDistinctId);
        }

        if (mAutoTrack && properties != null) {
            if (SensorsDataAPI.AutoTrackEventType.isAutoTrackType(eventName)) {
                SensorsDataAPI.AutoTrackEventType trackEventType = SensorsDataAPI.AutoTrackEventType.autoTrackEventTypeFromEventName(eventName);
                if (trackEventType != null) {
                    if (!isAutoTrackEventTypeIgnored(trackEventType)) {
                        if (properties.has("$screen_name")) {
                            String screenName = properties.getString("$screen_name");
                            if (!TextUtils.isEmpty(screenName)) {
                                String[] screenNameArray = screenName.split("\\|");
                                if (screenNameArray.length > 0) {
                                    libDetail = String.format("%s##%s##%s##%s", screenNameArray[0], "", "", "");
                                }
                            }
                        }
                    }
                }
            }
        }

        if (TextUtils.isEmpty(libDetail)) {
            StackTraceElement[] trace = (new Exception()).getStackTrace();
            if (trace.length > 1) {
                StackTraceElement traceElement = trace[0];
                libDetail = String.format("%s##%s##%s##%s", traceElement
                                .getClassName(), traceElement.getMethodName(), traceElement.getFileName(),
                        traceElement.getLineNumber());
            }
        }

        libProperties.put("$lib_detail", libDetail);

        //防止用户自定义事件以及公共属性可能会加 $device_id 属性，导致覆盖 sdk 原始的 $device_id 属性值
        if (sendProperties.has("$device_id")) {//由于 profileSet 等类型事件没有 $device_id 属性，故加此判断
            mSAContextManager.addKeyIfExist(sendProperties, "$device_id");
        }
        if (eventType.isTrack()) {
            boolean isEnterDb = isEnterDb(eventName, sendProperties);
            if (!isEnterDb) {
                SALog.d(TAG, eventName + " event can not enter database");
                return;
            }
            if (!isTrackEventWithPluginVersion && !sendProperties.has("$lib_plugin_version")) {
                JSONArray libPluginVersion = getPluginVersion();
                if (libPluginVersion == null) {
                    isTrackEventWithPluginVersion = true;
                } else {
                    try {
                        sendProperties.put("$lib_plugin_version", libPluginVersion);
                        isTrackEventWithPluginVersion = true;
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            }
        }
        dataObj.put("properties", sendProperties);

        try {
            if (mEventListenerList != null && eventType.isTrack()) {
                for (SAEventListener eventListener : mEventListenerList) {
                    eventListener.trackEvent(dataObj);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }

        try {
            if (mFunctionListenerList != null && eventType.isTrack()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("eventJSON", dataObj);
                for (SAFunctionListener listener : mFunctionListenerList) {
                    listener.call("trackEvent", jsonObject);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }

        mMessages.enqueueEventMessage(eventType.getEventType(), dataObj);
        if ("$AppStart".equals(eventName)) {
            mSAContextManager.setAppStartSuccess(true);
        }
        if (SALog.isLogEnabled()) {
            SALog.i(TAG, "track event:\n" + JSONUtils.formatJson(dataObj.toString()));
        }
    }

    /**
     * 如果没有授权时，需要将已执行的的缓存队列切换到真正的 TaskQueue 中
     */
    private void transformEventTaskQueue(final EventType eventType, final String eventName, final JSONObject properties, final JSONObject sendProperties,
                                         final String originalDistinctId, final String distinctId, final String loginId, final EventTimeInfo eventTimer) {
        try {
            if (!sendProperties.has("$time") && !("$AppStart".equals(eventName) || "$AppEnd".equals(eventName))) {
                sendProperties.put("$time", new Date(System.currentTimeMillis()));
            }
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
        mTrackTaskManager.transformTaskQueue(new Runnable() {
            @Override
            public void run() {
                try {
                    if (eventType.isTrack()) {
                        JSONObject jsonObject = new JSONObject(mSAContextManager.getDeviceInfo());
                        JSONUtils.mergeDistinctProperty(jsonObject, sendProperties);
                    }
                    if ("$SignUp".equals(eventName)) {// 如果是 "$SignUp" 则需要重新补上 originalId
                        trackEventInternal(eventType, eventName, properties, sendProperties, distinctId, loginId, getAnonymousId(), eventTimer);
                    } else {
                        trackEventInternal(eventType, eventName, properties, sendProperties, distinctId, loginId, originalDistinctId, eventTimer);
                    }
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        });
    }

    private void transformH5TaskQueue(String eventInfo) {
        try {
            final JSONObject eventObject = new JSONObject(eventInfo);
            JSONObject propertiesObject = eventObject.optJSONObject("properties");
            if (propertiesObject != null && !propertiesObject.has("$time")) {
                propertiesObject.put("$time", System.currentTimeMillis());
            }
            if (SALog.isLogEnabled()) {
                SALog.i(TAG, "track H5, isDataCollectEnable = false, eventInfo = " + JSONUtils.formatJson(eventInfo));
            }
            mTrackTaskManager.transformTaskQueue(new Runnable() {
                @Override
                public void run() {
                    try {
                        trackEventH5(eventObject.toString());
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
            });
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
    }

    private void transformItemTaskQueue(final String itemType, final String itemId, final String eventType, final long time, final JSONObject properties) {
        if (SALog.isLogEnabled()) {
            SALog.i(TAG, "track item, isDataCollectEnable = false, itemType = " + itemType + ",itemId = " + itemId);
        }
        mTrackTaskManager.transformTaskQueue(new Runnable() {
            @Override
            public void run() {
                try {
                    trackItemEvent(itemType, itemId, eventType, time, properties);
                } catch (Exception e) {
                    SALog.printStackTrace(e);
                }
            }
        });
    }

    private JSONArray getPluginVersion() {
        try {
            if (!TextUtils.isEmpty(SensorsDataAPI.ANDROID_PLUGIN_VERSION)) {
                SALog.i(TAG, "android plugin version: " + SensorsDataAPI.ANDROID_PLUGIN_VERSION);
                JSONArray libPluginVersion = new JSONArray();
                libPluginVersion.put("android:" + SensorsDataAPI.ANDROID_PLUGIN_VERSION);
                return libPluginVersion;
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return null;
    }

    /**
     * 注册 ActivityLifecycleCallbacks
     */
    private void registerLifecycleCallbacks() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                final Application app = (Application) mContext.getApplicationContext();
                final SensorsDataActivityLifecycleCallbacks lifecycleCallbacks = new SensorsDataActivityLifecycleCallbacks();
                app.registerActivityLifecycleCallbacks(lifecycleCallbacks);
                app.registerActivityLifecycleCallbacks(AppStateManager.getInstance());
                ActivityLifecycleCallbacks activityLifecycleCallbacks = new ActivityLifecycleCallbacks((SensorsDataAPI) this, mFirstStart, mFirstDay, mContext);
                lifecycleCallbacks.addActivityLifecycleCallbacks(activityLifecycleCallbacks);
                SensorsDataExceptionHandler.addExceptionListener(activityLifecycleCallbacks);
                FragmentTrackHelper.addFragmentCallbacks(new FragmentViewScreenCallbacks());

                if (mSAConfigOptions.isTrackPageLeave()) {
                    ActivityPageLeaveCallbacks pageLeaveCallbacks = new ActivityPageLeaveCallbacks();
                    lifecycleCallbacks.addActivityLifecycleCallbacks(pageLeaveCallbacks);
                    SensorsDataExceptionHandler.addExceptionListener(pageLeaveCallbacks);

                    FragmentPageLeaveCallbacks fragmentPageLeaveCallbacks = new FragmentPageLeaveCallbacks();
                    FragmentTrackHelper.addFragmentCallbacks(fragmentPageLeaveCallbacks);
                    SensorsDataExceptionHandler.addExceptionListener(fragmentPageLeaveCallbacks);
                }
                if (mSAConfigOptions.isEnableTrackPush()) {
                    lifecycleCallbacks.addActivityLifecycleCallbacks(new PushLifecycleCallbacks());
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    /**
     * 注册 ContentObserver 监听
     */
    private void registerObserver() {
        // 注册跨进程业务的 ContentObserver 监听
        SensorsDataContentObserver contentObserver = new SensorsDataContentObserver();
        ContentResolver contentResolver = mContext.getContentResolver();
        contentResolver.registerContentObserver(DbParams.getInstance().getDataCollectUri(), false, contentObserver);
        contentResolver.registerContentObserver(DbParams.getInstance().getSessionTimeUri(), false, contentObserver);
        contentResolver.registerContentObserver(DbParams.getInstance().getLoginIdUri(), false, contentObserver);
        contentResolver.registerContentObserver(DbParams.getInstance().getDisableSDKUri(), false, contentObserver);
        contentResolver.registerContentObserver(DbParams.getInstance().getEnableSDKUri(), false, contentObserver);
    }

    /**
     * $AppDeeplinkLaunch 事件是否包含 $ios_install_source 属性
     *
     * @return boolean
     */
    public boolean isDeepLinkInstallSource() {
        return mEnableDeepLinkInstallSource;
    }

    /**
     * 延迟初始化任务
     */
    protected void delayInitTask() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isSaveDeepLinkInfo()) {
                        ChannelUtils.loadUtmByLocal(mContext);
                    } else {
                        ChannelUtils.clearLocalUtm(mContext);
                    }
                    registerNetworkListener();
                } catch (Exception ex) {
                    SALog.printStackTrace(ex);
                }
            }
        });
    }

    /**
     * 重新读取运营商信息
     *
     * @param property Property
     */
    private void getCarrier(JSONObject property) {
        try {
            if (TextUtils.isEmpty(property.optString("$carrier")) && mSAConfigOptions.isDataCollectEnable) {
                String carrier = SensorsDataUtils.getCarrier(mContext);
                if (!TextUtils.isEmpty(carrier)) {
                    property.put("$carrier", carrier);
                }
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }
}
