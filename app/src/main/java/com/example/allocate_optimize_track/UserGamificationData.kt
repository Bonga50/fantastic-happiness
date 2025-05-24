package com.example.allocate_optimize_track

data class UserGamificationData(
    var lastEntryTimestamp: Long = 0L,
    var currentStreak: Int = 0
) {
    constructor() : this(0L, 0) // For Firebase
}