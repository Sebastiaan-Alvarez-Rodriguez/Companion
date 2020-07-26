package com.python.companion.security.converters;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.python.companion.db.entity.Note;
import com.python.companion.security.Guard;
import com.python.companion.util.genericinterfaces.ErrorListener;
import com.python.companion.util.genericinterfaces.ResultListener;

import org.msgpack.core.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

/**
 * Class containing convenient support classes to convert notes between plaintext and ciphertext
 */
public class NoteConverter {

    /**
     * Object to handle one Note encryption
     */
    public static class Encrypter {
        private @NonNull FragmentManager manager;
        private @NonNull Context context;

        private @Nullable ResultListener<Note> finishListener;
        private @Nullable ErrorListener errorListener;

        public static Encrypter from(@NonNull FragmentManager manager, @NonNull Context context) {
            return new Encrypter(manager, context);
        }

        protected Encrypter(@NonNull FragmentManager manager, @NonNull Context context) {
            this.manager = manager;
            this.context = context;
        }

        /**
         * Sets a <code>ResultListener</code>. Called when encryption finished without fatal errors.
         * To get a call when encryption had a fatal error, see {@link #setOnErrorListener(ErrorListener)}
         * @param listener Object to receive call on finish
         */
        public Encrypter setOnFinishListener(@NonNull ResultListener<Note> listener) {
            this.finishListener = listener;
            return this;
        }

        /**
         * Sets a <code>ErrorListener</code>. Called when encryption finished with one/more fatal errors.
         * To get a call when encryption had success, see {@link #setOnFinishListener(ResultListener)}
         * @param listener Object to receive call on fatal error occurrence
         */
        public Encrypter setOnErrorListener(@NonNull ErrorListener listener) {
            this.errorListener = listener;
            return this;
        }


        public void encrypt(@NonNull Note note) {
            Guard.getGuard().encrypt(note, manager, context, new Guard.EncryptedCallback() {
                @Override
                public void onEncrypted(@NonNull Note n) {
                    if (finishListener != null)
                        finishListener.onResult(n);
                }

                @Override
                public void onEncryptFailure() {
                    if (errorListener != null)
                        errorListener.onError("Unknown error during encryption encountered");
                }
            }, error -> {
                if (errorListener != null)
                    errorListener.onError(error);
            });
        }
    }


    /**
     * Object to handle one Note decryption
     */
    public static class Decrypter {
        private @NonNull FragmentManager manager;
        private @NonNull Context context;

        private @Nullable ResultListener<Note> finishListener;
        private @Nullable ErrorListener errorListener;

        public static Decrypter from(@NonNull FragmentManager manager, @NonNull Context context) {
            return new Decrypter(manager, context);
        }

        protected Decrypter(@NonNull FragmentManager manager, @NonNull Context context) {
            this.manager = manager;
            this.context = context;
        }

        /**
         * Sets a <code>ResultListener</code>. Called when decryption finished without fatal errors.
         * To get a call when decryption had a fatal error, see {@link #setOnErrorListener(ErrorListener)}
         * @param listener Object to receive call on finish
         */
        public Decrypter setOnFinishListener(@NonNull ResultListener<Note> listener) {
            this.finishListener = listener;
            return this;
        }

        /**
         * Sets a <code>ErrorListener</code>. Called when decryption finished with one/more fatal errors.
         * To get a call when decryption had success, see {@link #setOnFinishListener(ResultListener)}
         * @param listener Object to receive call on fatal error occurrence
         */
        public Decrypter setOnErrorListener(@NonNull ErrorListener listener) {
            this.errorListener = listener;
            return this;
        }


        public void decrypt(@NonNull Note note) {
            if (!note.isSecure() || note.getIv() == null) {
                if (errorListener != null)
                    errorListener.onError("Note already plaintext");
                return;
            }

            Guard.getGuard().decrypt(note, manager, context, new Guard.DecryptedCallback() {
                @Override
                public void onDecrypted(@NonNull Note note) {
                    if (finishListener != null)
                        finishListener.onResult(note);
                }

                @Override
                public void onDecryptFailure() {
                    if (errorListener != null)
                        errorListener.onError("Unknown error during decryption encountered");
                }
            }, error -> {
                if (errorListener != null)
                    errorListener.onError(error);
            });
        }
    }


