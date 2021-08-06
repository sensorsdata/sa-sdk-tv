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
package com.sensorsdata.analytics.android.sdk.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;

class SensorsDataDBHelper extends SQLiteOpenHelper {
    private static final String TAG = "SA.SQLiteOpenHelper";
    // 创建数据 events 表
    private static final String CREATE_EVENTS_TABLE =
            String.format("CREATE TABLE %s (_id INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT NOT NULL, %s INTEGER NOT NULL);", DbParams.TABLE_EVENTS, DbParams.KEY_DATA, DbParams.KEY_CREATED_AT);
    private static final String EVENTS_TIME_INDEX =
            String.format("CREATE INDEX IF NOT EXISTS time_idx ON %s (%s);", DbParams.TABLE_EVENTS, DbParams.KEY_CREATED_AT);
    // 创建渠道 t_channel 表
    private static final String CHANNEL_EVENT_PERSISTENT_TABLE = String.format("CREATE TABLE IF NOT EXISTS %s (%s TEXT PRIMARY KEY, %s INTEGER)",
            DbParams.TABLE_CHANNEL_PERSISTENT, DbParams.KEY_CHANNEL_EVENT_NAME, DbParams.KEY_CHANNEL_RESULT);
    // 创建 event_timer 表
    private static final String CREATE_EVENTS_TIMER_TABLE = String.format("CREATE TABLE IF NOT EXISTS %s (%s TEXT PRIMARY KEY, %s TEXT, %s INTEGER, %s long, %s long, %s long, %s Text, %s Text, %s Text);",
                    DbParams.TABLE_EVENTS_TIMER, DbParams.EVENTS_NAME, DbParams.EVENTS_PROPERTY, DbParams.EVENTS_STATE, DbParams.EVENTS_DURATION,
            DbParams.EVENTS_START_TIME, DbParams.EVENTS_END_TIME, DbParams.EVENTS_DISTINCT_ID, DbParams.EVENTS_LOGIN_ID, DbParams.EVENTS_ANONYMOUS_ID);
    private static final String EVENTS_TIME_ALERT_DISTINCT_ID =
            String.format("ALTER TABLE %s ADD %s TEXT", DbParams.TABLE_EVENTS_TIMER, DbParams.EVENTS_DISTINCT_ID);
    private static final String EVENTS_TIME_ALERT_LOGIN_ID =
            String.format("ALTER TABLE %s ADD %s TEXT", DbParams.TABLE_EVENTS_TIMER, DbParams.EVENTS_LOGIN_ID);
    private static final String EVENTS_TIME_ALERT_ANONYMOUS_ID =
            String.format("ALTER TABLE %s ADD %s TEXT", DbParams.TABLE_EVENTS_TIMER, DbParams.EVENTS_ANONYMOUS_ID);

    // 创建数据 events 缓存表
    private static final String CREATE_EVENTS_CACHE_TABLE =
            String.format("CREATE TABLE %s (_id INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT NOT NULL, %s INTEGER NOT NULL);", DbParams.TABLE_EVENTS_CACHE, DbParams.KEY_DATA, DbParams.KEY_CREATED_AT);

    SensorsDataDBHelper(Context context) {
        super(context, DbParams.DATABASE_NAME, null, DbParams.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        SALog.i(TAG, "Creating a new Sensors Analytics DB");
        db.execSQL(CREATE_EVENTS_TIMER_TABLE);
        db.execSQL(CREATE_EVENTS_TABLE);
        db.execSQL(EVENTS_TIME_INDEX);
        db.execSQL(CHANNEL_EVENT_PERSISTENT_TABLE);
        db.execSQL(CREATE_EVENTS_CACHE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        SALog.i(TAG, "Upgrading Database, execute SQL : " + EVENTS_TIME_ALERT_DISTINCT_ID);
        db.execSQL(CREATE_EVENTS_TIMER_TABLE);
        try {
            db.execSQL(EVENTS_TIME_ALERT_DISTINCT_ID);
        } catch (Exception ex) {
            //ignore
        }
        try {
            db.execSQL(EVENTS_TIME_ALERT_LOGIN_ID);
            db.execSQL(EVENTS_TIME_ALERT_ANONYMOUS_ID);
        } catch (Exception ex) {
            //ignore
        }
        db.execSQL(CHANNEL_EVENT_PERSISTENT_TABLE);
        // 版本 7 中添加
        db.execSQL(CREATE_EVENTS_CACHE_TABLE);
    }
}
