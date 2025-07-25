package com.innopia.bist.util;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A custom LiveData that only sends updates once.
 * This is useful for events like navigation, showing a SnackBar, or displaying a dialog,
 * which should only happen once and not be re-triggered on configuration changes (like screen rotation).
 *
 * @param <T> The type of data held by this instance.
 */
public class SingleLiveEvent<T> extends MutableLiveData<T> {

	// A flag to track whether the event has been handled.
	// Using AtomicBoolean for thread safety.
	private final AtomicBoolean mPending = new AtomicBoolean(false);

	/**
	 * Observes the LiveData, but only delivers the update if it hasn't been handled yet.
	 *
	 * @param owner    The LifecycleOwner which controls the observer.
	 * @param observer The observer that will receive the data.
	 */
	@MainThread
	@Override
	public void observe(@NonNull LifecycleOwner owner, @NonNull final Observer<? super T> observer) {
		// We wrap the original observer with our custom logic.
		super.observe(owner, t -> {
			// If mPending is true, and we successfully set it to false, then deliver the event.
			// compareAndSet is an atomic operation that ensures this check-and-set happens as a single unit.
			if (mPending.compareAndSet(true, false)) {
				observer.onChanged(t);
			}
		});
	}

	/**
	 * Sets the value. If there are active observers, the value will be dispatched to them.
	 * It also marks the event as pending.
	 *
	 * @param value The new value.
	 */
	@MainThread
	@Override
	public void setValue(@Nullable T value) {
		// Mark the event as pending before setting the value.
		mPending.set(true);
		super.setValue(value);
	}

	/**
	 * A convenience method for events that do not carry data.
	 */
	@MainThread
	public void call() {
		setValue(null);
	}
}