    /**
     * Object to handle decryption for multiple Notes at once
     */
    public static class BatchDecrypter {
        private @NonNull FragmentManager manager;
        private @NonNull Context context;

        private @Nullable ConvertCallback individualCallback;
        private @Nullable ResultListener<Stream<Note>> finishListener;
        private @Nullable ErrorListener errorListener;

        public static BatchDecrypter from(@NonNull FragmentManager manager, @NonNull Context context) {
            return new BatchDecrypter(manager, context);
        }

        protected BatchDecrypter(@NonNull FragmentManager manager, @NonNull Context context) {
            this.manager = manager;
            this.context = context;
        }

        public BatchDecrypter setProgressCallback(@NonNull ConvertCallback callback) {
            this.individualCallback = callback;
            return this;
        }

        public BatchDecrypter setOnFinishListener(@NonNull ResultListener<Stream<Note>> listener) {
            this.finishListener = listener;
            return this;
        }

        public BatchDecrypter setOnErrorListener(@NonNull ErrorListener listener) {
            this.errorListener = listener;
            return this;
        }

        public void decrypt(@NonNull List<Note> notes) {
            if (notes.size() == 0) {
                finishListener.onResult(Stream.empty());
                return;
            }
            Stream<Note> noteStream = notes.stream();
            Guard.getGuard().decrypt(noteStream, manager, context, new Guard.DecryptedCallback() {
                @Override
                public void onDecrypted(@NonNull Note note) {
                    if (individualCallback != null)
                        individualCallback.onSuccess(note);
                }
                @Override
                public void onDecryptFailure() {
                    if (individualCallback != null)
                        individualCallback.onFailure();
                }
            }, resultStream -> {
                if (finishListener != null) {
                    finishListener.onResult(resultStream);
                }
            }, error -> {
                if (errorListener != null)
                    errorListener.onError(error);
            });
        }
    }


    /**
     * Object to handle encryption for multiple Notes at once
     */
    public static class BatchEncrypter {
        private @NonNull FragmentManager manager;
        private @NonNull Context context;

        private @Nullable ConvertCallback individualCallback;
        private @Nullable ResultListener<Stream<Note>> finishListener;
        private @Nullable ErrorListener errorListener;

        public static BatchEncrypter from(@NonNull FragmentManager manager, @NonNull Context context) {
            return new BatchEncrypter(manager, context);
        }

        protected BatchEncrypter(@NonNull FragmentManager manager, @NonNull Context context) {
            this.manager = manager;
            this.context = context;
        }

        public BatchEncrypter setProgressCallback(@NonNull ConvertCallback callback) {
            this.individualCallback = callback;
            return this;
        }

        public BatchEncrypter setOnFinishListener(@NonNull ResultListener<Stream<Note>> listener) {
            this.finishListener = listener;
            return this;
        }

        public BatchEncrypter setOnErrorListener(@NonNull ErrorListener listener) {
            this.errorListener = listener;
            return this;
        }

        public void encrypt(@NonNull List<Note> notes) {
            if (notes.size() == 0) {
                finishListener.onResult(Stream.empty());
                return;
            }
            Stream<Note> noteStream = notes.stream();
            Guard.getGuard().encrypt(noteStream, manager, context, new Guard.EncryptedCallback() {
                @Override
                public void onEncrypted(@NonNull Note note) {
                    if (individualCallback != null)
                        individualCallback.onSuccess(note);
                }
                @Override
                public void onEncryptFailure() {
                    if (individualCallback != null)
                        individualCallback.onFailure();
                }
            }, resultStream -> {
                if (finishListener != null) {
                    finishListener.onResult(resultStream);
                }
            }, error -> {
                if (errorListener != null)
                    errorListener.onError(error);
            });
        }
    }


    /** Callback used to receive conversion result */
    public interface ConvertCallback {
        void onSuccess(@NonNull Note note);
        void onFailure();
    }
}
