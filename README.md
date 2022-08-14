# Companion

An Android application to create notes.

Currently supports:
 + Reading and writing notes:
   + Plain text
   + Markdown
   + Latex
 + Secure notes (`AES/GCM/NoPadding`, hardware-backed encryption and key storage)
   + Secure notes hidden until authenticated
   + Password authentication (Argon hashed)
   + Biometrics authentication
 + Secure export-import functionality

## Building
To compile source code:
```bash
./gradlew assemble
```

To test source code:
```bash
./gradlew test
```

To create deployable (debug) application:
```bash
./gradlew assembleDebug 
./gradlew assembleRelease
```
This produces `Companion-<VERSION>-debug.apk` in 
`companion/app/build/outputs/apk/`, which can be installed on Android smartphones.