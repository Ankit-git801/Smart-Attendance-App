package com.ankit.smartattendance.data

/**
 * Data class to hold the results of the bunk analysis calculation.
 *
 * @property classesToBunk The number of classes the user can safely miss.
 * @property classesToAttend The number of classes the user must attend to meet the target.
 */
data class BunkAnalysis(
    val classesToBunk: Int,
    val classesToAttend: Int
)
