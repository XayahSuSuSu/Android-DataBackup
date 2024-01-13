/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xayah.core.ui.material3.util

import kotlin.math.roundToLong

/**
 * Linearly interpolate between [start] and [stop] with [fraction] fraction between them.
 */
fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (1 - fraction) * start + fraction * stop
}

/**
 * Linearly interpolate between [start] and [stop] with [fraction] fraction between them.
 */
fun lerp(start: Int, stop: Int, fraction: Float): Int {
    return start + ((stop - start) * fraction.toDouble()).fastRoundToInt()
}

/**
 * Linearly interpolate between [start] and [stop] with [fraction] fraction between them.
 */
fun lerp(start: Long, stop: Long, fraction: Float): Long {
    return start + ((stop - start) * fraction.toDouble()).roundToLong()
}

/**
 * Returns the smaller of the given values. If any value is NaN, returns NaN.
 * Preferred over `kotlin.comparisons.minfOf()` for 4 arguments as it avoids
 * allocaing an array because of the varargs.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun fastMinOf(a: Float, b: Float, c: Float, d: Float): Float {
    // ART inlines everything and generates only 3 fmin instructions
    return minOf(a, minOf(b, minOf(c, d)))
}

/**
 * Returns the largest of the given values. If any value is NaN, returns NaN.
 * Preferred over `kotlin.comparisons.maxOf()` for 4 arguments as it avoids
 * allocaing an array because of the varargs.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun fastMaxOf(a: Float, b: Float, c: Float, d: Float): Float {
    // ART inlines everything and generates only 3 fmax instructions
    return maxOf(a, maxOf(b, maxOf(c, d)))
}

/**
 * Returns this float value clamped in the inclusive range defined by
 * [minimumValue] and [maximumValue]. Unlike [Float.coerceIn], the range
 * is not validated: the caller must ensure that [minimumValue] is less than
 * [maximumValue].
 */
@Suppress("NOTHING_TO_INLINE")
inline fun Float.fastCoerceIn(minimumValue: Float, maximumValue: Float) =
    this.fastCoerceAtLeast(minimumValue).fastCoerceAtMost(maximumValue)

/**
 * Ensures that this value is not less than the specified [minimumValue].
 */
@Suppress("NOTHING_TO_INLINE")
inline fun Float.fastCoerceAtLeast(minimumValue: Float): Float {
    return if (this < minimumValue) minimumValue else this
}

/**
 * Ensures that this value is not greater than the specified [maximumValue].
 */
@Suppress("NOTHING_TO_INLINE")
inline fun Float.fastCoerceAtMost(maximumValue: Float): Float {
    return if (this > maximumValue) maximumValue else this
}

/**
 * Returns this double value clamped in the inclusive range defined by
 * [minimumValue] and [maximumValue]. Unlike [Float.coerceIn], the range
 * is not validated: the caller must ensure that [minimumValue] is less than
 * [maximumValue].
 */
@Suppress("NOTHING_TO_INLINE")
inline fun Double.fastCoerceIn(minimumValue: Double, maximumValue: Double) =
    this.fastCoerceAtLeast(minimumValue).fastCoerceAtMost(maximumValue)

/**
 * Ensures that this value is not less than the specified [minimumValue].
 */
@Suppress("NOTHING_TO_INLINE")
inline fun Double.fastCoerceAtLeast(minimumValue: Double): Double {
    return if (this < minimumValue) minimumValue else this
}

/**
 * Ensures that this value is not greater than the specified [maximumValue].
 */
@Suppress("NOTHING_TO_INLINE")
inline fun Double.fastCoerceAtMost(maximumValue: Double): Double {
    return if (this > maximumValue) maximumValue else this
}

/**
 * Fast, approximate cube root function. Returns the cube root
 * of [x]; for any [x] `fastCbrt(-x) == -fastCbrt(x)`.
 *
 * When [x] is:
 * - [Float.NaN], returns [Float.NaN]
 * - [Float.POSITIVE_INFINITY], returns [Float.NaN]
 * - [Float.NEGATIVE_INFINITY], returns [Float.NaN]
 * - Zero, returns a value close to 0 (~8.3e-14) with the same sign
 *
 * The maximum error compared to [kotlin.math.cbrt] is:
 * - 8.3446500E-7 in the range -1f..1f
 * - 6.6757200E-6 in the range -256f..256f
 * - 1.0681152E-4 in the range -65_536f..65_536f
 * - 2.1362305E-4 in the range -16_777_216..16_777_216f
 */
