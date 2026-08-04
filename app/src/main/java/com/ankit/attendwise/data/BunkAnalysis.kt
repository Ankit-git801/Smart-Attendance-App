package com.ankit.attendwise.data

import androidx.annotation.Keep

/**
 * Data class to hold the results of the bunk analysis calculation.
 *
 * @property classesToBunk The number of classes the user can safely miss.
 * @property classesToAttend The number of classes the user must attend to meet the target.
 */
@Keep
data class BunkAnalysis(
    val classesToBunk: Int,
    val classesToAttend: Int
)
