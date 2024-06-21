/*
 * Created by dengshiwei on 2021/04/07.
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

package com.sensorsdata.analytics.android.sdk.data.adapter;

import android.net.Uri;

public class DbParams {
    /* 数据库中的表名 */
    public static final String TABLE_EVENTS = "events";
    public static final String TABLE_EVENTS_CACHE = "events_cache";
    public static final String TABLE_CHANNEL_PERSISTENT = "t_channel";
    public static final int DB_OUT_OF_MEMORY_ERROR = -2;
    public static final String KEY_CHANNEL_EVENT_NAME = "event_name";
    public static final String KEY_CHANNEL_RESULT = "result";
    /* 数据库名称 */
    public static final String DATABASE_NAME = "sensorsdata";
    /* 数据库版本号 */
    public static final int DATABASE_VERSION = 8;
    public static final String TABLE_ACTIVITY_START_COUNT = "activity_started_count";
    public static final String TABLE_APP_START_TIME = "app_start_time";
    public static final String TABLE_APP_END_DATA = "app_end_data";
    public static final String TABLE_SUB_PROCESS_FLUSH_DATA = "sub_process_flush_data";
    public static final String TABLE_FIRST_PROCESS_START = "first_process_start";
    public static final String TABLE_SESSION_INTERVAL_TIME = "session_interval_time";
    public static final String TABLE_DATA_COLLECT = "data_collect";
    public static final String TABLE_DATA_ENABLE_SDK = "enable_SDK";
    public static final String TABLE_DATA_DISABLE_SDK = "disable_SDK";
    public static final String TABLE_REMOTE_CONFIG = "remote_config";
    public static final String TABLE_LOGIN_ID = "events_login_id";
    /* Event 表字段 */
    public static final String KEY_DATA = "data";
    public static final String KEY_CREATED_AT = "created_at";
    /* 数据库状态 */
    static final int DB_UPDATE_ERROR = -1;
    static final String VALUE = "value";
    public static final String GZIP_DATA_EVENT = "1";
    public static final String GZIP_DATA_ENCRYPT = "9";
    /* 删除所有数据 */
    static final String DB_DELETE_ALL = "DB_DELETE_ALL";
    private static DbParams instance;
    /* 事件时长统计表名 */
    public static final String TABLE_EVENTS_TIMER = "events_timer";
    /* 事件名称 */
    public static final String EVENTS_NAME = "events_name";
    /* 事件最后状态：0 代表 pause, 1 代表时间统计状态 */
    public static final String EVENTS_STATE = "events_state";
    /* 事件当前发生的时长 */
    public static final String EVENTS_DURATION = "events_duration";
    /* 事件开始时间 */
    public static final String EVENTS_START_TIME = "events_start_time";
    /* 事件结束时间 */
    public static final String EVENTS_END_TIME = "events_end_time";
    /* 事件属性 */
    public static final String EVENTS_PROPERTY = "events_property";
    /* distinct_id */
    public static final String EVENTS_DISTINCT_ID = "events_distinct_id";
    /* loginId */
    public static final String EVENTS_LOGIN_ID = "events_login_id";
    /* anonymous_id */
    public static final String EVENTS_ANONYMOUS_ID = "events_anonymous_id";
    public static final String APP_START_DATA = "app_start_data";
    private final Uri mUri, mActivityStartCountUri, mAppStartTimeUri, mDataCollectUri,
            mAppEndDataUri, mSessionTimeUri, mLoginIdUri, mChannelPersistentUri, mSubProcessUri,
            mFirstProcessUri, mEnableSDKUri, mDisableSDKUri, mRemoteConfigUri;
    private final Uri mEventTimerUri, mEventCacheUri;

    private DbParams(String packageName) {
        mUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_EVENTS);
        mActivityStartCountUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_ACTIVITY_START_COUNT);
        mAppStartTimeUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_APP_START_TIME);
        mAppEndDataUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_APP_END_DATA);
        mSessionTimeUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_SESSION_INTERVAL_TIME);
        mLoginIdUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_LOGIN_ID);
        mChannelPersistentUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_CHANNEL_PERSISTENT);
        mSubProcessUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_SUB_PROCESS_FLUSH_DATA);
        mFirstProcessUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_FIRST_PROCESS_START);
        mDataCollectUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_DATA_COLLECT);
        mEnableSDKUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_DATA_ENABLE_SDK);
        mDisableSDKUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_DATA_DISABLE_SDK);
        mRemoteConfigUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_REMOTE_CONFIG);
        mEventTimerUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_EVENTS_TIMER);
        mEventCacheUri = Uri.parse("content://" + packageName + ".SensorsDataContentProvider/" + TABLE_EVENTS_CACHE);
    }

    public static DbParams getInstance(String packageName) {
        if (instance == null) {
            instance = new DbParams(packageName);
        }
        return instance;
    }

    public static DbParams getInstance() {
        if (instance == null) {
            throw new IllegalStateException("The static method getInstance(String packageName) should be called before calling getInstance()");
        }
        return instance;
    }

    /**
     * 获取 Event Uri
     *
     * @return Uri
     */
    public Uri getEventUri() {
        return mUri;
    }

    /**
     * 获取 AppStart Uri
     *
     * @return Uri
     */
    public Uri getActivityStartCountUri() {
        return mActivityStartCountUri;
    }

    /**
     * 获取 AppStartTime Uri
     *
     * @return Uri
     */
    Uri getAppStartTimeUri() {
        return mAppStartTimeUri;
    }

    /**
     * 获取 AppEndData Uri
     *
     * @return Uri
     */
    Uri getAppEndDataUri() {
        return mAppEndDataUri;
    }

    /**
     * 获取 SessionTime Uri
     *
     * @return Uri
     */
    public Uri getSessionTimeUri() {
        return mSessionTimeUri;
    }

    /**
     * 获取 LoginId 的 Uri
     *
     * @return Uri
     */
    public Uri getLoginIdUri() {
        return mLoginIdUri;
    }

    /**
     * 获取 Channel 持久化 Uri
     *
     * @return Uri
     */
    public Uri getChannelPersistentUri() {
        return mChannelPersistentUri;
    }

    /**
     * 多进程上报数据标记位 Uri
     *
     * @return Uri
     */
    Uri getSubProcessUri() {
        return mSubProcessUri;
    }

    /**
     * 是否首个启动的进程 Uri
     *
     * @return Uri
     */
    public Uri getFirstProcessUri() {
        return mFirstProcessUri;
    }

    /**
     * 开启数据采集 Uri
     *
     * @return Uri
     */
    public Uri getDataCollectUri() {
        return mDataCollectUri;
    }

    /**
     * 获取 EventTimer 的 Uri
     *
     * @return Uri
     */
    Uri getEventTimerUri() {
        return mEventTimerUri;
    }

    /**
     * 获取缓存表的 Uri 地址
     *
     * @return Uri
     */
    public Uri getEventCacheUri() {
        return mEventCacheUri;
    }

    public Uri getDisableSDKUri() {
        return mDisableSDKUri;
    }

    public Uri getEnableSDKUri() {
        return mEnableSDKUri;
    }

    public Uri getRemoteConfigUri() {
        return mRemoteConfigUri;
    }
}
