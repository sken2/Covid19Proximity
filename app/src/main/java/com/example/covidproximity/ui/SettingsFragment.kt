package com.example.covidproximity.ui

import android.os.Bundle
import android.util.Log
import androidx.preference.PreferenceFragmentCompat
import com.example.covidproximity.Const
import com.example.covidproximity.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val pm = preferenceManager
        Log.v(Const.TAG, "SettingFragment::onCreatePreferences ${pm.sharedPreferencesName}")
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}