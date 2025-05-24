package com.example.allocate_optimize_track

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class GamificationViewModel : ViewModel() {
    private val repository = GamificationRepository()

    val userGamificationData: LiveData<UserGamificationData?> = repository.getGamificationData()

    fun recordUserActivity() {
        viewModelScope.launch {
            repository.recordUserActivityAndManageStreak()
        }
    }
}