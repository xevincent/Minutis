package org.crf.minutis;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.design.widget.Snackbar;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

import java.net.MalformedURLException;
import java.net.URL;

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

		String serverAddress = mSharedPref.getString(KEY_SERVER_ADDRESS, "").trim();
		if (serverAddress.isEmpty()) {
			serverAddress = getString(R.string.pref_default_server_address);
		}
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
			String phone = sp.getString(key, "");
			if (phoneNumberIsValid(phone)) {
				PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
				try {
					PhoneNumber numberProto = phoneUtil.parse(phone, "FR");
					phone = phoneUtil.format(numberProto, PhoneNumberFormat.NATIONAL)
					    .replaceAll("\\s","");
				} catch (NumberParseException e) {
				}
				sp.edit().putString(SettingsFragment.KEY_PHONE_NUMBER, phone).apply();
			} else {
				sp.edit().remove(KEY_PHONE_NUMBER).apply();
			}
			Preference pref = findPreference(KEY_PHONE_NUMBER);
			pref.setSummary(formatPhoneNumber(sp.getString(key, "")));
        } else if (KEY_SERVER_ADDRESS.equals(key)) {
            Preference pref = findPreference(KEY_SERVER_ADDRESS);
			String serverAddress = sp.getString(key, "").trim();
			if (serverAddress.isEmpty()) {
				sp.edit().remove(KEY_SERVER_ADDRESS).apply();
				serverAddress = getString(R.string.pref_default_server_address);
			} else  {
				try {
					new URL(serverAddress);
				} catch (MalformedURLException ex) {
					sp.edit().remove(KEY_SERVER_ADDRESS).apply();
					serverAddress = getString(R.string.pref_server_address_malformed);
				}
			}
			pref.setSummary(serverAddress);
		}
    }

	public boolean onPreferenceTreeClick (PreferenceScreen ps, Preference p) {
		boolean ret;
		if (KEY_RESET_SERVER_ADDRESS.equals(p.getKey())) {
			String address = mSharedPref.getString(KEY_SERVER_ADDRESS, "").trim();
			if (!address.isEmpty()) {
				mSharedPref.edit().remove(KEY_SERVER_ADDRESS).apply();
				undoReset(address);
			}
			ret = true;
		} else {
			ret = false;
		}
		return ret;
	}

	private boolean phoneNumberIsValid(String phone) {
		boolean ret;
		PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
		try {
			PhoneNumber numberProto = phoneUtil.parse(phone, "FR");
			ret = phoneUtil.isValidNumber(numberProto);
		} catch (NumberParseException e) {
			ret = false;
		}
		return ret;
	}

	private String formatPhoneNumber(String phoneNumber) {
		String formatedNumber;
		PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
		try {
			PhoneNumber numberProto = phoneUtil.parse(phoneNumber, "FR");
			formatedNumber = phoneUtil.format(numberProto, PhoneNumberFormat.NATIONAL);
		} catch (NumberParseException e) {
			formatedNumber = getString(R.string.all_undefined);
		}
		return formatedNumber;
	}

	private void undoReset(final String address) {
		Snackbar sb = Snackbar.make(getView(), R.string.pref_server_address_reset, Snackbar.LENGTH_LONG)
		    .setAction(R.string.all_cancel, new View.OnClickListener() {
		    	@Override
		    	public void onClick(View v) {
		    		mSharedPref.edit().putString(KEY_SERVER_ADDRESS, address).apply();
		    	}
		    });
		int sbTextId = android.support.design.R.id.snackbar_text;
		TextView tv = (TextView) sb.getView().findViewById(sbTextId);
		tv.setTextColor(Color.WHITE);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		sb.show();
	}
}
