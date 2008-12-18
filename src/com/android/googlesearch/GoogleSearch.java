/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.googlesearch;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.text.TextUtils;

/**
 * This class is purely here to get search queries and route them to
 * the global {@link Intent#ACTION_WEB_SEARCH}.
 */
public class GoogleSearch extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if ((intent != null) && Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            if (!TextUtils.isEmpty(query)) {
                // forward query to browser for Google search
                Intent search = new Intent(Intent.ACTION_WEB_SEARCH);
                search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                search.putExtra(SearchManager.QUERY, query);
                final Bundle appData = intent.getBundleExtra(SearchManager.APP_DATA);
                if (appData != null) {
                    search.putExtra(SearchManager.APP_DATA, appData);
                }
                startActivity(search);
            }
        }
        finish();
    }
}
