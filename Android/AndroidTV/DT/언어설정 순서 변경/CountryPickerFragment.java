package com.android.tv.settings.system.locale;

import static com.android.tv.settings.system.locale.LaunagePickerFragment.KEY_LOCALE_INFO;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.LocaleList;
import android.os.Handler;
import android.os.Bundle;
import android.os.Looper;
import android.util.log;

import androidx.annotation.Keep;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.app.LocaleHelper;
import com.android.internal.app.LocaleStore;
import com.android.tv.settings.RadioPreference;
import com.android.tv.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

@Keep
public class CountryPickerFragment extends SettingsPreferenceFragment{
	private static final String EXTRA_PARENT_LOCALE = "PARENT_LOCALE";
	private static final String TAG = "CountryPickerFragment";
	private static final String COUNTRY_PICKER_RADIO_GROUP;
	private static final boolean DEBUG = Build.isDebugable();
	private static final String ENGLISH = "en";
	private static final String DEUTSCH = "de";
	private static final String FIRST_ORDER_ENGLISH = "en-US";
	private static final String FIRST_ORDER_DEUSTCH = "de-DE";

	private LocaleDataViewModel mLocaleDataViewModel;
	private Handler mHandler = new Handler(Looper.getMainLooper());

	public static Bundle prepareArgs(LocaleStore.LocaleInfo localeInfo){
		Bundle b = new Bundele();
		b.putSerializable(EXTRA_PARENT_LOCALE, localeInfo);
		return b;
	}

	private int indexOf(String country, ArrayList<LocaleStore.LocaleInfo> list){
		int index = -1;
		if(country == null) return index;
		for(int ii = 0; ii < list.size(); ii++){
			if(list.get(ii).getId().equals(country)){
				index = ii;
				break;
			}
		}
		return index;
	}

	private ArrayList<LocaleStore.LocaleInfo> sortCountryList(ArrayList<LocaleStore.LocaleInfo> countryList, LocaleStore.LocaleInfo parentLocale){
		String country = null;
		if(parentLocale != null && parentLocale.getId().equals(ENGLISH)) {
			country = FIRST_ORDER_ENGLISH;
		}else if(parentLocale != null && parentLocale.getId().equals(DEUTSCH)){
			country = FIRST_ORDER_DEUSTCH;
		}

		int index = indexOf(country, countryList);
		if(index != -1){
			countryList.add(0, countryList.remove(index));
		}
		return countryList;
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String s){
		mLocaleDataViewModel = new ViewModelProvider(getActivity()).get(LocaleDataViewModel.class);
		final Context themedContext = getPreferenceManager().getContext();
		LocaleStore.LocaleInfo parentLocale = (LocaleStore.LocaleInfo) getArguments().getSerializable(EXTRA_PARENT_LOCALE);
		final PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(themedContext);
		boolean neddSort = false;
		
	}



}
