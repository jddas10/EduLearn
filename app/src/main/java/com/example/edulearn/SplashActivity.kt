package com.example.edulearn

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import com.example.edulearn.databinding.ActivitySplashBinding
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var bgBreathAnimator: ValueAnimator? = null
    private var haloPulseAnimator: ValueAnimator? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = getColor(R.color.splash_bg_1)
        window.navigationBarColor = getColor(R.color.splash_bg_1)

        initStates()
        play()
    }

    private fun initStates() {
        binding.bg2.alpha = 0f

        binding.halo.alpha = 0f
        binding.halo.scaleX = 0.85f
        binding.halo.scaleY = 0.85f

        binding.logoImage.alpha = 0f
        binding.logoImage.scaleX = 0.92f
        binding.logoImage.scaleY = 0.92f
        binding.logoImage.translationY = 14f

        binding.appName.alpha = 0f
        binding.appName.translationY = 22f

        binding.tagline.alpha = 0f
        binding.tagline.translationY = 18f

        binding.developerText.alpha = 0f
        binding.developerText.translationY = 18f

        listOf(binding.p1, binding.p2, binding.p3, binding.p4, binding.p5, binding.p6).forEach {
            it.alpha = 0f
            it.scaleX = 0.6f
            it.scaleY = 0.6f
            it.translationX = 0f
            it.translationY = 0f
        }
    }

    private fun play() {
        startBackgroundBreath()
        mainHandler.postDelayed({ revealHaloAndLogo() }, 180)
    }

    private fun startBackgroundBreath() {
        bgBreathAnimator?.cancel()
        bgBreathAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 5200
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val t = anim.animatedValue as Float
                binding.bg2.alpha = 0.65f * t
            }
            start()
        }
    }

    private fun revealHaloAndLogo() {
        val haloIn = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(binding.halo, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(binding.halo, View.SCALE_X, 0.85f, 1.05f, 1f),
                ObjectAnimator.ofFloat(binding.halo, View.SCALE_Y, 0.85f, 1.05f, 1f)
            )
            duration = 900
            interpolator = OvershootInterpolator(0.9f)
        }

        val logoIn = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(binding.logoImage, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(binding.logoImage, View.SCALE_X, 0.92f, 1.02f, 1f),
                ObjectAnimator.ofFloat(binding.logoImage, View.SCALE_Y, 0.92f, 1.02f, 1f),
                ObjectAnimator.ofFloat(binding.logoImage, View.TRANSLATION_Y, 14f, 0f)
            )
            duration = 900
            interpolator = OvershootInterpolator(0.85f)
        }

        AnimatorSet().apply {
            playTogether(haloIn, logoIn)
            start()
            doOnEnd {
                startHaloPulse()
                startLogoFloat()
                revealText()
                burstParticles()
            }
        }
    }

    private fun startHaloPulse() {
        haloPulseAnimator?.cancel()
        haloPulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2400
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = DecelerateInterpolator()
            addUpdateListener { a ->
                val t = a.animatedValue as Float
                binding.halo.alpha = 0.65f + (0.18f * t)
                val s = 1f + (0.02f * t)
                binding.halo.scaleX = s
                binding.halo.scaleY = s
            }
            start()
        }
    }

    private fun startLogoFloat() {
        ObjectAnimator.ofFloat(binding.logoImage, View.TRANSLATION_Y, 0f, -6f, 0f).apply {
            duration = 2200
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = DecelerateInterpolator()
            startDelay = 150
            start()
        }
    }

    private fun revealText() {
        val nameAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(binding.appName, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(binding.appName, View.TRANSLATION_Y, 22f, 0f)
            )
            duration = 650
            startDelay = 180
            interpolator = OvershootInterpolator(0.9f)
        }

        val tagAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(binding.tagline, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(binding.tagline, View.TRANSLATION_Y, 18f, 0f)
            )
            duration = 650
            startDelay = 300
            interpolator = DecelerateInterpolator()
        }

        val devAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(binding.developerText, View.ALPHA, 0f, 0.9f),
                ObjectAnimator.ofFloat(binding.developerText, View.TRANSLATION_Y, 18f, 0f)
            )
            duration = 650
            startDelay = 450
            interpolator = DecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(nameAnim, tagAnim, devAnim)
            start()
            doOnEnd {
                mainHandler.postDelayed({ navigate() }, 1200)
            }
        }
    }

    private fun burstParticles() {
        val particles = listOf(binding.p1, binding.p2, binding.p3, binding.p4, binding.p5, binding.p6)
        particles.forEachIndexed { i, v ->
            mainHandler.postDelayed({ animateParticle(v, i) }, (i * 80L))
        }
    }

    private fun animateParticle(v: View, i: Int) {
        val baseAngle = (i * (360.0 / 6.0)) * (PI / 180.0)
        val jitter = (Random.nextDouble(-12.0, 12.0)) * (PI / 180.0)
        val angle = baseAngle + jitter

        val r1 = Random.nextInt(80, 120).toFloat()
        val r2 = r1 + Random.nextInt(30, 55)

        val x1 = (cos(angle) * r1).toFloat()
        val y1 = (sin(angle) * r1).toFloat()
        val x2 = (cos(angle) * r2).toFloat()
        val y2 = (sin(angle) * r2).toFloat()

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(v, View.ALPHA, 0f, 0.85f, 0f),
                ObjectAnimator.ofFloat(v, View.TRANSLATION_X, 0f, x1, x2),
                ObjectAnimator.ofFloat(v, View.TRANSLATION_Y, 0f, y1, y2),
                ObjectAnimator.ofFloat(v, View.SCALE_X, 0.6f, 1.15f, 0.8f),
                ObjectAnimator.ofFloat(v, View.SCALE_Y, 0.6f, 1.15f, 0.8f)
            )
            duration = 1400
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun navigate() {
        ObjectAnimator.ofFloat(binding.root, View.ALPHA, 1f, 0f).apply {
            duration = 320
            interpolator = DecelerateInterpolator()
            start()
            doOnEnd {
                startActivity(Intent(this@SplashActivity, RoleSelectionActivity::class.java))
                finish()
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }
    }

    override fun onDestroy() {
        bgBreathAnimator?.cancel()
        haloPulseAnimator?.cancel()
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Disable back on splash
    }
}
