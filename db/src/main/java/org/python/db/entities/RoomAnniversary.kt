package org.python.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Duration
import java.time.Instant

@Entity
data class RoomAnniversary(
    @PrimaryKey val name: String,
    val duration: Duration,
    val lastReported: Instant
)