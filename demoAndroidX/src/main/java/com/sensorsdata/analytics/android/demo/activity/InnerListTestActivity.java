/*
 * Created by zhangxiangwei on 2020/02/26.
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

package com.sensorsdata.analytics.android.demo.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sensorsdata.analytics.android.demo.R;
import com.sensorsdata.analytics.android.demo.custom.HorizonRecyclerDivider;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class InnerListTestActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_list);
        RecyclerView recyclerView  = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        recyclerView.addItemDecoration(new HorizonRecyclerDivider(this, HorizonRecyclerDivider.VERTICAL_LIST));
        List list = new ArrayList();
        for(int i = 0 ;i < 10;i++){
            list.add(i+"");
        }
        TestInnerGridViewAdapter testListAdapter = new TestInnerGridViewAdapter(this, list);
        recyclerView.setAdapter(testListAdapter);
    }
}
