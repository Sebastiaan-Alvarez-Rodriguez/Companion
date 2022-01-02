package org.python.backend.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;


public class PasswordSecurityProvider extends Guard {
    public void setPass(@NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull FinishListener finishListener, @NonNull ErrorListener errorListener) {
        SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.pass_preferences), Context.MODE_PRIVATE);
        if (prefs.contains(context.getString(R.string.pass_preferences_key_pass))) { // There already is a password set
            validate(fragmentManager, context, () -> {
                updatePassInternal(fragmentManager, context);
                finishListener.onFinish();
            }, errorListener);
        } else {
            updatePassInternal(fragmentManager, context);
            finishListener.onFinish();
        }
    }


    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean passIsSet(@NonNull Context context) {
        return context.getSharedPreferences(context.getString(R.string.pass_preferences), Context.MODE_PRIVATE).contains(context.getString(R.string.pass_preferences_key_pass));
    }

    /**
     * Function handling user validation. Sends a validation dialogfragment to user, requesting password
     */
    @Override
    protected void validate(@NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull FinishListener finishListener, @NonNull ErrorListener errorListener) {
        if (!passIsSet(context)) {
            updatePassInternal(fragmentManager, context, new ValidateCallback() {
                @Override
                public void onSuccess() {
                    passDialog(fragmentManager, context, finishListener, errorListener);
                }
                @Override
                public void onFailure() {
                    errorListener.onError("Must setup password in order to use security features!");
                }
            });
        } else {
            passDialog(fragmentManager, context, finishListener, errorListener);
        }
    }

    /** Boots up a password-requesting dialog. On success, the user entered the correct password */
    private void passDialog(@NonNull FragmentManager fragmentManager, @NonNull Context context, @NonNull FinishListener finishListener, @NonNull ErrorListener errorListener) {
        PassDialog dialog = new PassDialog.Builder()
                .setAcceptListener(stayLoggedIn -> {
                    if (stayLoggedIn)
                        super.validated = true;
                    finishListener.onFinish();
                })
                .setCancelListener(() -> {
                    errorListener.onError("Uses canceled authentication");
                })
                .build(password -> passCheck(context, password));

        dialog.show(fragmentManager, null);
    }

    /**
     * Takes a submitted password, checks it against actual password
     * @param password user-supplied password
     * @return {@code true} if password was correct, {@code false} otherwise
     */
    private boolean passCheck(@NonNull Context context, @NonNull byte[] password) {
        String password_actual = context.getSharedPreferences(context.getString(R.string.pass_preferences), Context.MODE_PRIVATE).getString(context.getString(R.string.pass_preferences_key_pass), "");
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
    private void updatePassInternal(@NonNull FragmentManager fragmentManager, @NonNull Context context, @Nullable ValidateCallback validateCallback) {
        PassSetDialog dialog = new PassSetDialog.Builder()
                .setAcceptListener(newpass -> {
                    try {
                        SharedPreferences.Editor editor = context.getSharedPreferences(context.getString(R.string.pass_preferences), Context.MODE_PRIVATE).edit();
                        editor.putString(context.getString(R.string.pass_preferences_key_pass), hash(newpass));
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
     * Equivalent to {@link PasswordSecurityProvider#updatePassInternal(FragmentManager, Context, ValidateCallback)}, without specifying callback
     * (!) Make sure only authenticated users arrive here
     */
    private void updatePassInternal(@NonNull FragmentManager fragmentManager, @NonNull Context context) {
        updatePassInternal(fragmentManager, context, null);
    }

    private @NonNull String hash(@NonNull byte[] password) throws Argon2Exception {
        byte[] salt = {0x42, 0x00, 0x77, 0x52, 0x36, 0x4F, 0x55, 0x13, 0x51, 0x33, 0x24, 0x45, 0x31, 0x10, 0x04, 0x39};
        Argon2 argon2 = new Argon2.Builder(Version.V13)
                .type(Argon2id)
                .memoryCost(MemoryCost.MiB(128))
                .parallelism(2)
                .iterations(4)
                .hashLength(32)
                .build();
        Argon2.Result result = argon2.hash(password, salt);
        return result.getEncoded();
    }
}
