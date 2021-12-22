package org.python.companion.ui.splash

import android.view.animation.OvershootInterpolator
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import org.python.companion.R
import kotlin.random.Random


const val DEFAULT_DELAY_MS = 1500L

private val cacti: Array<Int> = arrayOf(
        R.drawable.ic_cactus_0,
        R.drawable.ic_cactus_1,
        R.drawable.ic_cactus_2,
)

private fun randomCactus() = cacti[Random.nextInt(cacti.size)]

/**
 * Splash startup screen. Uses a random eligible cactus to draw on the screen while loading.
 * @param navController Controller to push the stack forward after loading has completed.
 * @param destination Destination to navigate to after loading.
 * @param executeOnLoad Function to handle loading data. Loading is considered complete after this function returns.
 * For cleanliness, the splash screen always appears a minimum of 1.5 seconds, even when it takes less than that to load.
 */
@Composable
fun SplashActor(navController: NavController, destination: String, executeOnLoad: () -> Unit) {
    val todaySpecial = false
    if (todaySpecial) {
        // TODO: something
    } else {
        SplashActor(navController, destination, executeOnLoad, drawableRes = remember { randomCactus() }, DEFAULT_DELAY_MS)
    }
}
/**
 * Splash startup screen.
 * @param drawableRes Cactus resource ID to display.
 * @see SplashActor(NavController , String, () -> Unit)
 */
@Composable
fun SplashActor(
    navController: NavController,
    destination: String,
    executeOnLoad: () -> Unit,
    @DrawableRes drawableRes: Int,
    minDelayMs: Long
) {
    val scale = remember { androidx.compose.animation.core.Animatable(0f) }

    // AnimationEffect
    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 0.7f,
            animationSpec = tween(durationMillis = 800, easing = { OvershootInterpolator(4f).getInterpolation(it) })
        )
        val startTime = System.currentTimeMillis()
        executeOnLoad()
        val deltaTime = System.currentTimeMillis() - startTime

        val timeToHandle = minDelayMs - deltaTime
        if (timeToHandle > 0)
            delay(timeToHandle)
        navController.navigate(destination)
    }

    // Image
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(id = drawableRes),
            contentDescription = "Logo",
            modifier = Modifier.scale(scale.value))
    }
}