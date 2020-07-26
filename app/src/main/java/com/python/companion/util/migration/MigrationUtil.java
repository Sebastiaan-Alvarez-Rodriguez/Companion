package com.python.companion.util.migration;

import android.util.Log;

import androidx.annotation.NonNull;

import org.msgpack.core.MessageUnpacker;

import java.io.IOException;
import java.util.Arrays;

public class MigrationUtil {
    public static final byte[] header = {42, 127, 127, 42, 42, 33, 54, 57, 66, 65, 10, 4, 1, 77, 51, 0};

    public static boolean checkHeader(@NonNull MessageUnpacker unpacker) {
        try {
            if (!unpacker.hasNext())
                return false;
            byte[] dst = new byte[16];
            unpacker.unpackBinaryHeader();
            unpacker.readPayload(dst);
            return Arrays.equals(header, dst);
        } catch (IOException e) {
            Log.e("MigrationUtil", "Header reading errr", e);
            return false;
        }
    }
}
