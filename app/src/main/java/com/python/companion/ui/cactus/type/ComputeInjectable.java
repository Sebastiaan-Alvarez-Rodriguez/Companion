package com.python.companion.ui.cactus.type;

import androidx.annotation.NonNull;

import java.time.LocalDate;

public interface ComputeInjectable {
    void onShiftDate(@NonNull LocalDate newDate);
}
