package org.crf.minutis;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.telephony.PhoneNumberUtils;

public class SettingsFragment extends PreferenceFragment
    implements OnSharedPreferenceChangeListener {

	static final String KEY_PHONE_NUMBER = "phone_number";
	private static final String KEY_RESET_SERVER_ADDRESS = "reset_server_address";
	static final String KEY_SERVER_ADDRESS = "server_address";

	private SharedPreferences mSharedPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

		mSharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

		String phoneNumber = mSharedPref.getString(KEY_PHONE_NUMBER, "");
		Preference phoneNumberPref = findPreference(KEY_PHONE_NUMBER);
		phoneNumberPref.setSummary(formatPhoneNumber(phoneNumber));

		String serverAddress = mSharedPref.getString(KEY_SERVER_ADDRESS, getString(R.string.all_undefined));
		Preference serverAddressPref = findPreference(KEY_SERVER_ADDRESS);
		serverAddressPref.setSummary(serverAddress);
    }

	@Override
	public void onResume() {
		super.onResume();
		mSharedPref.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		mSharedPref.unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}

	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (KEY_PHONE_NUMBER.equals(key)) {
            Preference pref = findPreference(KEY_PHONE_NUMBER);
            pref.setSummary(formatPhoneNumber(sp.getString(key, "")));
        } else if (KEY_SERVER_ADDRESS.equals(key)) {
            Preference pref = findPreference(KEY_SERVER_ADDRESS);
            pref.setSummary(sp.getString(key, getString(R.string.all_undefined)));
		}
    }

	public boolean onPreferenceTreeClick (PreferenceScreen ps, Preference p) {
		boolean ret;
		if (KEY_RESET_SERVER_ADDRESS.equals(p.getKey())) {
			mSharedPref.edit().putString(KEY_SERVER_ADDRESS,
			                             getString(R.string.default_server_address)).apply();
			ret = true;
		} else {
			ret = false;
		}
		return ret;
	}

	private String formatPhoneNumber(String phoneNumber) {
		String formatedNumber;
		if (phoneNumber.isEmpty()) {
			formatedNumber = getString(R.string.all_undefined);
		} else {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
				formatedNumber = PhoneNumberUtils.formatNumber(phoneNumber);
			} else {
				formatedNumber = PhoneNumberUtils.formatNumber(phoneNumber, "FR");
			}
		}
		return formatedNumber;
	}
}
