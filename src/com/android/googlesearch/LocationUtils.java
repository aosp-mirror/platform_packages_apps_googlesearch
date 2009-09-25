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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/**
 * Utility methods for dealing with location (such as opt-in stuff).
 */
public class LocationUtils {
    private Context mContext;
    
    // The singleton object.
    private static LocationUtils sLocationUtils;
        
    /**
     * Gets the singleton.
     */
    public static synchronized LocationUtils getLocationUtils(Context context) {
        if (sLocationUtils == null) {
            sLocationUtils = new LocationUtils(context);
        }
        return sLocationUtils;
    }
    
    /**
     * Private constructor for singleton class; use {@link #getLocationUtils(Context)}.
     */
    private LocationUtils(Context context) {
        mContext = context;
    }
    
    /**
     * Identifies whether this system has the GoogleSettingsProvider, which determines
     * whether the other methods in this class are relevant, or if we should just avoid
     * using location.
     */
    public boolean systemHasGoogleSettingsProvider() {
        try {
            return mContext.getPackageManager().getPackageInfo(
                    "com.google.android.providers.settings", 0) != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Checks whether the user has responded (either positively or negatively) to the
     * Google location opt-in.
     */
    public boolean userRespondedToLocationOptIn() {
        return android.provider.Settings.Secure.getInt(mContext.getContentResolver(),
                android.provider.Settings.Secure.USE_LOCATION_FOR_SERVICES, 2) != 2;
    }
    
    /**
     * Shows the location opt-in because the user has not yet responded to it. If
     * we have GoogleSettingsProvider, this fires up the 'privacy' settings
     * and requests to show the opt-in. If we do not, this does nothing.
     */
    public void showLocationOptIn() {
        if (systemHasGoogleSettingsProvider()) {
            Intent consent = new Intent(
                    android.provider.Settings.ACTION_PRIVACY_SETTINGS);
            consent.putExtra("SHOW_USE_LOCATION", true);
            consent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(consent);
        }
    }
    
    /**
     * Indicates whether the user has accepted the Google location opt-in. Checks the appropriate
     * setting depending on whether we are using the GoogleSettingsProvider setting or our
     * own package-local setting.
     * 
     * If the answer is false, it could be because the user responded negatively to the opt-in,
     * or because the system does not have GoogleSettingsProvider. Use
     * {@link #userRespondedToLocationOptIn()} to distinguish between these two cases.
     */
    public boolean userAcceptedLocationOptIn() {
        if (systemHasGoogleSettingsProvider()) {
            return android.provider.Settings.Secure.getInt(mContext.getContentResolver(),
                    android.provider.Settings.Secure.USE_LOCATION_FOR_SERVICES, 2) == 1;
        } else {
            return false;
        }
    }
}
