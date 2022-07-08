package org.python.exim

enum class MergeStrategy {
    DELETE_ALL_BEFORE,
    SKIP_ON_CONFLICT,
    OVERRIDE_ON_CONFLICT
}