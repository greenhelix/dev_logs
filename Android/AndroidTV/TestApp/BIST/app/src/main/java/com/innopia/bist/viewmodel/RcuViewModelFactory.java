package com.innopia.bist.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;


public class RcuViewModelFactory implements ViewModelProvider.Factory {

	private final Application application;
	private final MainViewModel mainViewModel;

	public RcuViewModelFactory(Application application, MainViewModel mainViewModel) {
		this.application = application;
		this.mainViewModel = mainViewModel;
	}

	@NonNull
	@Override
	@SuppressWarnings("unchecked")
	public <T extends ViewModel> T create(Class<T> modelClass) {
		if (modelClass.isAssignableFrom(RcuTestViewModel.class)) {
			return (T) new RcuTestViewModel(application, mainViewModel);
		}
		throw new IllegalArgumentException("Unknown ViewModel class");
	}
}
