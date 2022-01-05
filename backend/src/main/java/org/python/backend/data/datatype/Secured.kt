package org.python.backend.data.datatype

sealed class Secured<T>(member: T, secure: Boolean)

sealed class SecuredSymmetric<T>(member: T, secure: Boolean, iv: ByteArray) : Secured<T>(member, secure)