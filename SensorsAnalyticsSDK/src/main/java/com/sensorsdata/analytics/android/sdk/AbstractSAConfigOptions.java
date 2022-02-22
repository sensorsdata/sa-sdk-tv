/*
 * Created by dengshiwei on 2019/04/18.
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

import com.sensorsdata.analytics.android.sdk.encrypt.IPersistentSecretKey;
import com.sensorsdata.analytics.android.sdk.encrypt.SAEncryptListener;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLSocketFactory;

/**
 * SDK 配置抽象类
 */
abstract class AbstractSAConfigOptions {
    /**
     * 请求配置地址，默认从 ServerUrl 解析
     */
    public String mRemoteConfigUrl;

    /**
     * 远程配置请求最小间隔时长，单位：小时，默认 24
     */
    public int mMinRequestInterval = 24;

    /**
     * 远程配置请求最大间隔时长，单位：小时，默认 48
     */
    public int mMaxRequestInterval = 48;

    /**
     * 禁用随机时间请求远程配置
     */
    public boolean mDisableRandomTimeRequestRemoteConfig;

    /**
     * 设置 SSLSocketFactory
     */
    public SSLSocketFactory mSSLSocketFactory;

    /**
     * 禁用辅助工具
     */
    public boolean mDisableDebugAssistant;

    /**
     * 是否开启推送点击采集
     */
    public boolean mEnableTrackPush;

    /**
     * 数据上报服务器地址
     */
    String mServerUrl;

    /**
     * 配置代理
     */
    Proxy mProxy;

    /**
     * AutoTrack 类型
     */
    int mAutoTrackEventType;

    /**
     * 是否开启 TrackAppCrash
     */
    boolean mEnableTrackAppCrash;

    /**
     * 两次数据发送的最小时间间隔，单位毫秒
     */
    int mFlushInterval;

    /**
     * 本地缓存日志的最大条目数
     */
    int mFlushBulkSize;

    /**
     * 本地缓存上限值，单位 byte，默认为 32MB：32 * 1024 * 1024
     */
    long mMaxCacheSize = 32 * 1024 * 1024L;

    /**
     * 点击图是否可用
     */
    boolean mHeatMapEnabled;

    /**
     * 可视化全埋点是否可用
     */
    boolean mVisualizedEnabled;

    /**
     * 可视化全埋点自定义属性是否可用
     */
    boolean mVisualizedPropertiesEnabled;

    /**
     * 是否开启打印日志
     */
    boolean mLogEnabled;

    /**
     * 采集屏幕方向
     */
    boolean mTrackScreenOrientationEnabled;

    /**
     * 网络上传策略
     */
    int mNetworkTypePolicy = SensorsNetworkType.TYPE_3G | SensorsNetworkType.TYPE_4G | SensorsNetworkType.TYPE_WIFI | SensorsNetworkType.TYPE_5G;

    /**
     * AnonymousId，匿名 ID
     */
    String mAnonymousId;

    /**
     * 是否使用上次启动时保存的 utm 属性.
     */
    boolean mEnableSaveDeepLinkInfo = false;

    /**
     * 是否自动进行 H5 打通
     */
    boolean isAutoTrackWebView;

    /**
     * WebView 是否支持 JellyBean
     */
    boolean isWebViewSupportJellyBean;

    /**
     * 是否在手动埋点事件中自动添加渠道匹配信息
     */
    boolean isAutoAddChannelCallbackEvent;

    /**
     * 是否子进程上报数据
     */
    boolean isSubProcessFlushData = false;

    /**
     * 是否开启加密
     */
    boolean mEnableEncrypt = false;

    /**
     * 密钥存储相关接口
     */
    IPersistentSecretKey mPersistentSecretKey;

    /**
     * 关闭数据采集，默认开启数据采集
     */
    boolean isDataCollectEnable = true;

    /**
     * 是否关闭 SDK
     */
    boolean isDisableSDK = false;

    /**
     * 自定义加密实现接口
     */
    List<SAEncryptListener> mEncryptListeners;

    /**
     * 统一数据上报
     */
    boolean mEnableDataManagerService = false;
    List<SAEncryptListener> mEncryptors = new ArrayList<>();

    /**
     * 开启采集页面停留时长
     */
    protected boolean mIsTrackPageLeave = false;

    /**
     * 是否开启数据采集
     *
     * @return true 开启，false 未开启
     */
    public boolean isDataCollectEnable() {
        return isDataCollectEnable;
    }

    /**
     * 是否开启 DeepLink
     *
     * @return true 开启，false 未开启
     */
    public boolean isSaveDeepLinkInfo() {
        return mEnableSaveDeepLinkInfo;
    }

    /**
     * 是否允许多进程上报数据
     *
     * @return true 开启，false 未开启
     */
    public boolean isMultiProcessFlush() {
        return isSubProcessFlushData;
    }

    /**
     * 是否开启页面停留时长采集
     *
     * @return true 开启，false 未开启
     */
    public boolean isTrackPageLeave() {
        return mIsTrackPageLeave;
    }

    /**
     * 获取注册的加密插件列表
     *
     * @return 注册的加密插件列表
     */
    public List<SAEncryptListener> getEncryptors() {
        return mEncryptors;
    }

    /**
     * 是否禁止 SDK
     *
     * @return true 禁止了 SDK，false 未禁止
     */
    public boolean isDisableSDK() {
        return this.isDisableSDK;
    }

    /**
     * 是否开启推送
     *
     * @return true 开启推送，false 禁止推送
     */
    public boolean isEnableTrackPush() {
        return this.mEnableTrackPush;
    }

    /**
     * 自定义属性是否可用
     *
     * @return true 可用，false 不可用
     */
    public boolean isVisualizedPropertiesEnabled() {
        return this.mVisualizedPropertiesEnabled;
    }
}