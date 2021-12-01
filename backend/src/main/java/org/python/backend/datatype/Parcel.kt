package org.python.backend.datatype

import android.os.Parcelable

sealed class Parcel<T>(member: T) : Parcelable {
    override fun describeContents(): Int = 0
}
