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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.util.TimeUtils;

import org.json.JSONObject;

class EventDataOperation extends DataOperation {

    EventDataOperation(Context context) {
        super(context);
        TAG = this.getClass().getSimpleName();
    }

    @Override
    int insertData(Uri uri, JSONObject jsonObject) {
        try {
            if (deleteDataLowMemory(uri) != 0) {
                return DbParams.DB_OUT_OF_MEMORY_ERROR;
            }
            ContentValues cv = new ContentValues();
            cv.put(DbParams.KEY_DATA, jsonObject.toString() + "\t" + jsonObject.toString().hashCode());
            cv.put(DbParams.KEY_CREATED_AT, System.currentTimeMillis());
            contentResolver.insert(uri, cv);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return 0;
    }

    @Override
    int insertData(Uri uri, ContentValues contentValues) {
        try {
            if (deleteDataLowMemory(uri) != 0) {
                return DbParams.DB_OUT_OF_MEMORY_ERROR;
            }
            contentResolver.insert(uri, contentValues);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return 0;
    }

    @Override
    String[] queryData(Uri uri, int limit) {
        Cursor cursor = null;
        String data = null;
        String last_id = null;
        try {
            cursor = contentResolver.query(uri, null, null, null, DbParams.KEY_CREATED_AT + " ASC LIMIT " + limit);
            if (cursor != null) {
                JSONObject jsonObject;
                StringBuilder dataBuilder = new StringBuilder(4 * 1024);
                final String flush_time = ",\"_flush_time\":";
                String suffix = ",";
                dataBuilder.append("[");
                while (cursor.moveToNext()) {
                    if (cursor.isLast()) {
                        suffix = "]";
                        last_id = cursor.getString(cursor.getColumnIndex("_id"));
                    }
                    try {
                        String keyData = cursor.getString(cursor.getColumnIndex(DbParams.KEY_DATA));
                        if (!TextUtils.isEmpty(keyData)) {
                            keyData = parseData(keyData);
                            jsonObject = new JSONObject(keyData);
                            long trackTime = jsonObject.optLong("time");
                            if (TimeUtils.isDateInvalid(trackTime)) {
                                long elapsedRealtime = jsonObject.optLong("SAElapsedRealtime");
                                jsonObject.put("time", System.currentTimeMillis() - (SystemClock.elapsedRealtime() - elapsedRealtime));
                            }
                            jsonObject.remove("SAElapsedRealtime");
                            keyData = jsonObject.toString();
                            if (!TextUtils.isEmpty(keyData)) {
                                dataBuilder.append(keyData, 0, keyData.length() - 1)
                                        .append(flush_time)
                                        .append(System.currentTimeMillis())
                                        .append("}").append(suffix);
                            }
                        }
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                }
                data = dataBuilder.toString();
            }
        } catch (final Exception e) {
            SALog.i(TAG, "Could not pull records for SensorsData out of database events. Waiting to send.", e);
            last_id = null;
            data = null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (last_id != null) {
            return new String[]{last_id, data, DbParams.GZIP_DATA_EVENT};
        }
        return null;
    }

    @Override
    void deleteData(Uri uri, String id) {
        super.deleteData(uri, id);
    }
}
