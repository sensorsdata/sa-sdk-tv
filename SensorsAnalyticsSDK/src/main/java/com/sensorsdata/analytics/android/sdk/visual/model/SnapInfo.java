/*
 * Created by zhangxiangwei on 2020/02/28.
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

package com.sensorsdata.analytics.android.sdk.visual.model;

import java.util.List;

public class SnapInfo {
    public String screenName;
    public boolean hasFragment;
    public String activityTitle;
    public int elementLevel = -1;
    public boolean isWebView = false;
    public List<WebNodeInfo.AlertInfo> alertInfos;
    public String webViewUrl;
    public float webViewScale;
    public String webLibVersion;
}
