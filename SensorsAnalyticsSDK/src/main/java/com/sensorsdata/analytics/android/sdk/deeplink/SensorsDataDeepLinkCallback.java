/*
 * Created by chenru on 2020/07/18.
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

package com.sensorsdata.analytics.android.sdk.deeplink;

/**
 * DeepLink Callback
 */
public interface SensorsDataDeepLinkCallback {
    /**
     * @param params 链接设置的 App 内参数
     * @param success 是否请求成功
     * @param appAwakePassedTime 请求时长
     */
    void onReceive(String params, boolean success, long appAwakePassedTime);
}