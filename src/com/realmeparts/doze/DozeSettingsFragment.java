/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2019 The LineageOS Project
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

package com.realmeparts.doze;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import androidx.fragment.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.widget.MainSwitchPreference;

import com.realmeparts.R;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

public class DozeSettingsFragment extends SettingsBasePreferenceFragment implements OnPreferenceChangeListener {

    private MainSwitchPreference mSwitchBar;
    private SwitchPreferenceCompat mAlwaysOnDisplayPreference;
    private SwitchPreferenceCompat mAodDt2wPreference;
    private SwitchPreferenceCompat mPickUpPreference;
    private SwitchPreferenceCompat mRaiseToWakePreference;
    private SwitchPreferenceCompat mPocketPreference;
    private SwitchPreferenceCompat mSmartWakePreference;

    private Handler mHandler = new Handler();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.doze_settings, rootKey);

        SharedPreferences prefs = getActivity().getSharedPreferences("doze_settings",
                Activity.MODE_PRIVATE);
        if (savedInstanceState == null && !prefs.getBoolean("first_help_shown", false)) {
            showHelp();
        }

        boolean dozeEnabled = DozeUtils.isDozeEnabled(getActivity());

        mSwitchBar = (MainSwitchPreference) findPreference(DozeUtils.DOZE_ENABLE);
        mSwitchBar.setOnPreferenceChangeListener(this);
        mSwitchBar.setChecked(dozeEnabled);
        
        mAlwaysOnDisplayPreference = (SwitchPreferenceCompat) findPreference(DozeUtils.ALWAYS_ON_DISPLAY);
        mAlwaysOnDisplayPreference.setEnabled(dozeEnabled);
        mAlwaysOnDisplayPreference.setChecked(DozeUtils.isAlwaysOnEnabled(getActivity()));
        mAlwaysOnDisplayPreference.setOnPreferenceChangeListener(this);

        PreferenceCategory pickupSensorCategory = (PreferenceCategory) getPreferenceScreen().
                findPreference(DozeUtils.CATEG_PICKUP_SENSOR);
        PreferenceCategory proximitySensorCategory = (PreferenceCategory) getPreferenceScreen().
                findPreference(DozeUtils.CATEG_PROX_SENSOR);

        SwitchPreferenceCompat raiseToWakeGesture = (SwitchPreferenceCompat) getPreferenceScreen().
                findPreference(DozeUtils.GESTURE_RAISE_TO_WAKE);

        mAodDt2wPreference = (SwitchPreferenceCompat) findPreference(DozeUtils.AOD_DT2W_KEY);
        mAodDt2wPreference.setEnabled(dozeEnabled);
        mAodDt2wPreference.setOnPreferenceChangeListener(this);

        mPickUpPreference = (SwitchPreferenceCompat) findPreference(DozeUtils.GESTURE_PICK_UP_KEY);
        mPickUpPreference.setEnabled(dozeEnabled);
        mPickUpPreference.setOnPreferenceChangeListener(this);

        mRaiseToWakePreference = (SwitchPreferenceCompat) findPreference(DozeUtils.GESTURE_RAISE_TO_WAKE_KEY);
        mRaiseToWakePreference.setEnabled(dozeEnabled);
        mRaiseToWakePreference.setOnPreferenceChangeListener(this);

        mPocketPreference = (SwitchPreferenceCompat) findPreference(DozeUtils.GESTURE_POCKET_KEY);
        mPocketPreference.setEnabled(dozeEnabled);
        mPocketPreference.setOnPreferenceChangeListener(this);

        mSmartWakePreference = (SwitchPreferenceCompat) findPreference(DozeUtils.GESTURE_SMART_WAKE_KEY);
        mSmartWakePreference.setEnabled(dozeEnabled);
        mSmartWakePreference.setOnPreferenceChangeListener(this);

        // Hide AOD if not supported and set all its dependents otherwise
        if (!DozeUtils.alwaysOnDisplayAvailable(getActivity())) {
            PreferenceCategory aodCategory = (PreferenceCategory) getPreferenceScreen().findPreference("always_on_display_category");
            if (aodCategory != null) {
                getPreferenceScreen().removePreference(aodCategory);
            } else {
                getPreferenceScreen().removePreference(mAlwaysOnDisplayPreference);
            }
            mAodDt2wPreference.setEnabled(false);
        } else {
            mAodDt2wPreference.setEnabled(DozeUtils.isAlwaysOnEnabled(getActivity()));
            mPickUpPreference.setDependency(DozeUtils.ALWAYS_ON_DISPLAY);
            pickupSensorCategory.setDependency(DozeUtils.ALWAYS_ON_DISPLAY);
            proximitySensorCategory.setDependency(DozeUtils.ALWAYS_ON_DISPLAY);
            raiseToWakeGesture.setDependency(DozeUtils.ALWAYS_ON_DISPLAY);
            mSmartWakePreference.setDependency(DozeUtils.GESTURE_PICK_UP_KEY);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (DozeUtils.DOZE_ENABLE.equals(preference.getKey())) {
            boolean isChecked = (Boolean) newValue;
            DozeUtils.enableDoze(getActivity(), isChecked);
            DozeUtils.checkDozeService(getActivity());

            if (!isChecked) {
                DozeUtils.enableAlwaysOn(getActivity(), false);
                mAlwaysOnDisplayPreference.setChecked(false);
            }
            mAlwaysOnDisplayPreference.setEnabled(isChecked);
            mAodDt2wPreference.setEnabled(isChecked && DozeUtils.isAlwaysOnEnabled(getActivity()));

            mPickUpPreference.setEnabled(isChecked);
            mRaiseToWakePreference.setEnabled(isChecked);
            mPocketPreference.setEnabled(isChecked);
            mSmartWakePreference.setEnabled(isChecked);
        } else if (DozeUtils.ALWAYS_ON_DISPLAY.equals(preference.getKey())) {
            DozeUtils.enableAlwaysOn(getActivity(), (Boolean) newValue);
            mAodDt2wPreference.setEnabled((Boolean) newValue);
        }

        mHandler.post(() -> {
            DozeUtils.checkDozeService(getActivity());
            DozeUtils.checkAodDt2w(getActivity());
        });

        return true;
    }

    public void showHelp() {
        HelpDialogFragment fragment = new HelpDialogFragment();
        fragment.show(getFragmentManager(), "help_dialog");
    }

    public static class HelpDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.doze_settings_help_title)
                    .setMessage(R.string.doze_settings_help_text)
                    .setNegativeButton(R.string.dialog_ok, (dialog, which) -> dialog.cancel())
                    .create();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            getActivity().getSharedPreferences("doze_settings", Activity.MODE_PRIVATE)
                    .edit()
                    .putBoolean("first_help_shown", true)
                    .commit();
        }
    }
}
