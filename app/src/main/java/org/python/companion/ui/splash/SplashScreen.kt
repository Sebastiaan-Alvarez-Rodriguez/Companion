package org.python.companion.ui.splash

import android.view.animation.OvershootInterpolator
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import org.python.companion.R
import org.python.companion.support.UiUtil
import kotlin.random.Random


const val DEFAULT_DELAY_MS = 300

private val cacti: Array<Int> = arrayOf(
        R.drawable.ic_cactus_0,
        R.drawable.ic_cactus_1,
        R.drawable.ic_cactus_2,
)

private fun randomCactus() = cacti[Random.nextInt(cacti.size)]


/**
 * Simple builder for splash screens.
 * @param navController Navigation controller to use when moving to the next screen.
 * @param destination Next screen destination.
 * @param drawableRes Resource ID to draw and animate while loading.
 * @param minDelayMs Minimum amount of milliseconds to display animation.
 */
@Suppress("unused")
class SplashBuilder(
    var navController: NavController? = null,
    var destination: String? = null,
    @DrawableRes var drawableRes: Int? = null,
    var minDelayMs: Int = DEFAULT_DELAY_MS
) {
    fun withDrawableRes(@DrawableRes resID: Int): SplashBuilder {
        drawableRes = resID
        return this
    }

    fun withMinDelay(delay: Int): SplashBuilder {
        minDelayMs = if (delay <= 0) DEFAULT_DELAY_MS else delay
        return this
    }

    fun withDestination(destination: String): SplashBuilder {
        this.destination = destination
        return this
    }

    fun build(executeOnLoad: () -> Unit): @Composable () -> Unit {
        when {
            navController == null -> throw IllegalStateException("NavController must be initialized.")
            destination == null -> throw IllegalStateException("Destination must be initialized.")
            else -> return { SplashActor(
                navController = navController!!,
                destination = destination!!,
                executeOnLoad = executeOnLoad,
                drawableRes = drawableRes?: randomCactus(),
                minDelayMs = minDelayMs)
            }
        }
    }
}


/**
 * Splash startup screen.
 * @param navController Controller to push the stack forward after loading has completed.
 * @param destination Destination to navigate to after loading.
 * @param executeOnLoad Function to handle loading data. Loading is considered complete after this function returns.
 * @param drawableRes Cactus resource ID to display.
 * @param minDelayMs Minimum amount of time to display the animation in milliseconds.
 */
@Composable
private fun SplashActor(
    navController: NavController,
    destination: String,
    executeOnLoad: () -> Unit,
    @DrawableRes drawableRes: Int,
    minDelayMs: Int,
) {
    val scale = remember { androidx.compose.animation.core.Animatable(0f) }

    // AnimationEffect
    LaunchedEffect(true) {
        scale.animateTo(
            targetValue = 0.7f,
            animationSpec = tween(durationMillis = DEFAULT_DELAY_MS, easing = { OvershootInterpolator(4f).getInterpolation(it) })
        )
        val startTime = System.currentTimeMillis()
        executeOnLoad()
        val deltaTime = System.currentTimeMillis() - startTime

        val timeToHandle = minDelayMs - deltaTime
        if (timeToHandle > 0)
            delay(timeToHandle)

        UiUtil.navigatePop(navController, destination)
    }

    // Image
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(id = drawableRes),
            contentDescription = "Logo",
            modifier = Modifier
                .scale(scale.value)
                .defaultMinSize(minWidth = dimensionResource(id = R.dimen.splash_image_min_width), minHeight = dimensionResource(id = R.dimen.splash_image_min_height))
                .fillMaxSize(0.9f))
    }
}