package com.python.companion.security;

public interface ExceptionCallback {
    void onException(@Guard.GuardException int exception);
}
