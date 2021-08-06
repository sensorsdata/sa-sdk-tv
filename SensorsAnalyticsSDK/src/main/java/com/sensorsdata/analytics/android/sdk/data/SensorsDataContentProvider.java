/*
 * Created by wangzhuozhou on 2017/5/5.
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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;

public class SensorsDataContentProvider extends ContentProvider {
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final String TAG = "SA.SQL";

    private SensorsDataDBHelper dbHelper;
    private SAProviderHelper mProviderHelper;

    @Override
    public boolean onCreate() {
        try {
            Context context = getContext();
            if (context != null) {
                //这里是为了使用 ProviderTestRule
                String packageName;
                try {
                    packageName = context.getApplicationContext().getPackageName();
                } catch (UnsupportedOperationException e) {
                    packageName = "com.sensorsdata.analytics.android.sdk.test";
                }
                dbHelper = new SensorsDataDBHelper(context);
                mProviderHelper = new SAProviderHelper(context, dbHelper);
                mProviderHelper.appendUri(uriMatcher, packageName + ".SensorsDataContentProvider");
                /* 迁移数据，并删除老的数据库 */
                mProviderHelper.migratingDB(context, packageName);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int deletedCounts = 0;
        try {
            int code = uriMatcher.match(uri);
            return mProviderHelper.deleteEvents(code, selection, selectionArgs);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return deletedCounts;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // 不处理 values = null 或者 values 为空的情况
        if (values == null || values.size() == 0) {
            return uri;
        }
        try {
            int code = uriMatcher.match(uri);
            if (code == SAProviderHelper.URI_CODE.EVENTS) {
                return mProviderHelper.insertEvent(uri, DbParams.TABLE_EVENTS, values);
            } else if (code == SAProviderHelper.URI_CODE.CHANNEL_PERSISTENT) {
                return mProviderHelper.insertChannelPersistent(uri, values);
            } else if (code == SAProviderHelper.URI_CODE.EVENTS_CACHE) {
                return mProviderHelper.insertEvent(uri, DbParams.TABLE_EVENTS_CACHE, values);
            } else if (code == SAProviderHelper.URI_CODE.EVENTS_TIMER) {
                return mProviderHelper.insertEventTimer(uri, values);
            } else {
                mProviderHelper.insertPersistent(code, uri, values);
            }
            return uri;
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return uri;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        int numValues;
        SQLiteDatabase database = null;
        try {
            try {
                database = dbHelper.getWritableDatabase();
            } catch (SQLiteException e) {
                SALog.printStackTrace(e);
                return 0;
            }
            database.beginTransaction();
            numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                insert(uri, values[i]);
            }
            database.setTransactionSuccessful();
        } finally {
            if (database != null) {
                database.endTransaction();
            }
        }
        return numValues;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;
        try {
            int code = uriMatcher.match(uri);
            if (code == SAProviderHelper.URI_CODE.EVENTS) {
                cursor = mProviderHelper.queryByTable(DbParams.TABLE_EVENTS, projection, selection, selectionArgs, sortOrder);
            } else if (code == SAProviderHelper.URI_CODE.CHANNEL_PERSISTENT) {
                cursor = mProviderHelper.queryByTable(DbParams.TABLE_CHANNEL_PERSISTENT, projection, selection, selectionArgs, sortOrder);
            } else if (code == SAProviderHelper.URI_CODE.EVENTS_TIMER) {
                try {
                    cursor = dbHelper.getWritableDatabase().query(DbParams.TABLE_EVENTS_TIMER, projection, selection, selectionArgs, null, null, sortOrder);
                } catch (SQLiteException e) {
                    SALog.printStackTrace(e);
                }
            } else if (code == SAProviderHelper.URI_CODE.EVENTS_CACHE) {
                cursor = mProviderHelper.queryByTable(DbParams.TABLE_EVENTS_CACHE, projection, selection, selectionArgs, sortOrder);
            } else {
                cursor = mProviderHelper.queryPersistent(code);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        try {
            int code = uriMatcher.match(uri);
            if (code == SAProviderHelper.URI_CODE.EVENTS_TIMER) {
                return dbHelper.getWritableDatabase().update(DbParams.TABLE_EVENTS_TIMER, values, selection, selectionArgs);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return 0;
    }
}
