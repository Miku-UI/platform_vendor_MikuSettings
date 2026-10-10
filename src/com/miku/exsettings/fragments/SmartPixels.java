/*
 * Copyright (C) 2021-2023 Miku UI
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

package com.miku.exsettings.fragments;

import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class SmartPixels extends SettingsPreferenceFragment {

    private static final String TAG = "SmartPixels";

    private static final String SMART_PIXELS_ENABLE = "smart_pixels_enable";
    private static final String SMART_PIXELS_FOOTER = "smart_pixels_footer";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.smart_pixels);

        findPreference(SMART_PIXELS_FOOTER).setTitle(R.string.smart_pixels_warning_text);

        if (!getResources().getBoolean(
                com.android.internal.R.bool.config_supportSmartPixels)) {
            PreferenceScreen screen = getPreferenceScreen();
            for (int i = 0; i < screen.getPreferenceCount(); i++) {
                Preference pref = screen.getPreference(i);
                if (!SMART_PIXELS_FOOTER.equals(pref.getKey())) {
                    pref.setEnabled(false);
                }
            }
            findPreference(SMART_PIXELS_ENABLE).setSummary(R.string.smart_pixels_not_supported);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.MIKU;
    }
}
