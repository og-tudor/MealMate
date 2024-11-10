package com.example.mealmate.utils

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.View
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AnimationHandler(private val animationView: LottieAnimationView) {

    fun showAnimation(color: Int? = null) {
        // Apply color filter if a color is provided
        color?.let { colorInt ->
            animationView.addValueCallback(
                KeyPath("**"),
                LottieProperty.COLOR_FILTER
            ) { LottieFrameInfo -> PorterDuffColorFilter(colorInt, PorterDuff.Mode.SRC_ATOP) }
        }

        animationView.visibility = View.VISIBLE
        animationView.playAnimation()
    }

    fun hideAnimation() {
        // Ensure the animation cancellation runs on the main thread
        CoroutineScope(Dispatchers.Main).launch {
            animationView.cancelAnimation()
            animationView.visibility = View.GONE
        }
    }
}
