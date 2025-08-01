package com.innopia.bist.util;

import android.content.Context;
import android.view.KeyEvent;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SecretCodeManager {

    private final Context context;
    // Define the secret key sequence (Up, Up, Down, Down, Left, Right, Left, Right)
    private static final List<Integer> KONAMI_CODE = Arrays.asList(
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT
    );

    private static final List<Integer> ERROR_RESET = Arrays.asList(
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_DOWN
    );

    private final List<Integer> inputSequence = new ArrayList<>();

    /**
     * Constructor that requires context to show Toasts.
     * @param context The application or activity context.
     */
    public SecretCodeManager(Context context) {
        this.context = context;
    }

    /**
     * Processes a key press event to check if it matches the secret code.
     * @param keyCode The key code from the onKeyDown event.
     * @return true if the secret code sequence is completed, false otherwise.
     */
    public boolean onKeyPressed(int keyCode) {
        inputSequence.add(keyCode);

        // If the input sequence is longer than the Konami code, trim it from the start.
        if (inputSequence.size() > KONAMI_CODE.size()) {
            inputSequence.remove(0);
        }

        // Check if the current input sequence matches the Konami code.
        if (inputSequence.equals(KONAMI_CODE)) {
            // Reset the sequence for the next attempt.
            inputSequence.clear();
            return true;
        }

        return false;
    }

    /**
     * Shows a toast message indicating the new state of the focus highlight feature.
     * @param isEnabled The new state of the feature.
     */
    public void showToast(boolean isEnabled) {
        String message = isEnabled ? "focus highlight ON" : "focus highlight OFF";
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
