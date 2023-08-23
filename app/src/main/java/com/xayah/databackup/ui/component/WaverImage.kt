package com.xayah.databackup.ui.component

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Shader
import androidx.annotation.FloatRange
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.transform
import com.xayah.databackup.ui.token.WaverImageTokens
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Wave effects for images.
 * @see <a href="https://github.com/vitaviva/ComposeWaveLoading">ComposeWaveLoading</a>
 */

private fun Bitmap.toGray(): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint()
    val colorMatrix = ColorMatrix()
    colorMatrix.setSaturation(0f)
    val colorFilter = ColorMatrixColorFilter(colorMatrix)
    paint.colorFilter = colorFilter
    canvas.drawBitmap(this, 0f, 0f, paint)
    return bitmap
}

private data class WaveConfig(
    val duration: Int,
    val offsetX: Float,
    val offsetY: Float
)

private fun androidx.compose.ui.graphics.Canvas.drawWave(
    width: Float,
    height: Float,
    dpUnit: Float,
    @FloatRange(from = 0.0, to = 1.0) progress: Float,
    @FloatRange(from = 0.0, to = 1.0) amplitudeRatio: Float,
    paint: Paint
) {
    var waveCrest = (height * amplitudeRatio).roundToInt()
    val path = Path()
    val maxWave = height * 0f.coerceAtLeast(1 - progress)
    if (waveCrest > maxWave) {
        waveCrest = maxWave.toInt()
    }
    path.reset()
    path.moveTo(0f, height)
    path.lineTo(0f, height * (1 - progress))
    if (waveCrest > 0) {
        var x = dpUnit
        while (x < width) {
            path.lineTo(x, height * (1 - progress) - (waveCrest / 2f * sin(4 * Math.PI * x / width)).toFloat())
            x += dpUnit
        }
    }
    path.lineTo(width, height * (1 - progress))
    path.lineTo(width, height)
    path.close()
    drawPath(path, paint)
}


@Composable
private fun InfiniteTransition.waveAnimation(duration: Int) = animateFloat(
    initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(
        animation = tween(duration, easing = CubicBezierEasing(0.4f, 0.2f, 0.6f, 0.8f)),
        repeatMode = RepeatMode.Restart
    ), label = WaverImageTokens.WaveAnimationLabel
)

@Composable
fun WaverImage(
    width: Dp,
    height: Dp,
    duration: Int = 2000,
    @FloatRange(from = 0.0, to = 1.0) progress: Float,
    @FloatRange(from = 0.0, to = 1.0) amplitudeRatio: Float = 0.1f,
    sourceBitmap: Bitmap?
) {
    with(LocalDensity.current) {
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
        ) {
            if (sourceBitmap != null) {
                val matrix = Matrix().apply {
                    postScale(width.toPx() / sourceBitmap.width, height.toPx() / sourceBitmap.height)
                }
                val bitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, matrix, true)
                val transition = rememberInfiniteTransition(label = WaverImageTokens.WaveTransitionLabel)
                val foregroundPaint = remember(bitmap) {
                    Paint().apply {
                        alpha = 0.5f
                        shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                    }
                }
                val backgroundPaint =
                    remember(bitmap) { Paint().apply { shader = BitmapShader(bitmap.toGray(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) } }
                val waveConfigs = remember {
                    listOf(
                        WaveConfig(duration, 0f, 0f),
                        WaveConfig((duration * 0.75).toInt(), 0f, 0f),
                        WaveConfig((duration * 0.5).toInt(), 0f, 0f)
                    )
                }
                val waveAnimations = waveConfigs.map { transition.waveAnimation(duration = it.duration) }
                val dpUnit = 1.dp.toPx()
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawIntoCanvas { canvas ->
                        // Draw gray background.
                        canvas.drawRect(left = 0f, top = 0f, right = size.width, bottom = size.height, paint = backgroundPaint)

                        // Draw waves.
                        waveConfigs.forEachIndexed { index, waveConfig ->
                            canvas.withSave {
                                val maxWidth = 2 * size.width
                                val maxHeight = size.height
                                val offsetX = maxWidth / 2 * (1 - waveAnimations[index].value) - waveConfig.offsetX
                                val offsetY = waveConfig.offsetY
                                canvas.translate(
                                    -offsetX,
                                    -offsetY
                                )
                                foregroundPaint.shader?.transform {
                                    setTranslate(offsetX, 0f)
                                }
                                canvas.drawWave(
                                    width = maxWidth,
                                    height = maxHeight,
                                    dpUnit = dpUnit,
                                    progress = progress,
                                    amplitudeRatio = amplitudeRatio,
                                    paint = foregroundPaint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WaverImage(
    size: Dp,
    duration: Int = 2000,
    @FloatRange(from = 0.0, to = 1.0) progress: Float,
    @FloatRange(from = 0.0, to = 1.0) amplitudeRatio: Float = 0.1f,
    sourceBitmap: Bitmap?
) {
    WaverImage(width = size, height = size, duration = duration, progress = progress, amplitudeRatio = amplitudeRatio, sourceBitmap = sourceBitmap)
}