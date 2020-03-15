package com.python.companion.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;

@Entity(primaryKeys = {"namePlural"})
public class Measurement implements TemporalUnit {
    private @NonNull String nameSingular, namePlural;
    private Duration duration;

    public Measurement(@NonNull String nameSingular , @NonNull String namePlural, @NonNull Duration duration) {
        this.nameSingular = nameSingular;
        this.namePlural = namePlural;
        this.duration = duration;
    }

    @NonNull
    public String getNameSingular() {
        return nameSingular;
    }

    public void setNameSingular(@NonNull String nameSingular) {
        this.nameSingular = nameSingular;
    }

    @NonNull
    public String getNamePlural() {
        return namePlural;
    }

    public void setNamePlural(@NonNull String namePlural) {
        this.namePlural = namePlural;
    }

    @Override
    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    @Override
    public boolean isDurationEstimated() {
        return this.compareTo(ChronoUnit.DAYS) >= 0;
    }

    @Override
    public boolean isDateBased() {
        return this.compareTo(ChronoUnit.DAYS) >= 0;
    }

    @Override
    public boolean isTimeBased() {
        return this.compareTo(ChronoUnit.DAYS) < 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends Temporal> R addTo(R temporal, long amount) {
        return (R) temporal.plus(amount, this);
    }

    @Override
    public long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive) {
        return temporal1Inclusive.until(temporal2Exclusive, this);
    }

    public int compareTo(TemporalUnit other) {
        return this.duration.compareTo(other.getDuration());
    }

    @NonNull
    @Override
    public String toString() {
        return namePlural;
    }
}
