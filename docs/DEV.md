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
    - Check [here](https://developer.android.com/jetpack/androidx/releases/compose-kotlin) for *Compose Compiler* compatibility with Kotlin versions.
 + [navigation](https://developer.android.com/jetpack/compose/navigation)
 + [navigation returns](https://code.luasoftware.com/tutorials/android/jetpack-compose-navigation-return-result/)
 + [dialogs](https://stackoverflow.com/questions/68852110/)
 + [dialog resize bug](https://stackoverflow.com/questions/68469681/)
 + [splash screen](https://www.geeksforgeeks.org/animated-splash-screen-in-android-using-jetpack-compose/)
 + [simple color picker](https://stackoverflow.com/a/69116990)
 + [List searching](https://johncodeos.com/how-to-add-search-in-list-with-jetpack-compose/)

Markdown renderer
 + [markdown rendering](https://github.com/noties/Markwon)
 + [markdown in compose](https://github.com/jeziellago/compose-markdown)
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
 2. Splash screen returns after screen lock -> screen unlock (perhaps a `rememberSaveable` solution)
 3. Android lint refuses to see an indirectly-inheriting `Application` class as an instantiatable `Application` class.
    Unsolved problem in Android community, see:
    https://issuetracker.google.com/issues/197428346
    https://stackoverflow.com/questions/68899665/error-mainactivity-must-extend-android-app-activity-instantiatable

## Ideas
 1. Streamline backend data communication to frontend using some form of messaging.
 2. Using [Material You](https://proandroiddev.com/exploring-material-you-for-jetpack-compose-c2d9e8eb3b2c)

## Sample Functionality
 - [auto-complete/expanding search field](https://proandroiddev.com/jetpack-compose-auto-complete-search-bar-853023856f0f)