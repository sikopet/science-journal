package com.google.android.apps.forscience.whistlepunk;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class PrefsSensorHistoryStorage implements SensorHistoryStorage {
    private static final String PREFS_FILE = "PrefsSensorHistoryStorage";
    private static final String KEY_MOST_RECENT_IDS = "prefsKey";

    private Context mContext;

    public PrefsSensorHistoryStorage(Context context) {
        mContext = context;
    }

    @Override
    public List<String> getMostRecentSensorIds() {
        return decodeList(
                getPrefs().getStringSet(KEY_MOST_RECENT_IDS, Collections.<String>emptySet()));
    }

    public SharedPreferences getPrefs() {
        return mContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
    }

    @Override
    public void setMostRecentSensorIds(List<String> ids) {
        getPrefs().edit().putStringSet(KEY_MOST_RECENT_IDS, encodeList(ids)).apply();
    }

    private Set<String> encodeList(List<String> ids) {
        Set<String> encoded = new HashSet<>();
        for (int i = 0; i < ids.size(); i++) {
            encoded.add(i + ":" + ids.get(i));
        }
        return encoded;
    }

    private List<String> decodeList(Set<String> ids) {
        String[] decoded = new String[ids.size()];
        for (String id : ids) {
            String[] split = id.split(":", 2);
            decoded[Integer.valueOf(split[0])] = split[1];
        }
        return Arrays.asList(decoded);
    }
}
