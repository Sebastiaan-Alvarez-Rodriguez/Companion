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
 + [dialogs](https://stackoverflow.com/questions/68852110/)
 + [splash screen](https://www.geeksforgeeks.org/animated-splash-screen-in-android-using-jetpack-compose/)
 + [simple color picker](https://stackoverflow.com/a/69116990)

Room:
 + [repository tutorial](https://www.raywenderlich.com/24509368-repository-pattern-with-jetpack-compose#toc-anchor-012)
 + [room livedata](https://levelup.gitconnected.com/using-room-in-jetpack-compose-d2b6b674d3a5)
 + [room relations](https://developer.android.com/training/data-storage/room/relationships)
 + [room docs](https://developer.android.com/jetpack/androidx/releases/room)
 + [**legacy** room tips](https://medium.com/androiddevelopers/7-pro-tips-for-room-fbadea4bfbd1)

Kotlin:
 + [structured async](https://kotlinlang.org/docs/composing-suspending-functions.html#structured-concurrency-with-async)

Security:
 + [biometrics](https://www.raywenderlich.com/18782293-android-biometric-api-getting-started)

## Issues
Current issue list:
 1. Creating notes longer than the keyboard make the cursor go under the keyboard.
    Unsolved problem in Android community, see:
    https://issuetracker.google.com/issues/192043120?pli=1
    https://askandroidquestions.com/2021/08/19/jetpack-compose-textfield-soft-keyboard-obscures-text-entry/