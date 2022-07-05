package org.python.companion.support

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore

object PathUtil {
    public fun getPath(contentResolver: ContentResolver, uri: Uri): String {
        val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null);

        val path: String? = if (cursor == null) {
            uri.path
        } else {
            cursor.moveToFirst();
            val columnIndex: Int = cursor.getColumnIndexOrThrow(projection[0]);
            cursor.getString(columnIndex);
//            cursor.close(); TODO: Close cursor or not?
        }

        return if (path == null || path.isEmpty()) uri.path!! else path
    }
}