package com.python.companion.security.password;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.python.companion.R;
import com.python.companion.security.Guard;
import com.python.companion.security.ValidateCallback;

import org.signal.argon2.Argon2;
import org.signal.argon2.Argon2Exception;
import org.signal.argon2.MemoryCost;
import org.signal.argon2.Version;

import static org.signal.argon2.Type.Argon2id;


public class PassGuard extends Guard {
    public void setPass(@NonNull FragmentManager fragmentManager, @NonNull Context context) {
        setPass(fragmentManager, context, null);
    }

    public void setPass(@NonNull FragmentManager fragmentManager, @NonNull Context context, @Nullable ValidateCallback callback) {
        SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.pass_preferences), Context.MODE_PRIVATE);
        if (prefs.contains("p")) { // There already is a password set
            validate(fragmentManager, context, new ValidateCallback() {
                @Override
                public void onSuccess() {
                    updatePassInternal(fragmentManager, prefs);
                    if (callback != null)
                        callback.onSuccess();
                }

                @Override
                public void onFailure() {
                    if (callback != null)
                        callback.onFailure();
                }
            });
        } else {
            updatePassInternal(fragmentManager, prefs);
            callback.onSuccess();
        }
    }


    public static boolean passIsSet(@NonNull Context context) {
        return context.getSharedPreferences(context.getString(R.string.pass_preferences), Context.MODE_PRIVATE).contains("p");
    }

    /**
     * Function handling user validation. Sends a validation dialogfragment to user, requesting password
     */
    @Override
    protected void validate(@NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull ValidateCallback validateCallback) {
        if (!passIsSet(context)) {
            updatePassInternal(fragmentManager, context.getSharedPreferences(context.getString(R.string.pass_preferences), Context.MODE_PRIVATE), new ValidateCallback() {
                @Override
                public void onSuccess() {
                    passDialog(fragmentManager, context, validateCallback);
                }
                @Override
                public void onFailure() {
                    validateCallback.onFailure();
                }
            });
        } else {
            passDialog(fragmentManager, context, validateCallback);
        }
    }

    /** Boots up a password-requesting dialog. On success, the user entered the correct password */
    private void passDialog(@NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull ValidateCallback validateCallback) {
        PassDialog dialog = new PassDialog.Builder()
                .setAcceptListener(stayLoggedIn -> {
                    if (stayLoggedIn)
                        super.validated = true;
                    validateCallback.onSuccess();
                })
                .setCancelListener(validateCallback::onFailure)
                .build(password -> passCheck(context, password));

        dialog.show(fragmentManager, null);
    }

    /**
     * Takes a submitted password, checks it against actual password
     * @param password user-supplied password
     * @return {@code true} if password was correct, {@code false} otherwise
     */
    private boolean passCheck(@NonNull Context context, @NonNull byte[] password) {
        String password_actual = context.getSharedPreferences(context.getString(R.string.pass_preferences), Context.MODE_PRIVATE).getString("p", "");
        String password_current;
        try {
            password_current = hash(password);
        } catch (Argon2Exception e) {
            Log.e("PassGuard", "Hash exception: ", e);
            return false;
        }

        return (password_current.equals(password_actual));
    }


    /**
     * Asks user to give a password, then hashes and stores it.
     * (!) Make sure only authenticated users arrive here
     */
    private void updatePassInternal(@NonNull FragmentManager fragmentManager, @NonNull SharedPreferences prefs, @Nullable ValidateCallback validateCallback) {
        PassSetDialog dialog = new PassSetDialog.Builder()
                .setAcceptListener(newpass -> {
                    try {
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("p", hash(newpass));
                        editor.apply();
                        if (validateCallback != null)
                            validateCallback.onSuccess();
                    } catch (Argon2Exception e) {
                        Log.e("PassGuard", "Argon exception occurred: ", e);
                    }
                })
                .setCancelListener(() -> {
                    if (validateCallback != null)
                        validateCallback.onFailure();
                })
                .build(this);
        dialog.show(fragmentManager, null);
    }

    /**
     * Equivalent to {@link PassGuard#updatePassInternal(FragmentManager, SharedPreferences, ValidateCallback)}, without specifying callback
     * (!) Make sure only authenticated users arrive here
     */
    private void updatePassInternal(@NonNull FragmentManager fragmentManager, @NonNull SharedPreferences prefs) {
        updatePassInternal(fragmentManager, prefs, null);
    }

    private @NonNull String hash(@NonNull byte[] password) throws Argon2Exception {
        byte[] salt = {0x42, 0x00, 0x77, 0x52, 0x36, 0x4F, 0x55, 0x13, 0x51, 0x33, 0x24, 0x45, 0x31, 0x10, 0x04, 0x39};
        Argon2 argon2 = new Argon2.Builder(Version.V13)
                .type(Argon2id)
                .memoryCost(MemoryCost.MiB(32))
                .parallelism(1)
                .iterations(3)
                .hashLength(32)
                .build();
        Argon2.Result result = argon2.hash(password, salt);
        return result.getEncoded();
    }
}