fun fastCbrt(x: Float): Float {
    // Our fast cube root approximation is implemented using the binary
    // representation of a float as a log space (log2 in our case). In
    // log space, we can reason about the cube root function in a
    // different way:
    //
    // log2(cbrt(x)) = log2(x^1/3) = 1/3 * log2(x)
    //
    // Assuming x is a positive normal number, it can be written as:
    //
    // x = 2^e_x * (1 + m_x)
    //
    // Therefore:
    //
    // log2(x) = e_x + log2(1 + m_x)
    //
    // Since the m_x is in the range [0, 1), we can apply the following
    // approximation:
    //
    // log2(1 + m_x) ~= m_x + σ
    //
    // All together, we end up with:
    //
    // log2(x) = e_x + m_x + σ
    //
    // Using the binary/integer representation I_x of a float:
    //
    // I_x = E_x * L + M_x
    //
    // Where:
    // - B is the exponent bias, or B = 127 for single precision floats
    // - E_x is the biased exponent, or E_x = e_x + B
    // - L is the magnitude of the significand, or L = 2^23
    // - M_x is the significand M_x = m_x * L
    //
    // I_x = E_x * L + M_x
    //     = L * (e_x + B) + L * m_x
    //     = L * (e_x + m_x + B)
    //     = L * (e_x + m_x + σ + B - σ)
    //    ~= L * (log2(x) + B - σ)
    //    ~= L * log2(x) + L * (B - σ)
    //
    // We have thus:
    //
    // log2(x) ~= I_x / L - (B - σ)
    //
    // With:
    //
    // y = x^(1/3)
    //
    // We have:
    //
    // log2(y) = 1/3 * log2(x)
    //
    // I_y / L - (B - σ) ~!= 1/3 * (I_x / L - (B - σ))
    //
    // By simplification:
    //
    // I_y ~= 1/3 * L * (B - σ) + 1/3 * I_x
    //
    // We now need to find a good value for 1/3 * L * (B - σ),
    // which is equivalent to finding a good σ since L and B are fixed.
    //
    // Reusing the previous simplification, the approximation for the
    // cube root of 1 would be:
    //
    // I(1^1/3) ~= 1/3 * L * (B - σ) + 1/3 * I(1)
    //
    // 1/3 * L * (B - σ) ~= I(1^1/3) - 1/3 * I(1)
    //
    // Since I(1^1/3) == I(1):
    //
    // 1/3 * L * (B - σ) ~= 2/3 * I(1)
    //
    // For single precision floats:
    //
    // I(1) = 0x3f800000
    //
    // 2/3 * I(1) = 0x2a555555
    //
    // All together, we get:
    //
    // I_y = 0x2a555555 + I_x / 3
    //
    // Finally by going going back from an integer representation to a single
    // precision float, we obtain our first approximation of the cube root.
    //
    // We further improve that approximation by using two rounds of the Newton-
    // Rhapson method. One round proved not precise enough for our needs, and
    // more rounds don't improve the results significantly given our use cases.
    //
    // Note: the constant 0x2a555555 we computed earlier is only a standalone
    // approximation that doesn't account for the subsequent Newton-Rhapson
    // refinements. The approximation can be improved for Newton-Rhapson by
    // debiasing it. To debias 0x2a555555 we follow the solution used by the
    // now famous "Quake 3 inverse square root". Instead of using 0x5f400000
    // as the "magic constant" (following our derivation above, you can compute
    // the constant for an inverse square root using (1 - -1/2) * I(1)), the
    // Quake 3 solution uses 0x5f3759df. To improve our constant we simply
    // apply the same ratio:
    //
    // magic = 0x2a555555 * 0x5f3759df / 0x5f400000
    //       = 0x2a517d40
    val v = x.toRawBits().toLong() and 0x1ffffffffL
    var estimate = floatFromBits((0x2a517d40L + v / 3).toInt())

    // 2 rounds of the Newton-Rhapson method to improve accuracy
    estimate -= (estimate - x / (estimate * estimate)) * (1.0f / 3.0f)
    estimate -= (estimate - x / (estimate * estimate)) * (1.0f / 3.0f)

    return estimate
}