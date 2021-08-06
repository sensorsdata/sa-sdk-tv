/*
 * Created by dengshiwei on 2021/06/04.
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

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.encrypt.SensorsDataEncrypt;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class DataManagerOperation extends DataOperation {
    private final Uri uriDataManager = Uri.parse("content://com.sensorsdata.manager.SensorsDataContentProvider/events");
    private SensorsDataEncrypt mSensorsDataEncrypt;
    private EventDataOperation mEventDataOperation;

    DataManagerOperation(Context context, SensorsDataEncrypt sensorsDataEncrypt) {
        super(context);
        this.mSensorsDataEncrypt = sensorsDataEncrypt;
        mEventDataOperation = new EventDataOperation(context);
    }

    @Override
    int insertData(Uri uri, JSONObject jsonObject) {
        try {
            if (deleteDataLowMemory(uriDataManager) != 0) {
                return DbParams.DB_OUT_OF_MEMORY_ERROR;
            }
            if (mSensorsDataEncrypt != null) {
                jsonObject = mSensorsDataEncrypt.encryptTrackData(jsonObject);
            }
            ContentValues cv = new ContentValues();
            cv.put(DbParams.KEY_DATA, jsonObject.toString() + "\t" + jsonObject.toString().hashCode());
            cv.put(DbParams.KEY_CREATED_AT, System.currentTimeMillis());
            try {
                contentResolver.insert(uriDataManager, cv);
                sendCacheData();
            } catch (Exception exception) { // 出现异常或 ContentProvider 不存在时
                SALog.printStackTrace(exception);
                mEventDataOperation.insertData(DbParams.getInstance().getEventCacheUri(), cv);
            }
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return 0;
    }

    @Override
    int insertData(Uri uri, ContentValues contentValues) {
        return 0;
    }

    @Override
    String[] queryData(Uri uri, int limit) {
        return null;
    }

    /**
     * 发送缓存数据到公共服务
     */
    private void sendCacheData() {
        Cursor cursor = null;
        Uri eventCache = DbParams.getInstance().getEventCacheUri();
        JSONArray idArray = null;
        try {
            cursor = contentResolver.query(eventCache, null, null, null, DbParams.KEY_CREATED_AT + " ASC");
            if (cursor != null && cursor.getCount() > 0) {
                idArray = new JSONArray();
                while (cursor.moveToNext()) {
                    String keyData = cursor.getString(cursor.getColumnIndex(DbParams.KEY_DATA));
                    String KEY_CREATED_AT = cursor.getString(cursor.getColumnIndex(DbParams.KEY_DATA));
                    ContentValues cv = new ContentValues();
                    cv.put(DbParams.KEY_DATA, keyData);
                    cv.put(DbParams.KEY_CREATED_AT, KEY_CREATED_AT);
                    contentResolver.insert(uriDataManager, cv);
                    idArray.put(cursor.getString(cursor.getColumnIndex("_id")));
                }
            }
        } catch (Exception exception) {
            SALog.printStackTrace(exception);
        } finally {
            try {
                if (idArray != null) {
                    String whereCause = String.format("DELETE FROM %s WHERE _id in %s", DbParams.TABLE_EVENTS_CACHE, buildIds(idArray));
                    contentResolver.delete(eventCache, whereCause, null);
                }
            } catch (Exception e) {
                SALog.printStackTrace(e);
            }
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * 构造 SQL 中的 id 集合
     *
     * @param idArray id 集合
     * @return SQL 中 id 语句
     */
    private String buildIds(JSONArray idArray) throws JSONException {
        StringBuilder idArgs = new StringBuilder();
        idArgs.append("(");
        if (idArray != null && idArray.length() > 0) {
            for (int index = 0; index < idArray.length(); index++) {
                idArgs.append(idArray.get(index)).append(",");
            }
            idArgs.replace(idArgs.length() - 1, idArgs.length(), "");
        }
        idArgs.append(")");
        return idArgs.toString();
    }
}
