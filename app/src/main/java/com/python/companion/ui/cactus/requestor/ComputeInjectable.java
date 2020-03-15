package com.python.companion.ui.cactus.requestor;

import androidx.annotation.NonNull;

import java.time.LocalDate;

public interface ComputeInjectable {
    void onShiftDate(@NonNull LocalDate newDate);
}
