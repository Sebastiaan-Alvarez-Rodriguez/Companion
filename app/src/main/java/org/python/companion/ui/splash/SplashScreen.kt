package org.python.companion.ui.splash

import android.view.animation.OvershootInterpolator
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role.Companion.Image
import androidx.compose.ui.unit.dp
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
 * Splash startup screen.
 * @param navController Controller to push the stack forward after loading has completed.
 * @param executeOnLoad Function to handle loading data. Loading is considered complete after this function returns.
 * For cleanliness, the splash screen always appears a minimum of 1.5 seconds, even when it takes less than that to load.
 * @param drawableRes Cactus resource ID to display.
 */
@Composable
fun SplashActor(navController: NavController, executeOnLoad: () -> Unit) {
    val todaySpecial = false
    if (todaySpecial) {
        // TODO: something
    } else {
        SplashActor(navController, executeOnLoad, drawableRes = randomCactus(), DEFAULT_DELAY_MS)
    }
}
/**
 * Splash startup screen.
 * @param drawableRes Cactus resource ID to display.
 */
@Composable
fun SplashActor(
    navController: NavController,
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
        navController.navigate("main_screen")
    }

    // Image
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(id = drawableRes),
            contentDescription = "Logo",
            modifier = Modifier.scale(scale.value))
    }
}