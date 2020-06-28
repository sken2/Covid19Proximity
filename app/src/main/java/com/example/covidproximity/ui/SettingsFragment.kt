package com.example.covidproximity.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.example.covidproximity.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}