package org.python.companion.ui.support

import android.os.Parcelable
import java.util.*

object Intent {
    fun ensure(intent: android.content.Intent, keys: Iterable<String>): Boolean = keys.all { intent.hasExtra(it) || intent.getParcelableExtra<Parcelable>(it) != null }
    fun ensure(intent: android.content.Intent, key: String): Boolean = ensure(intent, Collections.singletonList(key))
}