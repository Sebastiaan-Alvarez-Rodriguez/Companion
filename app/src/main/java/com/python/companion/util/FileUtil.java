package com.python.companion.util;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static com.python.companion.db.Database.DB_NAME;

public class FileUtil {

    public static boolean exportDatabase(@NonNull Context context, @NonNull Uri location) {
        ContentResolver contentResolver = context.getContentResolver();
        try (OutputStream outStream = contentResolver.openOutputStream(location)){
            assert outStream != null;
            Files.copy(context.getDatabasePath(DB_NAME).toPath(), outStream);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean importDatabase(@NonNull Context context, @NonNull Uri location) {
        ContentResolver contentResolver = context.getContentResolver();
        try (InputStream outStream = contentResolver.openInputStream(location)){
            assert outStream != null;
            Files.copy(outStream, context.getDatabasePath(DB_NAME).toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
