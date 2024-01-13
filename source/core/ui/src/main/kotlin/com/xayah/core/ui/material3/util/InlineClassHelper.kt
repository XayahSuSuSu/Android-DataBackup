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

@file:Suppress("NOTHING_TO_INLINE")

package com.xayah.core.ui.material3.util

// These two functions are technically identical to Float.fromBits()
// and Double.fromBits(). However, since they are declared as top-
// level functions, they do not incur the cost of a static fetch
// through the Companion class. Using these top-level functions,
// the generated arm64 code after dex2oat is exactly a single `fmov`

/**
 * Returns the [Float] value corresponding to a given bit representation.
 */
inline fun floatFromBits(bits: Int): Float = java.lang.Float.intBitsToFloat(bits)

/**
 * Returns the [Double] value corresponding to a given bit representation.
 */
inline fun doubleFromBits(bits: Long): Double = java.lang.Double.longBitsToDouble(bits)

/**
 * Returns the closest integer to the argument, tying rounding to positive
 * infinity. Some values are treated differently:
 * - NaN becomes 0
 * - -Infinity or any value less than Integer.MIN_VALUE becomes
 *   Integer.MIN_VALUE.toFloat()
 * - +Infinity or any value greater than Integer.MAX_VALUE becomes
 *   Integer.MAX_VALUE.toFloat()
 */
inline fun Float.fastRoundToInt(): Int = Math.round(this)

/**
 * Returns the closest integer to the argument, tying rounding to positive
 * infinity. Some values are treated differently:
 * - NaN becomes 0
 * - -Infinity or any value less than Integer.MIN_VALUE becomes
 *   Integer.MIN_VALUE.toFloat()
 * - +Infinity or any value greater than Integer.MAX_VALUE becomes
 *   Integer.MAX_VALUE.toFloat()
 */
inline fun Double.fastRoundToInt(): Int = Math.round(this).toInt()

/**
 * Packs two Float values into one Long value for use in inline classes.
 */
inline fun packFloats(val1: Float, val2: Float): Long {
    val v1 = val1.toRawBits().toLong()
    val v2 = val2.toRawBits().toLong()
    return (v1 shl 32) or (v2 and 0xFFFFFFFF)
}

/**
 * Unpacks the first Float value in [packFloats] from its returned Long.
 */
inline fun unpackFloat1(value: Long): Float {
    return floatFromBits((value shr 32).toInt())
}

/**
 * Unpacks the first absolute Float value in [packFloats] from its returned Long.
 */
inline fun unpackAbsFloat1(value: Long): Float {
    return floatFromBits(((value shr 32) and 0x7FFFFFFF).toInt())
}

/**
 * Unpacks the second Float value in [packFloats] from its returned Long.
 */
inline fun unpackFloat2(value: Long): Float {
    return floatFromBits((value and 0xFFFFFFFF).toInt())
}

/**
 * Unpacks the second absolute Float value in [packFloats] from its returned Long.
 */
inline fun unpackAbsFloat2(value: Long): Float {
    return floatFromBits((value and 0x7FFFFFFF).toInt())
}

/**
 * Packs two Int values into one Long value for use in inline classes.
 */
inline fun packInts(val1: Int, val2: Int): Long {
    return (val1.toLong() shl 32) or (val2.toLong() and 0xFFFFFFFF)
}

/**
 * Unpacks the first Int value in [packInts] from its returned ULong.
 */
inline fun unpackInt1(value: Long): Int {
    return (value shr 32).toInt()
}

/**
 * Unpacks the second Int value in [packInts] from its returned ULong.
 */
inline fun unpackInt2(value: Long): Int {
    return (value and 0xFFFFFFFF).toInt()
}