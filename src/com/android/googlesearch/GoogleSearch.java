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

import com.google.android.providers.GoogleSettings.Partner;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

/**
 * This class is purely here to get search queries and route them to
 * the global {@link Intent#ACTION_WEB_SEARCH}.
 */
public class GoogleSearch extends Activity {
    private static final String TAG = "GoogleSearch";

    // The template URL we should use to format google search requests.
    private String googleSearchUrlBase = null;

    // "source" parameter for Google search requests from unknown sources (e.g. apps). This will get
    // prefixed with the string 'android-' before being sent on the wire.
    final static String GOOGLE_SEARCH_SOURCE_UNKNOWN = "unknown";
    
    private LocationUtils mLocationUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocationUtils = LocationUtils.getLocationUtils(this);
        Intent intent = getIntent();
        String action = intent != null ? intent.getAction() : null;
        if (Intent.ACTION_WEB_SEARCH.equals(action) || Intent.ACTION_SEARCH.equals(action)) {
            handleWebSearchIntent(intent);
        }
        finish();
    }

    /**
     * NOTE: This function is similar to the one found in
     * com.google.android.providers.enhancedgooglesearch.Launcher. If you are changing this
     * make sure you change both.
     */
    private void handleWebSearchIntent(Intent intent) {
        String query = intent.getStringExtra(SearchManager.QUERY);
        if (TextUtils.isEmpty(query)) {
            Log.w(TAG, "Got search intent with no query.");
            return;
        }

        if (googleSearchUrlBase == null) {
            Locale l = Locale.getDefault();
            String language = l.getLanguage();
            String country = l.getCountry().toLowerCase();
            // Chinese and Portuguese have two langauge variants.
            if ("zh".equals(language)) {
                if ("cn".equals(country)) {
                    language = "zh-CN";
                } else if ("tw".equals(country)) {
                    language = "zh-TW";
                }
            } else if ("pt".equals(language)) {
                if ("br".equals(country)) {
                    language = "pt-BR";
                } else if ("pt".equals(country)) {
                    language = "pt-PT";
                }
            }
            googleSearchUrlBase = getResources().getString(
                    R.string.google_search_base, language, country)
                    + "client=ms-"
                    + Partner.getString(this.getContentResolver(), Partner.CLIENT_ID);
        }

        // If the caller specified a 'source' url parameter, use that and if not use default.
        Bundle appSearchData = intent.getBundleExtra(SearchManager.APP_DATA);
        String source = GOOGLE_SEARCH_SOURCE_UNKNOWN;
        if (appSearchData != null) {
            source = appSearchData.getString(SearchManager.SOURCE);
        }
        
        // The browser can pass along an application id which it uses to figure out which
        // window to place a new search into. So if this exists, we'll pass it back to
        // the browser. Otherwise, add our own package name as the application id, so that
        // the browser can organize all searches launched from this provider together.
        String applicationId = intent.getStringExtra(Browser.EXTRA_APPLICATION_ID);
        if (applicationId == null) {
            applicationId = getPackageName();
        }

        try {
            String searchUri = googleSearchUrlBase
                    + "&source=android-" + source
                    + "&q=" + URLEncoder.encode(query, "UTF-8");
            Intent launchUriIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(searchUri));
            launchUriIntent.putExtra(Browser.EXTRA_APPLICATION_ID, applicationId);
            launchUriIntent.putExtra(Browser.EXTRA_POST_DATA, getLocationData());
            launchUriIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launchUriIntent);
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "Error", e);
        }
    }
    
    private byte[] getLocationData() {
        byte[] postData = null;
        ContentResolver cr = getContentResolver();
        
        // Don't send any location if the system does not have GoogleSettingsProvider.
        if (!mLocationUtils.systemHasGoogleSettingsProvider()) return postData;
        
        if (!mLocationUtils.userRespondedToLocationOptIn()) {
            // Bring up the consent dialog if it the user has yet responded to it. We
            // will not send the location info for this query.
            mLocationUtils.showLocationOptIn();
        } else if (mLocationUtils.userAcceptedLocationOptIn() &&
                Settings.Secure.isLocationProviderEnabled(cr, LocationManager.NETWORK_PROVIDER)) {
            Location location = ((LocationManager) getSystemService(
                    Context.LOCATION_SERVICE)).getLastKnownLocation(
                            LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                StringBuilder str = new StringBuilder("action=devloc&sll=");
                str.append(location.getLatitude()).append(',').append(location.getLongitude());
                postData = str.toString().getBytes();
            }
        }
        return postData;
    }
}
