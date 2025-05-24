package com.example.allocate_optimize_track

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class GamificationRepository {
    private val auth = Firebase.auth
    private val database = FirebaseDatabase.getInstance()

    companion object {
        private const val GAMIFICATION_PATH = "user_gamification_data"
    }

    private fun getUserGamificationRef(): DatabaseReference? {
        val userId = auth.currentUser?.uid ?: return null
        return database.getReference(GAMIFICATION_PATH).child(userId)
    }

    // LiveData to observe gamification data
    fun getGamificationData(): LiveData<UserGamificationData?> {
        val liveData = MutableLiveData<UserGamificationData?>()
        getUserGamificationRef()?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                liveData.postValue(snapshot.getValue(UserGamificationData::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                liveData.postValue(null) // Or handle error
            }
        }) ?: liveData.postValue(null)
        return liveData
    }

    // Update data after an action (e.g., adding an expense)
    suspend fun recordUserActivityAndManageStreak() {
        val userRef = getUserGamificationRef() ?: return
        val now = System.currentTimeMillis()
        val todayStart = getStartOfDayMillis(now)

        try {
            val snapshot = userRef.get().await()
            var data = snapshot.getValue(UserGamificationData::class.java)
                ?: UserGamificationData()

            val lastEntryDayStart = getStartOfDayMillis(data.lastEntryTimestamp)

            if (data.lastEntryTimestamp == 0L || lastEntryDayStart < getStartOfDayMillis(todayStart - (24 * 60 * 60 * 1000 - 1000))) { // More than 1 day ago, or first entry
                data.currentStreak = 1 // Reset or start streak
            } else if (lastEntryDayStart == getStartOfDayMillis(todayStart - (24 * 60 * 60 * 1000))) { // Yesterday
                data.currentStreak += 1
            } else if (lastEntryDayStart == todayStart) {
                // Already made an entry today, streak doesn't change
            } else {
                // Missed days but not more than one full day ago, should be covered by first condition
                data.currentStreak = 1
            }

            data.lastEntryTimestamp = now
            userRef.setValue(data).await()
        } catch (e: Exception) {
            // Handle exception
            Log.e("GamificationRepo", "Error updating streak", e)
        }
    }

    private fun getStartOfDayMillis(timestamp: Long): Long {
        if (timestamp == 0L) return 0L
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}