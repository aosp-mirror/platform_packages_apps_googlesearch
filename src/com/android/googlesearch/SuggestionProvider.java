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

import com.android.internal.database.ArrayListCursor;
import com.google.android.net.GoogleHttpClient;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Use network-based Google Suggests to provide search suggestions.
 * 
 * Future:  Merge live suggestions with saved recent queries
 */
public class SuggestionProvider extends ContentProvider {
    
    public static final Uri CONTENT_URI = Uri.parse(
            "content://com.android.googlesearch.SuggestionProvider");

    private static final String USER_AGENT = "Android/1.0";
    private String mSuggestUri;
    private static final int HTTP_TIMEOUT_MS = 1000;
    
    // TODO: this should be defined somewhere
    private static final String HTTP_TIMEOUT = "http.connection-manager.timeout";

    private static final String LOG_TAG = "GoogleSearch.SuggestionProvider";
    
    /* The suggestion columns used */
    private static final String[] COLUMNS = new String[] {
            "_id",
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_TEXT_2,
            SearchManager.SUGGEST_COLUMN_QUERY};

    private HttpClient mHttpClient;

    @Override
    public boolean onCreate() {
        mHttpClient = new GoogleHttpClient(getContext().getContentResolver(),
                USER_AGENT, false /* not gzip capable */);
        HttpParams params = mHttpClient.getParams();
        params.setLongParameter(HTTP_TIMEOUT, HTTP_TIMEOUT_MS);

        // NOTE:  Do not look up the resource here;  Localization changes may not have completed
        // yet (e.g. we may still be reading the SIM card).
        mSuggestUri = null;
        return true;
    }

    private static ArrayListCursor makeEmptyCursor() {
        return new ArrayListCursor(COLUMNS, new ArrayList<ArrayList>());
    }

    /**
     * This will always return {@link SearchManager#SUGGEST_MIME_TYPE} as this
     * provider is purely to provide suggestions.
     */
    @Override
    public String getType(Uri uri) {
        return SearchManager.SUGGEST_MIME_TYPE;
    }

    /**
     * Queries for a given search term and returns a cursor containing
     * suggestions ordered by best match.
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        String query = selectionArgs[0];
        if (TextUtils.isEmpty(query)) {
            
            /* Can't pass back null, things blow up */
            return makeEmptyCursor();
        }
        try {
            query = URLEncoder.encode(query, "UTF-8");
            // NOTE:  This code uses resources to optionally select the search Uri, based on the
            // MCC value from the SIM.  iThe default string will most likely be fine.  It is
            // paramerterized to accept info from the Locale, the language code is the first
            // parameter (%1$s) and the country code is the second (%2$s).  This code *must* 
            // function in the same way as a similar lookup in
            // com.android.browser.BrowserActivity#onCreate().  If you change
            // either of these functions, change them both.  (The same is true for the underlying
            // resource strings, which are stored in mcc-specific xml files.)
            if (mSuggestUri == null) {
                Locale l = Locale.getDefault();
                mSuggestUri = getContext().getResources().getString(R.string.google_search_base,
                                                                    l.getLanguage(), 
                                                                    l.getCountry().toLowerCase()) 
                        + "json=true&q=";
            }

            HttpPost method = new HttpPost(mSuggestUri + query);
            StringEntity content = new StringEntity("");
            method.setEntity(content);
            HttpResponse response = mHttpClient.execute(method);
            if (response.getStatusLine().getStatusCode() == 200) {
                
                /* Goto http://www.google.com/complete/search?json=true&q=foo
                 * to see what the data format looks like. It's basically a json
                 * array containing 4 other arrays. We only care about the middle
                 * 2 which contain the suggestions and their popularity.
                 */
                JSONArray results = new JSONArray(EntityUtils.toString(response.getEntity()));
                JSONArray suggestions = results.getJSONArray(1);
                JSONArray popularity = results.getJSONArray(2);
                return new SuggestionsCursor(suggestions, popularity);
            }
        } catch (UnsupportedEncodingException e) {
            Log.w(LOG_TAG, "Error", e);
        } catch (IOException e) {
            Log.w(LOG_TAG, "Error", e);
        } catch (JSONException e) {
            Log.w(LOG_TAG, "Error", e);
        }
        return makeEmptyCursor();
    }

    private static class SuggestionsCursor extends AbstractCursor {

        /* Contains the actual suggestions */
        final JSONArray mSuggestions;
        
        /* This contains the popularity of each suggestion
         * i.e. 165,000 results. It's not related to sorting.
         */
        final JSONArray mPopularity;
        public SuggestionsCursor(JSONArray suggestions, JSONArray popularity) {
            mSuggestions = suggestions;
            mPopularity = popularity;
        }

        @Override
        public int getCount() {
            return mSuggestions.length();
        }

        @Override
        public String[] getColumnNames() {
            return COLUMNS;
        }

        @Override
        public String getString(int column) {
            if ((mPos != -1)) {
                if ((column == 1) || (column == 3)) {
                    try {
                        return mSuggestions.getString(mPos);
                    } catch (JSONException e) {
                        Log.w(LOG_TAG, "Error", e);
                    }
                } else if (column == 2) {
                    try {
                        return mPopularity.getString(mPos);
                    } catch (JSONException e) {
                        Log.w(LOG_TAG, "Error", e);
                    }
                }
            }
            return null;
        }

        @Override
        public double getDouble(int column) {
            throw new UnsupportedOperationException();
        }

        @Override
        public float getFloat(int column) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getInt(int column) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getLong(int column) {
            if (column == 0) {
                return mPos;        // use row# as the _Id
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public short getShort(int column) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isNull(int column) {
            throw new UnsupportedOperationException();
        }
    }
    
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
