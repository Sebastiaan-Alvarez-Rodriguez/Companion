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

Room:
 + [repository tutorial](https://www.raywenderlich.com/24509368-repository-pattern-with-jetpack-compose#toc-anchor-012)
 + [room livedata](https://levelup.gitconnected.com/using-room-in-jetpack-compose-d2b6b674d3a5)
 + [room docs](https://developer.android.com/jetpack/androidx/releases/room)
 + [**legacy** room tips](https://medium.com/androiddevelopers/7-pro-tips-for-room-fbadea4bfbd1)

Kotlin:
 + [structured async](https://kotlinlang.org/docs/composing-suspending-functions.html#structured-concurrency-with-async)