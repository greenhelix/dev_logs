/*
 * Copyright (C) 2022 The Android Open Source Project
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

 package com.android.tv.settings.system.locale;


 import static com.android.tv.settings.system.locale.LanguagePickerFragment.KEY_LOCALE_INFO;
 
 import android.app.ActivityManager;
 import android.content.Context;
 import android.os.Build;
 import android.os.Bundle;
 import android.os.LocaleList;
 import android.os.Handler;
 import android.os.LocaleList;
 import android.os.Looper;
 import android.util.Log;
 
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
 
 /** Country picker settings screen for locale selection. */
 @Keep
 public class CountryPickerFragment extends SettingsPreferenceFragment {
	 private static final String EXTRA_PARENT_LOCALE = "PARENT_LOCALE";
	 private static final String TAG = "CountryPickerFragment";
	 private static final String COUNTRY_PICKER_RADIO_GROUP = "country_picker_group";
	//  private static final boolean DEBUG = Build.isDebuggable();
	 private static final boolean DEBUG = true;
 
	 private static final String ENGLISH = "en";
	 private static final String DEUTSCH = "de";
	 private static final String FIRST_ORDER_ENGLISH = "en-US";
	 private static final String FIRST_ORDER_DEUTSCH = "de-DE";
 
	 private LocaleDataViewModel mLocaleDataViewModel;
	 private Handler mHandler = new Handler(Looper.getMainLooper());
 
	 public static Bundle prepareArgs(LocaleStore.LocaleInfo localeInfo) {
		 Bundle b = new Bundle();
		 b.putSerializable(EXTRA_PARENT_LOCALE, localeInfo);
		 return b;
	 }
 
	 private class DTComparator implements Comparator<LocaleStore.LocaleInfo> 
	 {
		 @Override
		 public int compare(LocaleStore.LocaleInfo l1, LocaleStore.LocaleInfo l2)
		 {
			 if ( l1.getId().equals(FIRST_ORDER_DEUTSCH) || 
					 l1.getId().equals(FIRST_ORDER_ENGLISH) )
				 return -1;
 
			 return 1;
		 }
	 }
 
	 private int indexOf(String country, ArrayList<LocaleStore.LocaleInfo> list) {
		 int index = -1;
		 if ( country == null )
			 return index;
 
		 for ( int ii = 0; ii < list.size(); ii++ ) {
			 if ( list.get(ii).getId().equals(country) ) {
				 index = ii;
				 break;
			 }
		 }
 
		 return index;
	 }
 // INNOPIA
	 private ArrayList<LocaleStore.LocaleInfo> sortCountryList(
			 ArrayList<LocaleStore.LocaleInfo> countryList, LocaleStore.LocaleInfo parentLocale) {
		 String country = null;
		 if ( parentLocale != null && parentLocale.getId().equals(ENGLISH) ) {
			 country = FIRST_ORDER_ENGLISH;
		 } else if ( parentLocale != null && parentLocale.getId().equals(DEUTSCH) ) {
			 country = FIRST_ORDER_DEUTSCH;
		 } 
 
		 int index = indexOf(country, countryList);
		 if ( index != -1 ) {
			 countryList.add(0, countryList.remove(index));
		 }
 
		 return countryList;
	 }
// INNOPIA END
 
	 @Override
	 public void onCreatePreferences(Bundle savedInstanceState, String s) {
		 mLocaleDataViewModel = new ViewModelProvider(getActivity()).get(LocaleDataViewModel.class);
		 final Context themedContext = getPreferenceManager().getContext();
 
		 LocaleStore.LocaleInfo parentLocale = (LocaleStore.LocaleInfo) getArguments()
				 .getSerializable(EXTRA_PARENT_LOCALE);
		 final PreferenceScreen screen =
				 getPreferenceManager().createPreferenceScreen(themedContext);
 
		 boolean needSort = false;
		 if (parentLocale != null) {
			 screen.setTitle(parentLocale.getFullNameNative());
			 if ( DEBUG ) {
				 Log.d(TAG, "parentLocale - " + parentLocale.getId() + ", " + parentLocale.getFullNameNative());
			 }
			 if ( parentLocale.getId().equals(ENGLISH) || parentLocale.getId().equals(DEUTSCH) ) {
				 needSort = true;
			 }
		 }
		 Locale currentLocale = LocaleDataViewModel.getCurrentLocale();
		 ArrayList<LocaleStore.LocaleInfo> localeInfoCountryList = mLocaleDataViewModel
				 .getLocaleInfoList(parentLocale);
		 if ( needSort ){
			 //localeInfoCountryList.sort(new DTComparator());
			 localeInfoCountryList = sortCountryList(localeInfoCountryList, parentLocale);
		 }
 
		 Preference activePref = null;
		 if (localeInfoCountryList != null) {
			 for (LocaleStore.LocaleInfo localeInfo : localeInfoCountryList) {
				 //if ( DEBUG && needSort ) 
				 //	Log.d(TAG, "childLocale - " + localeInfo.getId() + ", " + localeInfo.getFullNameNative());
				 RadioPreference preference = new RadioPreference(getContext());
				 preference.setTitle(localeInfo.getFullCountryNameNative());
				 if (localeInfo.getLocale().equals(currentLocale)) {
					 activePref = preference;
					 preference.setChecked(true);
				 } else {
					 preference.setChecked(false);
				 }
				 preference.setRadioGroup(COUNTRY_PICKER_RADIO_GROUP);
				 preference.getExtras().putSerializable(KEY_LOCALE_INFO, localeInfo);
				 screen.addPreference(preference);
			 }
		 }
		 if (activePref != null) {
			 final Preference pref = activePref;
			 mHandler.post(() -> scrollToPreference(pref));
		 }
 
		 setPreferenceScreen(screen);
	 }
 
	 @Override
	 public boolean onPreferenceTreeClick(Preference preference) {
		 if (DEBUG) {
			 Log.d(TAG, "Preference clicked: " + preference.getTitle());
		 }
		 if (preference instanceof RadioPreference) {
			 RadioPreference localePref = (RadioPreference) preference;
			 if (!localePref.isChecked()) {
				 localePref.setChecked(true);
				 return true;
			 }
			 LocaleStore.LocaleInfo localeInfo = (LocaleStore.LocaleInfo)
					 localePref.getExtras().getSerializable(KEY_LOCALE_INFO);
			 if (localeInfo != null) {
				 getContext().getSystemService(ActivityManager.class).setDeviceLocales(
						 new LocaleList(localeInfo.getLocale()));
			 }
			 localePref.clearOtherRadioPreferences(getPreferenceScreen());
			 return true;
		 }
		 return super.onPreferenceTreeClick(preference);
	 }
 }
 