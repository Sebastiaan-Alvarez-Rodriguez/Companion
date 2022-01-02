package org.python.backend.data.datatype

import java.time.Duration
import java.time.Instant

data class Anniversary(val name: String, val duration: Duration, val lastReported: Instant)