/*
 * Copyright 2020 The Android Open Source Project
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

package com.xayah.databackup.ui.util.material3

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Iterates through a [List] using the index and calls [action] for each item.
 * This does not allocate an iterator like [Iterable.forEach].
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
inline fun <T> List<T>.fastForEach(action: (T) -> Unit) {
    contract { callsInPlace(action) }
    for (index in indices) {
        val item = get(index)
        action(item)
    }
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right
 * to current accumulator value and each element.
 *
 * Returns the specified [initial] value if the collection is empty.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn") // Treat Kotlin Contracts as non-experimental.
@OptIn(ExperimentalContracts::class)
inline fun <T, R> List<T>.fastFold(initial: R, operation: (acc: R, T) -> R): R {
    contract { callsInPlace(operation) }
    var accumulator = initial
    fastForEach { e ->
        accumulator = operation(accumulator, e)
    }
    return accumulator
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original collection.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
inline fun <T, R> List<T>.fastMap(transform: (T) -> R): List<R> {
    contract { callsInPlace(transform) }
    val target = ArrayList<R>(size)
    fastForEach {
        target += transform(it)
    }
    return target
}

// TODO: should be fastMaxByOrNull to match stdlib
/**
 * Returns the first element yielding the largest value of the given function or `null` if there
 * are no elements.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
inline fun <T, R : Comparable<R>> List<T>.fastMaxBy(selector: (T) -> R): T? {
    contract { callsInPlace(selector) }
    if (isEmpty()) return null
    var maxElem = get(0)
    var maxValue = selector(maxElem)
    for (i in 1..lastIndex) {
        val e = get(i)
        val v = selector(e)
        if (maxValue < v) {
            maxElem = e
            maxValue = v
        }
    }
    return maxElem
}
