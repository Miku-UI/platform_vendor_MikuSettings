/*
 * Copyright (C) 2026 Miku UI
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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.miku.settings.preferences.CustomImagePickerDialog;
import com.miku.settings.preferences.CustomImageStore;

public class ControlCenterSettings extends SettingsPreferenceFragment
        implements CustomImagePickerDialog.Callbacks {

    private static final String TAG = "ControlCenterSettings";
    private static final String KEY_QS_HEADER_IMAGE = "qs_header_image";
    private static final String SYSUI_PACKAGE = "com.android.systemui";
    private static final String SYSUI_HEADER_DRAWABLE = "statusbar_miku";

    private ActivityResultLauncher<Intent> mPickerLauncher;
    private CustomImagePickerDialog mDialog;
    private Drawable mDefaultHeader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.control_center_settings);

        mDefaultHeader = CustomImageStore.loadDrawableFromPackage(
                getActivity(), SYSUI_PACKAGE, SYSUI_HEADER_DRAWABLE);

        if (CustomImageStore.exists(getActivity())) {
            String path = CustomImageStore.publish(getActivity());
            if (path != null) {
                writeHeaderSettings(true, path);
            }
        } else if (Settings.System.getIntForUser(getContentResolver(),
                Settings.System.MIKU_QS_HEADER_CUSTOM, 0, UserHandle.USER_CURRENT) == 1) {
            writeHeaderSettings(false, null);
        }

        mPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                        return;
                    }
                    Uri uri = result.getData().getData();
                    applyPickedImage(uri);
                });

        Preference headerPref = findPreference(KEY_QS_HEADER_IMAGE);
        if (headerPref != null) {
            headerPref.setOnPreferenceClickListener(preference -> {
                showHeaderImageDialog();
                return true;
            });
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.MIKU;
    }

    @Override
    public void onDestroy() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        mDialog = null;
        super.onDestroy();
    }

    @Override
    public void onRestoreDefault() {
        Activity activity = getActivity();
        if (activity != null) {
            CustomImageStore.delete(activity);
        }
        writeHeaderSettings(false, null);
        refreshDialogPreview();
    }

    @Override
    public void onPickFromGallery() {
        mPickerLauncher.launch(CustomImageStore.createPickImageIntent(getActivity()));
    }

    @Override
    public void onAlphaChanged(int alpha) {
        Settings.System.putIntForUser(getContentResolver(),
                Settings.System.MIKU_QS_HEADER_ALPHA, alpha, UserHandle.USER_CURRENT);
    }

    private void showHeaderImageDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            return;
        }
        int alpha = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.MIKU_QS_HEADER_ALPHA, 100, UserHandle.USER_CURRENT);
        mDialog = new CustomImagePickerDialog(
                getActivity(),
                getString(R.string.qs_header_image_title),
                getString(R.string.qs_header_image_restore),
                getString(R.string.qs_header_image_pick),
                getString(R.string.qs_header_image_alpha),
                alpha,
                this);
        refreshDialogPreview();
        mDialog.show();
    }

    private void applyPickedImage(Uri uri) {
        Activity activity = getActivity();
        if (activity == null || uri == null) {
            return;
        }
        new Thread(() -> {
            boolean ok = CustomImageStore.saveFromUri(activity, uri);
            String path = ok ? CustomImageStore.publish(activity) : null;
            activity.runOnUiThread(() -> {
                if (path == null) {
                    Toast.makeText(activity, R.string.qs_header_image_load_failed,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                writeHeaderSettings(true, path);
                refreshDialogPreview();
                Log.i(TAG, "Published QS header image to " + path);
            });
        }, "qs-header-copy").start();
    }

    private void refreshDialogPreview() {
        if (mDialog == null) {
            return;
        }
        Bitmap custom = CustomImageStore.load(getActivity());
        if (custom != null) {
            mDialog.setPreviewBitmap(custom);
        } else if (mDefaultHeader != null) {
            mDialog.setPreviewDrawable(mDefaultHeader);
        }
    }

    private void writeHeaderSettings(boolean custom, String publishedPath) {
        ContentResolver resolver = getContentResolver();
        Settings.System.putIntForUser(resolver, Settings.System.MIKU_QS_HEADER_CUSTOM,
                custom ? 1 : 0, UserHandle.USER_CURRENT);
        Settings.System.putStringForUser(resolver, Settings.System.MIKU_QS_HEADER_PATH,
                custom ? publishedPath : null, UserHandle.USER_CURRENT);
        int revision = Settings.System.getIntForUser(resolver,
                Settings.System.MIKU_QS_HEADER_REVISION, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver, Settings.System.MIKU_QS_HEADER_REVISION,
                revision + 1, UserHandle.USER_CURRENT);
    }
}
