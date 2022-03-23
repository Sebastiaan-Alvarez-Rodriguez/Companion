# Development
Development of this app is in Kotlin.
 + The `compose` UI libraries are used to define a functioning, beautiful UI,
   without the usual XML hassle.
 + The `compose-navigation` UI libraries are used to define movements and routes
   between UI compositions.


## References
The following references form the basis of this project.

UI:
 + [compose](https://developer.android.com/jetpack/compose/state)
    - Also check [here](https://developer.android.com/jetpack/compose/state#state-holder-source-of-truth)
      for states and modularizing UI logic.
    - Also check [here](https://developer.android.com/jetpack/compose/layouts/basics)
      for basic layout help.
 + [navigation](https://developer.android.com/jetpack/compose/navigation)
 + [navigation returns](https://code.luasoftware.com/tutorials/android/jetpack-compose-navigation-return-result/)
 + [dialogs](https://stackoverflow.com/questions/68852110/)
 + [dialog resize bug](https://stackoverflow.com/questions/68469681/)
 + [splash screen](https://www.geeksforgeeks.org/animated-splash-screen-in-android-using-jetpack-compose/)
 + [simple color picker](https://stackoverflow.com/a/69116990)

Room:
 + [repository tutorial](https://www.raywenderlich.com/24509368-repository-pattern-with-jetpack-compose#toc-anchor-012)
 + [room livedata](https://levelup.gitconnected.com/using-room-in-jetpack-compose-d2b6b674d3a5)
 + [room relations](https://developer.android.com/training/data-storage/room/relationships)
 + [room docs](https://developer.android.com/jetpack/androidx/releases/room)
 + [room prepopulate](https://proandroiddev.com/pre-populating-your-room-i-b8e44fd965c1)
 + [**legacy** room tips](https://medium.com/androiddevelopers/7-pro-tips-for-room-fbadea4bfbd1)

Kotlin:
 + [structured async](https://kotlinlang.org/docs/composing-suspending-functions.html#structured-concurrency-with-async)
 + [performant coroutines](https://developer.android.com/kotlin/coroutines/coroutines-adv)

Security:
 + [biometrics](https://www.raywenderlich.com/18782293-android-biometric-api-getting-started)

## Issues
Current issue list:
 1. Creating notes longer than the keyboard make the cursor go under the keyboard.
    Unsolved problem in Android community, see:
    https://issuetracker.google.com/issues/192043120?pli=1
    https://askandroidquestions.com/2021/08/19/jetpack-compose-textfield-soft-keyboard-obscures-text-entry/

## Ideas
 1. Prepopulate a default category in the database.
    This reduces need for nullability of types and should simplify logic.
    Can create a database by hand at every schema change...
    Or can create and use something like:
    https://proandroiddev.com/pre-populating-your-room-i-b8e44fd965c1
    (https://github.com/motorro/room-populate)
    Only then, no js, and make it a gradle plugin/java package
 2. Creating a new secure note, then editing it, crashes:
 ```
E/Securer: Problem in getDecCipher
    java.lang.NullPointerException: null cannot be cast to non-null type java.security.KeyStore.SecretKeyEntry
        at org.python.backend.security.Securer.getDecCipher(Securer.kt:112)
        at org.python.backend.security.Securer.decrypt(Securer.kt:60)
        at org.python.backend.security.Securer.decrypt(Securer.kt:65)
        at org.python.backend.data.repositories.NoteRepository.secureToUI(NoteRepository.kt:90)
        at org.python.backend.data.repositories.NoteRepository.getWithCategory(NoteRepository.kt:38)
        at org.python.backend.data.repositories.NoteRepository$getWithCategory$1.invokeSuspend(Unknown Source:16)
        at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
        at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:106)
        at androidx.compose.ui.platform.AndroidUiDispatcher.performTrampolineDispatch(AndroidUiDispatcher.android.kt:81)
        at androidx.compose.ui.platform.AndroidUiDispatcher.access$performTrampolineDispatch(AndroidUiDispatcher.android.kt:41)
        at androidx.compose.ui.platform.AndroidUiDispatcher$dispatchCallback$1.run(AndroidUiDispatcher.android.kt:57)
        at android.os.Handler.handleCallback(Handler.java:883)
        at android.os.Handler.dispatchMessage(Handler.java:100)
        at android.os.Looper.loop(Looper.java:214)
        at android.app.ActivityThread.main(ActivityThread.java:7697)
        at java.lang.reflect.Method.invoke(Native Method)
        at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:516)
        at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:950)
E/AndroidRuntime: FATAL EXCEPTION: main
    Process: org.python.companion, PID: 22024
    java.lang.IllegalStateException: Could not decrypt note
        at org.python.backend.data.repositories.NoteRepository.getWithCategory(NoteRepository.kt:38)
        at org.python.backend.data.repositories.NoteRepository$getWithCategory$1.invokeSuspend(Unknown Source:16)
        at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
        at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:106)
        at androidx.compose.ui.platform.AndroidUiDispatcher.performTrampolineDispatch(AndroidUiDispatcher.android.kt:81)
        at androidx.compose.ui.platform.AndroidUiDispatcher.access$performTrampolineDispatch(AndroidUiDispatcher.android.kt:41)
        at androidx.compose.ui.platform.AndroidUiDispatcher$dispatchCallback$1.run(AndroidUiDispatcher.android.kt:57)
        at android.os.Handler.handleCallback(Handler.java:883)
        at android.os.Handler.dispatchMessage(Handler.java:100)
        at android.os.Looper.loop(Looper.java:214)
        at android.app.ActivityThread.main(ActivityThread.java:7697)
        at java.lang.reflect.Method.invoke(Native Method)
        at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:516)
        at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:950)
 ```