/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.Settings.System;

/**
 * Activity for setting Google search preferences.
 */
public class Settings extends PreferenceActivity implements OnPreferenceClickListener {

    private static final String SHOW_WEB_SUGGESTIONS_PREF = "show_web_suggestions";

    private CheckBoxPreference mShowWebSuggestionsPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        mShowWebSuggestionsPreference = (CheckBoxPreference)
                findPreference(SHOW_WEB_SUGGESTIONS_PREF);
        mShowWebSuggestionsPreference.setOnPreferenceClickListener(this);
    }

    public synchronized boolean onPreferenceClick(Preference preference) {
        if (preference == mShowWebSuggestionsPreference) {
            System.putInt(
                    getContentResolver(),
                    System.SHOW_WEB_SUGGESTIONS,
                    mShowWebSuggestionsPreference.isChecked() ? 1 : 0);
            return true;
        }
        return false;
    }

}
